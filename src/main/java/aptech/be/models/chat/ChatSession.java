package aptech.be.models.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "session_id", unique = true, nullable = false, length = 64)
	private String sessionId;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "language", length = 8)
	private String language;

	@Column(name = "status", length = 32)
	private String status; // active, ended, handed_over

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	@Column(name = "last_activity_at")
	private LocalDateTime lastActivityAt;

	@Column(name = "rating")
	private Integer rating; // 1-5 nullable

	@Column(name = "rating_note", columnDefinition = "TEXT")
	private String ratingNote;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	public String getLanguage() { return language; }
	public void setLanguage(String language) { this.language = language; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getEndedAt() { return endedAt; }
	public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
	public LocalDateTime getLastActivityAt() { return lastActivityAt; }
	public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
	public Integer getRating() { return rating; }
	public void setRating(Integer rating) { this.rating = rating; }
	public String getRatingNote() { return ratingNote; }
	public void setRatingNote(String ratingNote) { this.ratingNote = ratingNote; }
} 