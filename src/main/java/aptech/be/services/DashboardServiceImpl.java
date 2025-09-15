package aptech.be.services;

import aptech.be.dto.dashboard.*;
import aptech.be.repositories.CustomerRepository;
import aptech.be.repositories.OrderRepository;
import aptech.be.repositories.FoodRepository;
import aptech.be.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public RevenueAnalytics getRevenueAnalytics(String timeRange) {
        LocalDateTime startDate = getStartDate(timeRange);
        LocalDateTime endDate = LocalDateTime.now();

        System.out.println("[DashboardService] Getting revenue analytics for timeRange: " + timeRange);
        System.out.println("[DashboardService] Date range: " + startDate + " to " + endDate);

        // Completed filters
        List<String> completedStatuses = Arrays.asList("PAID", "COMPLETED");

        // Totals (completed orders only)
        BigDecimal totalRevenue = orderRepository.getTotalRevenueByDateRange(startDate, endDate);
        int totalOrders = orderRepository.getOrderCountByDateRange(startDate, endDate);

        System.out.println("[DashboardService] Total revenue: " + totalRevenue + ", Total orders: " + totalOrders);

        // Delivery: tính theo đơn đã giao xong; PayOS = confirmStatus PAID, Cash = paymentMethod CASH
        BigDecimal deliveryRevenue = orderRepository.getDeliveryRevenueByDeliveryStatus(startDate, endDate);
        int deliveryOrders = orderRepository.getDeliveryOrderCountByDeliveryStatus(startDate, endDate);
        
        System.out.println("[DashboardService] Delivery orders (confirmStatus=PAID AND deliveryStatus=DELIVERED) - Revenue: " + deliveryRevenue + ", Orders: " + deliveryOrders);
        
        // Fallback: try other delivery statuses if DELIVERED returns 0
        if (deliveryRevenue == null || deliveryRevenue.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("[DashboardService] No DELIVERED orders found, trying other delivery statuses");
            
            // Try to get any delivery orders with different statuses
            BigDecimal alternativeDeliveryRevenue = orderRepository.getRevenueByDeliveryStatus("COMPLETED", startDate, endDate);
            int alternativeDeliveryOrders = orderRepository.getOrderCountByDeliveryStatus("COMPLETED", startDate, endDate);
            
            if (alternativeDeliveryRevenue != null && alternativeDeliveryRevenue.compareTo(BigDecimal.ZERO) > 0) {
                deliveryRevenue = alternativeDeliveryRevenue;
                deliveryOrders = alternativeDeliveryOrders;
                System.out.println("[DashboardService] Found delivery orders with COMPLETED status: " + deliveryOrders);
            }
            
            // Try to get delivery orders by orderType = 'DELIVERY' regardless of deliveryStatus
            System.out.println("[DashboardService] Trying to get all DELIVERY orders regardless of status...");
            try {
                // Get all orders with orderType = 'DELIVERY' and completed status
                BigDecimal allDeliveryRevenue = orderRepository.getRevenueByOrderTypeAndStatuses("DELIVERY", completedStatuses, startDate, endDate);
                int allDeliveryOrders = orderRepository.getOrderCountByOrderTypeAndStatuses("DELIVERY", completedStatuses, startDate, endDate);
                
                System.out.println("[DashboardService] All DELIVERY orders (any completed status): Revenue=" + allDeliveryRevenue + ", Orders=" + allDeliveryOrders);
                
                if (allDeliveryRevenue != null && allDeliveryRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    deliveryRevenue = allDeliveryRevenue;
                    deliveryOrders = allDeliveryOrders;
                    System.out.println("[DashboardService] Using DELIVERY orders with completed status");
                }
            } catch (Exception e) {
                System.err.println("[DashboardService] Error getting DELIVERY orders: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[DashboardService] Final delivery revenue: " + deliveryRevenue + ", Final delivery orders: " + deliveryOrders);

        BigDecimal dineInRevenue = orderRepository.getRevenueByOrderTypeAndStatuses("DINE_IN", completedStatuses, startDate, endDate);
        int dineInOrders = orderRepository.getOrderCountByOrderTypeAndStatuses("DINE_IN", completedStatuses, startDate, endDate);

        BigDecimal takeAwayRevenue = orderRepository.getRevenueByOrderTypeAndStatuses("TAKE_AWAY", completedStatuses, startDate, endDate);
        int takeAwayOrders = orderRepository.getOrderCountByOrderTypeAndStatuses("TAKE_AWAY", completedStatuses, startDate, endDate);

        System.out.println("[DashboardService] Dine-in: " + dineInRevenue + " (" + dineInOrders + " orders)");
        System.out.println("[DashboardService] Take-away: " + takeAwayRevenue + " (" + takeAwayOrders + " orders)");

        // Average order value
        BigDecimal averageOrderValue = totalOrders > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Revenue over time
        List<RevenueDataPoint> revenueOverTime = getRevenueOverTime(startDate, endDate, timeRange);

        // Revenue by order type map
        Map<String, BigDecimal> revenueByOrderType = new HashMap<>();
        revenueByOrderType.put("DELIVERY", deliveryRevenue != null ? deliveryRevenue : BigDecimal.ZERO);
        revenueByOrderType.put("DINE_IN", dineInRevenue != null ? dineInRevenue : BigDecimal.ZERO);
        revenueByOrderType.put("TAKE_AWAY", takeAwayRevenue != null ? takeAwayRevenue : BigDecimal.ZERO);

        BigDecimal revenueGrowth = calculateRevenueGrowth(timeRange);

        return new RevenueAnalytics(
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                deliveryRevenue != null ? deliveryRevenue : BigDecimal.ZERO,
                dineInRevenue != null ? dineInRevenue : BigDecimal.ZERO,
                takeAwayRevenue != null ? takeAwayRevenue : BigDecimal.ZERO,
                timeRange,
                revenueOverTime,
                revenueByOrderType,
                revenueGrowth,
                averageOrderValue,
                totalOrders,
                deliveryOrders,
                dineInOrders,
                takeAwayOrders
        );
    }

    @Override
    public CustomerStatistics getCustomerStatistics() {
        // Get total customers
        long totalCustomers = customerRepository.count();

        // Không có trường createdAt trong Customer → sử dụng thống kê từ OrderEntity
        LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        long newCustomersThisMonth = orderRepository.getNewCustomerCountSince(monthStart);

        LocalDateTime weekStart = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).with(LocalTime.MIN);
        long newCustomersThisWeek = orderRepository.getNewCustomerCountSince(weekStart);

        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        long newCustomersToday = orderRepository.getNewCustomerCountSince(todayStart);

        long vipCustomers = customerRepository.countVIPCustomers();

        double averagePointsPerCustomer = customerRepository.getAveragePointsPerCustomer();

        LocalDateTime lastCustomerRegistration = orderRepository.getLastCustomerOrderDate();

        // For now, set website visits to 0 (would need separate tracking system)
        long totalWebsiteVisits = 0;
        long uniqueVisitors = 0;

        return new CustomerStatistics(
                totalCustomers,
                newCustomersThisMonth,
                newCustomersThisWeek,
                newCustomersToday,
                vipCustomers,
                averagePointsPerCustomer,
                lastCustomerRegistration,
                totalWebsiteVisits,
                uniqueVisitors
        );
    }

    @Override
    public List<VIPCustomer> getVIPCustomers(int limit) {
        return customerRepository.findAllWithDetail().stream()
            .sorted((a, b) -> Integer.compare(parsePoint(b.getCustomerDetail()), parsePoint(a.getCustomerDetail())))
            .limit(limit)
            .map(c -> {
                // Calculate actual values for each customer
                Long customerId = c.getId();
                
                // Get total spent (sum of all completed orders)
                BigDecimal totalSpent = orderRepository.getTotalSpentByCustomer(customerId);
                
                // Get total orders count
                int totalOrders = orderRepository.getOrderCountByCustomer(customerId);
                
                // Get last order date
                LocalDateTime lastOrderDate = orderRepository.getLastOrderDateByCustomer(customerId);
                
                // Get registration date (first order date)
                LocalDateTime registrationDate = orderRepository.getFirstOrderDateByCustomer(customerId);
                
                return new Object[]{
                    customerId,
                    c.getFullName(),
                    c.getEmail(),
                    c.getCustomerDetail() != null ? c.getCustomerDetail().getPhoneNumber() : null,
                    parsePoint(c.getCustomerDetail()),
                    totalSpent != null ? totalSpent : BigDecimal.ZERO,
                    totalOrders,
                    lastOrderDate,
                    registrationDate,
                    getRank(parsePoint(c.getCustomerDetail())) // Add rank calculation
                };
            })
            .map(this::mapToVIPCustomer)
            .collect(Collectors.toList());
    }

    @Override
    public OverallAnalytics getOverallAnalytics() {
        // Get total revenue (all time)
        BigDecimal totalRevenue = orderRepository.getTotalRevenueAllTime();

        // Get total orders
        long totalOrders = orderRepository.count();

        // Get total customers
        long totalCustomers = customerRepository.count();

        // Calculate average order value
        BigDecimal averageOrderValue = totalOrders > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;

        // Get total products
        long totalProducts = foodRepository.count();
        long activeProducts = foodRepository.countByStatus("AVAILABLE");

        // Get total staff and shippers
        long totalStaff = userRepository.countByRole("STAFF");
        long totalShippers = userRepository.countByRole("SHIPPER");

        // Calculate growth rates (simplified)
        double customerGrowthRate = calculateCustomerGrowthRate();
        double revenueGrowthRate = calculateRevenueGrowthRate();

        // For now, set website metrics to 0
        long totalWebsiteVisits = 0;
        long uniqueVisitors = 0;

        return new OverallAnalytics(
            totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
            totalOrders,
            totalCustomers,
            averageOrderValue,
            customerGrowthRate,
            revenueGrowthRate,
            totalWebsiteVisits,
            uniqueVisitors,
            totalProducts,
            activeProducts,
            totalStaff,
            totalShippers
        );
    }

    // Helper methods
    private LocalDateTime getStartDate(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeRange.toLowerCase()) {
            case "today":
                return now.with(LocalTime.MIN);
            case "week":
                return now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).with(LocalTime.MIN);
            case "month":
                return now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
            case "year":
                return now.with(TemporalAdjusters.firstDayOfYear()).with(LocalTime.MIN);
            case "all":
                return LocalDateTime.of(2020, 1, 1, 0, 0); // Arbitrary start date
            default:
                return now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        }
    }

    private List<RevenueDataPoint> getRevenueOverTime(LocalDateTime startDate, LocalDateTime endDate, String timeRange) {
        // This would be implemented with actual database queries
        // For now, return mock data
        List<RevenueDataPoint> dataPoints = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            dataPoints.add(new RevenueDataPoint(
                current,
                BigDecimal.valueOf(Math.random() * 1000 + 500),
                (int) (Math.random() * 20 + 5),
                BigDecimal.valueOf(Math.random() * 400 + 200),
                BigDecimal.valueOf(Math.random() * 300 + 150),
                BigDecimal.valueOf(Math.random() * 300 + 150)
            ));
            current = current.plusDays(1);
        }

        return dataPoints;
    }

    private BigDecimal calculateRevenueGrowth(String timeRange) {
        // Simplified calculation - in real implementation would compare with previous period
        return BigDecimal.valueOf(Math.random() * 20 - 10); // -10% to +10%
    }

    private double calculateCustomerGrowthRate() {
        // Simplified calculation
        return Math.random() * 15 - 5; // -5% to +10%
    }

    private double calculateRevenueGrowthRate() {
        // Simplified calculation
        return Math.random() * 25 - 10; // -10% to +15%
    }

    private VIPCustomer mapToVIPCustomer(Object[] result) {
        // This would map database query results to VIPCustomer object
        // For now, return mock data
        return new VIPCustomer(
            (Long) result[0],
            (String) result[1],
            (String) result[2],
            (String) result[3],
            (Integer) result[4],
            (BigDecimal) result[5],
            (Integer) result[6],
            (LocalDateTime) result[7],
            (LocalDateTime) result[8],
            (String) result[9] // Add rank
        );
    }

    private String getRank(int points) {
        if (points >= 1000) return "GOLD";
        if (points >= 500) return "SILVER";
        return "BRONZE";
    }

    private int parsePoint(aptech.be.models.CustomerDetail detail) {
        if (detail == null || detail.getPoint() == null) return 0;
        try {
            return Integer.parseInt(detail.getPoint());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
} 