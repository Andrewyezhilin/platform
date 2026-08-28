package com.smartlife.service;

import cn.hutool.core.util.StrUtil;
import com.smartlife.common.RedisConstants;
import com.smartlife.common.SystemConstants;
import com.smartlife.common.UserHolder;
import com.smartlife.common.exception.BizException;
import com.smartlife.dto.ScrollResult;
import com.smartlife.dto.UserDTO;
import com.smartlife.entity.Blog;
import com.smartlife.entity.User;
import com.smartlife.mapper.BlogMapper;
import com.smartlife.mapper.FollowMapper;
import com.smartlife.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 评论/探店笔记服务：点赞（ZSet 排行）+ Feed 流推模式消息推送。
 */
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogMapper blogMapper;
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发布评论，并以"推模式"将消息推送到所有粉丝的收件箱（Redis ZSet，score 为时间戳）。
     */
    public Long saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        if (blogMapper.insert(blog) <= 0) {
            throw new BizException("发布失败");
        }
        // Feed 推模式：写扩散到每个粉丝的收件箱
        List<Long> fansIds = followMapper.selectFansIds(user.getId());
        long now = System.currentTimeMillis();
        for (Long fanId : fansIds) {
            stringRedisTemplate.opsForZSet().add(
                    RedisConstants.FEED_KEY + fanId,
                    String.valueOf(blog.getId()), now);
        }
        return blog.getId();
    }

    /**
     * 点赞/取消点赞：ZSet 记录点赞用户与时间，支持点赞排行。
     */
    public void likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));
        if (score == null) {
            if (blogMapper.updateLiked(id, 1) > 0) {
                stringRedisTemplate.opsForZSet()
                        .add(key, String.valueOf(userId), System.currentTimeMillis());
            }
        } else {
            if (blogMapper.updateLiked(id, -1) > 0) {
                stringRedisTemplate.opsForZSet().remove(key, String.valueOf(userId));
            }
        }
    }

    public Blog queryById(Long id) {
        Blog blog = blogMapper.selectById(id);
        if (blog == null) {
            throw new BizException("笔记不存在");
        }
        fillBlogUser(blog);
        fillIsLiked(blog);
        return blog;
    }

    public List<Blog> queryHotBlog(Integer page) {
        int current = page == null || page < 1 ? 1 : page;
        List<Blog> blogs = blogMapper.selectHot(
                (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE,
                SystemConstants.DEFAULT_PAGE_SIZE);
        blogs.forEach(blog -> {
            fillBlogUser(blog);
            fillIsLiked(blog);
        });
        return blogs;
    }

    /**
     * 关注 Feed 流滚动分页：从收件箱 ZSet 按时间倒序拉取。
     *
     * @param max    上一页最小时间戳（首次为当前时间）
     * @param offset 与最小时间戳相同分值的已读条数（去重偏移）
     */
    public ScrollResult queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max,
                        offset == null ? 0 : offset, SystemConstants.DEFAULT_PAGE_SIZE);
        if (tuples == null || tuples.isEmpty()) {
            return new ScrollResult().setList(List.of()).setMinTime(0L).setOffset(0);
        }

        List<Long> ids = new ArrayList<>(tuples.size());
        long minTime = 0;
        int nextOffset = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                nextOffset++;
            } else {
                minTime = time;
                nextOffset = 1;
            }
        }

        List<Blog> blogs = blogMapper.selectByIds(ids);
        blogs.forEach(blog -> {
            fillBlogUser(blog);
            fillIsLiked(blog);
        });
        return new ScrollResult().setList(blogs).setMinTime(minTime).setOffset(nextOffset);
    }

    public List<Blog> queryByShopId(Long shopId, int size) {
        return blogMapper.selectByShopId(shopId, size);
    }

    private void fillBlogUser(Blog blog) {
        User user = userMapper.selectById(blog.getUserId());
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
    }

    private void fillIsLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            blog.setIsLike(false);
            return;
        }
        Double score = stringRedisTemplate.opsForZSet()
                .score(RedisConstants.BLOG_LIKED_KEY + blog.getId(), String.valueOf(user.getId()));
        blog.setIsLike(score != null);
    }
}
