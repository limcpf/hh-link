package com.hkhr.link.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// MyBatis 매퍼: USERS 테이블 INSERT
@Mapper
public interface UserMapper {
    int insertOne(UserRow row); // 단건 INSERT

    int insertAll(@Param("list") List<UserRow> rows); // INSERT ALL(벌크)
}
