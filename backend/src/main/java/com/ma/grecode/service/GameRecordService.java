package com.ma.grecode.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ma.grecode.entity.GameRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface GameRecordService extends IService<GameRecord> {

    GameRecord createRecord(GameRecord record);

    IPage<GameRecord> listRecords(Long userId, int page, int size);

    List<GameRecord> listRecordsWithGame(Long userId);

    List<Map<String, Object>> getHeatmapData(Long userId, int year);

    Map<String, Object> getStats(Long userId);

    List<Map<String, Object>> getTopGames(Long userId, int limit);

    GameRecord updateRecord(Long id, GameRecord record);

    void deleteRecord(Long id);
}
