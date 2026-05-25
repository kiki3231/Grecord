package com.ma.grecode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "game_record")
@Data
public class GameRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "game_id")
    private Long gameId;

    @TableField(value = "record_date")
    private Date recordDate;

    @TableField(value = "play_time")
    private Integer playTime;

    @TableField(value = "achievements")
    private String achievements;

    @TableField(value = "notes")
    private String notes;

    @TableField(value = "rating")
    private BigDecimal rating;

    @TableField(value = "status")
    private Integer status;

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
