package aptech.be.dto.dashboard;

import java.math.BigDecimal;

public class OverallAnalytics {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalCustomers;
    private BigDecimal averageOrderValue;
    private double customerGrowthRate;
    private double revenueGrowthRate;
    private long totalWebsiteVisits;
    private long uniqueVisitors;
    private long totalProducts;
    private long activeProducts;
    private long totalStaff;
    private long totalShippers;

    // Constructor
    public OverallAnalytics() {}

    public OverallAnalytics(BigDecimal totalRevenue, long totalOrders, long totalCustomers,
                           BigDecimal averageOrderValue, double customerGrowthRate, double revenueGrowthRate,
                           long totalWebsiteVisits, long uniqueVisitors, long totalProducts,
                           long activeProducts, long totalStaff, long totalShippers) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalCustomers = totalCustomers;
        this.averageOrderValue = averageOrderValue;
        this.customerGrowthRate = customerGrowthRate;
        this.revenueGrowthRate = revenueGrowthRate;
        this.totalWebsiteVisits = totalWebsiteVisits;
        this.uniqueVisitors = uniqueVisitors;
        this.totalProducts = totalProducts;
        this.activeProducts = activeProducts;
        this.totalStaff = totalStaff;
        this.totalShippers = totalShippers;
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
    
    public double getCustomerGrowthRate() { return customerGrowthRate; }
    public void setCustomerGrowthRate(double customerGrowthRate) { this.customerGrowthRate = customerGrowthRate; }
    
    public double getRevenueGrowthRate() { return revenueGrowthRate; }
    public void setRevenueGrowthRate(double revenueGrowthRate) { this.revenueGrowthRate = revenueGrowthRate; }
    
    public long getTotalWebsiteVisits() { return totalWebsiteVisits; }
    public void setTotalWebsiteVisits(long totalWebsiteVisits) { this.totalWebsiteVisits = totalWebsiteVisits; }
    
    public long getUniqueVisitors() { return uniqueVisitors; }
    public void setUniqueVisitors(long uniqueVisitors) { this.uniqueVisitors = uniqueVisitors; }
    
    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
    
    public long getActiveProducts() { return activeProducts; }
    public void setActiveProducts(long activeProducts) { this.activeProducts = activeProducts; }
    
    public long getTotalStaff() { return totalStaff; }
    public void setTotalStaff(long totalStaff) { this.totalStaff = totalStaff; }
    
    public long getTotalShippers() { return totalShippers; }
    public void setTotalShippers(long totalShippers) { this.totalShippers = totalShippers; }
} 