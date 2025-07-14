package aptech.be.services;

import aptech.be.models.Notification;
import aptech.be.repositories.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepo;

    public NotificationService(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    // Lấy tất cả notification của user
    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepo.findByTargetUserIdOrderByCreatedAtDesc(userId);
    }

    // Lấy notification chưa đọc
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepo.findByTargetUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    // Đánh dấu đã đọc
    public Notification markAsRead(Long id, Long userId) {
        Notification noti = notificationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!noti.getTargetUserId().equals(userId)) {
            throw new RuntimeException("Not your notification!");
        }
        noti.setIsRead(true);
        return notificationRepo.save(noti);
    }
}
