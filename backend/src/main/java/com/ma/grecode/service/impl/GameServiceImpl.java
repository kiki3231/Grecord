package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.Game;
import com.ma.grecode.exception.BusinessException;
import com.ma.grecode.mapper.GameMapper;
import com.ma.grecode.service.GameService;
import com.ma.grecode.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

    @Override
    public IPage<Game> searchGames(String keyword, String platform, String type,
                                   String sortBy, int page, int size) {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Game::getIsDelete, 0);

        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(Game::getName, kw)
                .or().like(Game::getGameTypes, kw)
                .or().like(Game::getPlatforms, kw)
                .or().like(Game::getDeveloper, kw)
            );
        }
        if (StringUtils.isNotBlank(platform)) {
            wrapper.like(Game::getPlatforms, platform.trim());
        }
        if (StringUtils.isNotBlank(type)) {
            wrapper.like(Game::getGameTypes, type.trim());
        }

        applySort(wrapper, sortBy);
        return page(new Page<>(page, size), wrapper);
    }

    private void applySort(LambdaQueryWrapper<Game> wrapper, String sortBy) {
        String key = sortBy == null ? "" : sortBy.trim().toLowerCase();
        if ("rating_asc".equals(key)) {
            wrapper.orderByAsc(Game::getRating).orderByDesc(Game::getId);
        } else if ("name_asc".equals(key)) {
            wrapper.orderByAsc(Game::getName).orderByDesc(Game::getId);
        } else if ("newest".equals(key)) {
            wrapper.orderByDesc(Game::getCreateTime).orderByDesc(Game::getId);
        } else {
            wrapper.orderByDesc(Game::getRating).orderByDesc(Game::getId);
        }
    }

    @Override
    public Game getGameDetail(Long id) {
        Game game = getById(id);
        if (game == null || game.getIsDelete() != null && game.getIsDelete() == 1) {
            throw new BusinessException(404, "游戏不存在");
        }
        return game;
    }

    @Override
    public Game createGame(Game game) {
        game.setSource("user");
        game.setStatus(0);
        game.setIsDelete(0);
        game.setCreateBy(SecurityUtils.getCurrentUserId());
        game.setCreateTime(new Date());
        game.setUpdateTime(new Date());
        save(game);
        return game;
    }

    @Override
    public IPage<Game> getHotGames(int page, int size) {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Game::getIsDelete, 0)
               .orderByDesc(Game::getRating);
        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 把 game 表里所有未删除游戏的 platforms / game_types 拆开来按频次聚合。
     * 这样筛选 chip 始终反映"当前真实可选项"。
     */
    @Override
    public Map<String, Object> getFilters() {
        LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Game::getIsDelete, 0)
               .select(Game::getPlatforms, Game::getGameTypes);
        List<Game> all = list(wrapper);

        Map<String, Integer> pf = new HashMap<>();
        Map<String, Integer> tp = new HashMap<>();
        for (Game g : all) {
            addTokens(g.getPlatforms(), pf);
            addTokens(g.getGameTypes(), tp);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platforms", toSortedList(pf));
        result.put("types", toSortedList(tp));
        return result;
    }

    private void addTokens(String csv, Map<String, Integer> bucket) {
        if (StringUtils.isBlank(csv)) return;
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            bucket.merge(t, 1, Integer::sum);
        }
    }

    private List<Map<String, Object>> toSortedList(Map<String, Integer> bucket) {
        List<Map<String, Object>> rows = new ArrayList<>(bucket.size());
        bucket.forEach((k, v) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", k);
            row.put("count", v);
            rows.add(row);
        });
        rows.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
        return rows;
    }
}
