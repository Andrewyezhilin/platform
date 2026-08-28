package com.smartlife.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 秒杀券库存与时间窗口。
 */
@Data
@Accessors(chain = true)
public class SeckillVoucher {

    private Long voucherId;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
