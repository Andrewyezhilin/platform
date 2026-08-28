package com.smartlife.common;

/**
 * Redis Key 与 TTL 常量。
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /** 登录验证码 */
    public static final String LOGIN_CODE_KEY = "smartlife:login:code:";
    public static final long LOGIN_CODE_TTL_MINUTES = 5L;

    /** 登录用户信息（配合 JWT 做服务端会话控制） */
    public static final String LOGIN_USER_KEY = "smartlife:login:user:";
    public static final long LOGIN_USER_TTL_MINUTES = 30L;

    /** 商户缓存 */
    public static final String CACHE_SHOP_KEY = "smartlife:cache:shop:";
    public static final long CACHE_SHOP_TTL_MINUTES = 30L;

    /** 商户类型缓存 */
    public static final String CACHE_SHOP_TYPE_KEY = "smartlife:cache:shop-type";
    public static final long CACHE_SHOP_TYPE_TTL_MINUTES = 60L;

    /** 空值缓存 TTL（防穿透兜底） */
    public static final long CACHE_NULL_TTL_MINUTES = 2L;

    /** 缓存重建互斥锁 */
    public static final String LOCK_SHOP_KEY = "smartlife:lock:shop:";

    /** 商户布隆过滤器 */
    public static final String BLOOM_SHOP_KEY = "smartlife:bloom:shop";

    /** 一级缓存（Caffeine）失效广播频道 */
    public static final String TOPIC_CACHE_EVICT = "smartlife:topic:cache-evict";

    /** 秒杀库存 */
    public static final String SECKILL_STOCK_KEY = "smartlife:seckill:stock:";

    /** 秒杀下单用户去重集合 */
    public static final String SECKILL_ORDER_KEY = "smartlife:seckill:order:";

    /** 秒杀订单消息队列（Redis Stream） */
    public static final String STREAM_ORDERS_KEY = "smartlife:stream:orders";
    public static final String STREAM_ORDERS_GROUP = "order-group";

    /** 一人一单分布式锁 */
    public static final String LOCK_ORDER_KEY = "smartlife:lock:order:";

    /** 博客点赞 */
    public static final String BLOG_LIKED_KEY = "smartlife:blog:liked:";

    /** 关注列表 */
    public static final String FOLLOW_KEY = "smartlife:follow:";

    /** Feed 流收件箱 */
    public static final String FEED_KEY = "smartlife:feed:";

    /** 全局 ID 生成器 */
    public static final String ICR_KEY = "smartlife:icr:";

    /** 智能客服会话记忆 */
    public static final String CHAT_MEMORY_KEY = "smartlife:ai:chat:";
    public static final long CHAT_MEMORY_TTL_HOURS = 24L;
}
