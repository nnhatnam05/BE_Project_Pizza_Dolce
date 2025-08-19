package aptech.be.models.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "session_id", nullable = false, length = 64)
	private String sessionId;

	@Column(name = "sender", length = 16)
	private String sender; // user | bot | agent

	@Column(name = "sender_name", length = 128)
	private String senderName; // lưu tên hiển thị của người gửi

	@Column(name = "intent", length = 64)
	private String intent; // optional for user message after NLU

	@Column(name = "content_raw", columnDefinition = "TEXT")
	private String contentRaw; // optional, can be null by privacy policy

	@Column(name = "content_masked", columnDefinition = "TEXT")
	private String contentMasked;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public String getSender() { return sender; }
	public void setSender(String sender) { this.sender = sender; }
	public String getSenderName() { return senderName; }
	public void setSenderName(String senderName) { this.senderName = senderName; }
	public String getIntent() { return intent; }
	public void setIntent(String intent) { this.intent = intent; }
	public String getContentRaw() { return contentRaw; }
	public void setContentRaw(String contentRaw) { this.contentRaw = contentRaw; }
	public String getContentMasked() { return contentMasked; }
	public void setContentMasked(String contentMasked) { this.contentMasked = contentMasked; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 