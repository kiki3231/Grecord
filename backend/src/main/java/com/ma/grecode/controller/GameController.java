package com.ma.grecode.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ma.grecode.entity.Game;
import com.ma.grecode.service.GameService;
import com.ma.grecode.utils.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "游戏模块")
@RestController
@RequestMapping("/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @Operation(summary = "搜索/筛选游戏")
    @GetMapping("/search")
    public AjaxResult<?> search(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "platform", required = false) String platform,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "sortBy", required = false, defaultValue = "rating_desc") String sortBy,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        IPage<Game> result = gameService.searchGames(keyword, platform, type, sortBy, page, size);
        return AjaxResult.success(result);
    }

    @Operation(summary = "游戏库可用筛选项（按出现频次聚合）")
    @GetMapping("/filters")
    public AjaxResult<?> filters() {
        return AjaxResult.success(gameService.getFilters());
    }

    @Operation(summary = "游戏详情")
    @GetMapping("/{id}")
    public AjaxResult<?> detail(@PathVariable("id") Long id) {
        return AjaxResult.success(gameService.getGameDetail(id));
    }

    @Operation(summary = "创建游戏条目")
    @PostMapping
    public AjaxResult<?> create(@RequestBody Game game) {
        return AjaxResult.success("创建成功", gameService.createGame(game));
    }

    @Operation(summary = "热门游戏")
    @GetMapping("/hot")
    public AjaxResult<?> hot(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return AjaxResult.success(gameService.getHotGames(page, size));
    }
}
