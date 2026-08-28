package com.smartlife.service;

import com.smartlife.common.RedisConstants;
import com.smartlife.entity.SeckillVoucher;
import com.smartlife.entity.Voucher;
import com.smartlife.mapper.SeckillVoucherMapper;
import com.smartlife.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 优惠券服务：秒杀券上架时把库存预热到 Redis，供 Lua 脚本原子扣减。
 */
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public List<Voucher> queryVoucherOfShop(Long shopId) {
        return voucherMapper.selectByShopId(shopId);
    }

    public Voucher queryById(Long id) {
        return voucherMapper.selectById(id);
    }

    /**
     * 新增秒杀券：券信息与库存落库，同时把库存写入 Redis 完成预热。
     */
    @Transactional
    public Long addSeckillVoucher(Voucher voucher) {
        voucher.setType(1);
        voucher.setStatus(1);
        voucherMapper.insert(voucher);

        SeckillVoucher seckillVoucher = new SeckillVoucher()
                .setVoucherId(voucher.getId())
                .setStock(voucher.getStock())
                .setBeginTime(voucher.getBeginTime())
                .setEndTime(voucher.getEndTime());
        seckillVoucherMapper.insert(seckillVoucher);

        // 库存预热：秒杀期间所有资格校验都在 Redis 内完成，不触碰数据库
        stringRedisTemplate.opsForValue().set(
                RedisConstants.SECKILL_STOCK_KEY + voucher.getId(),
                String.valueOf(voucher.getStock()));
        return voucher.getId();
    }
}
