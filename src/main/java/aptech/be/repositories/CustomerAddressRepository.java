package aptech.be.repositories;

import aptech.be.models.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerDetailId(Long customerDetailId);
    Optional<CustomerAddress> findByCustomerDetailIdAndIsDefaultTrue(Long customerDetailId);
    void deleteByCustomerDetailIdAndIdNotIn(Long customerDetailId, List<Long> ids);
}
