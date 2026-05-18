package com.ma.grecode.service;

import com.ma.grecode.entity.GameBacklog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface GameBacklogService extends IService<GameBacklog> {

    GameBacklog addToBacklog(GameBacklog backlog);

    List<GameBacklog> listBacklog(Long userId, Integer status);

    List<Map<String, Object>> countByStatus(Long userId);

    GameBacklog updateBacklog(Long id, GameBacklog backlog);

    GameBacklog updateStatus(Long id, Integer newStatus);

    void batchUpdateSort(List<Map<String, Object>> sortList);

    void removeFromBacklog(Long id);

    Map<String, Object> checkinFromBacklog(Long backlogId, Integer playTime, java.math.BigDecimal rating,
                                          Integer recordStatus, String notes);

    /**
     * 「游戏打卡」等写入 {@link com.ma.grecode.entity.GameRecord} 后调用：
     * 若清单中已有该游戏则更新状态；若尚未在清单则自动新增一条（状态与本次打卡一致，映射同清单内打卡）。
     */
    void syncBacklogAfterGameRecord(Long gameId, Integer recordStatus);
}
