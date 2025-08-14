package aptech.be.dto;

import aptech.be.models.Voucher;
import aptech.be.models.VoucherType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import aptech.be.config.LocalDateTimeDeserializer;

import java.time.LocalDateTime;

public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String type;
    private Double value;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private Integer totalQuantity;
    private Integer usedQuantity;
    private Integer remainingQuantity;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    
    private Boolean isActive;
    private Boolean isPublic;
    private String createdBy;
    private Boolean isExpired;
    private Boolean isAvailable;
    
    // Constructors
    public VoucherDTO() {}
    
    public VoucherDTO(Voucher voucher) {
        this.id = voucher.getId();
        this.code = voucher.getCode();
        this.name = voucher.getName();
        this.description = voucher.getDescription();
        this.type = voucher.getType().name();
        this.value = voucher.getValue();
        this.minOrderAmount = voucher.getMinOrderAmount();
        this.maxDiscountAmount = voucher.getMaxDiscountAmount();
        this.totalQuantity = voucher.getTotalQuantity();
        this.usedQuantity = voucher.getUsedQuantity();
        this.remainingQuantity = voucher.getRemainingQuantity();
        this.createdAt = voucher.getCreatedAt();
        this.expiresAt = voucher.getExpiresAt();
        this.isActive = voucher.getIsActive();
        this.isPublic = voucher.getIsPublic();
        this.createdBy = voucher.getCreatedBy();
        this.isExpired = voucher.isExpired();
        this.isAvailable = voucher.isAvailable();
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
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
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
    
    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }
    
    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
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
    
    public Boolean getIsExpired() {
        return isExpired;
    }
    
    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }
    
    public Boolean getIsAvailable() {
        return isAvailable;
    }
    
    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
} 