package aptech.be.controllers;

import aptech.be.dto.VoucherDTO;
import aptech.be.dto.CreateVoucherRequest;
import aptech.be.services.VoucherService;
import aptech.be.services.CustomerService;
import aptech.be.models.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
public class VoucherController {
    
    @Autowired
    private VoucherService voucherService;
    
    @Autowired
    private CustomerService customerService;
    
    // Lấy tất cả voucher
    @GetMapping
    public ResponseEntity<List<VoucherDTO>> getAllVouchers() {
        try {
            List<VoucherDTO> vouchers = voucherService.getAllVouchers();
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Lấy voucher theo ID
    @GetMapping("/{id}")
    public ResponseEntity<VoucherDTO> getVoucherById(@PathVariable Long id) {
        try {
            VoucherDTO voucher = voucherService.getVoucherById(id);
            return ResponseEntity.ok(voucher);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Tạo voucher mới
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVoucher(@RequestBody CreateVoucherRequest request, Authentication authentication) {
        try {
            String createdBy = authentication.getName();
            VoucherDTO createdVoucher = voucherService.createVoucher(request, createdBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher created successfully");
            response.put("voucher", createdVoucher);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create voucher: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Cập nhật voucher
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVoucher(@PathVariable Long id, @RequestBody CreateVoucherRequest request) {
        try {
            System.out.println("Updating voucher with ID: " + id);
            System.out.println("Received request: " + request.getName());
            
            VoucherDTO updatedVoucher = voucherService.updateVoucher(id, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher updated successfully");
            response.put("voucher", updatedVoucher);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating voucher: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update voucher: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Xóa voucher
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteVoucher(@PathVariable Long id) {
        try {
            voucherService.deleteVoucher(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete voucher: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Tặng voucher cho khách hàng cụ thể
    @PostMapping("/{voucherId}/give-to-customer/{customerId}")
    public ResponseEntity<Map<String, Object>> giveVoucherToCustomer(
            @PathVariable Long voucherId, 
            @PathVariable Long customerId) {
        try {
            String result = voucherService.giveVoucherToCustomer(voucherId, customerId);
            
            Map<String, Object> response = new HashMap<>();
            if ("Voucher given successfully".equals(result)) {
                response.put("success", true);
                response.put("message", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to give voucher: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Lấy voucher public (cho khách hàng xem)
    @GetMapping("/public")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<VoucherDTO>> getPublicVouchers() {
        try {
            List<VoucherDTO> vouchers = voucherService.getPublicVouchers();
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Lấy voucher của khách hàng cụ thể
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<VoucherDTO>> getCustomerVouchers(@PathVariable Long customerId) {
        try {
            List<VoucherDTO> vouchers = voucherService.getCustomerVouchers(customerId);
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Trigger manual cleanup (for testing)
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        try {
            voucherService.cleanupExpiredVouchers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleanup completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Cleanup failed: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Lấy danh sách customers cho admin
    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getCustomersForAdmin() {
        try {
            List<Customer> customers = customerService.getAllCustomers();
            
            List<Map<String, Object>> customerList = customers.stream()
                .map(customer -> {
                    Map<String, Object> customerMap = new HashMap<>();
                    customerMap.put("id", customer.getId());
                    customerMap.put("fullName", customer.getFullName());
                    customerMap.put("email", customer.getEmail());
                    return customerMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(customerList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 