package com.recruitplatform.auth_service.util;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
@Component public class JwtUtil {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration-ms}") private long expirationMs;
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    public String generateToken(String email, String role) { Date now = new Date(); Date expiry = new Date(now.getTime() + expirationMs); return Jwts.builder() .subject(email) .claim("role", role) .issuedAt(now) .expiration(expiry) .signWith(getSigningKey()) .compact(); }
    public String extractEmail(String token) {
        return Jwts.parser() .verifyWith(getSigningKey()) .build() .parseSignedClaims(token) .getPayload() .getSubject();
    }
    public String extractRole(String token) {
        return Jwts.parser() .verifyWith(getSigningKey()) .build() .parseSignedClaims(token) .getPayload() .get("role", String.class);
    }
    public boolean isTokenValid(String token) {
        try { Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token); return true; } catch (Exception e) { return false; } }
}