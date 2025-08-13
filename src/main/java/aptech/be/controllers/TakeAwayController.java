package aptech.be.controllers;

import aptech.be.models.*;
import aptech.be.repositories.*;
import aptech.be.services.CustomerService;
import aptech.be.services.EmailService;
import aptech.be.config.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/takeaway")
@CrossOrigin(origins = "http://localhost:3000")
public class TakeAwayController {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private FoodRepository foodRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CustomerDetailRepository customerDetailRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JwtUtil jwtUtil;

    // Helper method to validate staff ownership
    private Map<String, Object> validateStaffOwnership(OrderEntity order) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        
        Optional<UserEntity> currentStaffOpt = userRepository.findByEmail(currentUserEmail);
        if (currentStaffOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", false);
            result.put("message", "Current user not found");
            return result;
        }
        
        UserEntity currentStaff = currentStaffOpt.get();
        
        // Admin can manage any order
        if ("ADMIN".equals(currentStaff.getRole())) {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("currentStaff", currentStaff);
            return result;
        }
        
        // For staff, check ownership
        if (order.getStaff() == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", false);
            result.put("message", "Order has no assigned staff");
            return result;
        }
        
        if (!order.getStaff().getId().equals(currentStaff.getId())) {
            Map<String, Object> result = new HashMap<>();
            result.put("valid", false);
            result.put("message", "You can only manage orders you created. This order belongs to: " + 
                     order.getStaff().getName() + " (" + order.getStaff().getEmail() + ")");
            result.put("authorizedStaff", order.getStaff());
            return result;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("currentStaff", currentStaff);
        return result;
    }

