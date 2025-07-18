package aptech.be.repositories;

import aptech.be.models.CustomerDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDetailRepository extends JpaRepository<CustomerDetail, Long> {
    CustomerDetail findByCustomerId(Long customerId);
}
