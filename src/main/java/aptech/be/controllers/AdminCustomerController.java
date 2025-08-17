package aptech.be.controllers;

import aptech.be.dto.CustomerDTO;
import aptech.be.dto.VoucherDTO;
import aptech.be.dto.OrderResponseDTO;
import aptech.be.models.Customer;
import aptech.be.models.CustomerAddress;
import aptech.be.models.OrderEntity;
import aptech.be.repositories.CustomerRepository;
import aptech.be.repositories.CustomerAddressRepository;
import aptech.be.repositories.OrderRepository;
import aptech.be.services.VoucherService;
import aptech.be.services.WebSocketNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminCustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    // Lấy danh sách tất cả customers
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        try {
            List<Customer> customers = customerRepository.findAll();
            List<CustomerDTO> customerDTOs = customers.stream()
                    .map(customer -> {
                        // Xử lý trường hợp isActive có thể null
                        if (customer.getIsActive() == null) {
                            customer.setIsActive(true); // Set mặc định true nếu null
                        }
                        return convertToBasicDTO(customer);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(customerDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Lấy chi tiết customer với đầy đủ thông tin
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerDetails(@PathVariable Long customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // Xử lý trường hợp isActive có thể null
            if (customer.getIsActive() == null) {
                customer.setIsActive(true); // Set mặc định true nếu null
            }

            Map<String, Object> response = new HashMap<>();
            
            // Thông tin cơ bản
            response.put("customer", convertToDetailDTO(customer));
            
            // Lịch sử đơn hàng
            List<OrderEntity> orders = orderRepository.findByCustomerId(customerId);
            response.put("orders", orders.stream()
                    .map(this::convertOrderToDTO)
                    .collect(Collectors.toList()));
            
            // Vouchers hiện có
            List<VoucherDTO> vouchers = voucherService.getCustomerVouchers(customerId);
            response.put("vouchers", vouchers);
            
            // Địa chỉ đã lưu (cần customerDetailId)
            if (customer.getCustomerDetail() != null) {
                List<CustomerAddress> addresses = customerAddressRepository.findByCustomerDetailId(customer.getCustomerDetail().getId());
                response.put("addresses", addresses);
            } else {
                response.put("addresses", List.of());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Toggle customer active status (ADMIN only)
     */
    @PutMapping("/{customerId}/toggle-status")
    public ResponseEntity<?> toggleCustomerStatus(@PathVariable Long customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // Xử lý trường hợp isActive có thể null (database chưa có cột)
            Boolean currentStatus = customer.getIsActive();
            if (currentStatus == null) {
                // Nếu chưa có cột isActive, tạo mới với giá trị true
                currentStatus = true;
                customer.setIsActive(true);
            }

            // Toggle status
            customer.setIsActive(!currentStatus);
            customerRepository.save(customer);

            String status = customer.getIsActive() ? "activated" : "deactivated";
            
            System.out.println("[DEBUG] Customer status changed: " + customer.getEmail() + " -> " + status);
            System.out.println("[DEBUG] Customer ID: " + customer.getId());
            
            // Send WebSocket notification based on status change
            if (customer.getIsActive()) {
                // Customer was activated
                System.out.println("[DEBUG] Sending customer activation notification...");
                webSocketNotificationService.sendAccountActivationNotification(
                    customer.getId().toString(), 
                    customer.getEmail(), // Use email as username for customers
                    "CUSTOMER"
                );
            } else {
                // Customer was deactivated
                System.out.println("[DEBUG] Sending customer deactivation notification...");
                webSocketNotificationService.sendAccountDeactivationNotification(
                    customer.getId().toString(), 
                    customer.getEmail(), // Use email as username for customers
                    "CUSTOMER"
                );
            }
            
            System.out.println("[DEBUG] Customer WebSocket notification sent successfully");
            
            return ResponseEntity.ok(Map.of(
                "message", "Customer " + status + " successfully",
                "customerId", customerId,
                "isActive", customer.getIsActive()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get customers by active status (ADMIN only)
     */
    @GetMapping("/status/{isActive}")
    public ResponseEntity<List<CustomerDTO>> getCustomersByStatus(@PathVariable Boolean isActive) {
        try {
            List<Customer> customers = customerRepository.findByIsActive(isActive);
            List<CustomerDTO> customerDTOs = customers.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(customerDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Convert Customer to basic DTO for list view
    private CustomerDTO convertToBasicDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setEmail(customer.getEmail());
        dto.setIsActive(customer.getIsActive());
        
        if (customer.getCustomerDetail() != null) {
            dto.setPhoneNumber(customer.getCustomerDetail().getPhoneNumber());
            dto.setPoint(customer.getCustomerDetail().getPoint());
        }
        
        return dto;
    }

    // Convert Customer to detailed DTO
    private CustomerDTO convertToDetailDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setEmail(customer.getEmail());
        dto.setIsActive(customer.getIsActive());
        
        if (customer.getCustomerDetail() != null) {
            dto.setPhoneNumber(customer.getCustomerDetail().getPhoneNumber());
            dto.setPoint(customer.getCustomerDetail().getPoint());
            dto.setVoucher(customer.getCustomerDetail().getVoucher());
        }
        
        return dto;
    }

    // Convert Order to simple DTO
    private Map<String, Object> convertOrderToDTO(OrderEntity order) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", order.getId());
        dto.put("orderNumber", order.getOrderNumber());
        dto.put("totalPrice", order.getTotalPrice());
        dto.put("status", order.getStatus());
        dto.put("createdAt", order.getCreatedAt());
        dto.put("voucherCode", order.getVoucherCode());
        dto.put("voucherDiscount", order.getVoucherDiscount());
        return dto;
    }
} 