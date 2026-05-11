package com.ma.grecode.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ma.grecode.entity.Game;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface GameService extends IService<Game> {

    IPage<Game> searchGames(String keyword, String platform, String type,
                            String sortBy, int page, int size);

    Game getGameDetail(Long id);

    Game createGame(Game game);

    IPage<Game> getHotGames(int page, int size);

    /**
     * 聚合当前游戏库中真正出现过的平台/类型，按频次降序。
     * 返回结构：{ platforms: [{name, count}, ...], types: [{name, count}, ...] }
     */
    Map<String, Object> getFilters();
}
