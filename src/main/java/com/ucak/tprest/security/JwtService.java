package com.ucak.tprest.security;

import com.ucak.tprest.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret-base64}")
    private String secretBase64;

    @Value("${app.jwt.ttl-seconds:900}")
    private long ttlSeconds;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 est obligatoire et ne peut pas être vide");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(secretBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET_BASE64 doit être une chaîne valide encodée en Base64", e);
        }

        // Vérification de la taille : au moins 32 octets (256 bits) pour HS256
        if (decodedKey.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 doit comporter au moins 32 octets (256 bits) après décodage pour HS256");
        }

        this.signingKey = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (ttlSeconds * 1000));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
