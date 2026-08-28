package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.common.UserHolder;
import com.smartlife.dto.LoginFormDTO;
import com.smartlife.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 发送验证码（演示环境直接返回验证码） */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return Result.ok(userService.sendCode(phone));
    }

    /** 验证码登录，返回 JWT 令牌 */
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginFormDTO loginForm) {
        return Result.ok(userService.login(loginForm));
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }

    /** 退出登录（删除服务端会话，令牌立即失效） */
    @PostMapping("/logout")
    public Result logout() {
        userService.logout(UserHolder.getUser().getId());
        return Result.ok();
    }
}
