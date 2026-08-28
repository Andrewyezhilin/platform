package com.smartlife.utils;

import com.smartlife.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具：签发与校验访问令牌。
 *
 * <p>令牌中只存放非敏感的用户标识信息；服务端同时在 Redis 保存会话，
 * 支持主动踢下线与滑动续期。</p>
 */
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long ttlMillis;

    public JwtUtils(@Value("${smartlife.jwt.secret}") String secret,
                    @Value("${smartlife.jwt.ttl-minutes:30}") long ttlMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlMinutes * 60_000L;
    }

    /**
     * 为登录用户签发 JWT。
     */
    public String createToken(UserDTO user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(Map.of(
                        "nickName", user.getNickName() == null ? "" : user.getNickName(),
                        "icon", user.getIcon() == null ? "" : user.getIcon()))
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 校验并解析 JWT，失败返回 null。
     */
    public UserDTO parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new UserDTO()
                    .setId(Long.valueOf(claims.getSubject()))
                    .setNickName(claims.get("nickName", String.class))
                    .setIcon(claims.get("icon", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
