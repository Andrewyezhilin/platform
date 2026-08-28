package com.smartlife.mapper;

import com.smartlife.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论/探店笔记 Mapper。
 */
@Mapper
public interface BlogMapper {

    Blog selectById(@Param("id") Long id);

    List<Blog> selectHot(@Param("offset") int offset, @Param("size") int size);

    List<Blog> selectByIds(@Param("ids") List<Long> ids);

    List<Blog> selectByShopId(@Param("shopId") Long shopId, @Param("size") int size);

    int insert(Blog blog);

    int updateLiked(@Param("id") Long id, @Param("delta") int delta);
}
