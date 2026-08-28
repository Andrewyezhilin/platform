package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.entity.Blog;
import com.smartlife.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论/探店笔记接口。
 */
@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    /** 发布评论（推模式推送到粉丝收件箱） */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return Result.ok(blogService.saveBlog(blog));
    }

    /** 点赞/取消点赞 */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        blogService.likeBlog(id);
        return Result.ok();
    }

    /** 热门评论 */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryHotBlog(current));
    }

    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        return Result.ok(blogService.queryById(id));
    }

    /** 关注 Feed 流（滚动分页） */
    @GetMapping("/of-follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return Result.ok(blogService.queryBlogOfFollow(max, offset));
    }
}
