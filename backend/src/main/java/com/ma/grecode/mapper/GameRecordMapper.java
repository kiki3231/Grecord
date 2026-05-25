package com.ma.grecode.mapper;

import com.ma.grecode.entity.GameRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface GameRecordMapper extends BaseMapper<GameRecord> {

    List<GameRecord> selectRecordWithGame(@Param("userId") Long userId);

    List<Map<String, Object>> selectHeatmapData(@Param("userId") Long userId, @Param("year") int year);

    Map<String, Object> selectUserStats(@Param("userId") Long userId);

    List<Map<String, Object>> selectTopGames(@Param("userId") Long userId, @Param("limit") int limit);
}
