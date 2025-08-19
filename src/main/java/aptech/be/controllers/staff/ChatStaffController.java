package aptech.be.controllers.staff;

import aptech.be.models.chat.ChatMessage;
import aptech.be.models.chat.ChatSession;
import aptech.be.repositories.chat.ChatMessageRepository;
import aptech.be.repositories.chat.ChatSessionRepository;
import aptech.be.services.chat.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/chat")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class ChatStaffController {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository messageRepo;
    private final ChatService chatService;

    public ChatStaffController(ChatSessionRepository chatSessionRepository,
                               ChatMessageRepository messageRepo,
                               ChatService chatService) {
        this.chatSessionRepository = chatSessionRepository;
        this.messageRepo = messageRepo;
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public List<ChatSession> sessions() {
        // Nhân viên chỉ xem các phiên đã bàn giao
        return chatSessionRepository.findByStatusIn(Arrays.asList("handed_over"));
    }

    @GetMapping("/history")
    public List<ChatMessage> history(@RequestParam String sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/reply")
    public ResponseEntity<?> reply(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        chatService.sendAgentMessage(sessionId, message);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        return ResponseEntity.accepted().body(resp);
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<?> close(@PathVariable String sessionId) {
        chatService.closeSession(sessionId);
        return ResponseEntity.ok().build();
    }
} 