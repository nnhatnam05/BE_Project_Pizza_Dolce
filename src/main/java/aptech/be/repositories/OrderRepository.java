package aptech.be.repositories;

import aptech.be.models.Customer;
import aptech.be.models.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
