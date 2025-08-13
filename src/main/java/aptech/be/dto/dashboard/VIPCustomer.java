package aptech.be.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VIPCustomer {
    private Long customerId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private int points;
    private BigDecimal totalSpent;
    private int totalOrders;
    private LocalDateTime lastOrderDate;
    private LocalDateTime registrationDate;
    private String rank; // GOLD, SILVER, BRONZE
    
    // Constructor
    public VIPCustomer() {}

    public VIPCustomer(Long customerId, String fullName, String email, String phoneNumber,
                      int points, BigDecimal totalSpent, int totalOrders,
                      LocalDateTime lastOrderDate, LocalDateTime registrationDate, String rank) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.points = points;
        this.totalSpent = totalSpent;
        this.totalOrders = totalOrders;
        this.lastOrderDate = lastOrderDate;
        this.registrationDate = registrationDate;
        this.rank = rank;
    }

    // Getters and Setters
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    
    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }
    
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
} 