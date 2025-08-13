package aptech.be.dto.dashboard;

import java.time.LocalDateTime;

public class CustomerStatistics {
    private long totalCustomers;
    private long newCustomersThisMonth;
    private long newCustomersThisWeek;
    private long newCustomersToday;
    private long vipCustomers;
    private double averagePointsPerCustomer;
    private LocalDateTime lastCustomerRegistration;
    private long totalWebsiteVisits;
    private long uniqueVisitors;

    // Constructor
    public CustomerStatistics() {}

    public CustomerStatistics(long totalCustomers, long newCustomersThisMonth, long newCustomersThisWeek,
                            long newCustomersToday, long vipCustomers,
                            double averagePointsPerCustomer, LocalDateTime lastCustomerRegistration,
                            long totalWebsiteVisits, long uniqueVisitors) {
        this.totalCustomers = totalCustomers;
        this.newCustomersThisMonth = newCustomersThisMonth;
        this.newCustomersThisWeek = newCustomersThisWeek;
        this.newCustomersToday = newCustomersToday;
        this.vipCustomers = vipCustomers;
        this.averagePointsPerCustomer = averagePointsPerCustomer;
        this.lastCustomerRegistration = lastCustomerRegistration;
        this.totalWebsiteVisits = totalWebsiteVisits;
        this.uniqueVisitors = uniqueVisitors;
    }

    // Getters and Setters
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    
    public long getNewCustomersThisMonth() { return newCustomersThisMonth; }
    public void setNewCustomersThisMonth(long newCustomersThisMonth) { this.newCustomersThisMonth = newCustomersThisMonth; }
    
    public long getNewCustomersThisWeek() { return newCustomersThisWeek; }
    public void setNewCustomersThisWeek(long newCustomersThisWeek) { this.newCustomersThisWeek = newCustomersThisWeek; }
    
    public long getNewCustomersToday() { return newCustomersToday; }
    public void setNewCustomersToday(long newCustomersToday) { this.newCustomersToday = newCustomersToday; }
    
    public long getVipCustomers() { return vipCustomers; }
    public void setVipCustomers(long vipCustomers) { this.vipCustomers = vipCustomers; }
    
    public double getAveragePointsPerCustomer() { return averagePointsPerCustomer; }
    public void setAveragePointsPerCustomer(double averagePointsPerCustomer) { this.averagePointsPerCustomer = averagePointsPerCustomer; }
    
    public LocalDateTime getLastCustomerRegistration() { return lastCustomerRegistration; }
    public void setLastCustomerRegistration(LocalDateTime lastCustomerRegistration) { this.lastCustomerRegistration = lastCustomerRegistration; }
    
    public long getTotalWebsiteVisits() { return totalWebsiteVisits; }
    public void setTotalWebsiteVisits(long totalWebsiteVisits) { this.totalWebsiteVisits = totalWebsiteVisits; }
    
    public long getUniqueVisitors() { return uniqueVisitors; }
    public void setUniqueVisitors(long uniqueVisitors) { this.uniqueVisitors = uniqueVisitors; }
} 