package aptech.be.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RevenueDataPoint {
    private LocalDate date;
    private BigDecimal revenue;
    private int orderCount;
    private BigDecimal deliveryRevenue;
    private BigDecimal dineInRevenue;
    private BigDecimal takeAwayRevenue;

    // Constructor
    public RevenueDataPoint() {}

    public RevenueDataPoint(LocalDate date, BigDecimal revenue, int orderCount, 
                           BigDecimal deliveryRevenue, BigDecimal dineInRevenue, BigDecimal takeAwayRevenue) {
        this.date = date;
        this.revenue = revenue;
        this.orderCount = orderCount;
        this.deliveryRevenue = deliveryRevenue;
        this.dineInRevenue = dineInRevenue;
        this.takeAwayRevenue = takeAwayRevenue;
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    
    public BigDecimal getDeliveryRevenue() { return deliveryRevenue; }
    public void setDeliveryRevenue(BigDecimal deliveryRevenue) { this.deliveryRevenue = deliveryRevenue; }
    
    public BigDecimal getDineInRevenue() { return dineInRevenue; }
    public void setDineInRevenue(BigDecimal dineInRevenue) { this.dineInRevenue = dineInRevenue; }
    
    public BigDecimal getTakeAwayRevenue() { return takeAwayRevenue; }
    public void setTakeAwayRevenue(BigDecimal takeAwayRevenue) { this.takeAwayRevenue = takeAwayRevenue; }
} 