package aptech.be.controllers;

import aptech.be.config.JwtProvider;
import aptech.be.dto.customer.*;
import aptech.be.models.Customer;
import aptech.be.models.CustomerDetail;
import aptech.be.models.CustomerAddress;
import aptech.be.repositories.CustomerDetailRepository;
import aptech.be.repositories.CustomerRepository;
import aptech.be.services.CustomerService;
import aptech.be.services.EmailService;
import aptech.be.services.UserDetailsServiceImpl;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
public class CustomerAuthController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtProvider jwtProvider;

    @Value("${google.clientId}")
    private String googleClientId;
    @Autowired
    private CustomerDetailRepository customerDetailRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CustomerSignupRequest request) {
        if (customerService.findByEmail(request.getEmail()) != null) {
            return ResponseEntity.badRequest().body(new CustomerSignupResponse("Email already registered"));
        }
        customerService.generateAndSendVerificationCode(request.getEmail());
        return ResponseEntity.ok(new CustomerSignupResponse("Verification code sent to email"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody CustomerVerifyCodeRequest request) {
        CustomerSignupRequest signupReq = new CustomerSignupRequest();
        signupReq.setEmail(request.getEmail());
        signupReq.setFullName(request.getFullName());
        signupReq.setPassword(request.getPassword());

        String result = customerService.verifyCodeAndCreateCustomer(signupReq, request.getCode());
        if ("Registration successful".equals(result)) {
            return ResponseEntity.ok(new CustomerSignupResponse(result));
        }
        return ResponseEntity.badRequest().body(new CustomerSignupResponse(result));
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CustomerLoginRequest request) {
        Customer customer = customerService.findByEmail(request.getEmail());
        if (customer == null) {
            return ResponseEntity.status(401).body("Email not found");
        }
        
        // Kiểm tra customer có active không
        if (customer.getIsActive() != null && !customer.getIsActive()) {
            return ResponseEntity.status(401).body("Account is deactivated");
        }
        
        if (!customerService.checkPassword(request.getPassword(), customer.getPassword())) {
            return ResponseEntity.status(401).body("Invalid password");
        }
        String token = jwtProvider.generateTokenCustomer(customer);
        return ResponseEntity.ok(new CustomerLoginResponse(token));
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            System.out.println("GOOGLE LOGIN REQUEST: " + request.getIdToken());

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(java.util.Arrays.asList(
                            "820863045757-bchip9abkhu4h8om190hmmg7sd6t6cq2.apps.googleusercontent.com",
                            "820863045757-30j9es9v30ltnk3lqera90nijpcapplp.apps.googleusercontent.com",
                            "820863045757-uiaiscujfl7jiu61pn9nglgd45ecjlod.apps.googleusercontent.com"
                    ))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());

            if (idToken == null) {
                System.out.println("Invalid Google ID token.");
                return ResponseEntity.status(401).body("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            // Verify issuer strictly
            String issuer = payload.getIssuer();
            if (!"accounts.google.com".equals(issuer) &&
                !"https://accounts.google.com".equals(issuer)) {
                return ResponseEntity.status(401).body("Invalid token issuer");
            }
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Kiểm tra nếu Customer đã tồn tại, nếu chưa thì tạo mới
            Customer customer = customerService.findByEmail(email);
            if (customer == null) {
                // Có thể bổ sung lấy thêm thông tin avatar nếu muốn
                customer = new Customer();
                customer.setEmail(email);
                customer.setFullName(name);
                customer.setProvider("GOOGLE"); // tuỳ bạn thêm field này
                customer.setRole("CUSTOMER");
                customer.setPassword(UUID.randomUUID().toString());
                customer.setIsActive(true); // Set mặc định active cho customer mới
                customerService.save(customer);
            } else {
                // Kiểm tra customer có active không
                if (customer.getIsActive() != null && !customer.getIsActive()) {
                    return ResponseEntity.status(401).body("Account is deactivated");
                }
            }

            // Sinh JWT cho FE
            String token = jwtProvider.generateTokenCustomer(customer);

            return ResponseEntity.ok(new CustomerLoginResponse(token));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Google login error: " + e.getMessage());
        }
    }


    @PostMapping("/change-password")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> changePassword(@RequestBody CustomerChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName(); // Nếu JWT subject là email

        String result = customerService.changePassword(email, request.getOldPassword(), request.getNewPassword());
        if ("Password changed successfully".equals(result)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/me/detail")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyDetail(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerService.findByEmail(email);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }

        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            CustomerDetailDTO emptyDto = new CustomerDetailDTO();
            emptyDto.setEmail(customer.getEmail());
            emptyDto.setFullName(customer.getFullName());
        emptyDto.setCustomer(customer); // Thêm customer object để frontend có thể truy cập provider
            emptyDto.setAddresses(new ArrayList<>()); // Thêm empty addresses
            return ResponseEntity.ok(emptyDto);
        }

        CustomerDetailDTO dto = new CustomerDetailDTO();
        dto.setId(detail.getId());
        dto.setCustomerId(customer.getId());
        dto.setEmail(customer.getEmail());
        dto.setFullName(customer.getFullName());
        dto.setPhoneNumber(detail.getPhoneNumber());
        dto.setPoint(detail.getPoint());
        dto.setVoucher(detail.getVoucher());
        dto.setCustomer(customer); // Đảm bảo customer object được set để frontend có thể truy cập provider
        
        // Thêm danh sách địa chỉ
        dto.setAddresses(detail.getAddresses().stream()
                .map(this::convertAddressToDTO)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(dto);
    }


    @PutMapping("/me/detail")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateMyDetail(
            @RequestPart(value = "detail") CustomerDetailDTO detailDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            Authentication authentication) {
        
        if (detailDto == null) {
            return ResponseEntity.badRequest().body("Detail information is required");
        }

        String email = authentication.getName();
        Customer customer = customerService.findByEmail(email);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }

        CustomerDetail detail = customer.getCustomerDetail();
        if (detail == null) {
            detail = new CustomerDetail();
            detail.setCustomer(customer);
        }


        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // if (detail.getImageUrl() != null) {
                //     // Xử lý đúng path tới file ảnh cũ
                //     String oldImageUrl = detail.getImageUrl();
                //     // Loại bỏ dấu / đầu nếu có
                //     if (oldImageUrl.startsWith("/")) {
                //         oldImageUrl = oldImageUrl.substring(1);
                //     }
                //     Path oldImagePath = Paths.get(oldImageUrl);
                //     try {
                //         Files.deleteIfExists(oldImagePath);
                //     } catch (IOException ex) {
                //         // Nếu lỗi vẫn cho phép cập nhật thông tin, chỉ log ra cho biết
                //         ex.printStackTrace();
                //     }
                // }

                String uploadDir = "uploads/customer/";
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());

                // detail.setImageUrl("/uploads/customer/" + fileName); // Đã loại bỏ imageUrl

            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Failed to upload image");
            }
        }


        detail.setPhoneNumber(detailDto.getPhoneNumber());
        // detail.setAddress(detailDto.getAddress()); // Đã loại bỏ address
        // if (imageFile == null) {
        //     detail.setImageUrl(detailDto.getImageUrl()); // Đã loại bỏ imageUrl
        // }
        detail.setPoint(detailDto.getPoint());
        detail.setVoucher(detailDto.getVoucher());
        customer.setFullName(detailDto.getFullName());

        customerDetailRepository.save(detail);

        customer.setCustomerDetail(detail);
        customerService.save(customer);

        CustomerDetailDTO responseDto = new CustomerDetailDTO();
        responseDto.setId(detail.getId());
        responseDto.setCustomerId(customer.getId());
        responseDto.setEmail(customer.getEmail());
        responseDto.setFullName(customer.getFullName());
        responseDto.setPhoneNumber(detail.getPhoneNumber());
        responseDto.setPoint(detail.getPoint());
        responseDto.setVoucher(detail.getVoucher());
        responseDto.setCustomer(customer); // Đảm bảo customer object được set để frontend có thể truy cập provider
        responseDto.setAddresses(detail.getAddresses().stream()
                .map(this::convertAddressToDTO)
                .collect(Collectors.toList()));
        
        return ResponseEntity.ok(responseDto);
    }



    @DeleteMapping({"/me/detail", "/me/detail/"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteMyDetail(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerService.findByEmail(email);
        if (customer == null || customer.getCustomerDetail() == null) {
            return ResponseEntity.notFound().build();
        }
        CustomerDetail detail = customer.getCustomerDetail();

        // if (detail.getImageUrl() != null) {
        //     String filePathStr = "uploads/customer" + detail.getImageUrl().replace("/uploads/customer", "");
        //     Path filePath = Paths.get(filePathStr);
        //     try {
        //         Files.deleteIfExists(filePath);
        //     } catch (IOException e) {
        //         e.printStackTrace();
        //     }
        // }

        customer.setCustomerDetail(null);
        customerService.save(customer);
        customerDetailRepository.delete(detail);

        return ResponseEntity.ok("Customer detail deleted");
    }
    
    private CustomerAddressDTO convertAddressToDTO(CustomerAddress address) {
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        Customer customer = customerService.findByEmail(email);
        if (customer == null) {
            return ResponseEntity.status(401).body("Email not found");
        }
        
        // Kiểm tra customer có active không
        if (customer.getIsActive() != null && !customer.getIsActive()) {
            return ResponseEntity.status(401).body("Account is deactivated");
        }

        try {
            customerService.generateAndSendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent to your email");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send verification code");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (email == null || code == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Email, code, and new password are required");
        }

        Customer customer = customerService.findByEmail(email);
        if (customer == null) {
            return ResponseEntity.status(401).body("Email not found");
        }
        
        // Kiểm tra customer có active không
        if (customer.getIsActive() != null && !customer.getIsActive()) {
            return ResponseEntity.status(401).body("Account is deactivated");
        }

        try {
        String result = customerService.resetPassword(email, code, newPassword);
        if ("Password reset successful".equals(result)) {
                return ResponseEntity.ok("Password reset successful");
            } else {
        return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to reset password");
        }
    }

}

