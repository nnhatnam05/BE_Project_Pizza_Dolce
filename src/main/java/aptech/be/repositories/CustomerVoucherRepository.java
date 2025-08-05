package aptech.be.repositories;

import aptech.be.models.Customer;
import aptech.be.models.CustomerVoucher;
import aptech.be.models.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    
    // Lấy voucher của khách hàng (chưa sử dụng và chưa hết hạn)
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.customer.id = :customerId AND cv.isUsed = false AND cv.expiresAt > :now")
    List<CustomerVoucher> findValidVouchersByCustomerId(@Param("customerId") Long customerId, @Param("now") LocalDateTime now);
    
    // Lấy tất cả voucher của khách hàng
    List<CustomerVoucher> findByCustomerIdOrderByReceivedAtDesc(Long customerId);
    
    // Đếm số voucher khách hàng đang có (chưa sử dụng, chưa hết hạn)
    @Query("SELECT COUNT(cv) FROM CustomerVoucher cv WHERE cv.customer.id = :customerId AND cv.isUsed = false AND cv.expiresAt > :now")
    Long countValidVouchersByCustomerId(@Param("customerId") Long customerId, @Param("now") LocalDateTime now);
    
    // Kiểm tra khách hàng đã có voucher này chưa
    boolean existsByCustomerAndVoucherAndIsUsedFalse(Customer customer, Voucher voucher);
    
    // Tìm voucher của customer
    Optional<CustomerVoucher> findByCustomerAndVoucher(Customer customer, Voucher voucher);
    
    // Tìm voucher hết hạn
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.expiresAt < :now AND cv.isUsed = false")
    List<CustomerVoucher> findExpiredVouchers(@Param("now") LocalDateTime now);
    
    // Tìm voucher đã sử dụng
    List<CustomerVoucher> findByIsUsedTrueOrderByUsedAtDesc();
    
    // Tìm voucher của khách hàng theo code
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.customer.id = :customerId AND cv.voucher.code = :code AND cv.isUsed = false AND cv.expiresAt > :now")
    Optional<CustomerVoucher> findValidVoucherByCustomerIdAndCode(@Param("customerId") Long customerId, @Param("code") String code, @Param("now") LocalDateTime now);
} 