package com.smartlife.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 优惠券。
 */
@Data
@Accessors(chain = true)
public class Voucher {

    private Long id;
    private Long shopId;
    private String title;
    private String subTitle;
    private String rules;
    /** 支付金额（分） */
    private Long payValue;
    /** 抵扣金额（分） */
    private Long actualValue;
    /** 类型：0-普通券 1-秒杀券 */
    private Integer type;
    /** 状态：1-上架 2-下架 3-过期 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 秒杀券扩展字段（非本表字段） */
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
}
