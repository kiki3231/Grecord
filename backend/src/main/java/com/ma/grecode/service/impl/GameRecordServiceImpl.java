package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.GameRecord;
import com.ma.grecode.service.GameRecordService;
import com.ma.grecode.mapper.GameRecordMapper;
import org.springframework.stereotype.Service;

/**
* @author 19135
* @description 针对表【game_record(打卡记录表)】的数据库操作Service实现
* @createDate 2025-12-08 16:35:49
*/
@Service
public class GameRecordServiceImpl extends ServiceImpl<GameRecordMapper, GameRecord>
    implements GameRecordService{

}




