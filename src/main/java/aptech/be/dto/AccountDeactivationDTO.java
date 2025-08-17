package aptech.be.dto;

import java.time.LocalDateTime;

public class AccountDeactivationDTO {
    private String type;
    private String message;
    private String reason;
    private LocalDateTime deactivatedAt;
    private String userId;
    private String username;
    private String userType; // ADMIN, STAFF, SHIPPER, CUSTOMER

    public AccountDeactivationDTO() {
    }

    public AccountDeactivationDTO(String type, String message, String reason, String userId, String username, String userType) {
        this.type = type;
        this.message = message;
        this.reason = reason;
        this.deactivatedAt = LocalDateTime.now();
        this.userId = userId;
        this.username = username;
        this.userType = userType;
    }

    // Static factory method for account deactivation
    public static AccountDeactivationDTO accountDeactivated(String userId, String username, String userType) {
        return new AccountDeactivationDTO(
            "ACCOUNT_DEACTIVATED",
            "Your account has been deactivated by administrator",
            "Account deactivated by admin",
            userId,
            username,
            userType
        );
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(LocalDateTime deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
} 