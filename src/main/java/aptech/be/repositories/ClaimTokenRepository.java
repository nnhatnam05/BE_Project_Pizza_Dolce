package aptech.be.repositories;

import aptech.be.models.ClaimToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimTokenRepository extends JpaRepository<ClaimToken, Long> {
    
    Optional<ClaimToken> findByToken(String token);
    
    List<ClaimToken> findByClaimedByEmail(String email);
    
    @Query("SELECT ct FROM ClaimToken ct WHERE ct.expiresAt < :now AND ct.claimed = false")
    List<ClaimToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT ct FROM ClaimToken ct WHERE ct.claimed = false AND ct.expiresAt > :now")
    List<ClaimToken> findValidUnclaimedTokens(@Param("now") LocalDateTime now);
    
    boolean existsByTokenAndClaimed(String token, Boolean claimed);
} 