    // Helper method to add points to customer (same logic as OrderController)
    private int addPointsToCustomer(OrderEntity order) {
        try {
            Customer customer = order.getCustomer();
            if (customer != null && customer.getCustomerDetail() != null) {
                CustomerDetail customerDetail = customer.getCustomerDetail();
                
                // Calculate points: 10$ = 10 points (round down)
                double totalPrice = order.getTotalPrice();
                int pointsToAdd = (int) Math.floor(totalPrice / 10.0) * 10;
                
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
                
                System.out.println("[TAKEAWAY POINTS] Added " + pointsToAdd + " points to customer " + customer.getId() + 
                                 ". Total points: " + newPoints + " (Order total: $" + totalPrice + ")");
                
                return pointsToAdd;
            }
        } catch (Exception e) {
            System.err.println("[TAKEAWAY POINTS ERROR] Failed to add points: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Create new take-away order (alias)
     */
    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        return createTakeAwayOrder(request);
    }

    /**
     * Create new take-away order
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional
    public ResponseEntity<?> createTakeAwayOrder(@RequestBody Map<String, Object> request) {
        try {
            // Extract order data
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            String customerEmail = (String) request.get("customerEmail");
            String customerName = (String) request.get("customerName");
            String customerPhone = (String) request.get("customerPhone");
            
            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body("Order items cannot be empty");
            }

            // Get current staff user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            Optional<UserEntity> staffOpt = userRepository.findByEmail(currentUserEmail);
            if (staffOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Staff not found");
            }
            
            UserEntity staff = staffOpt.get();
            if (!"STAFF".equals(staff.getRole()) && !"ADMIN".equals(staff.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only staff can create take-away orders");
            }

            // Create order entity
            OrderEntity order = new OrderEntity();
            order.setOrderNumber(generateTakeAwayOrderNumber());
            order.setOrderType("TAKE_AWAY");
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now());
            order.setStaff(staff); // Set staff who created the order

            // Set customer if provided
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                Optional<Customer> customerOpt = customerRepository.findByEmail(customerEmail.trim());
                if (customerOpt.isPresent()) {
                    order.setCustomer(customerOpt.get());
                } else {
                    return ResponseEntity.badRequest().body("Customer email not found: " + customerEmail);
                }
            }

            // Set customer info for display (using existing fields)
            order.setRecipientName(customerName);
            order.setRecipientPhone(customerPhone);

            // Calculate total and create order foods
            double totalPrice = 0.0;
            List<OrderFood> orderFoods = new ArrayList<>();

            for (Map<String, Object> item : items) {
                Long foodId = Long.valueOf(item.get("foodId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                Optional<Food> foodOpt = foodRepository.findById(foodId);
                if (foodOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Food not found: " + foodId);
                }

                Food food = foodOpt.get();
                double itemTotal = food.getPrice() * quantity;
                totalPrice += itemTotal;

                // Create OrderFood
                OrderFood orderFood = new OrderFood();
                OrderFoodId orderFoodId = new OrderFoodId();
                orderFoodId.setOrderId(null); // Will be set after order is saved
                orderFoodId.setFoodId(foodId);
                
                orderFood.setId(orderFoodId);
                orderFood.setFood(food);
                orderFood.setQuantity(quantity);
                orderFood.setOrder(order);
                
                orderFoods.add(orderFood);
            }

            order.setTotalPrice(totalPrice);
            order.setOrderFoods(orderFoods);

            // Save order
            OrderEntity savedOrder = orderRepository.save(order);

            // Update OrderFood IDs
            for (OrderFood orderFood : orderFoods) {
                orderFood.getId().setOrderId(savedOrder.getId());
            }

            System.out.println("[TAKEAWAY] Created order: " + savedOrder.getOrderNumber() + 
                             " with total: $" + totalPrice);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            System.err.println("[TAKEAWAY ERROR] Failed to create order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create take-away order: " + e.getMessage());
        }
    }

    /**
     * Get all take-away orders (alias for pending)
     */
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> getAllOrders() {
        return getPendingOrders();
    }

    /**
     * Get all pending take-away orders
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> getPendingOrders() {
        try {
            List<OrderEntity> orders = orderRepository.findByOrderTypeAndStatusIn(
                "TAKE_AWAY", 
                Arrays.asList("PENDING", "PAID", "PREPARING", "READY")
            );
            
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            System.err.println("[TAKEAWAY ERROR] Failed to get pending orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get pending orders");
        }
    }

    /**
     * Confirm payment for take-away order
     */
    @PutMapping("/{orderId}/payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long orderId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) MultipartFile billImage) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            if (!"TAKE_AWAY".equals(order.getOrderType())) {
                return ResponseEntity.badRequest().body("Not a take-away order");
            }

            // Handle bill image upload for QR banking
            if ("QR_BANKING".equals(paymentMethod) && billImage != null) {
                String billImagePath = saveBillImage(billImage, orderId);
                order.setBillImageUrl(billImagePath);
            }

            order.setPaymentMethod(paymentMethod);
            order.setStatus("PAID");
            order.setUpdatedAt(LocalDateTime.now());

            OrderEntity savedOrder = orderRepository.save(order);

            System.out.println("[TAKEAWAY] Payment confirmed for order: " + order.getOrderNumber() + 
                             " via " + paymentMethod);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            System.err.println("[TAKEAWAY ERROR] Failed to confirm payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to confirm payment");
        }
    }

    /**
     * Mark order as ready for pickup
     */
    @PutMapping("/{orderId}/ready")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional
    public ResponseEntity<?> markReady(@PathVariable Long orderId) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            if (!"TAKE_AWAY".equals(order.getOrderType())) {
                return ResponseEntity.badRequest().body("Not a take-away order");
            }

            // Validate staff ownership
            Map<String, Object> validation = validateStaffOwnership(order);
            if (!(Boolean) validation.get("valid")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", validation.get("message"));
                if (validation.containsKey("authorizedStaff")) {
                    errorResponse.put("authorizedStaff", validation.get("authorizedStaff"));
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            order.setStatus("READY");
            order.setUpdatedAt(LocalDateTime.now());

            OrderEntity savedOrder = orderRepository.save(order);

            System.out.println("[TAKEAWAY] Order ready for pickup: " + order.getOrderNumber());

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            System.err.println("[TAKEAWAY ERROR] Failed to mark ready: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to mark order as ready");
        }
    }

