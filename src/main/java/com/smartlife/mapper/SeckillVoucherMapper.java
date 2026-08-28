package com.smartlife.mapper;

import com.smartlife.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀券 Mapper。
 */
@Mapper
public interface SeckillVoucherMapper {

    SeckillVoucher selectByVoucherId(@Param("voucherId") Long voucherId);

    int insert(SeckillVoucher seckillVoucher);

    /**
     * 扣减库存（乐观锁：stock &gt; 0 才允许扣减，DB 层兜底防超卖）。
     */
    int deductStock(@Param("voucherId") Long voucherId);
}
