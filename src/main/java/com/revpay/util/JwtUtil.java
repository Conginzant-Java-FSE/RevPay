package com.revpay.util;

import com.revpay.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public JwtUtil(BlacklistedTokenRepository blacklistedTokenRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
    }

    // ===============================
    // Generate Token
    // ===============================
    public String generateToken(Long userId, String email, String accountType) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("accountType", accountType);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)   // IMPORTANT: subject = email
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ===============================
    // Signing Key
    // ===============================
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ===============================
    // Extract All Claims
    // ===============================
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ===============================
    // Extract Username (Email)
    // ===============================
    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // ===============================
    // Extract Expiration
    // ===============================
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    // ===============================
    // Extract UserId
    // ===============================
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    // ===============================
    // Check Expired
    // ===============================
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // ===============================
    // Blacklist Check
    // ===============================
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    // ===============================
    // FINAL VALIDATION METHOD (WITH DEBUG)
    // ===============================
    public boolean validateToken(String token, UserDetails userDetails) {

        try {
            String extractedUsername = extractUsername(token);

            System.out.println("===== JWT DEBUG =====");
            System.out.println("Token username: " + extractedUsername);
            System.out.println("UserDetails username: " + userDetails.getUsername());
            System.out.println("Expired: " + isTokenExpired(token));
            System.out.println("Blacklisted: " + isTokenBlacklisted(token));
            System.out.println("=====================");

            return extractedUsername != null
                    && extractedUsername.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && !isTokenBlacklisted(token);

        } catch (Exception e) {
            return false;
        }
    }

    // ===============================
    // Generate Reset Token
    // ===============================
    public String generateResetToken(Long userId) {

        long resetExpiration = 5 * 60 * 1000; // 5 minutes

        return Jwts.builder()
                .setSubject("RESET_PASSWORD")
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + resetExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}