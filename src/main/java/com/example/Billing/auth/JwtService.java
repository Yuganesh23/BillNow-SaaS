package com.example.Billing.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@lombok.RequiredArgsConstructor
public class JwtService {

    private final ShopContextResolver shopContextResolver;

    @org.springframework.beans.factory.annotation.Value("${jwt.secret:SmartBillSecretKeyForJwtAuthentication2026SecureKey}")
    private String secretKey;

    private static final long EXPIRATION_TIME =
            1000 * 60 * 60; // 1 hour

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User_entity user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim(
                        "shopId",
                        shopContextResolver.resolveActiveShop(user) != null
                                ? shopContextResolver.resolveActiveShop(user).getId()
                                : null
                )
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + EXPIRATION_TIME)
                )
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {

        return getClaims(token)
                .getSubject();
    }

    public Long extractUserId(String token) {

        return getClaims(token)
                .get("userId", Long.class);
    }

    public Long extractShopId(String token) {

        return getClaims(token)
                .get("shopId", Long.class);
    }

    public String extractRole(String token) {

        return getClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {

        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}