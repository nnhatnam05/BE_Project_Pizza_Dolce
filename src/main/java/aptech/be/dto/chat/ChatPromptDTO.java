package aptech.be.dto.chat;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatPromptDTO {
    private Long id;
    private String name;
    private String systemPrompt;
    private String userExamples;
    private String assistantExamples;
    private String language;
    private Boolean active;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Manual getters and setters to avoid Lombok issues
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    
    public String getUserExamples() { return userExamples; }
    public void setUserExamples(String userExamples) { this.userExamples = userExamples; }
    
    public String getAssistantExamples() { return assistantExamples; }
    public void setAssistantExamples(String assistantExamples) { this.assistantExamples = assistantExamples; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 