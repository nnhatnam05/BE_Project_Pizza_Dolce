package aptech.be.repositories;

import aptech.be.models.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    
    List<OrderItems> findByOrderId(Long orderId);
    
    @Query("SELECT oi FROM OrderItems oi WHERE oi.order.table.id = :tableId")
    List<OrderItems> findByTableId(@Param("tableId") Long tableId);
    
    @Query("SELECT oi FROM OrderItems oi JOIN FETCH oi.food JOIN FETCH oi.order WHERE oi.order.table.id = :tableId AND oi.order.status NOT IN :excludedStatuses")
    List<OrderItems> findByTableIdAndOrderStatusNotIn(@Param("tableId") Long tableId, @Param("excludedStatuses") List<String> excludedStatuses);
    
    @Query("SELECT oi.food.id, SUM(oi.quantity) FROM OrderItems oi WHERE oi.order.table.id = :tableId AND oi.order.status NOT IN :excludedStatuses GROUP BY oi.food.id")
    List<Object[]> findTotalQuantityByFoodAndTableId(@Param("tableId") Long tableId, @Param("excludedStatuses") List<String> excludedStatuses);
} 