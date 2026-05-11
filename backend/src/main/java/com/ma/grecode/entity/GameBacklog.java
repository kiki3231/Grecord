package com.ma.grecode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "game_backlog")
@Data
public class GameBacklog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "game_id")
    private Long gameId;

    /** 0想玩 1在玩 2已完成 3已搁置 */
    @TableField(value = "status")
    private Integer status;

    /** 0高 1中 2低 */
    @TableField(value = "priority")
    private Integer priority;

    @TableField(value = "sort_order")
    private Integer sortOrder;

    @TableField(value = "notes")
    private String notes;

    @TableField(value = "started_date")
    private Date startedDate;

    @TableField(value = "completed_date")
    private Date completedDate;

    @TableField(value = "create_by")
    private Long createBy;

    @TableField(value = "update_by")
    private Long updateBy;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private Game game;
}
