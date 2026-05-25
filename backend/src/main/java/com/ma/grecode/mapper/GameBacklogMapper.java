package com.ma.grecode.mapper;

import com.ma.grecode.entity.GameBacklog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GameBacklogMapper extends BaseMapper<GameBacklog> {

    List<GameBacklog> selectBacklogWithGame(@Param("userId") Long userId, @Param("status") Integer status);

    List<Map<String, Object>> countByStatus(@Param("userId") Long userId);
}
