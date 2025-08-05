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

    // Lấy danh sách tất cả customers
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        try {
            List<Customer> customers = customerRepository.findAll();
            List<CustomerDTO> customerDTOs = customers.stream()
                    .map(this::convertToBasicDTO)
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

    // Convert Customer to basic DTO for list view
    private CustomerDTO convertToBasicDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setEmail(customer.getEmail());
        
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