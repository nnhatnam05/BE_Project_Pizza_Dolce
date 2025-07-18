package aptech.be.dto;

import java.time.LocalDate;

public class StaffProfileDTO {
    private Long id;
    private String staffCode;
    private String gender;
    private LocalDate dob;
    private String address;
    private String position;
    private String status;
    private String shiftType;
    private LocalDate joinDate;
    private String workLocation;
    // KHÔNG nhúng lại UserEntity hoặc UserDTO nữa, chỉ thông tin staff!

    public StaffProfileDTO(aptech.be.models.staff.StaffProfile entity) {
        this.id = entity.getId();
        this.staffCode = entity.getStaffCode();
        this.gender = entity.getGender();
        this.dob = entity.getDob();
        this.address = entity.getAddress();
        this.position = entity.getPosition();
        this.status = entity.getStatus();
        this.shiftType = entity.getShiftType();
        this.joinDate = entity.getJoinDate();
        this.workLocation = entity.getWorkLocation();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }
}
