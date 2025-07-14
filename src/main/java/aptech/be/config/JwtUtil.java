package aptech.be.config;

import aptech.be.models.UserEntity;
import aptech.be.services.CustomUserDetails;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "aptech-secret-key-aptech-secret-key";

    private SecretKey getSigningKey() {
        return new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String generateToken(UserDetails userDetails) {
        String subject;

        if (userDetails instanceof CustomUserDetails) {
            subject = ((CustomUserDetails) userDetails).getEmail();
        } else {
            subject = userDetails.getUsername(); // fallback
        }

        return Jwts.builder()
                .setSubject(subject)
                .claim("authorities", userDetails.getAuthorities()
                        .stream()
                        .map(a -> {
                            String auth = a.getAuthority();
                            return auth.startsWith("ROLE_") ? auth : "ROLE_" + auth;
                        }).toList())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        if (userDetails instanceof CustomUserDetails) {
            return username.equals(((CustomUserDetails) userDetails).getEmail());
        } else {
            return username.equals(userDetails.getUsername());
        }
    }

}
