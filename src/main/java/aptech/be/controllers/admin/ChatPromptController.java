package aptech.be.controllers.admin;

import aptech.be.dto.chat.ChatPromptDTO;
import aptech.be.models.chat.ChatPrompt;
import aptech.be.repositories.chat.ChatPromptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/chat/prompts")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatPromptController {
    
    @Autowired
    private ChatPromptRepository chatPromptRepository;
    
    @GetMapping
    public ResponseEntity<List<ChatPromptDTO>> getAllPrompts() {
        List<ChatPrompt> prompts = chatPromptRepository.findAll();
        List<ChatPromptDTO> dtos = prompts.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ChatPromptDTO> getPromptById(@PathVariable Long id) {
        return chatPromptRepository.findById(id)
            .map(this::convertToDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<ChatPromptDTO> createPrompt(@RequestBody ChatPromptDTO promptDTO) {
        ChatPrompt prompt = convertToEntity(promptDTO);
        prompt.setId(null); // Ensure new entity
        ChatPrompt saved = chatPromptRepository.save(prompt);
        return ResponseEntity.ok(convertToDTO(saved));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ChatPromptDTO> updatePrompt(@PathVariable Long id, @RequestBody ChatPromptDTO promptDTO) {
        return chatPromptRepository.findById(id)
            .map(existing -> {
                updateEntityFromDTO(existing, promptDTO);
                ChatPrompt saved = chatPromptRepository.save(existing);
                return ResponseEntity.ok(convertToDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable Long id) {
        if (chatPromptRepository.existsById(id)) {
            chatPromptRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/active/{language}")
    public ResponseEntity<List<ChatPromptDTO>> getActivePromptsByLanguage(@PathVariable String language) {
        List<ChatPrompt> prompts = chatPromptRepository.findActivePromptsByLanguage(language);
        List<ChatPromptDTO> dtos = prompts.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/{id}/toggle")
    public ResponseEntity<ChatPromptDTO> togglePromptStatus(@PathVariable Long id) {
        return chatPromptRepository.findById(id)
            .map(prompt -> {
                prompt.setActive(!prompt.getActive());
                ChatPrompt saved = chatPromptRepository.save(prompt);
                return ResponseEntity.ok(convertToDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/demo-data")
    public ResponseEntity<String> createDemoData() {
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
            chatPromptRepository.save(englishPrompt);
            
            // Create Vietnamese prompts
            ChatPrompt vietnamesePrompt = new ChatPrompt();
            vietnamesePrompt.setName("Restaurant Customer Service - Vietnamese");
            vietnamesePrompt.setSystemPrompt("Bạn là trợ lý khách hàng thân thiện và am hiểu của Nhà hàng Dolce. Bạn giúp khách hàng với các câu hỏi về thực đơn, thông tin giao hàng, giá cả và các thắc mắc chung. Luôn lịch sự, hữu ích và chính xác với thông tin.");
            vietnamesePrompt.setUserExamples("Giờ giao hàng của bạn là gì?\nPhí giao hàng bao nhiêu?\nBạn gợi ý pizza nào ngon?\nCó lựa chọn chay không?\nGiờ mở cửa của bạn?");
            vietnamesePrompt.setAssistantExamples("Giờ giao hàng của chúng tôi từ 10:00 sáng đến 10:00 tối hàng ngày.\nPhí giao hàng là 3.99$ cho đơn hàng dưới 25$, miễn phí cho đơn hàng từ 25$ trở lên.\nTôi rất khuyến nghị Pizza Margherita và Pepperoni Supreme!\nCó, chúng tôi có nhiều lựa chọn chay bao gồm pizza Veggie Delight và salad Địa Trung Hải.\nChúng tôi mở cửa từ 10:00 sáng đến 11:00 tối, 7 ngày trong tuần.");
            vietnamesePrompt.setLanguage("vi");
            vietnamesePrompt.setActive(true);
            vietnamesePrompt.setPriority(1);
            chatPromptRepository.save(vietnamesePrompt);
            
            // Create general prompt for all languages
            ChatPrompt generalPrompt = new ChatPrompt();
            generalPrompt.setName("General Restaurant Knowledge - All Languages");
            generalPrompt.setSystemPrompt("You are a helpful restaurant assistant. Provide accurate information about restaurant operations, policies, and services. Be consistent with information across all languages.");
            generalPrompt.setUserExamples("What is your return policy?\nDo you offer catering?\nCan I make a reservation?\nWhat payment methods do you accept?");
            generalPrompt.setAssistantExamples("We accept returns within 30 minutes of delivery if there's an issue with your order.\nYes, we offer catering services for events and parties.\nYes, you can make reservations by calling us or through our website.\nWe accept cash, credit cards, and digital payments including PayPal and Apple Pay.");
            generalPrompt.setLanguage("all");
            generalPrompt.setActive(true);
            generalPrompt.setPriority(2);
            chatPromptRepository.save(generalPrompt);
            
            return ResponseEntity.ok("Demo data created successfully! Created 3 prompt templates.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating demo data: " + e.getMessage());
        }
    }
    
    private ChatPromptDTO convertToDTO(ChatPrompt prompt) {
        ChatPromptDTO dto = new ChatPromptDTO();
        dto.setId(prompt.getId());
        dto.setName(prompt.getName());
        dto.setSystemPrompt(prompt.getSystemPrompt());
        dto.setUserExamples(prompt.getUserExamples());
        dto.setAssistantExamples(prompt.getAssistantExamples());
        dto.setLanguage(prompt.getLanguage());
        dto.setActive(prompt.getActive());
        dto.setPriority(prompt.getPriority());
        dto.setCreatedAt(prompt.getCreatedAt());
        dto.setUpdatedAt(prompt.getUpdatedAt());
        return dto;
    }
    
    private ChatPrompt convertToEntity(ChatPromptDTO dto) {
        ChatPrompt prompt = new ChatPrompt();
        prompt.setId(dto.getId());
        prompt.setName(dto.getName());
        prompt.setSystemPrompt(dto.getSystemPrompt());
        prompt.setUserExamples(dto.getUserExamples());
        prompt.setAssistantExamples(dto.getAssistantExamples());
        prompt.setLanguage(dto.getLanguage());
        prompt.setActive(dto.getActive() != null ? dto.getActive() : true);
        prompt.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        return prompt;
    }
    
    private void updateEntityFromDTO(ChatPrompt prompt, ChatPromptDTO dto) {
        if (dto.getName() != null) prompt.setName(dto.getName());
        if (dto.getSystemPrompt() != null) prompt.setSystemPrompt(dto.getSystemPrompt());
        if (dto.getUserExamples() != null) prompt.setUserExamples(dto.getUserExamples());
        if (dto.getAssistantExamples() != null) prompt.setAssistantExamples(dto.getAssistantExamples());
        if (dto.getLanguage() != null) prompt.setLanguage(dto.getLanguage());
        if (dto.getActive() != null) prompt.setActive(dto.getActive());
        if (dto.getPriority() != null) prompt.setPriority(dto.getPriority());
    }
} 