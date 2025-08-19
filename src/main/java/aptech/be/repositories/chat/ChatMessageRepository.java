package aptech.be.repositories.chat;

import aptech.be.models.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
	List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
	
	@Modifying
	@Query("DELETE FROM ChatMessage m WHERE m.sessionId = :sessionId")
	void deleteBySessionId(@Param("sessionId") String sessionId);
} 