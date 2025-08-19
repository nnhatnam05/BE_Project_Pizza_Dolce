package aptech.be.controllers.chat;

import aptech.be.models.chat.ChatMessage;
import aptech.be.models.chat.ChatSession;
import aptech.be.models.chat.ChatPrompt;
import aptech.be.repositories.chat.ChatPromptRepository;
import aptech.be.services.chat.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {
	private final ChatService chatService;
	
	@Autowired
	private ChatPromptRepository chatPromptRepository;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping("/session")
	public ResponseEntity<?> createSession(@RequestBody(required = false) Map<String, Object> body) {
		String language = body != null ? (String) body.getOrDefault("language", "en") : "en";
		ChatSession s = chatService.createSession(language);
		return ResponseEntity.ok(Map.of("sessionId", s.getSessionId()));
	}

	@PostMapping("/message")
	public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> body) {
		String sessionId = body.get("sessionId");
		String content = body.get("message");
		String displayName = body.getOrDefault("displayName", null);
		if (sessionId == null || content == null || content.isBlank()) {
			return ResponseEntity.badRequest().body("sessionId and message are required");
		}
		chatService.receiveUserMessage(sessionId, content, displayName);
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/greet")
	public ResponseEntity<?> greet(@RequestBody Map<String, String> body) {
		String sessionId = body.get("sessionId");
		String userName = body.get("userName");
		if (sessionId == null || sessionId.isBlank()) return ResponseEntity.badRequest().build();
		chatService.sendGreeting(sessionId, userName);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/history")
	public ResponseEntity<List<ChatMessage>> history(@RequestParam String sessionId) {
		return ResponseEntity.ok(chatService.getChatHistory(sessionId));
	}
	
	@PostMapping("/demo-prompts")
	public ResponseEntity<String> createDemoPrompts() {
		try {
			// Create English prompts
			ChatPrompt englishPrompt = new ChatPrompt();
			englishPrompt.setName("Restaurant Customer Service - English");
			englishPrompt.setSystemPrompt("You are a friendly and knowledgeable customer service assistant for Dolce Restaurant. You help customers with menu questions, delivery information, pricing, and general inquiries. Always be polite, helpful, and accurate with information.");
			englishPrompt.setUserExamples("What are your delivery hours?\nHow much is the delivery fee?\nWhat pizzas do you recommend?\nDo you have vegetarian options?\nWhat are your opening hours?");
			englishPrompt.setAssistantExamples("Our delivery hours are from 10:00 AM to 10:00 PM daily.\nDelivery fee is $3.99 for orders under $25, free for orders $25 and above.\nI highly recommend our Margherita Pizza and Pepperoni Supreme!\nYes, we have several vegetarian options including our Veggie Delight pizza and Mediterranean salad.\nWe're open from 10:00 AM to 11:00 PM, 7 days a week.");
			englishPrompt.setLanguage("en");
			englishPrompt.setActive(true);
			englishPrompt.setPriority(1);
			englishPrompt.setCreatedAt(java.time.LocalDateTime.now());
			englishPrompt.setUpdatedAt(java.time.LocalDateTime.now());
			
			// Create Vietnamese prompts
			ChatPrompt vietnamesePrompt = new ChatPrompt();
			vietnamesePrompt.setName("Restaurant Customer Service - Vietnamese");
			vietnamesePrompt.setSystemPrompt("Bạn là trợ lý khách hàng thân thiện và am hiểu của Nhà hàng Dolce. Bạn giúp khách hàng với các câu hỏi về thực đơn, thông tin giao hàng, giá cả và các thắc mắc chung. Luôn lịch sự, hữu ích và chính xác với thông tin.");
			vietnamesePrompt.setUserExamples("Giờ giao hàng của bạn là gì?\nPhí giao hàng bao nhiêu?\nBạn gợi ý pizza nào ngon?\nCó lựa chọn chay không?\nGiờ mở cửa của bạn?");
			vietnamesePrompt.setAssistantExamples("Giờ giao hàng của chúng tôi từ 10:00 sáng đến 10:00 tối hàng ngày.\nPhí giao hàng là 3.99$ cho đơn hàng dưới 25$, miễn phí cho đơn hàng từ 25$ trở lên.\nTôi rất khuyến nghị Pizza Margherita và Pepperoni Supreme!\nCó, chúng tôi có nhiều lựa chọn chay bao gồm pizza Veggie Delight và salad Địa Trung Hải.\nChúng tôi mở cửa từ 10:00 sáng đến 11:00 tối, 7 ngày trong tuần.");
			vietnamesePrompt.setLanguage("vi");
			vietnamesePrompt.setActive(true);
			vietnamesePrompt.setPriority(1);
			vietnamesePrompt.setCreatedAt(java.time.LocalDateTime.now());
			vietnamesePrompt.setUpdatedAt(java.time.LocalDateTime.now());
			
			// Save prompts
			chatPromptRepository.save(englishPrompt);
			chatPromptRepository.save(vietnamesePrompt);
			
			return ResponseEntity.ok("Demo prompts created successfully!");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Error creating demo prompts: " + e.getMessage());
		}
	}

	@PostMapping("/handover")
	public ResponseEntity<?> handover(@RequestBody Map<String, String> body) {
		String sessionId = body.get("sessionId");
		if (sessionId == null || sessionId.isBlank()) return ResponseEntity.badRequest().build();
		chatService.handoverToAgent(sessionId);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/rate")
	public ResponseEntity<?> rate(@RequestBody Map<String, Object> body) {
		String sessionId = (String) body.get("sessionId");
		Integer stars = null;
		try { stars = Integer.parseInt(String.valueOf(body.get("rating"))); } catch (Exception ignored) {}
		String note = body.get("note") != null ? String.valueOf(body.get("note")) : null;
		if (sessionId == null || stars == null) return ResponseEntity.badRequest().build();
		chatService.rateSession(sessionId, stars, note);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/close")
	public ResponseEntity<?> close(@RequestBody Map<String, String> body) {
		String sessionId = body.get("sessionId");
		if (sessionId == null || sessionId.isBlank()) return ResponseEntity.badRequest().build();
		chatService.closeSession(sessionId);
		return ResponseEntity.ok().build();
	}
} 