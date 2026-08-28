package com.smartlife.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Feed 流滚动分页结果。
 */
@Data
@Accessors(chain = true)
public class ScrollResult {

    private List<?> list;
    private Long minTime;
    private Integer offset;
}
