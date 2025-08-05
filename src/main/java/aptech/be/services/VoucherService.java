package aptech.be.services;

import aptech.be.dto.VoucherDTO;
import aptech.be.dto.CreateVoucherRequest;
import aptech.be.models.*;
import aptech.be.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class VoucherService {
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private CustomerVoucherRepository customerVoucherRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_VOUCHERS_PER_CUSTOMER = 5; // Giới hạn số voucher mỗi khách
    
    // Tạo voucher mới
    public VoucherDTO createVoucher(VoucherDTO voucherDTO, String createdBy) {
        Voucher voucher = new Voucher();
        
        // Generate unique code
        String code;
        do {
            code = generateVoucherCode();
        } while (voucherRepository.existsByCode(code));
        
        voucher.setCode(code);
        voucher.setName(voucherDTO.getName());
        voucher.setDescription(voucherDTO.getDescription());
        voucher.setType(VoucherType.valueOf(voucherDTO.getType()));
        voucher.setValue(voucherDTO.getValue());
        voucher.setMinOrderAmount(voucherDTO.getMinOrderAmount());
        voucher.setMaxDiscountAmount(voucherDTO.getMaxDiscountAmount());
        voucher.setTotalQuantity(voucherDTO.getTotalQuantity());
        voucher.setExpiresAt(voucherDTO.getExpiresAt());
        voucher.setIsActive(true);
        voucher.setIsPublic(voucherDTO.getIsPublic() != null ? voucherDTO.getIsPublic() : false);
        voucher.setCreatedBy(createdBy);
        
        Voucher savedVoucher = voucherRepository.save(voucher);
        return new VoucherDTO(savedVoucher);
    }
    
    // Tạo voucher từ CreateVoucherRequest
    public VoucherDTO createVoucher(CreateVoucherRequest request, String createdBy) {
        Voucher voucher = new Voucher();
        voucher.setCode(generateVoucherCode());
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        voucher.setType(VoucherType.valueOf(request.getType()));
        voucher.setValue(request.getValue());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setTotalQuantity(request.getTotalQuantity());
        voucher.setUsedQuantity(0);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setExpiresAt(request.getExpiresAt());
        voucher.setIsActive(true);
        voucher.setIsPublic(request.getIsPublic());
        voucher.setCreatedBy(createdBy);
        
        Voucher savedVoucher = voucherRepository.save(voucher);
        return new VoucherDTO(savedVoucher);
    }
    
    // Cập nhật voucher
    public VoucherDTO updateVoucher(Long id, VoucherDTO voucherDTO) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        
        voucher.setName(voucherDTO.getName());
        voucher.setDescription(voucherDTO.getDescription());
        voucher.setType(VoucherType.valueOf(voucherDTO.getType()));
        voucher.setValue(voucherDTO.getValue());
        voucher.setMinOrderAmount(voucherDTO.getMinOrderAmount());
        voucher.setMaxDiscountAmount(voucherDTO.getMaxDiscountAmount());
        voucher.setTotalQuantity(voucherDTO.getTotalQuantity());
        voucher.setExpiresAt(voucherDTO.getExpiresAt());
        voucher.setIsActive(voucherDTO.getIsActive());
        voucher.setIsPublic(voucherDTO.getIsPublic());
        
        Voucher savedVoucher = voucherRepository.save(voucher);
        return new VoucherDTO(savedVoucher);
    }
    
    // Cập nhật voucher từ CreateVoucherRequest
    public VoucherDTO updateVoucher(Long id, CreateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        
        voucher.setName(request.getName());
        voucher.setDescription(request.getDescription());
        voucher.setType(VoucherType.valueOf(request.getType()));
        voucher.setValue(request.getValue());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setTotalQuantity(request.getTotalQuantity());
        voucher.setExpiresAt(request.getExpiresAt());
        voucher.setIsPublic(request.getIsPublic());
        
        Voucher savedVoucher = voucherRepository.save(voucher);
        return new VoucherDTO(savedVoucher);
    }

    
    // Xóa voucher
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        
        // Xóa tất cả customer voucher liên quan
        List<CustomerVoucher> customerVouchers = customerVoucherRepository.findAll()
                .stream()
                .filter(cv -> cv.getVoucher().getId().equals(id))
                .collect(Collectors.toList());
        
        customerVoucherRepository.deleteAll(customerVouchers);
        voucherRepository.delete(voucher);
    }
    
    // Lấy tất cả voucher
    public List<VoucherDTO> getAllVouchers() {
        return voucherRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(VoucherDTO::new)
                .collect(Collectors.toList());
    }
    
    // Lấy voucher theo ID
    public VoucherDTO getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        return new VoucherDTO(voucher);
    }
    
    // Lấy voucher public (cho khách hàng)
    public List<VoucherDTO> getPublicVouchers() {
        return voucherRepository.findPublicActiveVouchers(LocalDateTime.now())
                .stream()
                .map(VoucherDTO::new)
                .collect(Collectors.toList());
    }
    
    // Tặng voucher cho khách hàng cụ thể
    public String giveVoucherToCustomer(Long voucherId, Long customerId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Kiểm tra voucher còn available không
        if (!voucher.isAvailable()) {
            return "Voucher is not available";
        }
        
        // Kiểm tra khách hàng đã có voucher này chưa
        if (customerVoucherRepository.existsByCustomerAndVoucherAndIsUsedFalse(customer, voucher)) {
            return "Customer already has this voucher";
        }
        
        // Kiểm tra giới hạn voucher của khách hàng
        Long currentVoucherCount = customerVoucherRepository.countValidVouchersByCustomerId(customerId, LocalDateTime.now());
        if (currentVoucherCount >= MAX_VOUCHERS_PER_CUSTOMER) {
            return "Customer has reached maximum voucher limit";
        }
        
        // Tạo customer voucher
        CustomerVoucher customerVoucher = new CustomerVoucher(customer, voucher);
        customerVoucherRepository.save(customerVoucher);
        
        return "Voucher given successfully";
    }
    
    // Claim voucher public bằng code
    public String claimVoucherByCode(String voucherCode, Long customerId) {
        // Tìm voucher theo code
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        
        // Kiểm tra voucher có phải public không
        if (!voucher.getIsPublic()) {
            throw new RuntimeException("This voucher is not available for public claim");
        }
        
        // Kiểm tra voucher còn hiệu lực không
        if (!voucher.getIsActive()) {
            throw new RuntimeException("This voucher is no longer active");
        }
        
        // Kiểm tra voucher đã hết hạn chưa
        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This voucher has expired");
        }
        
        // Kiểm tra voucher còn số lượng không
        if (voucher.getUsedQuantity() >= voucher.getTotalQuantity()) {
            throw new RuntimeException("This voucher is out of stock");
        }
        
        // Tìm customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Kiểm tra customer đã có voucher này chưa
        Optional<CustomerVoucher> existingVoucher = customerVoucherRepository
                .findByCustomerAndVoucher(customer, voucher);
        
        if (existingVoucher.isPresent()) {
            throw new RuntimeException("You already have this voucher");
        }
        
        // Tạo customer voucher mới
        CustomerVoucher customerVoucher = new CustomerVoucher();
        customerVoucher.setCustomer(customer);
        customerVoucher.setVoucher(voucher);
        customerVoucher.setIsUsed(false);
        customerVoucher.setReceivedAt(LocalDateTime.now());
        
        customerVoucherRepository.save(customerVoucher);
        
        return "Voucher claimed successfully! Check your vouchers to use it.";
    }
    
    // Validate voucher cho order
    public Map<String, Object> validateVoucherForOrder(String voucherCode, Long customerId, Double orderAmount) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Tìm voucher theo code
            Voucher voucher = voucherRepository.findByCode(voucherCode)
                    .orElseThrow(() -> new RuntimeException("Voucher not found"));
            
            // Tìm customer
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            
            // Kiểm tra customer có voucher này không
            Optional<CustomerVoucher> customerVoucher = customerVoucherRepository
                    .findByCustomerAndVoucher(customer, voucher);
            
            if (customerVoucher.isEmpty()) {
                throw new RuntimeException("You don't have this voucher");
            }
            
            if (customerVoucher.get().getIsUsed()) {
                throw new RuntimeException("This voucher has already been used");
            }
            
            // Kiểm tra voucher còn hiệu lực không
            if (!voucher.getIsActive()) {
                throw new RuntimeException("This voucher is no longer active");
            }
            
            // Kiểm tra voucher đã hết hạn chưa
            if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("This voucher has expired");
            }
            
            // Kiểm tra minimum order amount
            if (voucher.getMinOrderAmount() != null && orderAmount < voucher.getMinOrderAmount()) {
                throw new RuntimeException("Order amount must be at least $" + voucher.getMinOrderAmount());
            }
            
            // Tính discount
            Double discount = calculateDiscount(voucher, orderAmount);
            
            result.put("valid", true);
            result.put("voucher", new VoucherDTO(voucher));
            result.put("discount", discount);
            result.put("message", "Voucher is valid");
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", e.getMessage());
            result.put("discount", 0.0);
        }
        
        return result;
    }
    
    // Tính discount amount
    private Double calculateDiscount(Voucher voucher, Double orderAmount) {
        Double discount = 0.0;
        
        switch (voucher.getType()) {
            case PERCENTAGE:
                discount = orderAmount * (voucher.getValue() / 100.0);
                break;
            case FIXED_AMOUNT:
                discount = voucher.getValue();
                break;
            case FREE_SHIPPING:
                // Assume shipping fee is $5 (you can make this configurable)
                discount = 5.0;
                break;
            default:
                discount = 0.0;
                break;
        }
        
        // Apply max discount limit if exists
        if (voucher.getMaxDiscountAmount() != null && discount > voucher.getMaxDiscountAmount()) {
            discount = voucher.getMaxDiscountAmount();
        }
        
        // Discount cannot exceed order amount
        if (discount > orderAmount) {
            discount = orderAmount;
        }
        
        return discount;
    }
    
    // Mark voucher as used
    public void markVoucherAsUsed(String voucherCode, Long customerId) {
        // Tìm voucher theo code
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
        
        // Tìm customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Tìm customer voucher
        Optional<CustomerVoucher> customerVoucherOpt = customerVoucherRepository
                .findByCustomerAndVoucher(customer, voucher);
        
        if (customerVoucherOpt.isPresent()) {
            CustomerVoucher customerVoucher = customerVoucherOpt.get();
            customerVoucher.setIsUsed(true);
            customerVoucher.setUsedAt(LocalDateTime.now());
            customerVoucherRepository.save(customerVoucher);
            
            // Tăng used quantity của voucher
            voucher.setUsedQuantity(voucher.getUsedQuantity() + 1);
            voucherRepository.save(voucher);
        }
    }
    
    // Lấy voucher của khách hàng
    public List<VoucherDTO> getCustomerVouchers(Long customerId) {
        List<CustomerVoucher> customerVouchers = customerVoucherRepository
                .findValidVouchersByCustomerId(customerId, LocalDateTime.now());
        
        return customerVouchers.stream()
                .map(cv -> new VoucherDTO(cv.getVoucher()))
                .collect(Collectors.toList());
    }
    
    // Generate random voucher code
    private String generateVoucherCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(10);
        
        for (int i = 0; i < 10; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        return code.toString();
    }
    
    // Scheduled task để xóa voucher hết hạn (chạy mỗi giờ)
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredVouchers() {
        LocalDateTime now = LocalDateTime.now();
        
        // Xóa customer voucher hết hạn
        List<CustomerVoucher> expiredCustomerVouchers = customerVoucherRepository.findExpiredVouchers(now);
        if (!expiredCustomerVouchers.isEmpty()) {
            customerVoucherRepository.deleteAll(expiredCustomerVouchers);
            System.out.println("[VOUCHER CLEANUP] Deleted " + expiredCustomerVouchers.size() + " expired customer vouchers");
        }
        
        // Deactivate voucher hết hạn
        List<Voucher> expiredVouchers = voucherRepository.findExpiredVouchers(now);
        for (Voucher voucher : expiredVouchers) {
            voucher.setIsActive(false);
        }
        if (!expiredVouchers.isEmpty()) {
            voucherRepository.saveAll(expiredVouchers);
            System.out.println("[VOUCHER CLEANUP] Deactivated " + expiredVouchers.size() + " expired vouchers");
        }
        
        // Deactivate voucher đã hết số lượng
        List<Voucher> fullyUsedVouchers = voucherRepository.findFullyUsedVouchers();
        for (Voucher voucher : fullyUsedVouchers) {
            voucher.setIsActive(false);
        }
        if (!fullyUsedVouchers.isEmpty()) {
            voucherRepository.saveAll(fullyUsedVouchers);
            System.out.println("[VOUCHER CLEANUP] Deactivated " + fullyUsedVouchers.size() + " fully used vouchers");
        }
    }
    
    // Validate và apply voucher cho order
    public double calculateDiscount(String voucherCode, Long customerId, double orderAmount) {
        Optional<CustomerVoucher> customerVoucherOpt = customerVoucherRepository
                .findValidVoucherByCustomerIdAndCode(customerId, voucherCode, LocalDateTime.now());
        
        if (!customerVoucherOpt.isPresent()) {
            throw new RuntimeException("Invalid or expired voucher");
        }
        
        CustomerVoucher customerVoucher = customerVoucherOpt.get();
        Voucher voucher = customerVoucher.getVoucher();
        
        // Kiểm tra minimum order amount
        if (voucher.getMinOrderAmount() != null && orderAmount < voucher.getMinOrderAmount()) {
            throw new RuntimeException("Order amount does not meet minimum requirement");
        }
        
        double discount = 0;
        
        switch (voucher.getType()) {
            case PERCENTAGE:
                discount = orderAmount * (voucher.getValue() / 100);
                if (voucher.getMaxDiscountAmount() != null && discount > voucher.getMaxDiscountAmount()) {
                    discount = voucher.getMaxDiscountAmount();
                }
                break;
            case FIXED_AMOUNT:
                discount = Math.min(voucher.getValue(), orderAmount);
                break;
            case FREE_SHIPPING:
                // Implement free shipping logic
                discount = 0; // Placeholder
                break;
            default:
                discount = 0;
        }
        
        return discount;
    }
    
    // Use voucher (mark as used)
    public void useVoucher(String voucherCode, Long customerId, Long orderId) {
        Optional<CustomerVoucher> customerVoucherOpt = customerVoucherRepository
                .findValidVoucherByCustomerIdAndCode(customerId, voucherCode, LocalDateTime.now());
        
        if (!customerVoucherOpt.isPresent()) {
            throw new RuntimeException("Invalid or expired voucher");
        }
        
        CustomerVoucher customerVoucher = customerVoucherOpt.get();
        customerVoucher.markAsUsed(orderId);
        customerVoucherRepository.save(customerVoucher);
        
        // Increment used quantity in voucher
        Voucher voucher = customerVoucher.getVoucher();
        voucher.setUsedQuantity(voucher.getUsedQuantity() + 1);
        voucherRepository.save(voucher);
    }
} 