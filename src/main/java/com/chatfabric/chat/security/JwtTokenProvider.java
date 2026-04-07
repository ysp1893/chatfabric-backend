package com.chatfabric.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String secret;
    private final long expirationSeconds;

    private SecretKey secretKey;

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret,
                            @Value("${app.security.jwt.expiration-seconds}") long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    @PostConstruct
    public void initialize() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return generateToken(principal);
    }

    public String generateToken(SecurityUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .setSubject(principal.getUsername())
                .claim("userId", principal.getId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        return Long.valueOf(String.valueOf(userId));
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
