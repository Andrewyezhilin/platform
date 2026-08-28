package com.smartlife.service;

import com.smartlife.common.RedisConstants;
import com.smartlife.common.UserHolder;
import com.smartlife.entity.Follow;
import com.smartlife.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关注服务：关注关系落库，同时在 Redis Set 中维护关注列表（支持共同关注求交集）。
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public void follow(Long followUserId, boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_KEY + userId;
        if (isFollow) {
            Follow follow = new Follow()
                    .setUserId(userId)
                    .setFollowUserId(followUserId);
            if (followMapper.insert(follow) > 0) {
                stringRedisTemplate.opsForSet().add(key, String.valueOf(followUserId));
            }
        } else {
            if (followMapper.delete(userId, followUserId) > 0) {
                stringRedisTemplate.opsForSet().remove(key, String.valueOf(followUserId));
            }
        }
    }

    public boolean isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        return followMapper.count(userId, followUserId) > 0;
    }
}
