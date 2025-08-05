package aptech.be.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "order_foods")
public class OrderFood {
    @EmbeddedId
    private OrderFoodId id = new OrderFoodId();

    @ManyToOne
    @MapsId("orderId")
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private OrderEntity order;

    @ManyToOne
    @MapsId("foodId")
    @JoinColumn(name = "food_id")
    private Food food;

    private Integer quantity;

    public OrderFood() {}

    public OrderFood(OrderEntity order, Food food, Integer quantity) {
        this.order = order;
        this.food = food;
        this.quantity = quantity;
    }

    public OrderFoodId getId() {
        return id;
    }

    public void setId(OrderFoodId id) {
        this.id = id;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
