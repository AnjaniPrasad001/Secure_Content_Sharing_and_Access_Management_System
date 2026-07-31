package com.filevault.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            String preview = token != null ? (token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length() - 8) : token) : "<null>";
            log.error("Failed to parse username from token (preview={}): {}", preview, e.getMessage());
            throw e;
        }
    }
    
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            String preview = token != null ? (token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length() - 8) : token) : "<null>";
            log.error("Invalid JWT token (preview={}): {}", preview, e.getMessage());
            throw new RuntimeException("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            String preview = token != null ? (token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length() - 8) : token) : "<null>";
            log.error("Expired JWT token (preview={}): {}", preview, e.getMessage());
            throw new RuntimeException("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            String preview = token != null ? (token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length() - 8) : token) : "<null>";
            log.error("Unsupported JWT token (preview={}): {}", preview, e.getMessage());
            throw new RuntimeException("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            String preview = token != null ? (token.length() > 16 ? token.substring(0, 8) + "..." + token.substring(token.length() - 8) : token) : "<null>";
            log.error("JWT claims string empty (preview={}): {}", preview, e.getMessage());
            throw new RuntimeException("JWT claims string is empty: " + e.getMessage());
        }
    }
}
