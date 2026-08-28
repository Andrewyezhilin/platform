package com.smartlife.ai.mcp;

import cn.hutool.json.JSONUtil;
import com.smartlife.entity.Blog;
import com.smartlife.entity.Shop;
import com.smartlife.entity.Voucher;
import com.smartlife.entity.VoucherOrder;
import com.smartlife.service.BlogService;
import com.smartlife.service.ShopService;
import com.smartlife.service.ShopTypeService;
import com.smartlife.service.VoucherOrderService;
import com.smartlife.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 工具集：把平台核心能力以标准化 MCP Tool 的形式暴露，
 * 既供本服务的智能客服调用，也通过 MCP Server 提供给任意外部 MCP 客户端，
 * 实现标准化、可复用的 AI 服务接口。
 */
@Component
@RequiredArgsConstructor
public class SmartLifeTools {

    private final ShopService shopService;
    private final ShopTypeService shopTypeService;
    private final VoucherService voucherService;
    private final VoucherOrderService voucherOrderService;
    private final BlogService blogService;

    @Tool(description = "按名称关键字搜索商户，返回商户列表（含 ID、名称、地址、评分、人均价格）")
    public String searchShops(@ToolParam(description = "商户名称关键字") String keyword) {
        List<Shop> shops = shopService.queryByName(keyword, 1);
        if (shops.isEmpty()) {
            return "没有找到名称包含\"" + keyword + "\"的商户";
        }
        return JSONUtil.toJsonStr(shops.stream()
                .map(s -> Map.of(
                        "id", s.getId(),
                        "name", s.getName(),
                        "area", s.getArea(),
                        "address", s.getAddress(),
                        "score", s.getScore() / 10.0,
                        "avgPriceYuan", s.getAvgPrice() / 100.0))
                .toList());
    }

    @Tool(description = "查询商户详情，包括地址、营业时间、评分、销量等")
    public String getShopDetail(@ToolParam(description = "商户 ID") Long shopId) {
        Shop shop = shopService.queryById(shopId);
        if (shop == null) {
            return "商户不存在";
        }
        return JSONUtil.toJsonStr(shop);
    }

    @Tool(description = "查询平台全部商户分类")
    public String listShopTypes() {
        return JSONUtil.toJsonStr(shopTypeService.queryList());
    }

    @Tool(description = "查询指定商户在售的优惠券（含普通券与秒杀券、价格与库存）")
    public String listVouchersOfShop(@ToolParam(description = "商户 ID") Long shopId) {
        List<Voucher> vouchers = voucherService.queryVoucherOfShop(shopId);
        if (vouchers.isEmpty()) {
            return "该商户暂无在售优惠券";
        }
        return JSONUtil.toJsonStr(vouchers);
    }

    @Tool(description = "查询当前登录用户的优惠券订单列表（个性化服务，需要用户已登录）")
    public String getMyOrders(ToolContext toolContext) {
        Object userId = toolContext.getContext().get("userId");
        if (userId == null) {
            return "用户未登录，无法查询订单，请先登录";
        }
        List<VoucherOrder> orders = voucherOrderService.queryMyOrders((Long) userId);
        if (orders.isEmpty()) {
            return "您还没有任何订单";
        }
        return JSONUtil.toJsonStr(orders);
    }

    @Tool(description = "获取指定商户的用户评论（探店笔记），可用于评论情感分析与总结")
    public String getShopReviews(@ToolParam(description = "商户 ID") Long shopId) {
        List<Blog> blogs = blogService.queryByShopId(shopId, 20);
        if (blogs.isEmpty()) {
            return "该商户暂无用户评论";
        }
        return blogs.stream()
                .map(b -> "- [" + b.getTitle() + "] " + b.getContent()
                        + "（点赞 " + b.getLiked() + "）")
                .collect(Collectors.joining("\n"));
    }
}
