package com.ma.grecorde.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecorde.entity.GameType;
import com.ma.grecorde.service.GameTypeService;
import com.ma.grecorde.mapper.GameTypeMapper;
import org.springframework.stereotype.Service;

/**
* @author 19135
* @description 针对表【game_type(游戏类型表)】的数据库操作Service实现
* @createDate 2025-12-08 16:35:49
*/
@Service
public class GameTypeServiceImpl extends ServiceImpl<GameTypeMapper, GameType>
    implements GameTypeService{

}




