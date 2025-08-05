package aptech.be.repositories;

import aptech.be.models.Customer;
import aptech.be.models.OrderEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
