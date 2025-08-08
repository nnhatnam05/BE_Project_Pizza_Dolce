package aptech.be.services;

import aptech.be.models.ClaimToken;
import aptech.be.models.Customer;
import aptech.be.models.CustomerDetail;
import aptech.be.models.OrderEntity;
import aptech.be.repositories.ClaimTokenRepository;
import aptech.be.repositories.CustomerRepository;
import aptech.be.repositories.CustomerDetailRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClaimTokenService {
    
    @Autowired
    private ClaimTokenRepository claimTokenRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CustomerDetailRepository customerDetailRepository;
    
    @Autowired
    private EmailService emailService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Tạo claim token từ danh sách orders
     */
    @Transactional
    public ClaimToken createClaimToken(List<OrderEntity> orders) {
        // Calculate total amount and points
        double totalAmount = orders.stream()
            .mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0)
            .sum();
        
        // Calculate points: $10 = 10 points (round down)
        int pointsToEarn = (int) Math.floor(totalAmount / 10.0) * 10;
        
        // Convert order IDs to JSON array
        List<String> orderIdStrings = orders.stream()
            .map(order -> order.getId().toString())
            .collect(Collectors.toList());
        
        String orderIdsJson;
        try {
            orderIdsJson = objectMapper.writeValueAsString(orderIdStrings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order IDs", e);
        }
        
        // Create token
        ClaimToken token = new ClaimToken();
        token.setToken(UUID.randomUUID().toString());
        token.setOrderIds(orderIdsJson);
        token.setTotalAmount(totalAmount);
        token.setPointsToEarn(pointsToEarn);
        token.setClaimed(false);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusHours(12)); // 12 hours expiry
        
        ClaimToken savedToken = claimTokenRepository.save(token);
        
        System.out.println("[CLAIM TOKEN] Created token: " + savedToken.getToken() + 
                         " for " + orders.size() + " orders, " + pointsToEarn + " points");
        
        return savedToken;
    }
    
    /**
     * Claim points cho khách đã có tài khoản
     */
    @Transactional
    public boolean claimPointsExistingCustomer(String tokenStr, String email) {
        ClaimToken token = validateToken(tokenStr);
        if (token == null) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn");
        }
        
        Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
        
        // Add points to customer
        int pointsAdded = addPointsToCustomer(customer, token.getPointsToEarn());
        
        // Mark as claimed
        markTokenAsClaimed(token, email);
        
        // Send congratulations email
        try {
            int currentTotalPoints = getCurrentTotalPoints(customer);
            emailService.sendPointsEarnedEmail(email, pointsAdded, 
                token.getTotalAmount(), currentTotalPoints);
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send points earned email: " + e.getMessage());
        }
        
        System.out.println("[CLAIM SUCCESS] Customer " + email + " claimed " + pointsAdded + " points");
        return true;
    }
    
    /**
     * Auto claim points cho khách mới đăng ký
     */
    @Transactional
    public boolean autoClaimForNewCustomer(String tokenStr, String email) {
        ClaimToken token = validateToken(tokenStr);
        if (token == null) {
            System.err.println("[AUTO CLAIM] Invalid token: " + tokenStr);
            return false;
        }
        
        Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + email));
        
        // Add points to customer
        int pointsAdded = addPointsToCustomer(customer, token.getPointsToEarn());
        
        // Mark as claimed
        markTokenAsClaimed(token, email);
        
        // Send congratulations email
        try {
            int currentTotalPoints = getCurrentTotalPoints(customer);
            emailService.sendPointsEarnedEmail(email, pointsAdded, 
                token.getTotalAmount(), currentTotalPoints);
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send points earned email: " + e.getMessage());
        }
        
        System.out.println("[AUTO CLAIM SUCCESS] New customer " + email + " auto-claimed " + pointsAdded + " points");
        return true;
    }
    
    /**
     * Get token info
     */
    public ClaimToken getTokenInfo(String tokenStr) {
        return claimTokenRepository.findByToken(tokenStr).orElse(null);
    }
    
    /**
     * Validate token (not expired, not claimed)
     */
    private ClaimToken validateToken(String tokenStr) {
        ClaimToken token = claimTokenRepository.findByToken(tokenStr).orElse(null);
        if (token == null) {
            return null;
        }
        
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null; // Expired
        }
        
        if (token.getClaimed()) {
            throw new RuntimeException("Đơn hàng này đã được nhận điểm trước đó.");
        }
        
        return token;
    }
    
    /**
     * Add points to customer
     */
    private int addPointsToCustomer(Customer customer, int pointsToAdd) {
        try {
            CustomerDetail customerDetail = customer.getCustomerDetail();
            if (customerDetail == null) {
                // Auto-create CustomerDetail if not exists
                System.out.println("[AUTO-CREATE] Creating CustomerDetail for customer: " + customer.getId());
                customerDetail = createDefaultCustomerDetail(customer);
            }
            
            // Get current points
            String currentPointsStr = customerDetail.getPoint();
            int currentPoints = 0;
            if (currentPointsStr != null && !currentPointsStr.isEmpty()) {
                try {
                    currentPoints = Integer.parseInt(currentPointsStr);
                } catch (NumberFormatException e) {
                    currentPoints = 0;
                }
            }
            
            // Add new points
            int newPoints = currentPoints + pointsToAdd;
            customerDetail.setPoint(String.valueOf(newPoints));
            
            // Save customer detail
            customerDetailRepository.save(customerDetail);
            
            System.out.println("[POINTS] Added " + pointsToAdd + " points to customer " + customer.getId() + 
                             ". Total points: " + newPoints);
            
            return pointsToAdd;
        } catch (Exception e) {
            System.err.println("[POINTS ERROR] Failed to add points: " + e.getMessage());
            throw new RuntimeException("Failed to add points to customer");
        }
    }
    
    /**
     * Get current total points for customer
     */
    private int getCurrentTotalPoints(Customer customer) {
        try {
            CustomerDetail customerDetail = customer.getCustomerDetail();
            if (customerDetail == null) {
                // Auto-create CustomerDetail if not exists
                System.out.println("[AUTO-CREATE] Creating CustomerDetail for getCurrentTotalPoints: " + customer.getId());
                customerDetail = createDefaultCustomerDetail(customer);
            }
            
            if (customerDetail.getPoint() == null || customerDetail.getPoint().isEmpty()) {
                return 0;
            }
            return Integer.parseInt(customerDetail.getPoint());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Create default CustomerDetail for new customer
     */
    private CustomerDetail createDefaultCustomerDetail(Customer customer) {
        try {
            CustomerDetail customerDetail = new CustomerDetail();
            customerDetail.setCustomer(customer);
            customerDetail.setPoint("0"); // Start with 0 points
            customerDetail.setPhoneNumber(null); // Will be filled later
            customerDetail.setVoucher(null); // No vouchers initially
            
            // Save the customer detail
            CustomerDetail savedDetail = customerDetailRepository.save(customerDetail);
            
            // Update customer reference
            customer.setCustomerDetail(savedDetail);
            customerRepository.save(customer);
            
            System.out.println("[AUTO-CREATE SUCCESS] Created CustomerDetail for customer: " + customer.getId());
            return savedDetail;
        } catch (Exception e) {
            System.err.println("[AUTO-CREATE ERROR] Failed to create CustomerDetail: " + e.getMessage());
            throw new RuntimeException("Failed to create customer detail");
        }
    }
    
    /**
     * Mark token as claimed
     */
    private void markTokenAsClaimed(ClaimToken token, String email) {
        token.setClaimed(true);
        token.setClaimedByEmail(email);
        token.setClaimedAt(LocalDateTime.now());
        claimTokenRepository.save(token);
    }
    
    /**
     * Clean up expired tokens (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTokens() {
        try {
            List<ClaimToken> expiredTokens = claimTokenRepository.findExpiredTokens(LocalDateTime.now());
            if (!expiredTokens.isEmpty()) {
                claimTokenRepository.deleteAll(expiredTokens);
                System.out.println("[CLEANUP] Deleted " + expiredTokens.size() + " expired claim tokens");
            }
        } catch (Exception e) {
            System.err.println("[CLEANUP ERROR] Failed to cleanup expired tokens: " + e.getMessage());
        }
    }
} 