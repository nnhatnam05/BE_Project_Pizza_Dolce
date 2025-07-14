package aptech.be.controllers;

import aptech.be.dto.AuthRequest;
import aptech.be.config.JwtUtil;
import aptech.be.dto.staff.UserWithProfileRequest;
import aptech.be.models.UserEntity;
import aptech.be.models.staff.StaffProfile;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.staff.AttendanceRecordRepository;
import aptech.be.repositories.staff.StaffProfileRepository;
import aptech.be.services.CustomUserDetails;
import aptech.be.services.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserDetailsService userDetailsService;

    private final EmailService emailService;



    private final ConcurrentHashMap<String, String> verificationCodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> blockedEmails = new ConcurrentHashMap<>();
    private final StaffProfileRepository staffProfileRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public AuthController(
            UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            JwtUtil jwtUtil, UserDetailsService userDetailsService, EmailService emailService,
            StaffProfileRepository staffProfileRepository, AttendanceRecordRepository attendanceRecordRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
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
        String email = user.getEmail();

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email not set for user");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        verificationCodes.put(email, code);
        emailService.sendVerificationCode(email, code);
        System.out.println("VERIFY EMAIL: " + email);
        System.out.println("VERIFY CODE: " + code);

        String tempJwt = jwtUtil.generateToken(userDetailsService.loadUserByUsername(request.getUsername()));

        return ResponseEntity.ok(Map.of(
                "message", "Verification code sent to email",
                "token", tempJwt
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

        if (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("STAFF")) {
            return ResponseEntity.badRequest().body("Role must be ADMIN or STAFF");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setRole(role.toUpperCase());

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
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
    public ResponseEntity<?> sendMail(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserEntity user = userOpt.get();
        String email = user.getEmail();

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email not set for user");
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

        emailService.sendVerificationCode(email, code); // Make sure EmailService is @Autowired
        System.out.println("VERIFY EMAIL: " + email);
        System.out.println("VERIFY CODE: " + code);
        return ResponseEntity.ok("Verification code sent");
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (!verificationCodes.containsKey(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No code sent to this email");
        }

        if (!verificationCodes.get(email).equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect code");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        verificationCodes.remove(email);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(userOpt.get().getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(Collections.singletonMap("token", jwt));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            UserEntity user = customUserDetails.getUserEntity();
            return ResponseEntity.ok(user);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token or user");
    }




}