    /**
     * Complete take-away order and award points
     */
    @PutMapping("/{orderId}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional
    public ResponseEntity<?> completeOrder(@PathVariable Long orderId) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            if (!"TAKE_AWAY".equals(order.getOrderType())) {
                return ResponseEntity.badRequest().body("Not a take-away order");
            }

            // Validate staff ownership
            Map<String, Object> validation = validateStaffOwnership(order);
            if (!(Boolean) validation.get("valid")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", validation.get("message"));
                if (validation.containsKey("authorizedStaff")) {
                    errorResponse.put("authorizedStaff", validation.get("authorizedStaff"));
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            order.setStatus("COMPLETED");
            order.setUpdatedAt(LocalDateTime.now());

            // Award points if customer exists
            int pointsEarned = 0;
            if (order.getCustomer() != null) {
                pointsEarned = addPointsToCustomer(order);
                
                // Send email notification
                try {
                    emailService.sendPaymentSuccessEmail(
                        order.getCustomer().getEmail(), 
                        order, 
                        pointsEarned
                    );
                } catch (Exception e) {
                    System.err.println("[EMAIL ERROR] Failed to send completion email: " + e.getMessage());
                }
            }

            OrderEntity savedOrder = orderRepository.save(order);

            Map<String, Object> response = new HashMap<>();
            response.put("order", savedOrder);
            response.put("pointsEarned", pointsEarned);

            System.out.println("[TAKEAWAY] Order completed: " + order.getOrderNumber() + 
                             " with " + pointsEarned + " points awarded");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("[TAKEAWAY ERROR] Failed to complete order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to complete order");
        }
    }

    /**
     * Verify customer email for loyalty points
     */
    @PostMapping("/customer/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<?> verifyCustomerEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Email is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Optional<Customer> customerOpt = customerRepository.findByEmail(email.trim());
            
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                
                // Get current points
                int currentPoints = 0;
                if (customer.getCustomerDetail() != null && 
                    customer.getCustomerDetail().getPoint() != null) {
                    try {
                        currentPoints = Integer.parseInt(customer.getCustomerDetail().getPoint());
                    } catch (NumberFormatException e) {
                        currentPoints = 0;
                    }
                }
                
                // Create customer response map with null safety
                Map<String, Object> customerData = new HashMap<>();
                customerData.put("id", customer.getId() != null ? customer.getId() : 0L);
                customerData.put("email", customer.getEmail() != null ? customer.getEmail() : "");
                customerData.put("fullName", customer.getFullName() != null ? customer.getFullName() : "");
                customerData.put("currentPoints", currentPoints);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Customer found successfully!");
                response.put("customer", customerData);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No account found with this email address. Would you like to create a new account?");
                response.put("showCreateOption", true);
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            System.err.println("[TAKEAWAY ERROR] Failed to verify customer: " + errorMessage);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to verify customer email: " + errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create new customer account
     */
    @PostMapping("/customer/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Transactional
    public ResponseEntity<?> createCustomer(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String fullName = request.get("fullName");
            String phone = request.get("phone");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Full name is required");
            }

            // Check if customer already exists
            if (customerRepository.findByEmail(email.trim()).isPresent()) {
                return ResponseEntity.badRequest().body("Customer with this email already exists");
            }

            // Create customer using existing service
            Customer customer = customerService.createCustomerByStaff(email.trim(), fullName.trim(), phone);

            System.out.println("[TAKEAWAY] Created new customer: " + email);

            return ResponseEntity.ok(customer);

        } catch (Exception e) {
            System.err.println("[TAKEAWAY ERROR] Failed to create customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create customer: " + e.getMessage());
        }
    }

    /**
     * Generate unique take-away order number
     */
    private String generateTakeAwayOrderNumber() {
        String prefix = "TA";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return prefix + timestamp;
    }

    /**
     * Save bill image for QR banking payment
     */
    private String saveBillImage(MultipartFile file, Long orderId) throws IOException {
        String uploadDir = "uploads/takeaway/bills/";
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = "bill_" + orderId + "_" + System.currentTimeMillis() + ".jpg";
        Path filePath = uploadPath.resolve(fileName);
        
        Files.copy(file.getInputStream(), filePath);
        
        return "/" + uploadDir + fileName;
    }
} 