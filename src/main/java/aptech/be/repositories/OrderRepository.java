package aptech.be.repositories;

import aptech.be.models.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity,Long> {
    List<OrderEntity> findByStatus(String status);
    List<OrderEntity> findByTableId(Long tableId);
}
