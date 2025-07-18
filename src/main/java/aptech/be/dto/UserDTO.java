package aptech.be.dto;

import aptech.be.dto.StaffProfileDTO;

public class UserDTO {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String imageUrl;
    private String role;
    private StaffProfileDTO staffProfile; // chỉ cần thông tin cơ bản

    // Constructor, getter, setter...

    public UserDTO(aptech.be.models.UserEntity entity) {
        this.id = entity.getId();
        this.username = entity.getUsername();
        this.name = entity.getName();
        this.email = entity.getEmail();
        this.phone = entity.getPhone();
        this.imageUrl = entity.getImageUrl();
        this.role = entity.getRole();
        if (entity.getStaffProfile() != null) {
            this.staffProfile = new StaffProfileDTO(entity.getStaffProfile());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public StaffProfileDTO getStaffProfile() {
        return staffProfile;
    }

    public void setStaffProfile(StaffProfileDTO staffProfile) {
        this.staffProfile = staffProfile;
    }
}
