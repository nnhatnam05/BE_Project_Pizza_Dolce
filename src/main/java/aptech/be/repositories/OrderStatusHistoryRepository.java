package aptech.be.repositories;

import aptech.be.models.OrderStatusHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);
    @Modifying
    @Transactional
    @Query("DELETE FROM OrderStatusHistory o WHERE o.order.id = :orderId")
    void deleteByOrderId(Long orderId);
}
