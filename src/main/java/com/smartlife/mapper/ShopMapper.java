package com.smartlife.mapper;

import com.smartlife.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商户 Mapper。
 */
@Mapper
public interface ShopMapper {

    Shop selectById(@Param("id") Long id);

    List<Long> listAllIds();

    List<Shop> selectByTypeId(@Param("typeId") Long typeId,
                              @Param("offset") int offset,
                              @Param("size") int size);

    List<Shop> selectByName(@Param("name") String name,
                            @Param("offset") int offset,
                            @Param("size") int size);

    int update(Shop shop);
}
