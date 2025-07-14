package aptech.be.repositories;

import aptech.be.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Lấy notification của user (mới nhất trước)
    List<Notification> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    // Lấy notification chưa đọc
    List<Notification> findByTargetUserIdAndIsReadFalseOrderByCreatedAtDesc(Long targetUserId);
}
