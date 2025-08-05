package aptech.be.dto.customer;

import aptech.be.models.Customer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDetailDTO {
    private Long id;             // id của customer_detail (chi tiết)
    private Long customerId;     // id của bảng customers (gốc, quan trọng!)
    private String email;
    private String fullName;
    private String phoneNumber;
    // private String address; // Đã loại bỏ, thay bằng addresses list
    // private String imageUrl; // Đã loại bỏ, thay bằng file upload
    private String point;
    private String voucher;
    private Customer customer;
    private List<CustomerAddressDTO> addresses = new ArrayList<>();

    // Thêm getter/setter cho addresses
    public List<CustomerAddressDTO> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<CustomerAddressDTO> addresses) {
        this.addresses = addresses;
    }

    public CustomerDetailDTO() {}

    public CustomerDetailDTO(Long id, Long customerId, String email, String fullName, String phoneNumber, String point, String voucher, Customer customer, List<CustomerAddressDTO> addresses) {
        this.id = id;
        this.customerId = customerId;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        // this.imageUrl = imageUrl; // Đã loại bỏ
        this.point = point;
        this.voucher = voucher;
        this.customer = customer;
        this.addresses = addresses;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // public String getAddress() {
    //     return address;
    // }
    // public void setAddress(String address) {
    //     this.address = address;
    // }

    // public String getImageUrl() {
    //     return imageUrl;
    // }
    // public void setImageUrl(String imageUrl) {
    //     this.imageUrl = imageUrl;
    // }

    public String getPoint() {
        return point;
    }
    public void setPoint(String point) {
        this.point = point;
    }

    public String getVoucher() {
        return voucher;
    }
    public void setVoucher(String voucher) {
        this.voucher = voucher;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Customer getCustomer() {
        return customer;
    }
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
