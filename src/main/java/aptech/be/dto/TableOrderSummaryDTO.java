package aptech.be.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TableOrderSummaryDTO {
    private Long tableId;
    private Integer tableNumber;
    private List<OrderSummaryDTO> orders;
    private Map<Long, Integer> totalItemQuantities; // foodId -> total quantity across all orders
    private Double totalAmount;
    private Integer totalOrders;
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class OrderSummaryDTO {
    private Long orderId;
    private String orderNumber;
    private String status;
    private Double totalPrice;
    private String createdAt;
    private List<OrderItemSummaryDTO> items;
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class OrderItemSummaryDTO {
    private Long foodId;
    private String foodName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
} 