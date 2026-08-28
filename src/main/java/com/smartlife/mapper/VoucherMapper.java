package com.smartlife.mapper;

import com.smartlife.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 优惠券 Mapper。
 */
@Mapper
public interface VoucherMapper {

    Voucher selectById(@Param("id") Long id);

    List<Voucher> selectByShopId(@Param("shopId") Long shopId);

    int insert(Voucher voucher);
}
