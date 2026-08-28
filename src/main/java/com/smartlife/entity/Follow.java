package com.smartlife.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 关注关系。
 */
@Data
@Accessors(chain = true)
public class Follow {

    private Long id;
    private Long userId;
    private Long followUserId;
    private LocalDateTime createTime;
}
