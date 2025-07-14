package aptech.be.controllers;

import aptech.be.models.Notification;
import aptech.be.services.NotificationService;
import aptech.be.repositories.UserRepository;
import aptech.be.models.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Autowired
    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // Lấy tất cả notification (có thể truyền unreadOnly)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(value = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly,
            Principal principal) {
        String username = principal.getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();

        List<Notification> list = unreadOnly ?
                notificationService.getUnreadNotifications(userId) :
                notificationService.getAllNotifications(userId);

        return ResponseEntity.ok(list);
    }

    // Đánh dấu đã đọc
    @PutMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id, Principal principal) {
        String username = principal.getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();

        Notification noti = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(noti);
    }
}
