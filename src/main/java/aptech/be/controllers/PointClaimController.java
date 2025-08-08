package aptech.be.controllers;

import aptech.be.models.ClaimToken;
import aptech.be.services.ClaimTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dinein/points")
@CrossOrigin(origins = "http://localhost:3000")
public class PointClaimController {
    
    @Autowired
    private ClaimTokenService claimTokenService;
    
    /**
     * Claim points cho khách đã có tài khoản
     */
    @PostMapping("/claim-existing")
    public ResponseEntity<?> claimPointsExistingCustomer(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String email = request.get("email");
        
        System.out.println("[CLAIM API] Claim request - Token: " + token + ", Email: " + email);
        
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Token không được để trống"
            ));
        }
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email không được để trống"
            ));
        }
        
        try {
            boolean success = claimTokenService.claimPointsExistingCustomer(token, email.trim());
            if (success) {
                ClaimToken claimToken = claimTokenService.getTokenInfo(token);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chúc mừng! Bạn vừa nhận được " + claimToken.getPointsToEarn() + " điểm!",
                    "pointsEarned", claimToken.getPointsToEarn()
                ));
            }
        } catch (Exception e) {
            System.err.println("[CLAIM API ERROR] " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
        
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "Không thể nhận điểm. Vui lòng thử lại."
        ));
    }
    
    /**
     * Get thông tin token
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<?> getTokenInfo(@PathVariable String token) {
        System.out.println("[TOKEN INFO API] Checking token: " + token);
        
        try {
            ClaimToken claimToken = claimTokenService.getTokenInfo(token);
            if (claimToken == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Token không tồn tại"
                ));
            }
            
            if (claimToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Token đã hết hạn"
                ));
            }
            
            if (claimToken.getClaimed()) {
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "claimed", true,
                    "message", "Đơn hàng này đã được nhận điểm trước đó.",
                    "claimedByEmail", claimToken.getClaimedByEmail(),
                    "claimedAt", claimToken.getClaimedAt().toString()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "claimed", false,
                "pointsToEarn", claimToken.getPointsToEarn(),
                "totalAmount", claimToken.getTotalAmount(),
                "expiresAt", claimToken.getExpiresAt().toString()
            ));
            
        } catch (Exception e) {
            System.err.println("[TOKEN INFO API ERROR] " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "valid", false,
                "message", "Lỗi hệ thống. Vui lòng thử lại."
            ));
        }
    }
    
    /**
     * Claim points cho khách mới đăng ký (auto-claim sau khi verify)
     */
    @PostMapping("/claim-new-user")
    public ResponseEntity<?> claimPointsNewUser(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String email = request.get("email");
        
        System.out.println("[CLAIM API] New user claim request - Token: " + token + ", Email: " + email);
        
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Token không được để trống"
            ));
        }
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email không được để trống"
            ));
        }
        
        try {
            // Sử dụng cùng logic với existing customer nhưng với context khác
            boolean success = claimTokenService.claimPointsExistingCustomer(token, email.trim());
            if (success) {
                ClaimToken claimToken = claimTokenService.getTokenInfo(token);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Chúc mừng! Bạn đã nhận được %d điểm từ đơn hàng. Chào mừng bạn đến với hệ thống tích điểm của chúng tôi!", 
                        claimToken.getPointsToEarn()),
                    "pointsEarned", claimToken.getPointsToEarn(),
                    "isNewUser", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không thể nhận điểm. Token có thể đã hết hạn hoặc đã được sử dụng."
                ));
            }
        } catch (Exception e) {
            System.err.println("[CLAIM API] Error claiming points for new user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Lỗi hệ thống khi nhận điểm. Vui lòng thử lại sau."
            ));
        }
    }
    
    /**
     * Test endpoint để kiểm tra API hoạt động
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok(Map.of(
            "message", "Point Claim API is working!",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
} 