package aptech.be.models;

import jakarta.persistence.*;

@Entity
@Table(name = "complaint_settings")
public class ComplaintSettings {
    @Id
    private Long id = 1L;

    private Long assignedSupportStaffId;
    private Boolean autoDecisionEnabled = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAssignedSupportStaffId() { return assignedSupportStaffId; }
    public void setAssignedSupportStaffId(Long assignedSupportStaffId) { this.assignedSupportStaffId = assignedSupportStaffId; }

    public Boolean getAutoDecisionEnabled() { return autoDecisionEnabled; }
    public void setAutoDecisionEnabled(Boolean autoDecisionEnabled) { this.autoDecisionEnabled = autoDecisionEnabled; }
}


