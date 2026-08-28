package com.smartlife.mapper;

import com.smartlife.entity.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 优惠券订单 Mapper。
 */
@Mapper
public interface VoucherOrderMapper {

    int insert(VoucherOrder order);

    int countByUserIdAndVoucherId(@Param("userId") Long userId,
                                  @Param("voucherId") Long voucherId);

    List<VoucherOrder> selectByUserId(@Param("userId") Long userId);
}
