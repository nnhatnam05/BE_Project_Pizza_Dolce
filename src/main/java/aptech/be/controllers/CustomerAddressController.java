package aptech.be.controllers;

import aptech.be.dto.customer.CustomerAddressDTO;
import aptech.be.models.Customer;
import aptech.be.models.CustomerAddress;
import aptech.be.models.CustomerDetail;
import aptech.be.repositories.CustomerAddressRepository;
import aptech.be.repositories.CustomerRepository;
import aptech.be.services.AddressValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {
    
    @Autowired
    private CustomerAddressRepository addressRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private AddressValidationService addressValidationService;
    
    @GetMapping
    public List<CustomerAddressDTO> getMyAddresses(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            return List.of();
        }
        
        return addressRepository.findByCustomerDetailId(detail.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @PostMapping
    public CustomerAddressDTO createAddress(@RequestBody CustomerAddressDTO addressDto, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            throw new RuntimeException("Customer detail not found");
        }
        
        // Validate địa chỉ HCM
        if (!addressValidationService.validateHoChiMinhCityAddress(
                addressDto.getAddress(), addressDto.getLatitude(), addressDto.getLongitude())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Address must be within Ho Chi Minh City!");
        }
        
        CustomerAddress address = new CustomerAddress();
        address.setCustomerDetail(detail);
        address.setName(addressDto.getName());
        address.setPhoneNumber(addressDto.getPhoneNumber());
        address.setAddress(addressDto.getAddress());
        address.setLatitude(addressDto.getLatitude());
        address.setLongitude(addressDto.getLongitude());
        address.setNote(addressDto.getNote());
        address.setIsDefault(addressDto.getIsDefault() != null ? addressDto.getIsDefault() : false);
        
        // Nếu đặt làm mặc định, bỏ mặc định của các địa chỉ khác
        if (address.getIsDefault()) {
            List<CustomerAddress> existingAddresses = addressRepository.findByCustomerDetailId(detail.getId());
            for (CustomerAddress existingAddress : existingAddresses) {
                existingAddress.setIsDefault(false);
                addressRepository.save(existingAddress);
            }
        }
        
        return convertToDTO(addressRepository.save(address));
    }
    
    @PutMapping("/{id}")
    public CustomerAddressDTO updateAddress(@PathVariable Long id, @RequestBody CustomerAddressDTO addressDto, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            throw new RuntimeException("Customer detail not found");
        }
        
        CustomerAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Kiểm tra địa chỉ thuộc về customer này
        if (!address.getCustomerDetail().getId().equals(detail.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own addresses");
        }
        
        // Validate địa chỉ HCM
        if (!addressValidationService.validateHoChiMinhCityAddress(
                addressDto.getAddress(), addressDto.getLatitude(), addressDto.getLongitude())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Address must be within Ho Chi Minh City!");
        }
        
        address.setName(addressDto.getName());
        address.setPhoneNumber(addressDto.getPhoneNumber());
        address.setAddress(addressDto.getAddress());
        address.setLatitude(addressDto.getLatitude());
        address.setLongitude(addressDto.getLongitude());
        address.setNote(addressDto.getNote());
        
        // Nếu đặt làm mặc định, bỏ mặc định của các địa chỉ khác
        if (addressDto.getIsDefault() != null && addressDto.getIsDefault()) {
            List<CustomerAddress> existingAddresses = addressRepository.findByCustomerDetailId(detail.getId());
            for (CustomerAddress existingAddress : existingAddresses) {
                if (!existingAddress.getId().equals(id)) {
                    existingAddress.setIsDefault(false);
                    addressRepository.save(existingAddress);
                }
            }
            address.setIsDefault(true);
        } else {
            address.setIsDefault(addressDto.getIsDefault() != null ? addressDto.getIsDefault() : false);
        }
        
        return convertToDTO(addressRepository.save(address));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            throw new RuntimeException("Customer detail not found");
        }
        
        CustomerAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Kiểm tra địa chỉ thuộc về customer này
        if (!address.getCustomerDetail().getId().equals(detail.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own addresses");
        }
        
        addressRepository.delete(address);
        return ResponseEntity.ok("Address deleted successfully");
    }
    
    @PutMapping("/{id}/default")
    public CustomerAddressDTO setDefaultAddress(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            throw new RuntimeException("Customer detail not found");
        }
        
        CustomerAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Kiểm tra địa chỉ thuộc về customer này
        if (!address.getCustomerDetail().getId().equals(detail.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only set your own addresses as default");
        }
        
        // Bỏ mặc định của tất cả địa chỉ khác
        List<CustomerAddress> existingAddresses = addressRepository.findByCustomerDetailId(detail.getId());
        for (CustomerAddress existingAddress : existingAddresses) {
            existingAddress.setIsDefault(false);
            addressRepository.save(existingAddress);
        }
        
        // Đặt địa chỉ này làm mặc định
        address.setIsDefault(true);
        return convertToDTO(addressRepository.save(address));
    }
    
    private CustomerAddressDTO convertToDTO(CustomerAddress address) {
        CustomerAddressDTO dto = new CustomerAddressDTO();
        dto.setId(address.getId());
        dto.setName(address.getName());
        dto.setPhoneNumber(address.getPhoneNumber());
        dto.setAddress(address.getAddress());
        dto.setLatitude(address.getLatitude());
        dto.setLongitude(address.getLongitude());
        dto.setNote(address.getNote());
        dto.setIsDefault(address.getIsDefault());
        return dto;
    }
} 