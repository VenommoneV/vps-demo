package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
    // MyBatis Plus 自动帮你实现了 insert, select 等方法，不用写 SQL
}

