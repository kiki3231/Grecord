package com.ma.grecode.mapper;

import com.ma.grecode.entity.Game;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 19135
* @description 针对表【game(游戏表)】的数据库操作Mapper
* @createDate 2025-12-08 16:35:49
* @Entity grecorde.entity.Game
*/
@Mapper
public interface GameMapper extends BaseMapper<Game> {

}




