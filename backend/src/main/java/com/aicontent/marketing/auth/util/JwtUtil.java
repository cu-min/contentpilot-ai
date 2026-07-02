package com.aicontent.marketing.auth.util;

import com.aicontent.marketing.user.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;

    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.secret = secret;
        this.expiration = expiration;
    }

    public String generateToken(SysUser user) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(userId));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
