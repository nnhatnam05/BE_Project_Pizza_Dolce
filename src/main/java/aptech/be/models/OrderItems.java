package aptech.be.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private OrderEntity order;

    @ManyToOne
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice; // Giá tại thời điểm đặt

    @Column(name = "total_price", nullable = false)
    private Double totalPrice; // quantity * unitPrice

    // Default constructor for Hibernate
    public OrderItems() {
    }

    // Constructor with all fields
    public OrderItems(Long id, OrderEntity order, Food food, Integer quantity, Double unitPrice, Double totalPrice) {
        this.id = id;
        this.order = order;
        this.food = food;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    // Custom constructor for business logic
    public OrderItems(OrderEntity order, Food food, Integer quantity) {
        this.order = order;
        this.food = food;
        this.quantity = quantity;
        this.unitPrice = food.getPrice();
        this.totalPrice = this.unitPrice * quantity;
    }
    
    // Calculate total price when quantity or unit price changes
    public void calculateTotalPrice() {
        if (this.unitPrice != null && this.quantity != null) {
            this.totalPrice = this.unitPrice * this.quantity;
        }
    }
    
    // Manual getters and setters (in case Lombok doesn't work)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }
    
    public Food getFood() { return food; }
    public void setFood(Food food) { this.food = food; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { 
        this.quantity = quantity; 
        calculateTotalPrice();
    }
    
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { 
        this.unitPrice = unitPrice; 
        calculateTotalPrice();
    }
    
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
} 