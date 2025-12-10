package com.ma.grecode.mapper;

import com.ma.grecode.entity.GameRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 19135
* @description 针对表【game_record(打卡记录表)】的数据库操作Mapper
* @createDate 2025-12-08 16:35:49
* @Entity grecorde.entity.GameRecord
*/
@Mapper
public interface GameRecordMapper extends BaseMapper<GameRecord> {

}




