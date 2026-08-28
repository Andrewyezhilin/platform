package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.entity.Voucher;
import com.smartlife.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券接口。
 */
@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    /** 查询商户在售的优惠券 */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return Result.ok(voucherService.queryVoucherOfShop(shopId));
    }

    /** 新增秒杀券（库存同步预热到 Redis） */
    @PostMapping("/seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        return Result.ok(voucherService.addSeckillVoucher(voucher));
    }
}
