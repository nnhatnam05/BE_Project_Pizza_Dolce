package aptech.be.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class RevenueAnalytics {
    private BigDecimal totalRevenue;
    private BigDecimal deliveryRevenue;
    private BigDecimal dineInRevenue;
    private BigDecimal takeAwayRevenue;
    private String timeRange;
    private List<RevenueDataPoint> revenueOverTime;
    private Map<String, BigDecimal> revenueByOrderType;
    private BigDecimal revenueGrowth;
    private BigDecimal averageOrderValue;
    private int totalOrders;
    private int deliveryOrders;
    private int dineInOrders;
    private int takeAwayOrders;

    // Constructor
    public RevenueAnalytics() {}

    public RevenueAnalytics(BigDecimal totalRevenue, BigDecimal deliveryRevenue, BigDecimal dineInRevenue, 
                           BigDecimal takeAwayRevenue, String timeRange, List<RevenueDataPoint> revenueOverTime,
                           Map<String, BigDecimal> revenueByOrderType, BigDecimal revenueGrowth, 
                           BigDecimal averageOrderValue, int totalOrders, int deliveryOrders, 
                           int dineInOrders, int takeAwayOrders) {
        this.totalRevenue = totalRevenue;
        this.deliveryRevenue = deliveryRevenue;
        this.dineInRevenue = dineInRevenue;
        this.takeAwayRevenue = takeAwayRevenue;
        this.timeRange = timeRange;
        this.revenueOverTime = revenueOverTime;
        this.revenueByOrderType = revenueByOrderType;
        this.revenueGrowth = revenueGrowth;
        this.averageOrderValue = averageOrderValue;
        this.totalOrders = totalOrders;
        this.deliveryOrders = deliveryOrders;
        this.dineInOrders = dineInOrders;
        this.takeAwayOrders = takeAwayOrders;
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public BigDecimal getDeliveryRevenue() { return deliveryRevenue; }
    public void setDeliveryRevenue(BigDecimal deliveryRevenue) { this.deliveryRevenue = deliveryRevenue; }
    
    public BigDecimal getDineInRevenue() { return dineInRevenue; }
    public void setDineInRevenue(BigDecimal dineInRevenue) { this.dineInRevenue = dineInRevenue; }
    
    public BigDecimal getTakeAwayRevenue() { return takeAwayRevenue; }
    public void setTakeAwayRevenue(BigDecimal takeAwayRevenue) { this.takeAwayRevenue = takeAwayRevenue; }
    
    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
    
    public List<RevenueDataPoint> getRevenueOverTime() { return revenueOverTime; }
    public void setRevenueOverTime(List<RevenueDataPoint> revenueOverTime) { this.revenueOverTime = revenueOverTime; }
    
    public Map<String, BigDecimal> getRevenueByOrderType() { return revenueByOrderType; }
    public void setRevenueByOrderType(Map<String, BigDecimal> revenueByOrderType) { this.revenueByOrderType = revenueByOrderType; }
    
    public BigDecimal getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(BigDecimal revenueGrowth) { this.revenueGrowth = revenueGrowth; }
    
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
    
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    
    public int getDeliveryOrders() { return deliveryOrders; }
    public void setDeliveryOrders(int deliveryOrders) { this.deliveryOrders = deliveryOrders; }
    
    public int getDineInOrders() { return dineInOrders; }
    public void setDineInOrders(int dineInOrders) { this.dineInOrders = dineInOrders; }
    
    public int getTakeAwayOrders() { return takeAwayOrders; }
    public void setTakeAwayOrders(int takeAwayOrders) { this.takeAwayOrders = takeAwayOrders; }
} 