package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.GameBacklog;
import com.ma.grecode.entity.GameRecord;
import com.ma.grecode.exception.BusinessException;
import com.ma.grecode.mapper.GameBacklogMapper;
import com.ma.grecode.service.GameBacklogService;
import com.ma.grecode.service.GameRecordService;
import com.ma.grecode.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GameBacklogServiceImpl extends ServiceImpl<GameBacklogMapper, GameBacklog> implements GameBacklogService {

    @Autowired
    private GameBacklogMapper gameBacklogMapper;

    @Autowired
    private GameRecordService gameRecordService;

    @Override
    public GameBacklog addToBacklog(GameBacklog backlog) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<GameBacklog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GameBacklog::getUserId, userId)
               .eq(GameBacklog::getGameId, backlog.getGameId())
               .eq(GameBacklog::getIsDelete, 0);
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "该游戏已在清单中");
        }
        backlog.setUserId(userId);
        backlog.setCreateBy(userId);
        backlog.setIsDelete(0);
        backlog.setCreateTime(new Date());
        backlog.setUpdateTime(new Date());
        if (backlog.getStatus() == null) backlog.setStatus(0);
        if (backlog.getPriority() == null) backlog.setPriority(1);
        if (backlog.getSortOrder() == null) backlog.setSortOrder(0);
        save(backlog);
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
        existing.setStatus(newStatus);
        existing.setUpdateBy(userId);
        existing.setUpdateTime(new Date());
        if (newStatus == 1 && existing.getStartedDate() == null) {
            existing.setStartedDate(new Date());
        }
        if (newStatus == 2) {
            existing.setCompletedDate(new Date());
        }
        updateById(existing);
        return existing;
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

    @Override
    @Transactional
    public Map<String, Object> checkinFromBacklog(Long backlogId, Integer playTime, String notes) {
        GameBacklog backlog = getById(backlogId);
        if (backlog == null || backlog.getIsDelete() == 1) {
            throw new BusinessException(404, "清单项不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!backlog.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }

        GameRecord record = new GameRecord();
        record.setGameId(backlog.getGameId());
        record.setPlayTime(playTime);
        record.setNotes(notes);
        record.setStatus(1);
        gameRecordService.createRecord(record);

        if (backlog.getStatus() == 0) {
            updateStatus(backlogId, 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("backlog", getById(backlogId));
        return result;
    }
}
