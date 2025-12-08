package com.ma.grecorde.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecorde.entity.Game;
import com.ma.grecorde.service.GameService;
import com.ma.grecorde.mapper.GameMapper;
import org.springframework.stereotype.Service;

/**
* @author 19135
* @description 针对表【game(游戏表)】的数据库操作Service实现
* @createDate 2025-12-08 16:35:49
*/
@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game>
    implements GameService{

}




