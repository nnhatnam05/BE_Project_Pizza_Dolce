package aptech.be.repositories;

import aptech.be.models.ComplaintCase;
import aptech.be.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintCaseRepository extends JpaRepository<ComplaintCase, Long> {
    List<ComplaintCase> findByCustomerId(Long customerId);
    List<ComplaintCase> findByAssignedStaff(UserEntity staff);
    List<ComplaintCase> findByStatus(String status);
    List<ComplaintCase> findByStatusIn(List<String> statuses);
    List<ComplaintCase> findByAssignedStaffIsNull();
    long countByOrderIdAndCustomerIdAndStatusIn(Long orderId, Long customerId, List<String> statuses);
}


