package aptech.be.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ai nhận thông báo (có thể là admin hoặc staff)
    private Long targetUserId;

    // Nội dung thông báo
    private String message;

    // Dùng để xác định loại thông báo
    private String type; // Ví dụ: REQUEST_NEW, REQUEST_CONFIRMED

    // Thông tin bổ sung, có thể chứa id request, v.v.
    private String data;

    // Đã đọc hay chưa
    private Boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters, setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
