package com.smartlife.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.smartlife.common.RedisConstants;
import com.smartlife.common.SystemConstants;
import com.smartlife.common.exception.BizException;
import com.smartlife.dto.LoginFormDTO;
import com.smartlife.dto.UserDTO;
import com.smartlife.entity.User;
import com.smartlife.mapper.UserMapper;
import com.smartlife.utils.JwtUtils;
import com.smartlife.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户服务：手机验证码登录 + JWT 无状态令牌 + Redis 会话。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;

    /**
     * 发送验证码（演示环境直接返回验证码，生产环境对接短信网关）。
     */
    public String sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BizException("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_CODE_KEY + phone, code,
                Duration.ofMinutes(RedisConstants.LOGIN_CODE_TTL_MINUTES));
        log.info("发送验证码到 {}: {}", phone, code);
        return code;
    }

    /**
     * 登录/注册：验证码通过后签发 JWT，并在 Redis 建立服务端会话。
     */
    public String login(LoginFormDTO form) {
        if (RegexUtils.isPhoneInvalid(form.getPhone())) {
            throw new BizException("手机号格式错误");
        }
        String cacheCode = stringRedisTemplate.opsForValue()
                .get(RedisConstants.LOGIN_CODE_KEY + form.getPhone());
        if (cacheCode == null || !cacheCode.equals(form.getCode())) {
            throw new BizException("验证码错误或已过期");
        }
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + form.getPhone());

        User user = userMapper.selectByPhone(form.getPhone());
        if (user == null) {
            user = createUserWithPhone(form.getPhone());
        }

        UserDTO userDTO = new UserDTO()
                .setId(user.getId())
                .setNickName(user.getNickName())
                .setIcon(user.getIcon());
        // JWT + Redis 双重会话：JWT 无状态校验，Redis 支持续期与踢下线
        String token = jwtUtils.createToken(userDTO);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_USER_KEY + user.getId(),
                JSONUtil.toJsonStr(userDTO),
                Duration.ofMinutes(RedisConstants.LOGIN_USER_TTL_MINUTES));
        return token;
    }

    public void logout(Long userId) {
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + userId);
    }

    private User createUserWithPhone(String phone) {
        User user = new User()
                .setPhone(phone)
                .setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8))
                .setIcon("");
        userMapper.insert(user);
        return user;
    }
}
