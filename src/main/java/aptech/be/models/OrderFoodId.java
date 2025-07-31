package aptech.be.models;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OrderFoodId implements Serializable {
    private Long orderId;
    private Long foodId;

    public OrderFoodId() {}

    public OrderFoodId(Long orderId, Long foodId) {
        this.orderId = orderId;
        this.foodId = foodId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getFoodId() {
        return foodId;
    }

    public void setFoodId(Long foodId) {
        this.foodId = foodId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderFoodId)) return false;
        OrderFoodId that = (OrderFoodId) o;
        return Objects.equals(getOrderId(), that.getOrderId()) &&
                Objects.equals(getFoodId(), that.getFoodId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrderId(), getFoodId());
    }
}
