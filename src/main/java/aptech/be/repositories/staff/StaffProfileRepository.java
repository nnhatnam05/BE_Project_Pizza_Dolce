package aptech.be.repositories.staff;

import aptech.be.models.UserEntity;
import aptech.be.models.staff.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByStaffCode(String staffCode);
    Optional<StaffProfile> findByUser(UserEntity user);

}
