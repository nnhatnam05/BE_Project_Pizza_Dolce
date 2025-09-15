package aptech.be.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import aptech.be.models.ComplaintAttachment;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaint_cases")
public class ComplaintCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "assigned_staff_id")
    private UserEntity assignedStaff;

    private String status; // OPEN, NEED_ADMIN_APPROVAL, APPROVED, REJECTED, RESOLVED, CANCELLED, EXPIRED
    private String type;   // QUALITY_ISSUE, REFUND_REQUEST, OTHER

    private String decisionType; // REFUND, REDELIVER, NONE
    private Double refundAmount;
    private Long reDeliveryOrderId;
    private String refundQrUrl; // QR do khách cung cấp để chuyển khoản hoàn tiền
    private String refundStatus; // PENDING, COMPLETED
    private String refundReference; // mã giao dịch/ghi chú hoàn tiền
    private Long decidedByStaffId;
    private Long approvedByAdminId;

    private Boolean autoDecisionEnabledSnapshot;

    @Column(length = 2000)
    private String reason;
    @Column(length = 2000)
    private String staffNote;
    @Column(length = 2000)
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAtSnapshot;
    private LocalDateTime lockedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ComplaintMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ComplaintAttachment> attachments = new ArrayList<>();

    public ComplaintCase() {}

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters/setters
    public Long getId() { return id; }
    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public UserEntity getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(UserEntity assignedStaff) { this.assignedStaff = assignedStaff; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }
    public Double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Double refundAmount) { this.refundAmount = refundAmount; }
    public Long getReDeliveryOrderId() { return reDeliveryOrderId; }
    public void setReDeliveryOrderId(Long reDeliveryOrderId) { this.reDeliveryOrderId = reDeliveryOrderId; }
    public Boolean getAutoDecisionEnabledSnapshot() { return autoDecisionEnabledSnapshot; }
    public void setAutoDecisionEnabledSnapshot(Boolean autoDecisionEnabledSnapshot) { this.autoDecisionEnabledSnapshot = autoDecisionEnabledSnapshot; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getDeliveredAtSnapshot() { return deliveredAtSnapshot; }
    public void setDeliveredAtSnapshot(LocalDateTime deliveredAtSnapshot) { this.deliveredAtSnapshot = deliveredAtSnapshot; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public List<ComplaintMessage> getMessages() { return messages; }
    public List<ComplaintAttachment> getAttachments() { return attachments; }

    public String getRefundQrUrl() { return refundQrUrl; }
    public void setRefundQrUrl(String refundQrUrl) { this.refundQrUrl = refundQrUrl; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String refundReference) { this.refundReference = refundReference; }
    public Long getDecidedByStaffId() { return decidedByStaffId; }
    public void setDecidedByStaffId(Long decidedByStaffId) { this.decidedByStaffId = decidedByStaffId; }
    public Long getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(Long approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }
}


