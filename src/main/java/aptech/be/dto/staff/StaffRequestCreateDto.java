package aptech.be.dto.staff;

import java.time.LocalDate;

public class StaffRequestCreateDto {
    private String type;      // "LEAVE", "SWAP", "OVERTIME" (frontend gửi lên dạng chuỗi)
    private String reason;    // Lý do xin nghỉ/đổi ca
    private LocalDate targetDate;

    // Getter & Setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
}
