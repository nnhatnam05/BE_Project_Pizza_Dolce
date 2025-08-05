package aptech.be.controllers;

import aptech.be.dto.VoucherDTO;
import aptech.be.models.Customer;
import aptech.be.repositories.CustomerRepository;
import aptech.be.services.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/vouchers")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerVoucherController {
    
    @Autowired
    private VoucherService voucherService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    // Lấy voucher của khách hàng hiện tại
    @GetMapping("/my-vouchers")
    public ResponseEntity<List<VoucherDTO>> getMyVouchers(Authentication authentication) {
        try {
            String email = authentication.getName();
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            
            List<VoucherDTO> vouchers = voucherService.getCustomerVouchers(customer.getId());
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Lấy voucher public (có thể nhận)
    @GetMapping("/public")
    public ResponseEntity<List<VoucherDTO>> getPublicVouchers() {
        try {
            List<VoucherDTO> vouchers = voucherService.getPublicVouchers();
            return ResponseEntity.ok(vouchers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    

    // Claim voucher public bằng code
    @PostMapping("/claim/{voucherCode}")
    public ResponseEntity<Map<String, Object>> claimVoucher(@PathVariable String voucherCode, Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            Customer customer = customerRepository.findByEmail(customerEmail)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            
            String result = voucherService.claimVoucherByCode(voucherCode, customer.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Validate voucher cho order
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateVoucher(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            Customer customer = customerRepository.findByEmail(customerEmail)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            
            String voucherCode = (String) request.get("voucherCode");
            Double orderAmount = ((Number) request.get("orderAmount")).doubleValue();
            
            Map<String, Object> result = voucherService.validateVoucherForOrder(voucherCode, customer.getId(), orderAmount);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }
} 