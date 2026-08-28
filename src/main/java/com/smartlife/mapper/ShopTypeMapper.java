package com.smartlife.mapper;

import com.smartlife.entity.ShopType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商户类型 Mapper。
 */
@Mapper
public interface ShopTypeMapper {

    List<ShopType> listAll();
}
