package aptech.be.repositories;

import aptech.be.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    List<Customer> findByIsActive(Boolean isActive); // Thêm method tìm theo active status
    
    // Dashboard Analytics Methods
    @Query("SELECT COUNT(c) FROM Customer c WHERE EXISTS (SELECT 1 FROM CustomerDetail cd WHERE cd.customer = c AND CAST(cd.point AS integer) > 100)")
    long countVIPCustomers();
    
    @Query("SELECT COALESCE(AVG(CAST(cd.point AS double)), 0) FROM CustomerDetail cd")
    double getAveragePointsPerCustomer();
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.customerDetail")
    List<Customer> findAllWithDetail();
}
