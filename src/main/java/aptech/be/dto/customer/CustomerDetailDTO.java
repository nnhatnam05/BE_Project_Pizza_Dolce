package aptech.be.dto.customer;

import aptech.be.models.Customer;

public class CustomerDetailDTO {
    private Long id;             // id của customer_detail (chi tiết)
    private Long customerId;     // id của bảng customers (gốc, quan trọng!)
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String imageUrl;
    private String point;
    private String voucher;
    private Customer customer;

    public CustomerDetailDTO() {}

    public CustomerDetailDTO(Customer customer) {
        if (customer != null) {
            this.customerId = customer.getId();     // id bảng customers
            this.email = customer.getEmail();
            this.fullName = customer.getFullName();
            this.customer = customer;

            if (customer.getCustomerDetail() != null) {
                this.id = customer.getCustomerDetail().getId(); // id của customer_detail
                this.phoneNumber = customer.getCustomerDetail().getPhoneNumber();
                this.address = customer.getCustomerDetail().getAddress();
                this.imageUrl = customer.getCustomerDetail().getImageUrl();
                this.point = customer.getCustomerDetail().getPoint();
                this.voucher = customer.getCustomerDetail().getVoucher();
            }
        }
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

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

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
