package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.common.UserHolder;
import com.smartlife.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券订单接口。
 */
@RestController
@RequestMapping("/voucher-order")
@RequiredArgsConstructor
public class VoucherOrderController {

    private final VoucherOrderService voucherOrderService;

    /** 秒杀下单：Lua 原子判定资格，订单异步落库，立即返回订单号 */
    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return Result.ok(voucherOrderService.seckillVoucher(voucherId));
    }

    /** 我的订单 */
    @GetMapping("/mine")
    public Result myOrders() {
        return Result.ok(voucherOrderService.queryMyOrders(UserHolder.getUser().getId()));
    }
}
