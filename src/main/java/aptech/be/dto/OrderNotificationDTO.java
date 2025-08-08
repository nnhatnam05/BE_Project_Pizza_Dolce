package aptech.be.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderNotificationDTO {
    private Long orderId;
    private String orderNumber;
    private String status;
    private Double totalPrice;
    private Long tableId;
    private Integer tableNumber;
    private String orderType;
    
    public OrderNotificationDTO(Long orderId, String orderNumber, String status, 
                               Double totalPrice, Long tableId, Integer tableNumber, String orderType) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.totalPrice = totalPrice;
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.orderType = orderType;
    }
} 