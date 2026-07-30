package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分变动日志实体类
 */
@Data
@TableName("points_log")
@Schema(description = "积分变动日志实体类")
public class PointsLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "积分变动（正数=获得，负数=消耗）")
    private Integer pointsChange;

    @Schema(description = "变动后余额")
    private Integer pointsAfter;

    @Schema(description = "变动类型")
    private String changeType;

    @Schema(description = "关联ID")
    private Long relatedId;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
