package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.GameType;
import com.ma.grecode.service.GameTypeService;
import com.ma.grecode.mapper.GameTypeMapper;
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




