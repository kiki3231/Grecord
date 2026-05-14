package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.GameBacklog;
import com.ma.grecode.entity.GameRecord;
import com.ma.grecode.exception.BusinessException;
import com.ma.grecode.mapper.GameBacklogMapper;
import com.ma.grecode.service.GameBacklogService;
import com.ma.grecode.service.GameRecordService;
import com.ma.grecode.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GameBacklogServiceImpl extends ServiceImpl<GameBacklogMapper, GameBacklog> implements GameBacklogService {

    @Autowired
    private GameBacklogMapper gameBacklogMapper;

    @Autowired
    private GameRecordService gameRecordService;

    @Override
    @Transactional
    public GameBacklog addToBacklog(GameBacklog backlog) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (backlog.getGameId() == null) {
            throw new BusinessException(400, "缺少 gameId");
        }
        Date now = new Date();

        // 1) 优先尝试"复用"软删除过的同 (user, game) 记录，避免历史脏数据 + 唯一索引冲突
        LambdaQueryWrapper<GameBacklog> softDeleted = new LambdaQueryWrapper<GameBacklog>()
                .eq(GameBacklog::getUserId, userId)
                .eq(GameBacklog::getGameId, backlog.getGameId())
                .eq(GameBacklog::getIsDelete, 1)
                .orderByDesc(GameBacklog::getId)
                .last("LIMIT 1");
        GameBacklog reusable = getOne(softDeleted, false);
        if (reusable != null) {
            reusable.setIsDelete(0);
            reusable.setStatus(backlog.getStatus() != null ? backlog.getStatus() : 0);
            reusable.setPriority(backlog.getPriority() != null ? backlog.getPriority() : 1);
            reusable.setSortOrder(backlog.getSortOrder() != null ? backlog.getSortOrder() : 0);
            if (backlog.getNotes() != null) reusable.setNotes(backlog.getNotes());
            reusable.setUpdateBy(userId);
            reusable.setUpdateTime(now);
            updateById(reusable);
            return reusable;
        }

        // 2) 已存在活动记录（is_delete=0）直接 409
        LambdaQueryWrapper<GameBacklog> active = new LambdaQueryWrapper<GameBacklog>()
                .eq(GameBacklog::getUserId, userId)
                .eq(GameBacklog::getGameId, backlog.getGameId())
                .eq(GameBacklog::getIsDelete, 0);
        if (count(active) > 0) {
            throw new BusinessException(409, "该游戏已在清单中");
        }

        // 3) 否则插入新行；唯一索引 uk_user_game(user_id, game_id, is_delete) 兜底并发竞争
        backlog.setId(null);
        backlog.setUserId(userId);
        backlog.setCreateBy(userId);
        backlog.setIsDelete(0);
        backlog.setCreateTime(now);
        backlog.setUpdateTime(now);
        if (backlog.getStatus() == null) backlog.setStatus(0);
        if (backlog.getPriority() == null) backlog.setPriority(1);
        if (backlog.getSortOrder() == null) backlog.setSortOrder(0);
        try {
            save(backlog);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "该游戏已在清单中");
        }
        return backlog;
    }

    @Override
    public List<GameBacklog> listBacklog(Long userId, Integer status) {
        return gameBacklogMapper.selectBacklogWithGame(userId, status);
    }

    @Override
    public List<Map<String, Object>> countByStatus(Long userId) {
        return gameBacklogMapper.countByStatus(userId);
    }

    @Override
    public GameBacklog updateBacklog(Long id, GameBacklog backlog) {
        GameBacklog existing = getById(id);
        if (existing == null || existing.getIsDelete() == 1) {
            throw new BusinessException(404, "清单项不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改");
        }
        backlog.setId(id);
        backlog.setUpdateBy(userId);
        backlog.setUpdateTime(new Date());
        updateById(backlog);
        return getById(id);
    }

    @Override
    public GameBacklog updateStatus(Long id, Integer newStatus) {
        GameBacklog existing = getById(id);
        if (existing == null || existing.getIsDelete() == 1) {
            throw new BusinessException(404, "清单项不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改");
        }
        Date now = new Date();
        if (newStatus == null) {
            // 去除 status（置为 NULL）：updateById 会跳过 null 字段，须用 UpdateWrapper 显式写入
            UpdateWrapper<GameBacklog> wrapper = new UpdateWrapper<GameBacklog>()
                    .set("status", null)
                    .set("update_by", userId)
                    .set("update_time", now)
                    .eq("id", id);
            update(wrapper);
        } else {
            existing.setStatus(newStatus);
            existing.setUpdateBy(userId);
            existing.setUpdateTime(now);
            if (newStatus == 1 && existing.getStartedDate() == null) {
                existing.setStartedDate(now);
            }
            if (newStatus == 2) {
                existing.setCompletedDate(now);
            }
            updateById(existing);
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void batchUpdateSort(List<Map<String, Object>> sortList) {
        Long userId = SecurityUtils.getCurrentUserId();
        for (Map<String, Object> item : sortList) {
            Long itemId = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            GameBacklog backlog = new GameBacklog();
            backlog.setId(itemId);
            backlog.setSortOrder(sortOrder);
            backlog.setUpdateBy(userId);
            backlog.setUpdateTime(new Date());
            updateById(backlog);
        }
    }

    @Override
    public void removeFromBacklog(Long id) {
        GameBacklog existing = getById(id);
        if (existing == null) {
            throw new BusinessException(404, "清单项不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除");
        }
        existing.setIsDelete(1);
        existing.setUpdateBy(userId);
        existing.setUpdateTime(new Date());
        updateById(existing);
    }

    /**
     * 与「今日打卡」页一致：record.status 为 1在玩 2通关 3搁置 4白金；
     * 同步到清单：1→在玩，2/4→已完成，3→搁置（想玩 tab 仅含 status=0）。
     */
    private int recordStatusToBacklogStatus(int recordStatus) {
        return switch (recordStatus) {
            case 1 -> 1;
            case 2, 4 -> 2;
            case 3 -> 3;
            default -> 1;
        };
    }

    @Override
    @Transactional
    public Map<String, Object> checkinFromBacklog(Long backlogId, Integer playTime, BigDecimal rating,
                                                  Integer recordStatus, String notes) {
        GameBacklog backlog = getById(backlogId);
        if (backlog == null || backlog.getIsDelete() == 1) {
            throw new BusinessException(404, "清单项不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!backlog.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }

        int rs = recordStatus != null ? recordStatus : 1;
        if (rs < 1 || rs > 4) {
            throw new BusinessException(400, "无效的游戏状态");
        }

        GameRecord record = new GameRecord();
        record.setGameId(backlog.getGameId());
        record.setPlayTime(playTime);
        record.setNotes(notes);
        record.setStatus(rs);
        if (rating != null) {
            record.setRating(rating);
        }
        gameRecordService.createRecord(record);

        int newBacklogStatus = recordStatusToBacklogStatus(rs);
        updateStatus(backlogId, newBacklogStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("backlog", getById(backlogId));
        return result;
    }
}
