package aptech.be.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String code; // Mã voucher 10 ký tự random
    
    @Column(nullable = false)
    private String name; // Tên voucher
    
    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả voucher
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type; // Loại voucher: PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING, etc.
    
    @Column(nullable = false)
    private Double value; // Giá trị ưu đãi (% hoặc số tiền)
    
    @Column
    private Double minOrderAmount; // Giá trị đơn hàng tối thiểu để áp dụng
    
    @Column
    private Double maxDiscountAmount; // Số tiền giảm tối đa (cho voucher %)
    
    @Column(nullable = false)
    private Integer totalQuantity; // Tổng số lượng voucher
    
    @Column(nullable = false)
    private Integer usedQuantity = 0; // Số lượng đã sử dụng
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime expiresAt; // Thời gian hết hạn voucher
    
    @Column(nullable = false)
    private Boolean isActive = true; // Voucher có đang hoạt động không
    
    @Column(nullable = false)
    private Boolean isPublic = false; // Voucher có được hiển thị công khai không
    
    @Column
    private String createdBy; // Admin tạo voucher
    
    // Constructors
    public Voucher() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Voucher(String code, String name, String description, VoucherType type, Double value) {
        this();
        this.code = code;
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = value;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public VoucherType getType() {
        return type;
    }
    
    public void setType(VoucherType type) {
        this.type = type;
    }
    
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    public Double getMinOrderAmount() {
        return minOrderAmount;
    }
    
    public void setMinOrderAmount(Double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }
    
    public Double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }
    
    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Integer getUsedQuantity() {
        return usedQuantity;
    }
    
    public void setUsedQuantity(Integer usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isAvailable() {
        return isActive && !isExpired() && usedQuantity < totalQuantity;
    }
    
    public int getRemainingQuantity() {
        return totalQuantity - usedQuantity;
    }
} 