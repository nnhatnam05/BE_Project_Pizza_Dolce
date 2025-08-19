package aptech.be.controllers.admin;

import aptech.be.models.chat.ChatSession;
import aptech.be.repositories.chat.ChatSessionRepository;
import aptech.be.repositories.chat.ChatMessageRepository;
import aptech.be.services.chat.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/chat")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
public class ChatAdminController {
	private final ChatSessionRepository chatSessionRepository;
	private final ChatMessageRepository messageRepo;
	private final ChatService chatService;

	public ChatAdminController(ChatSessionRepository chatSessionRepository,
							   ChatMessageRepository messageRepo,
							   ChatService chatService) {
		this.chatSessionRepository = chatSessionRepository;
		this.messageRepo = messageRepo;
		this.chatService = chatService;
	}

	// Sessions Management
	@GetMapping("/sessions")
	public List<ChatSession> sessions() { 
		return chatSessionRepository.findByStatusIn(Arrays.asList("active","handed_over","ended")); 
	}

	@GetMapping("/sessions/{sessionId}")
	public Map<String, Object> sessionDetail(@PathVariable String sessionId) {
		Map<String, Object> resp = new HashMap<>();
		ChatSession s = chatSessionRepository.findBySessionId(sessionId).orElse(null);
		resp.put("session", s);
		resp.put("history", messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId));
		return resp;
	}
	
	@PostMapping("/sessions/{sessionId}/handover")
	public ResponseEntity<?> handover(@PathVariable String sessionId) { 
		chatService.handoverToAgent(sessionId); 
		return ResponseEntity.ok().build(); 
	}
	
	@PostMapping("/sessions/{sessionId}/agent-message")
	public ResponseEntity<?> agentMessage(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
		chatService.sendAgentMessage(sessionId, body.getOrDefault("message", "")); 
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/analytics/basic")
	public Map<String, Object> analyticsBasic() {
		Map<String, Object> m = new HashMap<>();
		m.put("totalSessions", chatSessionRepository.findByStatusIn(Arrays.asList("active", "handed_over")).size());
		m.put("totalMessages", messageRepo.count());
		return m;
	}
	
	// Delete session and all related messages
	@DeleteMapping("/sessions/{sessionId}")
	@Transactional
	public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
		try {
			// Delete all messages for this session first
			messageRepo.deleteBySessionId(sessionId);
			
			// Delete the session
			chatSessionRepository.deleteBySessionId(sessionId);
			
			return ResponseEntity.ok().body("Session deleted successfully");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Error deleting session: " + e.getMessage());
		}
	}
	
	// Delete all sessions (for testing purposes)
	@DeleteMapping("/sessions/clear-all")
	@Transactional
	public ResponseEntity<?> clearAllSessions() {
		try {
			// Delete all messages first
			messageRepo.deleteAll();
			
			// Delete all sessions
			chatSessionRepository.deleteAll();
			
			return ResponseEntity.ok().body("All sessions cleared successfully");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Error clearing sessions: " + e.getMessage());
		}
	}
} 