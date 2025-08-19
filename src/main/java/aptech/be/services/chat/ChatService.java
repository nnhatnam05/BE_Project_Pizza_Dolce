package aptech.be.services.chat;

import aptech.be.models.chat.*;
import aptech.be.repositories.chat.*;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.CustomerRepository;
import aptech.be.services.chat.LLMClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ChatService {
    
    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ChatPromptRepository chatPromptRepository;
    
    @Autowired
    private LLMClient llmClient;
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    public ChatSession createSession(String language) {
        ChatSession session = new ChatSession();
        session.setSessionId(java.util.UUID.randomUUID().toString());
        session.setLanguage(language);
        session.setStatus("active");
        session.setCreatedAt(java.time.LocalDateTime.now());
        session.setLastActivityAt(java.time.LocalDateTime.now());
        // Gắn user nếu request đang đăng nhập
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null &&
                !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
                String emailOrUsername;
                if (auth.getPrincipal() instanceof aptech.be.services.CustomUserDetails cud) {
                    emailOrUsername = cud.getEmail();
                } else if (auth.getPrincipal() instanceof UserDetails ud) {
                    emailOrUsername = ud.getUsername();
                } else {
                    emailOrUsername = auth.getName();
                }
                // Ưu tiên map sang Customer
                aptech.be.models.Customer customer = customerRepository.findByEmail(emailOrUsername).orElse(null);
                if (customer != null) {
                    session.setUserId(customer.getId());
                } else {
                    aptech.be.models.UserEntity u = userRepository.findByEmail(emailOrUsername)
                        .or(() -> userRepository.findByUsername(emailOrUsername))
                        .orElse(null);
                    if (u != null) {
                        session.setUserId(u.getId());
                    }
                }
            }
        } catch (Exception ignored) {}
        return chatSessionRepository.save(session);
    }
    
    public void receiveUserMessage(String sessionId, String message) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) return;
        
        // Gắn userId vào session nếu chưa có và người dùng đang đăng nhập
        if (session.getUserId() == null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null &&
                    !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
                    String emailOrUsername;
                    if (auth.getPrincipal() instanceof aptech.be.services.CustomUserDetails cud) {
                        emailOrUsername = cud.getEmail();
                    } else if (auth.getPrincipal() instanceof UserDetails ud) {
                        emailOrUsername = ud.getUsername();
                    } else {
                        emailOrUsername = auth.getName();
                    }
                    aptech.be.models.Customer customer = customerRepository.findByEmail(emailOrUsername).orElse(null);
                    if (customer != null) {
                        session.setUserId(customer.getId());
                        chatSessionRepository.save(session);
                    } else {
                        aptech.be.models.UserEntity u = userRepository.findByEmail(emailOrUsername)
                            .or(() -> userRepository.findByUsername(emailOrUsername))
                            .orElse(null);
                        if (u != null) {
                            session.setUserId(u.getId());
                            chatSessionRepository.save(session);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // Mask PII before storing
        String maskedMessage = PIIMasker.mask(message);
        
        // Store user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setSender("user");
        // tên khách nếu có
        String customerName = "Khách vãng lai";
        try {
            // Ưu tiên lấy từ Customer (theo auth/email hoặc theo session.userId)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String emailFromAuth = null;
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null &&
                !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
                if (auth.getPrincipal() instanceof aptech.be.services.CustomUserDetails cud) {
                    emailFromAuth = cud.getEmail();
                } else if (auth.getPrincipal() instanceof UserDetails ud) {
                    emailFromAuth = ud.getUsername();
                } else {
                    emailFromAuth = auth.getName();
                }
            }
            aptech.be.models.Customer c = null;
            if (emailFromAuth != null) {
                c = customerRepository.findByEmail(emailFromAuth).orElse(null);
            }
            if (c == null && session.getUserId() != null) {
                c = customerRepository.findById(session.getUserId()).orElse(null);
            }
            if (c != null) {
                if (c.getFullName() != null && !c.getFullName().isBlank()) {
                    customerName = c.getFullName();
                } else if (c.getEmail() != null) {
                    customerName = c.getEmail();
                }
            } else if (session.getUserId() != null) {
                aptech.be.models.UserEntity u = userRepository.findById(session.getUserId()).orElse(null);
                if (u != null) {
                    if (u.getName() != null && !u.getName().isBlank()) customerName = u.getName();
                    else if (u.getUsername() != null && !u.getUsername().isBlank()) customerName = u.getUsername();
                }
            }
        } catch (Exception ignored) {}
        userMsg.setSenderName(customerName);
        userMsg.setContentRaw(message);
        userMsg.setContentMasked(maskedMessage);
        userMsg.setCreatedAt(java.time.LocalDateTime.now());
        userMsg = chatMessageRepository.save(userMsg);
        // Update last activity
        session.setLastActivityAt(java.time.LocalDateTime.now());
        chatSessionRepository.save(session);
        
        // If session handed over, stop AI and forward to agent in realtime
        if ("handed_over".equals(session.getStatus())) {
            if (messagingTemplate != null) {
                String escaped = message.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
                String json = "{\"type\":\"user\",\"content\":\"" + escaped + "\",\"sessionId\":\"" + sessionId + "\",\"messageId\":" + userMsg.getId() + ",\"senderName\":\"" + customerName.replace("\"","\\\"") + "\"}";
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId, json);
            }
            return;
        }
        
        try {
            // Gửi tín hiệu start sớm để FE hiển thị typing ngay lập tức
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId,
                    "{\"type\":\"start\",\"sessionId\":\"" + sessionId + "\"}");
            }
            
            // Build AI prompt with training data và gọi LLM
            String aiPrompt = buildAIPrompt(session.getLanguage(), message);
            String aiResponse = llmClient.generateResponse(aiPrompt);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                // Store AI response
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setSessionId(sessionId);
                aiMsg.setSender("bot");
                aiMsg.setSenderName("BOT");
                aiMsg.setContentRaw(aiResponse);
                aiMsg.setContentMasked(aiResponse);
                aiMsg.setCreatedAt(java.time.LocalDateTime.now());
                chatMessageRepository.save(aiMsg);
                
                // Stream response to client
                if (messagingTemplate != null) {
                    streamAsBot(sessionId, aiResponse);
                }
            } else {
                // Fallback response
                sendFallbackResponse(sessionId, session.getLanguage());
            }
        } catch (Exception e) {
            // Fallback response on error
            sendFallbackResponse(sessionId, session.getLanguage());
        }
    }
    
    public void receiveUserMessage(String sessionId, String message, String displayName) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) return;
        // Lưu displayName tạm vào ThreadLocal để dùng trong luồng xử lý gốc (đơn giản: gọi lại logic nhưng truyền qua biến cục bộ)
        String fallbackName = (displayName != null && !displayName.isBlank()) ? displayName : null;
        // Mask PII before storing
        String maskedMessage = PIIMasker.mask(message);
        // Store user message sớm với fallbackName nếu có
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setSender("user");
        String customerName = null;
        if (fallbackName != null) customerName = fallbackName;
        if (customerName == null) {
            try {
                if (session.getUserId() != null) {
                    aptech.be.models.Customer c = customerRepository.findById(session.getUserId()).orElse(null);
                    if (c != null && c.getFullName() != null && !c.getFullName().isBlank()) customerName = c.getFullName();
                }
            } catch (Exception ignored) {}
        }
        if (customerName == null || customerName.isBlank()) customerName = "Khách vãng lai";
        userMsg.setSenderName(customerName);
        userMsg.setContentRaw(message);
        userMsg.setContentMasked(maskedMessage);
        userMsg.setCreatedAt(java.time.LocalDateTime.now());
        userMsg = chatMessageRepository.save(userMsg);
        session.setLastActivityAt(java.time.LocalDateTime.now());
        chatSessionRepository.save(session);
        if ("handed_over".equals(session.getStatus())) {
            if (messagingTemplate != null) {
                String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                String json = "{\"type\":\"user\",\"content\":\"" + escaped + "\",\"sessionId\":\"" + sessionId + "\",\"messageId\":" + userMsg.getId() + ",\"senderName\":\"" + customerName.replace("\"","\\\"") + "\"}";
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId, json);
            }
            return;
        }
        try {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId, "{\"type\":\"start\",\"sessionId\":\"" + sessionId + "\"}");
            }
            String aiPrompt = buildAIPrompt(session.getLanguage(), message);
            String aiResponse = llmClient.generateResponse(aiPrompt);
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setSessionId(sessionId);
                aiMsg.setSender("bot");
                aiMsg.setSenderName("BOT");
                aiMsg.setContentRaw(aiResponse);
                aiMsg.setContentMasked(aiResponse);
                aiMsg.setCreatedAt(java.time.LocalDateTime.now());
                chatMessageRepository.save(aiMsg);
                if (messagingTemplate != null) { streamAsBot(sessionId, aiResponse); }
            } else { sendFallbackResponse(sessionId, session.getLanguage()); }
        } catch (Exception e) { sendFallbackResponse(sessionId, session.getLanguage()); }
    }
    
    private String buildAIPrompt(String language, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        // Get active prompts for the language
        List<ChatPrompt> prompts = chatPromptRepository.findActivePromptsByLanguage(language);
        
        if (prompts.isEmpty()) {
            // Fallback to default prompt
            prompt.append("You are a helpful customer service assistant for Dolce Restaurant. ");
            prompt.append("Answer questions about menu, prices, delivery times, and policies. ");
            prompt.append("Be friendly and helpful. ");
            prompt.append("Current time: ").append(LocalTime.now().toString()).append("\n\n");
        } else {
            // Use trained prompts
            for (ChatPrompt promptData : prompts) {
                prompt.append("=== SYSTEM PROMPT ===\n");
                prompt.append(promptData.getSystemPrompt()).append("\n\n");
                
                if (promptData.getUserExamples() != null && !promptData.getUserExamples().trim().isEmpty()) {
                    prompt.append("=== USER EXAMPLES ===\n");
                    prompt.append(promptData.getUserExamples()).append("\n\n");
                }
                
                if (promptData.getAssistantExamples() != null && !promptData.getAssistantExamples().trim().isEmpty()) {
                    prompt.append("=== ASSISTANT EXAMPLES ===\n");
                    prompt.append(promptData.getAssistantExamples()).append("\n\n");
                }
            }
        }
        
        // Add user message
        prompt.append("=== USER MESSAGE ===\n");
        prompt.append(userMessage).append("\n\n");
        
        // Add response instruction
        prompt.append("=== RESPONSE ===\n");
        prompt.append("Please provide a helpful response based on the above context. ");
        prompt.append("If you don't know the answer, suggest contacting customer service. ");
        prompt.append("Keep responses concise but informative.\n\n");
        
        return prompt.toString();
    }
    
    private void sendFallbackResponse(String sessionId, String language) {
        String fallbackMessage = language.equals("vi") ? 
            "Xin lỗi, tôi không thể xử lý câu hỏi này ngay bây giờ. Vui lòng liên hệ nhân viên hỗ trợ để được giúp đỡ." :
            "Sorry, I couldn't process that right now. Please contact customer service for assistance.";
        
        // Store fallback response
        ChatMessage fallbackMsg = new ChatMessage();
        fallbackMsg.setSessionId(sessionId);
        fallbackMsg.setSender("bot");
        fallbackMsg.setSenderName("BOT");
        fallbackMsg.setContentRaw(fallbackMessage);
        fallbackMsg.setContentMasked(fallbackMessage);
        fallbackMsg.setCreatedAt(java.time.LocalDateTime.now());
        chatMessageRepository.save(fallbackMsg);
        
        // Stream fallback response
        if (messagingTemplate != null) {
            streamAsBot(sessionId, fallbackMessage);
        }
    }
    
    private void sendHandoverMessage(String sessionId, String language) {
        String handoverMessage = language.equals("vi") ? 
            "Cuộc trò chuyện đã được chuyển cho nhân viên hỗ trợ. Vui lòng chờ trong giây lát." :
            "This conversation has been handed over to a human agent. Please wait a moment.";
        
        if (messagingTemplate != null) {
            streamAsBot(sessionId, handoverMessage);
        }
    }
    
    public void handoverToAgent(String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session != null) {
            session.setStatus("handed_over");
            session.setEndedAt(java.time.LocalDateTime.now());
            chatSessionRepository.save(session);
            
            // Send notification to customer
            if (messagingTemplate != null) {
                String message = session.getLanguage().equals("vi") ? 
                    "Đã chuyển cuộc trò chuyện cho nhân viên hỗ trợ. Nhân viên sẽ trả lời bạn sớm nhất." :
                    "Conversation handed over to a human agent. An agent will respond to you shortly.";
                streamAsBot(sessionId, message);
            }
        }
    }
    
    public void sendAgentMessage(String sessionId, String message) {
        ChatMessage agentMsg = new ChatMessage();
        agentMsg.setSessionId(sessionId);
        agentMsg.setSender("agent");
        agentMsg.setContentRaw(message);
        agentMsg.setContentMasked(message);
        agentMsg.setCreatedAt(java.time.LocalDateTime.now());
        // staffName xác định bên dưới, set lại sau khi tính xong
        String staffName = "Agent";
        
        // Update last activity
        ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session != null) {
            session.setLastActivityAt(java.time.LocalDateTime.now());
            chatSessionRepository.save(session);
        }
        
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() != null) {
                String emailOrUsername;
                if (auth.getPrincipal() instanceof aptech.be.services.CustomUserDetails cud) {
                    emailOrUsername = cud.getEmail();
                } else if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                    emailOrUsername = ud.getUsername();
                } else {
                    emailOrUsername = auth.getName();
                }
                aptech.be.models.UserEntity u = userRepository.findByEmail(emailOrUsername)
                    .or(() -> userRepository.findByUsername(emailOrUsername))
                    .orElse(null);
                if (u != null) {
                    String name = u.getName();
                    if (name == null || name.isBlank()) name = u.getUsername();
                    staffName = name != null ? name : "Nhân viên";
                }
            }
        } catch (Exception ignored) {}
        // Gán senderName vào DB
        agentMsg.setSenderName(staffName);
        agentMsg = chatMessageRepository.save(agentMsg);
        
        if (messagingTemplate != null) {
            String escaped = message.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
            String json = "{\"type\":\"agent\",\"content\":\"" + escaped + "\",\"sessionId\":\"" + sessionId + "\",\"messageId\":" + agentMsg.getId() + ",\"senderName\":\"" + staffName.replace("\"","\\\"") + "\"}";
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId, json);
        }
    }
    
    public void sendGreeting(String sessionId, String userName) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) return;
        
        // Kiểm tra xem session đã có messages chưa
        List<ChatMessage> existingMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (!existingMessages.isEmpty()) {
            System.out.println("Session " + sessionId + " already has messages, skipping greeting");
            return;
        }
        
        String greeting = generateGreeting(session.getLanguage(), userName);
        
        ChatMessage greetingMsg = new ChatMessage();
        greetingMsg.setSessionId(sessionId);
        greetingMsg.setSender("bot");
        greetingMsg.setSenderName("BOT");
        greetingMsg.setContentRaw(greeting);
        greetingMsg.setContentMasked(greeting);
        greetingMsg.setCreatedAt(java.time.LocalDateTime.now());
        chatMessageRepository.save(greetingMsg);
        
        if (messagingTemplate != null) {
            streamAsBot(sessionId, greeting);
        }
    }
    
    private String generateGreeting(String language, String userName) {
        // Lời chào chung, không chèn tên người dùng để tránh hiển thị sai khi cache/localStorage còn tên cũ
        if ("vi".equalsIgnoreCase(language)) {
            return "Xin chào bạn! Tôi có thể giúp gì cho bạn hôm nay?";
        } else {
            return "Hello! How can I help you today?";
        }
    }
    
    private void streamAsBot(String sessionId, String message) {
        if (messagingTemplate == null) return;
        
        // Validate message
        if (message == null || message.trim().isEmpty()) {
            System.err.println("Cannot stream null or empty message for session: " + sessionId);
            return;
        }
        
        try {
            // Send start signal
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId, 
                "{\"type\":\"start\",\"sessionId\":\"" + sessionId + "\"}");
            
            // Stream message in smaller chunks for better reliability
            int chunkSize = 15; // Giảm chunk size để ổn định hơn
            String[] chunks = message.split("(?<=\\G.{" + chunkSize + "})");
            
            System.out.println("Streaming message: " + message);
            System.out.println("Total chunks: " + chunks.length);
            
            for (int i = 0; i < chunks.length; i++) {
                try {
                    Thread.sleep(30); // Tăng delay để ổn định hơn
                    
                    String chunk = chunks[i];
                    // Escape special characters for JSON
                    String escapedChunk = chunk.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                    
                    String deltaMessage = "{\"type\":\"delta\",\"delta\":\"" + escapedChunk + "\",\"sessionId\":\"" + sessionId + "\"}";
                    
                    System.out.println("Sending chunk " + (i+1) + "/" + chunks.length + ": " + chunk);
                    messagingTemplate.convertAndSend("/topic/chat/" + sessionId, deltaMessage);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Streaming interrupted for session: " + sessionId);
                    break;
                } catch (Exception e) {
                    System.err.println("Error sending chunk " + (i+1) + " for session " + sessionId + ": " + e.getMessage());
                }
            }
            
            // Send end signal
            String endMessage = "{\"type\":\"end\",\"sessionId\":\"" + sessionId + "\"}";
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId, endMessage);
            System.out.println("Streaming completed for session: " + sessionId);
            
        } catch (Exception e) {
            System.err.println("Error in streamAsBot for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: send message as single chunk if streaming fails
            try {
                // Escape the entire message for fallback
                String escapedMessage = message.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId, escapedMessage);
            } catch (Exception fallbackError) {
                System.err.println("Fallback also failed for session " + sessionId + ": " + fallbackError.getMessage());
            }
        }
    }
    
    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

	public void closeSession(String sessionId) {
		ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
		if (session == null) return;
		session.setStatus("ended");
		session.setEndedAt(java.time.LocalDateTime.now());
		chatSessionRepository.save(session);
		if (messagingTemplate != null) {
			String json = "{\"type\":\"closed\",\"sessionId\":\"" + sessionId + "\"}";
			messagingTemplate.convertAndSend("/topic/chat/" + sessionId, json);
		}
	}

	public void rateSession(String sessionId, int rating) {
		ChatSession s = chatSessionRepository.findBySessionId(sessionId).orElse(null);
		if (s == null) return;
		s.setRating(Math.max(1, Math.min(5, rating)));
		chatSessionRepository.save(s);
	}

	public void rateSession(String sessionId, int rating, String note) {
		ChatSession s = chatSessionRepository.findBySessionId(sessionId).orElse(null);
		if (s == null) return;
		s.setRating(Math.max(1, Math.min(5, rating)));
		if (note != null) s.setRatingNote(note);
		chatSessionRepository.save(s);
	}

	// Gửi cảnh báo sau 5 phút và đóng sau 10 phút
	@Scheduled(fixedDelay = 60000)
	public void autoWarnAndCloseIdleSessions() {
		LocalDateTime now = LocalDateTime.now();
		// Warn threshold: 5 minutes idle
		LocalDateTime warnThreshold = now.minusMinutes(5);
		// Close threshold: 10 minutes idle
		LocalDateTime closeThreshold = now.minusMinutes(10);

		// Close sessions idle >= 10 minutes
		List<ChatSession> toClose = chatSessionRepository.findActiveSessionsIdleBefore(closeThreshold);
		for (ChatSession s : toClose) {
			try {
				closeSession(s.getSessionId());
			} catch (Exception ignored) {}
		}

		// Warn sessions idle >= 5 minutes but < 10 minutes: gửi 1 lần
		List<ChatSession> toWarn = chatSessionRepository.findActiveSessionsIdleBefore(warnThreshold);
		for (ChatSession s : toWarn) {
			// Kiểm tra đã gửi cảnh báo chưa: lưu 1 ChatMessage sender=bot với nội dung warning để không gửi lại
			List<ChatMessage> msgs = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(s.getSessionId());
			boolean alreadyWarned = msgs.stream().anyMatch(m -> "bot".equals(m.getSender()) && m.getContentMasked() != null && m.getContentMasked().contains("sẽ tự động đóng"));
			if (alreadyWarned) continue;
			String vi = "Phiên sẽ tự động đóng nếu không nhận được phản hồi sau 5 phút.";
			String en = "The session will auto-close if we don't receive a reply within 5 minutes.";
			String warning = "vi".equalsIgnoreCase(s.getLanguage()) ? vi : en;
			// Lưu message
			ChatMessage warnMsg = new ChatMessage();
			warnMsg.setSessionId(s.getSessionId());
			warnMsg.setSender("bot");
			warnMsg.setSenderName("BOT");
			warnMsg.setContentRaw(warning);
			warnMsg.setContentMasked(warning);
			warnMsg.setCreatedAt(LocalDateTime.now());
			chatMessageRepository.save(warnMsg);
			// Gửi qua WS (non-stream)
			if (messagingTemplate != null) {
				String escaped = warning.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
				String json = "{\"type\":\"agent\",\"content\":\"" + escaped + "\",\"sessionId\":\"" + s.getSessionId() + "\",\"senderName\":\"BOT\"}";
				messagingTemplate.convertAndSend("/topic/chat/" + s.getSessionId(), json);
			}
		}
	}
} 