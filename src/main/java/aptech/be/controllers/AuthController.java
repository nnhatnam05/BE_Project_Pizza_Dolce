package aptech.be.controllers;

import aptech.be.dto.AuthRequest;
import aptech.be.config.JwtUtil;
import aptech.be.dto.UserDTO;
import aptech.be.dto.staff.UserWithProfileRequest;
import aptech.be.models.UserEntity;
import aptech.be.models.staff.StaffProfile;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.staff.AttendanceRecordRepository;
import aptech.be.repositories.staff.StaffProfileRepository;
import aptech.be.services.CustomUserDetails;
import aptech.be.services.EmailService;
import aptech.be.services.WebSocketNotificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    @Autowired
    @Qualifier("userDetailsServiceImpl")
    private UserDetailsService userDetailsService;

    private final EmailService emailService;
    private final WebSocketNotificationService webSocketNotificationService;


    private final ConcurrentHashMap<String, String> verificationCodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> blockedEmails = new ConcurrentHashMap<>();
    private final StaffProfileRepository staffProfileRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public AuthController(
            UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,@Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService, EmailService emailService,
            WebSocketNotificationService webSocketNotificationService, StaffProfileRepository staffProfileRepository, AttendanceRecordRepository attendanceRecordRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
        this.webSocketNotificationService = webSocketNotificationService;
        this.staffProfileRepository = staffProfileRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Kiểm tra user có active không
        if (user.getIsActive() != null && !user.getIsActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account is deactivated. Please contact administrator.");
        }
        
        String email = user.getEmail();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email not set for user");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        System.out.println("DEBUG: Creating verification code for email: " + email);
        System.out.println("DEBUG: Generated code: " + code);
        
        verificationCodes.put(email, code);
        System.out.println("DEBUG: Code stored in verificationCodes map");
        
        try {
        emailService.sendVerificationCode(email, code);
            System.out.println("DEBUG: Email sent successfully");
        } catch (Exception e) {
            System.out.println("DEBUG: Email sending failed: " + e.getMessage());
        }
        
        System.out.println("VERIFY EMAIL: " + email);
        System.out.println("VERIFY CODE: " + code);

        String tempJwt = jwtUtil.generateToken(userDetailsService.loadUserByUsername(request.getUsername()));

        return ResponseEntity.ok(Map.of(
                "message", "Verification code sent to email",
                "token", tempJwt,
                "email", email
        ));

    }




    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("role") String role,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "shiftType", required = false) String shiftType,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "workLocation", required = false) String workLocation
    ) {
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (email != null && userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already exists");
        }

        if (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("STAFF") && !role.equalsIgnoreCase("SHIPPER")) {
            return ResponseEntity.badRequest().body("Role must be ADMIN or STAFF or SHIPPER");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setRole(role.toUpperCase());
        newUser.setIsActive(true); // Set mặc định active cho user mới

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String uploadDir = "uploads/users/";
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                newUser.setImageUrl("/uploads/users/" + fileName);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image");
            }
        }

        if (role.equalsIgnoreCase("STAFF")) {
            if (position == null || shiftType == null) {
                return ResponseEntity.badRequest().body("Missing staff details for STAFF role");
            }

            StaffProfile profile = new StaffProfile();
            profile.setAddress(address);
            profile.setDob(dob);
            profile.setGender(gender);
            profile.setWorkLocation(workLocation);
            profile.setPosition(position);
            profile.setShiftType(shiftType);
            profile.setStatus("Đang làm");
            profile.setJoinDate(LocalDate.now());
            profile.setStaffCode("NV" + String.format("%03d", new Random().nextInt(999)));
            profile.setUser(newUser);
            newUser.setStaffProfile(profile);
        }

        userRepository.save(newUser);
        return ResponseEntity.ok("User registered successfully");
    }








    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        List<UserDTO> users = userRepository.findAll().stream()
                .map(user -> {
                    // Xử lý trường hợp isActive có thể null
                    if (user.getIsActive() == null) {
                        user.setIsActive(true); // Set mặc định true nếu null
                    }
                    return new UserDTO(user);
                })
                .toList();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            // Xử lý trường hợp isActive có thể null
            if (user.getIsActive() == null) {
                user.setIsActive(true); // Set mặc định true nếu null
            }
            return ResponseEntity.ok(new UserDTO(user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }








    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestParam("username") String username,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("role") String role,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "shiftType", required = false) String shiftType,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "workLocation", required = false) String workLocation,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        return userRepository.findById(id).map(user -> {
            if (!username.equals(user.getUsername()) &&
                    userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Username already exists"));
            }

            if (!email.equals(user.getEmail()) &&
                    userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email already exists"));
            }

            if (!phone.equals(user.getPhone()) &&
                    userRepository.findByPhone(phone).isPresent()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Phone already exists"));
            }

            user.setUsername(username);
            user.setName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(role.toUpperCase());

            if (password != null && !password.isBlank()) {
                user.setPassword(passwordEncoder.encode(password));
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    if (user.getImageUrl() != null) {
                        Path oldPath = Paths.get("uploads").resolve(user.getImageUrl().replace("/uploads/", ""));
                        Files.deleteIfExists(oldPath);
                    }

                    String uploadDir = "uploads/users/";
                    String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                    Path filePath = Paths.get(uploadDir + fileName);

                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, imageFile.getBytes());

                    user.setImageUrl("/uploads/users/" + fileName);
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image");
                }
            }

            if ("STAFF".equalsIgnoreCase(role)) {
                StaffProfile profile = user.getStaffProfile();
                if (profile == null) {
                    profile = new StaffProfile();
                    profile.setUser(user);
                    profile.setStaffCode("NV" + String.format("%03d", new Random().nextInt(999)));
                    profile.setJoinDate(LocalDate.now());
                }

                profile.setPosition(position);
                profile.setShiftType(shiftType);
                profile.setAddress(address);
                profile.setDob(dob);
                profile.setGender(gender);
                profile.setWorkLocation(workLocation);
                profile.setStatus("Đang làm");

                user.setStaffProfile(profile);
            } else {
                user.setStaffProfile(null); // Nếu role không còn là STAFF
            }

            userRepository.save(user);
            return ResponseEntity.ok("User updated successfully");
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }


    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user");
        }

        UserEntity user = customUserDetails.getUserEntity();
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Password cannot be blank");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Password updated successfully");
    }





    @Transactional
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserEntity user = userOpt.get();

        // Xoá ảnh nếu có
        if (user.getImageUrl() != null) {
            try {
                Path imagePath = Paths.get("uploads").resolve(user.getImageUrl().replace("/uploads/", ""));
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete image");
            }
        }

        // Nếu là STAFF → xoá attendance_records trước
        if ("STAFF".equalsIgnoreCase(user.getRole()) && user.getStaffProfile() != null) {
            Long staffId = user.getStaffProfile().getId();
            attendanceRecordRepository.deleteByStaffId(staffId); // Spring JPA custom
            staffProfileRepository.deleteById(staffId);
        }

        userRepository.delete(user);
        return ResponseEntity.ok("User deleted successfully");
    }






    @PostMapping("/forgot-password")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
        }

        if (blockedEmails.containsKey(email)) {
            LocalDateTime unblockTime = blockedEmails.get(email);
            if (LocalDateTime.now().isBefore(unblockTime)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Too many attempts. Try again later.");
            } else {
                blockedEmails.remove(email);
                failedAttempts.remove(email);
            }
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        verificationCodes.put(email, code);
        failedAttempts.put(email, 0);

        emailService.sendVerificationCode(email, code);
        System.out.println("VERIFY EMAIL: " + email);
        System.out.println("VERIFY CODE: " + code);

        return ResponseEntity.ok("Verification code sent");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (!verificationCodes.containsKey(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No code sent to this email");
        }



        if (blockedEmails.containsKey(email) && LocalDateTime.now().isBefore(blockedEmails.get(email))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access blocked. Try again later.");
        }

        String correctCode = verificationCodes.get(email);
        if (correctCode.equals(code)) {
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                user.setPassword(passwordEncoder.encode("pizza123"));
                userRepository.save(user);
                verificationCodes.remove(email);
                failedAttempts.remove(email);
                return ResponseEntity.ok("Password reset to 'pizza123'");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        } else {
            int attempts = failedAttempts.getOrDefault(email, 0) + 1;
            if (attempts >= 3) {
                blockedEmails.put(email, LocalDateTime.now().plusMinutes(3));
                verificationCodes.remove(email);
                failedAttempts.remove(email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Too many wrong attempts. Blocked for 3 minutes");
            } else {
                failedAttempts.put(email, attempts);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect code. Attempt " + attempts);
            }

        }

    }

    @Scheduled(fixedRate = 60000)
    public void clearExpiredBlocks() {
        blockedEmails.entrySet().removeIf(e -> LocalDateTime.now().isAfter(e.getValue()));
    }

    @PostMapping("/send-mail")
    public ResponseEntity<?> sendMail(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authorization header");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Kiểm tra user có active không trước khi gửi code mới
        if (user.getIsActive() != null && !user.getIsActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account is deactivated. Please contact administrator.");
        }
        
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email not set for user");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        System.out.println("DEBUG: Creating verification code for email: " + email);
        System.out.println("DEBUG: Generated code: " + code);
        
        verificationCodes.put(email, code);
        System.out.println("DEBUG: Code stored in verificationCodes map");

        try {
        emailService.sendVerificationCode(email, code);
            System.out.println("DEBUG: Email sent successfully");
        } catch (Exception e) {
            System.out.println("DEBUG: Email sending failed: " + e.getMessage());
        }
        
        System.out.println("VERIFY EMAIL: " + email);
        System.out.println("VERIFY CODE: " + code);
        return ResponseEntity.ok("Verification code sent");
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        System.out.println("DEBUG: verify2FA called with email: " + email + ", code: " + code);

        // Validate email trước
        if (email == null || email.isBlank()) {
            System.out.println("DEBUG: Email is null or blank");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
        }

        System.out.println("DEBUG: Checking if verificationCodes contains email: " + email);
        System.out.println("DEBUG: verificationCodes keys: " + verificationCodes.keySet());

        // Kiểm tra code có tồn tại không
        if (!verificationCodes.containsKey(email)) {
            System.out.println("DEBUG: No code sent to this email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No code sent to this email");
        }

        String storedCode = verificationCodes.get(email);
        System.out.println("DEBUG: Stored code for email " + email + ": " + storedCode);
        System.out.println("DEBUG: Provided code: " + code);
        System.out.println("DEBUG: Codes match: " + storedCode.equals(code));

        // Kiểm tra code có đúng không
        if (!storedCode.equals(code)) {
            System.out.println("DEBUG: Incorrect code provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect code");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Kiểm tra user có active không ở bước 2
        if (user.getIsActive() != null && !user.getIsActive()) {
            System.out.println("DEBUG: User account is deactivated");
            verificationCodes.remove(email); // Xóa code để tránh spam
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account is deactivated. Please contact administrator.");
        }

        verificationCodes.remove(email);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(Collections.singletonMap("token", jwt));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        String email = jwt.getSubject(); // đây chính là email theo bạn thiết lập trong token

        // Lấy user từ DB theo email
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(new UserDTO(user));
    }

    /**
     * Toggle user active status (ADMIN only)
     */
    @PutMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        Optional<UserEntity> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Không cho phép deactivate chính mình
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            if (customUserDetails.getUserEntity().getId().equals(id)) {
                return ResponseEntity.badRequest().body("Cannot deactivate your own account");
            }
        }

        // Xử lý trường hợp isActive có thể null (database chưa có cột)
        Boolean currentStatus = user.getIsActive();
        if (currentStatus == null) {
            // Nếu chưa có cột isActive, tạo mới với giá trị true
            currentStatus = true;
            user.setIsActive(true);
        }

        // Toggle status
        user.setIsActive(!currentStatus);
        userRepository.save(user);

        String status = user.getIsActive() ? "activated" : "deactivated";
        
        System.out.println("[DEBUG] User status changed: " + user.getUsername() + " -> " + status);
        System.out.println("[DEBUG] User ID: " + user.getId() + ", Role: " + user.getRole());
        
        // Send WebSocket notification based on status change
        if (user.getIsActive()) {
            // User was activated
            System.out.println("[DEBUG] Sending activation notification...");
            webSocketNotificationService.sendAccountActivationNotification(
                user.getId().toString(), 
                user.getUsername(), 
                user.getRole()
            );
        } else {
            // User was deactivated
            System.out.println("[DEBUG] Sending deactivation notification...");
            webSocketNotificationService.sendAccountDeactivationNotification(
                user.getId().toString(), 
                user.getUsername(), 
                user.getRole()
            );
        }
        
        System.out.println("[DEBUG] WebSocket notification sent successfully");
        
        return ResponseEntity.ok(Map.of(
            "message", "User " + status + " successfully",
            "userId", id,
            "isActive", user.getIsActive()
        ));
    }

    /**
     * Test WebSocket endpoint (for debugging)
     */
    @PostMapping("/test-websocket")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testWebSocket(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String username = request.get("username");
        String userType = request.get("userType");
        
        System.out.println("[TEST] Testing WebSocket for user: " + username + " (ID: " + userId + ", Type: " + userType + ")");
        
        try {
            webSocketNotificationService.sendAccountDeactivationNotification(userId, username, userType);
            System.out.println("[TEST] WebSocket test notification sent successfully");
            return ResponseEntity.ok("WebSocket test notification sent successfully");
        } catch (Exception e) {
            System.err.println("[TEST] WebSocket test failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("WebSocket test failed: " + e.getMessage());
        }
    }

    /**
     * Get users by active status (ADMIN only)
     */
    @GetMapping("/users/status/{isActive}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersByStatus(@PathVariable Boolean isActive) {
        List<UserDTO> users = userRepository.findByIsActive(isActive)
                .stream()
                .map(UserDTO::new)
                .toList();
        return ResponseEntity.ok(users);
    }








}
