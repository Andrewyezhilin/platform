package com.smartlife.interceptor;

import cn.hutool.json.JSONUtil;
import com.smartlife.common.RedisConstants;
import com.smartlife.common.UserHolder;
import com.smartlife.dto.UserDTO;
import com.smartlife.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * JWT 令牌解析拦截器（拦截所有请求，但不强制登录）。
 *
 * <p>双重校验：JWT 签名合法 + Redis 会话存在。命中后滑动续期会话 TTL，
 * 并把用户信息放入 ThreadLocal 供业务使用。</p>
 */
@RequiredArgsConstructor
public class JwtTokenInterceptor implements HandlerInterceptor {

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            return true;
        }
        String token = header.substring(TOKEN_PREFIX.length());
        // 1. 校验 JWT 签名与有效期
        UserDTO user = jwtUtils.parseToken(token);
        if (user == null) {
            return true;
        }
        // 2. 校验服务端会话（支持主动踢下线）
        String sessionKey = RedisConstants.LOGIN_USER_KEY + user.getId();
        String json = stringRedisTemplate.opsForValue().get(sessionKey);
        if (json == null) {
            return true;
        }
        // 3. 滑动续期 + 写入 ThreadLocal
        stringRedisTemplate.expire(sessionKey,
                Duration.ofMinutes(RedisConstants.LOGIN_USER_TTL_MINUTES));
        UserHolder.saveUser(JSONUtil.toBean(json, UserDTO.class));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
