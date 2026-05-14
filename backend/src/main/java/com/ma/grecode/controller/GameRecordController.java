package com.ma.grecode.controller;

import com.ma.grecode.entity.GameRecord;
import com.ma.grecode.service.GameRecordService;
import com.ma.grecode.utils.AjaxResult;
import com.ma.grecode.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "打卡模块")
@RestController
@RequestMapping("/record")
public class GameRecordController {

    @Autowired
    private GameRecordService gameRecordService;

    @Operation(summary = "新建打卡记录")
    @PostMapping
    public AjaxResult<?> create(@RequestBody GameRecord record) {
        return AjaxResult.success("打卡成功", gameRecordService.createRecord(record));
    }

    @Operation(summary = "打卡历史列表")
    @GetMapping("/list")
    public AjaxResult<?> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return AjaxResult.success(gameRecordService.listRecords(userId, page, size));
    }

    @Operation(summary = "带游戏信息的打卡列表")
    @GetMapping("/listWithGame")
    public AjaxResult<?> listWithGame() {
        Long userId = SecurityUtils.getCurrentUserId();
        return AjaxResult.success(gameRecordService.listRecordsWithGame(userId));
    }

    @Operation(summary = "热力图数据")
    @GetMapping("/heatmap")
    public AjaxResult<?> heatmap(@RequestParam(name = "year", required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (year == null) year = LocalDate.now().getYear();
        return AjaxResult.success(gameRecordService.getHeatmapData(userId, year));
    }

    @Operation(summary = "统计面板")
    @GetMapping("/stats")
    public AjaxResult<?> stats() {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("summary", gameRecordService.getStats(userId));
        result.put("topGames", gameRecordService.getTopGames(userId, 5));
        return AjaxResult.success(result);
    }

    @Operation(summary = "修改打卡记录")
    @PutMapping("/{id}")
    public AjaxResult<?> update(@PathVariable("id") Long id, @RequestBody GameRecord record) {
        return AjaxResult.success("修改成功", gameRecordService.updateRecord(id, record));
    }

    @Operation(summary = "删除打卡记录")
    @DeleteMapping("/{id}")
    public AjaxResult<?> delete(@PathVariable("id") Long id) {
        gameRecordService.deleteRecord(id);
        return AjaxResult.success("删除成功");
    }
}
