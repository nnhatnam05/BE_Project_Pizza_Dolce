package aptech.be.dto;

import java.util.List;

public class OrderDeliveryStatusDTO {
    private Long orderId;
    private String deliveryStatus;
    private String deliveryNote;
    private List<OrderStatusHistoryDTO> statusHistory;

    public OrderDeliveryStatusDTO() {
    }

    public OrderDeliveryStatusDTO(Long orderId, String deliveryStatus, String deliveryNote, List<OrderStatusHistoryDTO> statusHistory) {
        this.orderId = orderId;
        this.deliveryStatus = deliveryStatus;
        this.deliveryNote = deliveryNote;
        this.statusHistory = statusHistory;
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

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public List<OrderStatusHistoryDTO> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<OrderStatusHistoryDTO> statusHistory) {
        this.statusHistory = statusHistory;
    }
}
