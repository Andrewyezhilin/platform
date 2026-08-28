package com.smartlife.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商户。
 */
@Data
@Accessors(chain = true)
public class Shop {

    private Long id;
    private String name;
    private Long typeId;
    private String images;
    private String area;
    private String address;
    private Double x;
    private Double y;
    /** 均价（分） */
    private Long avgPrice;
    private Integer sold;
    private Integer comments;
    /** 评分 * 10，如 46 表示 4.6 分 */
    private Integer score;
    private String openHours;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
