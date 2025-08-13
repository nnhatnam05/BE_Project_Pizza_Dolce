package aptech.be.services;

import aptech.be.dto.dashboard.*;

import java.util.List;

public interface DashboardService {
    
    /**
     * Get revenue analytics with time range filter
     * @param timeRange - today, week, month, year, all
     * @return RevenueAnalytics object
     */
    RevenueAnalytics getRevenueAnalytics(String timeRange);
    
    /**
     * Get customer statistics
     * @return CustomerStatistics object
     */
    CustomerStatistics getCustomerStatistics();
    
    /**
     * Get VIP customers list based on points
     * @param limit - number of customers to return
     * @return List of VIP customers
     */
    List<VIPCustomer> getVIPCustomers(int limit);
    
    /**
     * Get overall analytics summary
     * @return OverallAnalytics object
     */
    OverallAnalytics getOverallAnalytics();
} 