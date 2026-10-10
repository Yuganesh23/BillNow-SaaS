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

    @org.springframework.beans.factory.annotation.Value("${jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User_entity user) {

        return Jwts.builder()
                .subject(user.getId().toString())
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
                        new Date(System.currentTimeMillis() + accessTokenMinutes * 60_000)
                )
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public Long extractUserId(String token) {
        Number userId = getClaims(token).get("userId", Number.class);
        return userId == null ? null : userId.longValue();
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

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }
}
