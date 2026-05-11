package com.ma.grecode.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ma.grecode.entity.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Test
    void searchGames_withKeyword_returnsResults() {
        IPage<Game> result = gameService.searchGames("塞尔达", null, null, "rating_desc", 1, 10);
        assertNotNull(result);
        assertNotNull(result.getRecords());
    }

    @Test
    void searchGames_withEmptyKeyword_returnsAll() {
        IPage<Game> result = gameService.searchGames("", null, null, "rating_desc", 1, 10);
        assertNotNull(result);
    }

    @Test
    void getGameDetail_withInvalidId_throwsException() {
        assertThrows(Exception.class, () -> gameService.getGameDetail(999999L));
    }

    @Test
    void getHotGames_returnsPagedResults() {
        IPage<Game> result = gameService.getHotGames(1, 10);
        assertNotNull(result);
        assertTrue(result.getSize() <= 10);
    }
}
