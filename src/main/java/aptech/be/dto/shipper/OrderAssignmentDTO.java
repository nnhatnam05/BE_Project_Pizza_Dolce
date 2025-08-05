package aptech.be.dto.shipper;

import lombok.Data;

@Data
public class OrderAssignmentDTO {
    private Long orderId;
    private Long shipperId;
    private String note;

    public OrderAssignmentDTO() {
    }

    public OrderAssignmentDTO(Long orderId, Long shipperId, String note) {
        this.orderId = orderId;
        this.shipperId = shipperId;
        this.note = note;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getShipperId() {
        return shipperId;
    }

    public void setShipperId(Long shipperId) {
        this.shipperId = shipperId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
