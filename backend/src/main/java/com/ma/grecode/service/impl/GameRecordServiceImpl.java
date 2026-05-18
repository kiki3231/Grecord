package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.GameRecord;
import com.ma.grecode.exception.BusinessException;
import com.ma.grecode.mapper.GameRecordMapper;
import com.ma.grecode.service.GameBacklogService;
import com.ma.grecode.service.GameRecordService;
import com.ma.grecode.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GameRecordServiceImpl extends ServiceImpl<GameRecordMapper, GameRecord> implements GameRecordService {

    private static final Logger log = LoggerFactory.getLogger(GameRecordServiceImpl.class);

    @Autowired
    private GameRecordMapper gameRecordMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 与 GameBacklogServiceImpl 互相引用，必须 @Lazy 避免循环依赖导致注入失败（表现为只写入 record、不同步 backlog） */
    @Autowired
    @Lazy
    private GameBacklogService gameBacklogService;

    /**
     * 打卡写入 game_record 后同步 game_backlog（清单无则自动加入），保证所有调用 createRecord 的入口行为一致。
     */
    private void syncBacklogAfterPersistedRecord(GameRecord saved) {
        if (saved == null || saved.getGameId() == null) {
            return;
        }
        Integer st = saved.getStatus() != null ? saved.getStatus() : 1;
        gameBacklogService.syncBacklogAfterGameRecord(saved.getGameId(), st);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameRecord createRecord(GameRecord record) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (record.getGameId() == null) {
            throw new BusinessException(400, "请选择要打卡的游戏");
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(new Date());
        }
        Date recordDate = record.getRecordDate();
        Date now = new Date();

        // 唯一约束 (user_id, game_id, record_date) —— 同一天同一游戏只允许一条
        // 已存在则视为"补充打卡"，合并字段并更新，让用户感觉幂等友好。
        GameRecord existing = findExisting(userId, record.getGameId(), recordDate);
        if (existing != null) {
            if (record.getPlayTime() != null) existing.setPlayTime(record.getPlayTime());
            if (record.getRating() != null)   existing.setRating(record.getRating());
            if (record.getStatus() != null)   existing.setStatus(record.getStatus());
            if (record.getNotes() != null)    existing.setNotes(record.getNotes());
            if (record.getAchievements() != null) existing.setAchievements(record.getAchievements());
            existing.setUpdateBy(userId);
            existing.setUpdateTime(now);
            updateById(existing);
            evictHeatmapCache(userId, recordDate);
            syncBacklogAfterPersistedRecord(existing);
            return existing;
        }

        record.setUserId(userId);
        record.setCreateBy(userId);
        record.setIsDelete(0);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        save(record);
        evictHeatmapCache(userId, recordDate);
        syncBacklogAfterPersistedRecord(record);
        return record;
    }

    private GameRecord findExisting(Long userId, Long gameId, Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date dayStart = c.getTime();
        c.add(Calendar.DAY_OF_MONTH, 1);
        Date nextDay = c.getTime();

        LambdaQueryWrapper<GameRecord> w = new LambdaQueryWrapper<>();
        w.eq(GameRecord::getUserId, userId)
         .eq(GameRecord::getGameId, gameId)
         .eq(GameRecord::getIsDelete, 0)
         .ge(GameRecord::getRecordDate, dayStart)
         .lt(GameRecord::getRecordDate, nextDay)
         .last("LIMIT 1");
        return getOne(w, false);
    }

    @Override
    public IPage<GameRecord> listRecords(Long userId, int page, int size) {
        LambdaQueryWrapper<GameRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GameRecord::getUserId, userId)
               .eq(GameRecord::getIsDelete, 0)
               .orderByDesc(GameRecord::getRecordDate)
               .orderByDesc(GameRecord::getId);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<GameRecord> listRecordsWithGame(Long userId) {
        return gameRecordMapper.selectRecordWithGame(userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getHeatmapData(Long userId, int year) {
        String cacheKey = "record:heatmap:" + userId + ":" + year;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<Map<String, Object>>) cached;
            }
        } catch (Exception e) {
            log.warn("[heatmap] Redis 读取失败，退回直查 DB：{}", e.getMessage());
        }
        List<Map<String, Object>> data = gameRecordMapper.selectHeatmapData(userId, year);
        try {
            redisTemplate.opsForValue().set(cacheKey, data, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[heatmap] Redis 写入失败，已忽略：{}", e.getMessage());
        }
        return data;
    }

    @Override
    public Map<String, Object> getStats(Long userId) {
        return gameRecordMapper.selectUserStats(userId);
    }

    @Override
    public List<Map<String, Object>> getTopGames(Long userId, int limit) {
        return gameRecordMapper.selectTopGames(userId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameRecord updateRecord(Long id, GameRecord record) {
        GameRecord existing = getById(id);
        if (existing == null || existing.getIsDelete() == 1) {
            throw new BusinessException(404, "记录不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改此记录");
        }
        record.setId(id);
        record.setUserId(userId);
        record.setUpdateBy(userId);
        record.setUpdateTime(new Date());
        updateById(record);
        evictHeatmapCache(userId, existing.getRecordDate());
        if (record.getRecordDate() != null) {
            evictHeatmapCache(userId, record.getRecordDate());
        }
        GameRecord saved = getById(id);
        syncBacklogAfterPersistedRecord(saved);
        return saved;
    }

    @Override
    public void deleteRecord(Long id) {
        GameRecord existing = getById(id);
        if (existing == null) {
            throw new BusinessException(404, "记录不存在");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除此记录");
        }
        existing.setIsDelete(1);
        existing.setUpdateBy(userId);
        existing.setUpdateTime(new Date());
        updateById(existing);
        evictHeatmapCache(userId, existing.getRecordDate());
    }

    private void evictHeatmapCache(Long userId, Date recordDate) {
        if (recordDate == null) return;
        try {
            LocalDate ld = recordDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String cacheKey = "record:heatmap:" + userId + ":" + ld.getYear();
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            // Redis 没启动也不影响业务，热力图下次直接 fall back 到 DB 查询
            log.warn("[evictHeatmapCache] Redis 不可用，跳过：{}", e.getMessage());
        }
    }
}
