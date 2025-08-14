package aptech.be.repositories;

import aptech.be.models.Customer;
import aptech.be.models.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity,Long> {
    List<OrderEntity> findByStatus(String status);
    List<OrderEntity> findByCustomerId(Long customerId);
    List<OrderEntity> findByConfirmStatus(String confirmStatus);
    List<OrderEntity> findByCustomerIdAndConfirmStatus(Long customerId, String confirmStatus);


    Optional<OrderEntity> findFirstByCustomerAndConfirmStatus(Customer customer, String confirmStatus);

    List<OrderEntity> findByStatusAndCreatedAtBefore(String status, LocalDateTime before);

    List<OrderEntity> findByCustomerIdAndStatus(Long customerId, String status);



    @Query("SELECT o FROM OrderEntity o WHERE o.shipper.id = :shipperId AND o.status = :status")
    List<OrderEntity> findByShipperIdAndStatus(@Param("shipperId") Long shipperId, @Param("status") String status);
    
    // Dine-in specific methods
    List<OrderEntity> findByTableIdAndStatusNotIn(Long tableId, List<String> statuses);
    List<OrderEntity> findByTableIdOrderByCreatedAtDesc(Long tableId);
    List<OrderEntity> findByOrderTypeAndStatus(String orderType, String status);
    List<OrderEntity> findByOrderType(String orderType);

    @Query("SELECT o FROM OrderEntity o WHERE o.shipper.id = :shipperId AND o.deliveryStatus = :deliveryStatus")
    List<OrderEntity> findByShipperIdAndDeliveryStatus(@Param("shipperId") Long shipperId, @Param("deliveryStatus") String deliveryStatus);

    @Query("SELECT o FROM OrderEntity o WHERE o.deliveryStatus = :deliveryStatus AND o.shipper IS NULL")
    List<OrderEntity> findByDeliveryStatusAndShipperIsNull(@Param("deliveryStatus") String deliveryStatus);

    @Query("SELECT o FROM OrderEntity o WHERE o.shipper.id = :shipperId AND o.deliveryStatus IN ('PREPARING', 'WAITING_FOR_SHIPPER', 'DELIVERING')")
    List<OrderEntity> findActiveOrdersByShipper(@Param("shipperId") Long shipperId);
    
    // Take-away specific methods
    List<OrderEntity> findByOrderTypeAndStatusIn(String orderType, List<String> statuses);
    
    // Admin order management methods
    List<OrderEntity> findByOrderTypeOrderByCreatedAtDesc(String orderType);
    List<OrderEntity> findByStatusOrderByCreatedAtDesc(String status);
    
    @Query("SELECT o FROM OrderEntity o WHERE o.staff.id = :staffId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByStaffIdOrderByCreatedAtDesc(@Param("staffId") Long staffId);
    
    // Dashboard Analytics Methods (inclusive totals)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED') AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED') AND o.createdAt BETWEEN :startDate AND :endDate")
    int getOrderCountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED')")
    BigDecimal getTotalRevenueAllTime();

    // Delivery-only analytics
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.orderType = 'DELIVERY' AND o.deliveryStatus = :deliveryStatus AND o.confirmStatus = 'PAID' AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueByDeliveryStatus(@Param("deliveryStatus") String deliveryStatus,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.orderType = 'DELIVERY' AND o.deliveryStatus = :deliveryStatus AND o.confirmStatus = 'PAID' AND o.createdAt BETWEEN :startDate AND :endDate")
    int getOrderCountByDeliveryStatus(@Param("deliveryStatus") String deliveryStatus,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Dine-in / Take-away analytics by order status list (PAID/COMPLETED)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.orderType = :orderType AND o.status IN :statuses AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueByOrderTypeAndStatuses(@Param("orderType") String orderType,
                                                @Param("statuses") List<String> statuses,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.orderType = :orderType AND o.status IN :statuses AND o.createdAt BETWEEN :startDate AND :endDate")
    int getOrderCountByOrderTypeAndStatuses(@Param("orderType") String orderType,
                                            @Param("statuses") List<String> statuses,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // New customers since date based on first order time
    @Query("SELECT COUNT(DISTINCT o.customer) FROM OrderEntity o WHERE o.createdAt > :since")
    long getNewCustomerCountSince(@Param("since") LocalDateTime since);

    // Last customer order date
    @Query("SELECT MAX(o.createdAt) FROM OrderEntity o")
    LocalDateTime getLastCustomerOrderDate();

    // Debug methods for delivery orders
    List<OrderEntity> findByDeliveryStatus(String deliveryStatus);
    
    List<OrderEntity> findByStatusIn(List<String> statuses);

    // New method: Get delivery orders by delivery_status = 'DELIVERED' (regardless of order_type)
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.deliveryStatus = 'DELIVERED' AND o.confirmStatus = 'PAID' AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getDeliveryRevenueByDeliveryStatus(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.deliveryStatus = 'DELIVERED' AND o.confirmStatus = 'PAID' AND o.createdAt BETWEEN :startDate AND :endDate")
    int getDeliveryOrderCountByDeliveryStatus(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // New method specifically for delivery orders with correct conditions
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.orderType = 'DELIVERY' AND o.confirmStatus = 'PAID' AND o.deliveryStatus = 'DELIVERED' AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getDeliveryRevenueWithCorrectConditions(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.orderType = 'DELIVERY' AND o.confirmStatus = 'PAID' AND o.deliveryStatus = 'DELIVERED' AND o.createdAt BETWEEN :startDate AND :endDate")
    int getDeliveryOrderCountWithCorrectConditions(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Customer-specific analytics methods
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM OrderEntity o WHERE o.customer.id = :customerId AND (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED')")
    BigDecimal getTotalSpentByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.customer.id = :customerId AND (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED')")
    int getOrderCountByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT MAX(o.createdAt) FROM OrderEntity o WHERE o.customer.id = :customerId AND (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED')")
    LocalDateTime getLastOrderDateByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT MIN(o.createdAt) FROM OrderEntity o WHERE o.customer.id = :customerId AND (o.deliveryStatus = 'DELIVERED' OR o.status IN ('PAID','COMPLETED') OR o.confirmStatus = 'CONFIRMED')")
    LocalDateTime getFirstOrderDateByCustomer(@Param("customerId") Long customerId);
}
