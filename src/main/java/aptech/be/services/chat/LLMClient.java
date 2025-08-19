package aptech.be.services.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class LLMClient {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMClient.class);
    
    @Value("${llm.baseUrl}")
    private String baseUrl;
    
    @Value("${llm.apiKey}")
    private String apiKey;
    
    @Value("${llm.model}")
    private String model;
    
    @Value("${llm.site}")
    private String site;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String generateResponse(String prompt) {
        if (baseUrl == null || baseUrl.trim().isEmpty() || 
            apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("LLM API not configured, using fallback response");
            return generateFallbackResponse(prompt);
        }
        
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", site);
            headers.set("X-Title", "Dolce Restaurant Chatbot");
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful AI assistant."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);
            
            // Make API call
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode choices = responseJson.get("choices");
                
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    JsonNode message = firstChoice.get("message");
                    
                    if (message != null && message.has("content")) {
                        String content = message.get("content").asText();
                        if (content != null && !content.trim().isEmpty()) {
                            logger.info("LLM response generated successfully");
                            return content.trim();
                        }
                    }
                }
            }
            
            logger.warn("LLM API returned invalid response, using fallback");
            return generateFallbackResponse(prompt);
            
        } catch (Exception e) {
            logger.error("Error calling LLM API: " + e.getMessage(), e);
            return generateFallbackResponse(prompt);
        }
    }
    
    private String generateFallbackResponse(String prompt) {
        // Simple keyword-based fallback responses
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("delivery") || lowerPrompt.contains("giao hàng")) {
            return "Our delivery service is available from 10:00 AM to 10:00 PM daily. Delivery fee is $3.99 for orders under $25, free for orders $25 and above.";
        }
        
        if (lowerPrompt.contains("menu") || lowerPrompt.contains("thực đơn")) {
            return "We offer a wide variety of pizzas, pastas, salads, and beverages. Our most popular items include Margherita Pizza, Pepperoni Supreme, and Mediterranean Salad.";
        }
        
        if (lowerPrompt.contains("price") || lowerPrompt.contains("giá") || lowerPrompt.contains("cost")) {
            return "Our prices range from $8.99 for personal pizzas to $24.99 for large specialty pizzas. We also offer combo deals and family packages.";
        }
        
        if (lowerPrompt.contains("hours") || lowerPrompt.contains("giờ") || lowerPrompt.contains("open")) {
            return "We're open from 10:00 AM to 11:00 PM, 7 days a week. Our kitchen closes at 10:30 PM for last orders.";
        }
        
        if (lowerPrompt.contains("vegetarian") || lowerPrompt.contains("chay")) {
            return "Yes, we have several vegetarian options including our Veggie Delight pizza, Mediterranean salad, and various pasta dishes.";
        }
        
        // Default fallback
        return "I'm here to help with your restaurant questions! I can assist with menu information, delivery details, pricing, and general inquiries. If you have a specific question, please let me know.";
    }
    
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.trim().isEmpty() && 
               apiKey != null && !apiKey.trim().isEmpty();
    }
} 