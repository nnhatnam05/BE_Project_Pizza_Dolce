package aptech.be.dto.shipper;

import lombok.Data;

@Data
public class DeliveryUpdateDTO {
    private Long orderId;
    private String deliveryStatus;
    private String note;
    private Double latitude;
    private Double longitude;
    private String address;

    public DeliveryUpdateDTO() {
    }

    public DeliveryUpdateDTO(String deliveryStatus, String note, Double latitude, Double longitude, String address) {
        this.deliveryStatus = deliveryStatus;
        this.note = note;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public DeliveryUpdateDTO(Long orderId, String deliveryStatus, String note, Double latitude, Double longitude, String address) {
        this.orderId = orderId;
        this.deliveryStatus = deliveryStatus;
        this.note = note;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
