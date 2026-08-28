package com.smartlife.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户评论/探店笔记。
 */
@Data
@Accessors(chain = true)
public class Blog {

    private Long id;
    private Long shopId;
    private Long userId;
    private String title;
    private String images;
    private String content;
    private Integer liked;
    private Integer comments;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 非表字段：作者信息与当前用户是否点赞 */
    private String name;
    private String icon;
    private Boolean isLike;
}
