package com.ma.grecorde.mapper;

import com.ma.grecorde.entity.GameType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 19135
* @description 针对表【game_type(游戏类型表)】的数据库操作Mapper
* @createDate 2025-12-08 16:35:49
* @Entity grecorde.entity.GameType
*/
@Mapper
public interface GameTypeMapper extends BaseMapper<GameType> {

}




