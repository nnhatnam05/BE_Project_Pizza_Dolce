package aptech.be.repositories.staff;

import aptech.be.models.staff.StaffRequest;
import aptech.be.models.staff.StaffProfile;
import aptech.be.models.staff.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRequestRepository extends JpaRepository<StaffRequest, Long> {
    // Lấy tất cả request của 1 nhân viên
    List<StaffRequest> findByStaff(StaffProfile staff);

    // Lấy tất cả request có status cụ thể (cho admin lọc)
    List<StaffRequest> findByStatus(RequestStatus status);
}
