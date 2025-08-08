package aptech.be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationDTO {
    private String type; // NEW_ORDER, ORDER_STATUS_UPDATE, STAFF_CALL, PAYMENT_REQUEST
    private String title;
    private String message;
    private Object data; // Additional data (order, table info, etc.)
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String priority; // HIGH, MEDIUM, LOW
    
    public NotificationDTO(String type, String title, String message, Object data, LocalDateTime timestamp, String priority) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.priority = priority;
    }
    
    public static NotificationDTO newOrder(Object orderData) {
        return new NotificationDTO(
            "NEW_ORDER",
            "New Dine-In Order",
            "A new dine-in order has been placed",
            orderData,
            LocalDateTime.now(),
            "HIGH"
        );
    }
    
    public static NotificationDTO orderStatusUpdate(Object orderData, String status) {
        return new NotificationDTO(
            "ORDER_STATUS_UPDATE",
            "Order Status Updated",
            "Order status has been updated to: " + status,
            orderData,
            LocalDateTime.now(),
            "MEDIUM"
        );
    }
    
    public static NotificationDTO staffCall(Long tableId, String reason) {
        return new NotificationDTO(
            "STAFF_CALL",
            "Staff Call Request",
            "Table is requesting assistance: " + reason,
            tableId,
            LocalDateTime.now(),
            "HIGH"
        );
    }
    
    public static NotificationDTO paymentRequest(Long tableId) {
        return new NotificationDTO(
            "PAYMENT_REQUEST",
            "Payment Request",
            "Table is requesting payment",
            tableId,
            LocalDateTime.now(),
            "HIGH"
        );
    }
} 