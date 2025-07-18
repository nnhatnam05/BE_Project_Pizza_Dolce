package aptech.be.controllers;

import aptech.be.config.JwtProvider;
import aptech.be.dto.customer.*;
import aptech.be.models.Customer;
import aptech.be.models.CustomerDetail;
import aptech.be.repositories.CustomerDetailRepository;
import aptech.be.repositories.CustomerRepository;
import aptech.be.services.CustomerService;
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
import java.util.UUID;

import java.util.Collections;

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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CustomerSignupRequest request) {
        String result = customerService.register(request);
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
                    .setAudience(Collections.singletonList("820863045757-bchip9abkhu4h8om190hmmg7sd6t6cq2.apps.googleusercontent.com")) // Client ID của bạn
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());

            if (idToken == null) {
                System.out.println("Invalid Google ID token.");
                return ResponseEntity.status(401).body("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
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
                customerService.save(customer);
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
        System.out.println("Authorities: " + authentication.getAuthorities());
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
            return ResponseEntity.ok(emptyDto);
        }

        return ResponseEntity.ok(new CustomerDetailDTO(customer));
    }


    @PutMapping("/me/detail")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateMyDetail(
            @RequestPart("detail") CustomerDetailDTO detailDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            Authentication authentication) {

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
                if (detail.getImageUrl() != null) {
                    // Xử lý đúng path tới file ảnh cũ
                    String oldImageUrl = detail.getImageUrl();
                    // Loại bỏ dấu / đầu nếu có
                    if (oldImageUrl.startsWith("/")) {
                        oldImageUrl = oldImageUrl.substring(1);
                    }
                    Path oldImagePath = Paths.get(oldImageUrl);
                    try {
                        Files.deleteIfExists(oldImagePath);
                    } catch (IOException ex) {
                        // Nếu lỗi vẫn cho phép cập nhật thông tin, chỉ log ra cho biết
                        ex.printStackTrace();
                    }
                }

                String uploadDir = "uploads/customer/";
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());

                detail.setImageUrl("/uploads/customer/" + fileName);

            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Failed to upload image");
            }
        }


        detail.setPhoneNumber(detailDto.getPhoneNumber());
        detail.setAddress(detailDto.getAddress());
        if (imageFile == null) {
            detail.setImageUrl(detailDto.getImageUrl());
        }
        detail.setPoint(detailDto.getPoint());
        detail.setVoucher(detailDto.getVoucher());
        customer.setFullName(detailDto.getFullName());

        customerDetailRepository.save(detail);

        customer.setCustomerDetail(detail);
        customerService.save(customer);

        return ResponseEntity.ok(new CustomerDetailDTO(customer));
    }



    @DeleteMapping("/me/detail")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteMyDetail(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerService.findByEmail(email);
        if (customer == null || customer.getCustomerDetail() == null) {
            return ResponseEntity.notFound().build();
        }
        CustomerDetail detail = customer.getCustomerDetail();

        if (detail.getImageUrl() != null) {
            String filePathStr = "uploads/customer" + detail.getImageUrl().replace("/uploads/customer", "");
            Path filePath = Paths.get(filePathStr);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        customer.setCustomerDetail(null);
        customerService.save(customer);
        customerDetailRepository.delete(detail);

        return ResponseEntity.ok("Customer detail deleted");
    }

}

