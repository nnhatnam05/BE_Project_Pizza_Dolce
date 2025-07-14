package aptech.be.repositories.staff;

import aptech.be.models.staff.AttendanceRecord;
import aptech.be.models.staff.ShiftAssignment;
import aptech.be.models.staff.StaffProfile;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByStaffAndDate(StaffProfile staff, LocalDate date);

    List<AttendanceRecord> findByStaffAndDateBetween(StaffProfile staff, LocalDate dateAfter, LocalDate dateBefore);

    @Modifying
    @Transactional
    @Query("DELETE FROM AttendanceRecord a WHERE a.staff.id = :staffId")
    void deleteByStaffId(@Param("staffId") Long staffId);

}




