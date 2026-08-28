package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.service.ShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户类型接口。
 */
@RestController
@RequestMapping("/shop-type")
@RequiredArgsConstructor
public class ShopTypeController {

    private final ShopTypeService shopTypeService;

    @GetMapping("/list")
    public Result queryList() {
        return Result.ok(shopTypeService.queryList());
    }
}
