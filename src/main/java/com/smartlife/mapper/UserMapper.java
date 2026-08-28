package com.smartlife.mapper;

import com.smartlife.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper。
 */
@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByPhone(@Param("phone") String phone);

    int insert(User user);
}
