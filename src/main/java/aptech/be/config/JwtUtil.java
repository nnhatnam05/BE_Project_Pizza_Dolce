package aptech.be.config;

import aptech.be.services.CustomUserDetails;
import aptech.be.services.CustomerDetails;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        System.out.println("Using JWT secret key: [" + jwtSecret + "]");
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }


    public String generateToken(UserDetails userDetails) {
        String subject;
        if (userDetails instanceof CustomUserDetails) {
            subject = ((CustomUserDetails) userDetails).getEmail();
        } else if (userDetails instanceof CustomerDetails) {
            subject = ((CustomerDetails) userDetails).getUsername();
        } else {
            subject = userDetails.getUsername();
        }

        List<String> authorities = userDetails.getAuthorities().stream()
                .map(a -> {
                    String auth = a.getAuthority();
                    return auth.startsWith("ROLE_") ? auth : "ROLE_" + auth;
                })
                .toList();

        String token = Jwts.builder()
                .setSubject(subject)
                .claim("authorities", authorities)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10h
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        return token;
    }


    // Extract email từ token (subject)
    public String extractUsername(String token) {
        System.out.println("Extracting username (email) from token...");
        String email = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        System.out.println("Extracted email: " + email);
        return email;
    }

    // So sánh đúng email
    public boolean validateToken(String token, UserDetails userDetails) {
        String email = extractUsername(token);
        System.out.println("Validating token for email: " + email);

        boolean isValid;
        if (userDetails instanceof CustomUserDetails) {
            String userEmail = ((CustomUserDetails) userDetails).getEmail();
            System.out.println("UserDetails email: " + userEmail);
            isValid = email.equals(userEmail);
        } else if (userDetails instanceof CustomerDetails) {
            String userEmail = userDetails.getUsername();
            System.out.println("CustomerDetails username (email): " + userEmail);
            isValid = email.equals(userEmail);
        } else {
            String userEmail = userDetails.getUsername();
            System.out.println("Generic UserDetails username: " + userEmail);
            isValid = email.equals(userEmail);
        }

        System.out.println("Token validation result: " + isValid);
        return isValid;
    }
}
