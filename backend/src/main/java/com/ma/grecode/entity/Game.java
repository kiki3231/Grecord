package com.ma.grecode.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "game")
@Data
public class Game {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    private String name;

    @TableField(value = "description")
    private String description;

    @TableField(value = "icon")
    private String icon;

    @TableField(value = "platforms")
    private String platforms;

    @TableField(value = "game_types")
    private String gameTypes;

    @TableField(value = "avg_play_time")
    private Integer avgPlayTime;

    @TableField(value = "rating")
    private BigDecimal rating;

    @TableField(value = "review")
    private String review;

    @TableField(value = "developer")
    private String developer;

    @TableField(value = "source")
    private String source;

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
}
