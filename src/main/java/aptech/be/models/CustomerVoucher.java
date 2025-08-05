package aptech.be.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_vouchers")
public class CustomerVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;
    
    @Column(nullable = false)
    private LocalDateTime receivedAt; // Thời gian nhận voucher
    
    @Column
    private LocalDateTime usedAt; // Thời gian sử dụng voucher
    
    @Column
    private LocalDateTime expiresAt; // Thời gian hết hạn (24h từ khi nhận)
    
    @Column(nullable = false)
    private Boolean isUsed = false; // Đã sử dụng chưa
    
    @Column
    private Long orderId; // ID đơn hàng đã sử dụng voucher (nếu có)
    
    // Constructors
    public CustomerVoucher() {
        this.receivedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24); // Hết hạn sau 24h
    }
    
    public CustomerVoucher(Customer customer, Voucher voucher) {
        this();
        this.customer = customer;
        this.voucher = voucher;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public Voucher getVoucher() {
        return voucher;
    }
    
    public void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Boolean getIsUsed() {
        return isUsed;
    }
    
    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !isUsed && !isExpired();
    }
    
    public void markAsUsed(Long orderId) {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
    }
} 