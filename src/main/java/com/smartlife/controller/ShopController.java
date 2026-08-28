package com.smartlife.controller;

import com.smartlife.common.Result;
import com.smartlife.entity.Shop;
import com.smartlife.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户接口。
 */
@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /** 商户详情（多级缓存，毫秒级响应） */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        return Result.ok(shopService.queryById(id));
    }

    /** 更新商户（Cache Aside：先更新数据库，再删除缓存） */
    @PutMapping
    public Result update(@RequestBody Shop shop) {
        shopService.update(shop);
        return Result.ok();
    }

    @GetMapping("/of-type")
    public Result queryByType(@RequestParam("typeId") Long typeId,
                              @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(shopService.queryByType(typeId, current));
    }

    @GetMapping("/of-name")
    public Result queryByName(@RequestParam(value = "name", required = false) String name,
                              @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(shopService.queryByName(name, current));
    }
}
