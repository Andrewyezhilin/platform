package com.smartlife.mapper;

import com.smartlife.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 关注关系 Mapper。
 */
@Mapper
public interface FollowMapper {

    int insert(Follow follow);

    int delete(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    int count(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    /** 查询某用户的粉丝 ID 列表（用于 Feed 推模式投递） */
    List<Long> selectFansIds(@Param("followUserId") Long followUserId);
}
