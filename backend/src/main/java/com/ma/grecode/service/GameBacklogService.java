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

    Map<String, Object> checkinFromBacklog(Long backlogId, Integer playTime, String notes);
}
