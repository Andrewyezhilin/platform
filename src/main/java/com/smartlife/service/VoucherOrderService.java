package com.smartlife.service;

import com.smartlife.common.RedisConstants;
import com.smartlife.common.UserHolder;
import com.smartlife.common.exception.BizException;
import com.smartlife.entity.SeckillVoucher;
import com.smartlife.entity.VoucherOrder;
import com.smartlife.mapper.SeckillVoucherMapper;
import com.smartlife.mapper.VoucherOrderMapper;
import com.smartlife.utils.RedisIdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀下单服务。
 *
 * <p>链路：请求 -> Lua 脚本原子完成【库存校验 + 预扣 + 一人一单判定 + 消息入队】
 * -> 立即返回订单号（异步下单）-> 后台消费者从 Redis Stream 取消息
 * -> Redisson 分布式锁兜底"一人一单" -> 乐观锁扣减 DB 库存 -> 订单落库。</p>
 *
 * <p>超卖问题由三层防线保证：Lua 原子判定（主）、DB 乐观锁 stock &gt; 0（兜底）、
 * 订单表 (user_id, voucher_id) 唯一索引（最终一致）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderService {

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final RedisIdWorker redisIdWorker;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * 秒杀入口：Lua 脚本判定资格后立即返回，订单由消息队列异步落库。
     */
    public long seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        // 校验秒杀时间窗口
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectByVoucherId(voucherId);
        if (seckillVoucher == null) {
            throw new BizException("秒杀券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillVoucher.getBeginTime())) {
            throw new BizException("秒杀尚未开始");
        }
        if (now.isAfter(seckillVoucher.getEndTime())) {
            throw new BizException("秒杀已经结束");
        }

        long orderId = redisIdWorker.nextId("order");
        // Lua 原子执行：库存校验 + 扣减 + 去重 + XADD 入队
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                List.of(RedisConstants.SECKILL_STOCK_KEY + voucherId,
                        RedisConstants.SECKILL_ORDER_KEY + voucherId,
                        RedisConstants.STREAM_ORDERS_KEY),
                String.valueOf(voucherId),
                String.valueOf(userId),
                String.valueOf(orderId));

        int r = result == null ? -1 : result.intValue();
        if (r == 1) {
            throw new BizException("库存不足");
        }
        if (r == 2) {
            throw new BizException("同一用户不允许重复下单");
        }
        if (r != 0) {
            throw new BizException("下单失败，请稍后重试");
        }
        return orderId;
    }

    /**
     * 异步落库（由 Stream 消费者调用）：Redisson 分布式锁保证"一人一单"，
     * 事务内完成 DB 校验、乐观锁扣库存与订单写入。
     */
    public void createVoucherOrder(VoucherOrder order) {
        Long userId = order.getUserId();
        Long voucherId = order.getVoucherId();
        RLock lock = redissonClient.getLock(
                RedisConstants.LOCK_ORDER_KEY + userId + ":" + voucherId);
        if (!lock.tryLock()) {
            log.warn("重复的下单请求, userId={}, voucherId={}", userId, voucherId);
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                // 一人一单兜底校验（Lua 已在 Redis 层判定，此处防御消息重放）
                if (voucherOrderMapper.countByUserIdAndVoucherId(userId, voucherId) > 0) {
                    log.warn("用户已购买过该券, userId={}, voucherId={}", userId, voucherId);
                    return;
                }
                // 乐观锁扣库存：stock > 0 才能成功，DB 层兜底防超卖
                if (seckillVoucherMapper.deductStock(voucherId) <= 0) {
                    log.warn("数据库库存不足, voucherId={}", voucherId);
                    return;
                }
                voucherOrderMapper.insert(order);
            });
        } finally {
            lock.unlock();
        }
    }

    public List<VoucherOrder> queryMyOrders(Long userId) {
        return voucherOrderMapper.selectByUserId(userId);
    }
}
