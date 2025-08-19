package aptech.be.repositories.chat;

import aptech.be.models.chat.ChatPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatPromptRepository extends JpaRepository<ChatPrompt, Long> {
    
    @Query("SELECT p FROM ChatPrompt p WHERE p.active = true AND (p.language = :language OR p.language = 'all') ORDER BY p.priority DESC, p.createdAt DESC")
    List<ChatPrompt> findActivePromptsByLanguage(@Param("language") String language);
    
    @Query("SELECT p FROM ChatPrompt p WHERE p.active = true ORDER BY p.priority DESC, p.createdAt DESC")
    List<ChatPrompt> findAllActivePrompts();
    
    List<ChatPrompt> findByLanguageAndActiveOrderByPriorityDesc(String language, Boolean active);
} 