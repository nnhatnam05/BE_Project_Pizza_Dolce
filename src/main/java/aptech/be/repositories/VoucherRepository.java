package aptech.be.repositories;

import aptech.be.models.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    // Tìm voucher theo code
    Optional<Voucher> findByCode(String code);
    
    // Tìm voucher theo code và đang active
    Optional<Voucher> findByCodeAndIsActiveTrue(String code);
    
    // Lấy tất cả voucher public và đang active
    @Query("SELECT v FROM Voucher v WHERE v.isPublic = true AND v.isActive = true AND (v.expiresAt IS NULL OR v.expiresAt > :now)")
    List<Voucher> findPublicActiveVouchers(@Param("now") LocalDateTime now);
    
    // Lấy voucher đang active
    List<Voucher> findByIsActiveTrueOrderByCreatedAtDesc();
    
    // Tìm voucher hết hạn
    @Query("SELECT v FROM Voucher v WHERE v.expiresAt IS NOT NULL AND v.expiresAt < :now")
    List<Voucher> findExpiredVouchers(@Param("now") LocalDateTime now);
    
    // Tìm voucher đã hết số lượng
    @Query("SELECT v FROM Voucher v WHERE v.usedQuantity >= v.totalQuantity")
    List<Voucher> findFullyUsedVouchers();
    
    // Kiểm tra code đã tồn tại chưa
    boolean existsByCode(String code);
    
    // Tìm voucher theo người tạo
    List<Voucher> findByCreatedByOrderByCreatedAtDesc(String createdBy);
} 