package aptech.be.controllers.admin;

import aptech.be.dto.dashboard.*;
import aptech.be.models.OrderEntity;
import aptech.be.repositories.OrderRepository;
import aptech.be.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private OrderRepository orderRepository;

    /**
     * Get revenue analytics with time range filter
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueAnalytics> getRevenueAnalytics(
            @RequestParam(defaultValue = "month") String timeRange) {
        try {
            RevenueAnalytics analytics = dashboardService.getRevenueAnalytics(timeRange);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR] Failed to fetch revenue analytics: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get customer statistics
     */
    @GetMapping("/customers")
    public ResponseEntity<CustomerStatistics> getCustomerStatistics() {
        try {
            CustomerStatistics statistics = dashboardService.getCustomerStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR] Failed to fetch customer statistics: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get VIP customers list based on points
     */
    @GetMapping("/vip-customers")
    public ResponseEntity<List<VIPCustomer>> getVIPCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<VIPCustomer> vipCustomers = dashboardService.getVIPCustomers(limit);
            return ResponseEntity.ok(vipCustomers);
        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR] Failed to fetch VIP customers: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get overall analytics summary
     */
    @GetMapping("/analytics")
    public ResponseEntity<OverallAnalytics> getOverallAnalytics() {
        try {
            OverallAnalytics analytics = dashboardService.getOverallAnalytics();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            System.err.println("[DASHBOARD ERROR] Failed to fetch overall analytics: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/delivery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> debugDeliveryData() {
        try {
            Map<String, Object> debugInfo = new HashMap<>();
            
            // Get all orders with orderType = 'DELIVERY'
            List<OrderEntity> allDeliveryOrders = orderRepository.findByOrderType("DELIVERY");
            debugInfo.put("totalDeliveryOrders", allDeliveryOrders.size());
            
            // Get orders with deliveryStatus = 'DELIVERED'
            List<OrderEntity> deliveredOrders = orderRepository.findByDeliveryStatus("DELIVERED");
            debugInfo.put("totalDeliveredOrders", deliveredOrders.size());
            
            // Get orders with status = 'PAID' or 'COMPLETED'
            List<OrderEntity> paidOrders = orderRepository.findByStatusIn(Arrays.asList("PAID", "COMPLETED"));
            debugInfo.put("totalPaidOrders", paidOrders.size());
            
            // Sample order details
            if (!allDeliveryOrders.isEmpty()) {
                OrderEntity sampleOrder = allDeliveryOrders.get(0);
                Map<String, Object> sampleDetails = new HashMap<>();
                sampleDetails.put("id", sampleOrder.getId());
                sampleDetails.put("orderNumber", sampleOrder.getOrderNumber());
                sampleDetails.put("orderType", sampleOrder.getOrderType());
                sampleDetails.put("status", sampleOrder.getStatus());
                sampleDetails.put("deliveryStatus", sampleOrder.getDeliveryStatus());
                sampleDetails.put("confirmStatus", sampleOrder.getConfirmStatus());
                sampleDetails.put("totalPrice", sampleOrder.getTotalPrice());
                sampleDetails.put("createdAt", sampleOrder.getCreatedAt());
                debugInfo.put("sampleOrder", sampleDetails);
            }
            
            // Get revenue by different methods
            LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
            LocalDateTime now = LocalDateTime.now();
            
            BigDecimal revenueByDeliveryStatus = orderRepository.getRevenueByDeliveryStatus("DELIVERED", monthStart, now);
            BigDecimal revenueByOrderType = orderRepository.getRevenueByOrderTypeAndStatuses("DELIVERY", Arrays.asList("PAID", "COMPLETED"), monthStart, now);
            
            debugInfo.put("revenueByDeliveryStatus", revenueByDeliveryStatus);
            debugInfo.put("revenueByOrderType", revenueByOrderType);
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error debugging delivery data: " + e.getMessage());
        }
    }

    @GetMapping("/test/delivery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testDeliveryData() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test 1: Count all orders
            long totalOrders = orderRepository.count();
            result.put("totalOrders", totalOrders);
            
            // Test 2: Count orders by orderType
            List<OrderEntity> deliveryOrders = orderRepository.findByOrderType("DELIVERY");
            result.put("deliveryOrdersCount", deliveryOrders.size());
            
            List<OrderEntity> dineInOrders = orderRepository.findByOrderType("DINE_IN");
            result.put("dineInOrdersCount", dineInOrders.size());
            
            List<OrderEntity> takeAwayOrders = orderRepository.findByOrderType("TAKE_AWAY");
            result.put("takeAwayOrdersCount", takeAwayOrders.size());
            
            // Test 3: Sample delivery order details
            if (!deliveryOrders.isEmpty()) {
                OrderEntity sample = deliveryOrders.get(0);
                Map<String, Object> sampleDetails = new HashMap<>();
                sampleDetails.put("id", sample.getId());
                sampleDetails.put("orderNumber", sample.getOrderNumber());
                sampleDetails.put("orderType", sample.getOrderType());
                sampleDetails.put("status", sample.getStatus());
                sampleDetails.put("deliveryStatus", sample.getDeliveryStatus());
                sampleDetails.put("totalPrice", sample.getTotalPrice());
                result.put("sampleDeliveryOrder", sampleDetails);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test/delivery-new")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testNewDeliveryMethod() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test new delivery method
            LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
            LocalDateTime now = LocalDateTime.now();
            
            BigDecimal deliveryRevenue = orderRepository.getDeliveryRevenueByDeliveryStatus(monthStart, now);
            int deliveryOrders = orderRepository.getDeliveryOrderCountByDeliveryStatus(monthStart, now);
            
            result.put("deliveryRevenue", deliveryRevenue);
            result.put("deliveryOrders", deliveryOrders);
            result.put("dateRange", monthStart + " to " + now);
            
            // Also test old method for comparison
            BigDecimal oldDeliveryRevenue = orderRepository.getRevenueByDeliveryStatus("DELIVERED", monthStart, now);
            int oldDeliveryOrders = orderRepository.getOrderCountByDeliveryStatus("DELIVERED", monthStart, now);
            
            result.put("oldDeliveryRevenue", oldDeliveryRevenue);
            result.put("oldDeliveryOrders", oldDeliveryOrders);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test/vip-customers-simple")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testVIPCustomersSimple() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test VIP customers without order type details
            List<VIPCustomer> vipCustomers = dashboardService.getVIPCustomers(3); // Get top 3
            
            result.put("vipCustomers", vipCustomers);
            result.put("totalVIPCustomers", vipCustomers.size());
            
            // Test individual customer basic data
            if (!vipCustomers.isEmpty()) {
                List<Map<String, Object>> basicCustomers = new ArrayList<>();
                
                for (VIPCustomer customer : vipCustomers) {
                    Map<String, Object> customerDetail = new HashMap<>();
                    customerDetail.put("customerId", customer.getCustomerId());
                    customerDetail.put("fullName", customer.getFullName());
                    customerDetail.put("email", customer.getEmail());
                    customerDetail.put("points", customer.getPoints());
                    customerDetail.put("rank", customer.getRank());
                    customerDetail.put("totalSpent", customer.getTotalSpent());
                    customerDetail.put("totalOrders", customer.getTotalOrders());
                    customerDetail.put("lastOrderDate", customer.getLastOrderDate());
                    customerDetail.put("registrationDate", customer.getRegistrationDate());
                    
                    basicCustomers.add(customerDetail);
                }
                
                result.put("basicCustomers", basicCustomers);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test/all-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testAllData() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test all dashboard data (excluding website traffic)
            CustomerStatistics customerStats = dashboardService.getCustomerStatistics();
            List<VIPCustomer> vipCustomers = dashboardService.getVIPCustomers(3);
            
            result.put("customerStats", customerStats);
            result.put("vipCustomers", vipCustomers);
            
            // Test individual data points
            Map<String, Object> dataPoints = new HashMap<>();
            dataPoints.put("totalCustomers", customerStats.getTotalCustomers());
            dataPoints.put("vipCustomers", customerStats.getVipCustomers());
            dataPoints.put("averagePoints", customerStats.getAveragePointsPerCustomer());
            
            result.put("dataPoints", dataPoints);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
} 