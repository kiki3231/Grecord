package com.ma.grecode.controller;

import com.ma.grecode.entity.GameBacklog;
import com.ma.grecode.service.GameBacklogService;
import com.ma.grecode.utils.AjaxResult;
import com.ma.grecode.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "清单模块")
@RestController
@RequestMapping("/backlog")
public class GameBacklogController {

    @Autowired
    private GameBacklogService gameBacklogService;

    @Operation(summary = "添加到清单")
    @PostMapping
    public AjaxResult<?> add(@RequestBody GameBacklog backlog) {
        return AjaxResult.success("添加成功", gameBacklogService.addToBacklog(backlog));
    }

    @Operation(summary = "查询清单列表")
    @GetMapping("/list")
    public AjaxResult<?> list(@RequestParam(name = "status", required = false) Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("items", gameBacklogService.listBacklog(userId, status));
        result.put("statusCounts", gameBacklogService.countByStatus(userId));
        return AjaxResult.success(result);
    }

    @Operation(summary = "更新清单项")
    @PutMapping("/{id}")
    public AjaxResult<?> update(@PathVariable("id") Long id, @RequestBody GameBacklog backlog) {
        return AjaxResult.success("更新成功", gameBacklogService.updateBacklog(id, backlog));
    }

    @Operation(summary = "状态流转")
    @PutMapping("/{id}/status")
    public AjaxResult<?> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Integer> body) {
        Integer newStatus = body.get("status");
        return AjaxResult.success("状态更新成功", gameBacklogService.updateStatus(id, newStatus));
    }

    @Operation(summary = "批量排序")
    @PutMapping("/sort")
    public AjaxResult<?> batchSort(@RequestBody List<Map<String, Object>> sortList) {
        gameBacklogService.batchUpdateSort(sortList);
        return AjaxResult.success("排序更新成功");
    }

    @Operation(summary = "移除清单项")
    @DeleteMapping("/{id}")
    public AjaxResult<?> remove(@PathVariable("id") Long id) {
        gameBacklogService.removeFromBacklog(id);
        return AjaxResult.success("已移除");
    }

    @Operation(summary = "从清单打卡")
    @PostMapping("/{id}/checkin")
    public AjaxResult<?> checkin(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Integer playTime = body.get("playTime") != null ? Integer.valueOf(body.get("playTime").toString()) : 0;
        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        Integer recordStatus = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : 1;
        BigDecimal rating = null;
        if (body.get("rating") != null) {
            rating = new BigDecimal(body.get("rating").toString());
        }
        return AjaxResult.success("打卡成功",
                gameBacklogService.checkinFromBacklog(id, playTime, rating, recordStatus, notes));
    }
}
