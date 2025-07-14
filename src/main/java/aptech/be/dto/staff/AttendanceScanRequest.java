package aptech.be.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AttendanceScanRequest {
    private String staffCode;

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }
}



