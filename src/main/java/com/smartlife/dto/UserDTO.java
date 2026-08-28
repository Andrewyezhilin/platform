package com.smartlife.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 登录用户脱敏信息。
 */
@Data
@Accessors(chain = true)
public class UserDTO {

    private Long id;
    private String nickName;
    private String icon;
}
