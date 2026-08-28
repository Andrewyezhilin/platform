package com.smartlife.config;

import com.smartlife.interceptor.JwtTokenInterceptor;
import com.smartlife.interceptor.LoginInterceptor;
import com.smartlife.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 拦截器注册。
 */
@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局：解析 JWT 并刷新会话（不强制登录）
        registry.addInterceptor(new JwtTokenInterceptor(jwtUtils, stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        // 需要登录的路径
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop/**",
                        "/shop-type/**",
                        "/voucher/list/**",
                        "/blog/hot",
                        "/sse/**",
                        "/mcp/**",
                        "/error"
                )
                .order(1);
    }
}
