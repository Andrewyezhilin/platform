package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注接口。
 */
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,
                         @PathVariable("isFollow") Boolean isFollow) {
        followService.follow(followUserId, isFollow);
        return Result.ok();
    }

    @GetMapping("/or-not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return Result.ok(followService.isFollow(followUserId));
    }
}
