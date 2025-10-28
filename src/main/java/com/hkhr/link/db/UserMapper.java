package com.hkhr.link.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    int insertOne(UserRow row);

    int insertAll(@Param("list") List<UserRow> rows);
}

