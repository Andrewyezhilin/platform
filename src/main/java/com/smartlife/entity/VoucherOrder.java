package com.smartlife.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 优惠券订单。
 */
@Data
@Accessors(chain = true)
public class VoucherOrder {

    private Long id;
    private Long userId;
    private Long voucherId;
    /** 支付方式：1-余额 2-支付宝 3-微信 */
    private Integer payType;
    /** 状态：1-未支付 2-已支付 3-已核销 4-已取消 5-退款中 6-已退款 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime useTime;
    private LocalDateTime refundTime;
    private LocalDateTime updateTime;
}
