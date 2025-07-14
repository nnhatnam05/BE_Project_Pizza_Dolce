package aptech.be.repositories.staff;

import aptech.be.models.staff.ShiftAssignment;
import aptech.be.models.staff.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {
    List<ShiftAssignment> findByStaffAndDate(StaffProfile staff, LocalDate date);

}