package aptech.be.repositories.chat;

import aptech.be.models.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
	Optional<ChatSession> findBySessionId(String sessionId);
	List<ChatSession> findByStatusIn(List<String> statuses);
	
	@Modifying
	@Query("DELETE FROM ChatSession s WHERE s.sessionId = :sessionId")
	void deleteBySessionId(@Param("sessionId") String sessionId);

	@Query("SELECT s FROM ChatSession s WHERE s.status = 'active' AND s.lastActivityAt < :threshold")
	List<ChatSession> findActiveSessionsIdleBefore(@Param("threshold") LocalDateTime threshold);
} 