package com.ma.grecode.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GameRecordServiceTest {

    @Autowired
    private GameRecordService gameRecordService;

    @Test
    void getHeatmapData_returnsListForYear() {
        List<Map<String, Object>> data = gameRecordService.getHeatmapData(1L, 2026);
        assertNotNull(data);
    }

    @Test
    void getStats_returnsSummary() {
        Map<String, Object> stats = gameRecordService.getStats(1L);
        assertNotNull(stats);
        assertNotNull(stats.get("totalMinutes"));
        assertNotNull(stats.get("gameCount"));
    }

    @Test
    void getTopGames_returnsLimitedList() {
        List<Map<String, Object>> topGames = gameRecordService.getTopGames(1L, 5);
        assertNotNull(topGames);
        assertTrue(topGames.size() <= 5);
    }
}
