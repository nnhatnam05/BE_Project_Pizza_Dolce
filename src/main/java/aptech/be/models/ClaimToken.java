package aptech.be.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "claim_tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String token; // UUID
    
    @Column(name = "order_ids", columnDefinition = "TEXT", nullable = false)
    private String orderIds; // JSON array: ["123","124","125"]
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount; // Tổng tiền tất cả orders
    
    @Column(name = "points_to_earn", nullable = false)
    private Integer pointsToEarn; // Điểm sẽ nhận được
    
    @Column(nullable = false)
    private Boolean claimed = false; // Đã claim chưa
    
    @Column(name = "claimed_by_email")
    private String claimedByEmail; // Email đã claim
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // createdAt + 12 hours
    
    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
    
    // Manual getters and setters for compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getOrderIds() { return orderIds; }
    public void setOrderIds(String orderIds) { this.orderIds = orderIds; }
    
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public Integer getPointsToEarn() { return pointsToEarn; }
    public void setPointsToEarn(Integer pointsToEarn) { this.pointsToEarn = pointsToEarn; }
    
    public Boolean getClaimed() { return claimed; }
    public void setClaimed(Boolean claimed) { this.claimed = claimed; }
    
    public String getClaimedByEmail() { return claimedByEmail; }
    public void setClaimedByEmail(String claimedByEmail) { this.claimedByEmail = claimedByEmail; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
} 