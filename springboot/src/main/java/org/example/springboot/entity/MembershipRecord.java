package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员记录实体类
 */
@Data
@TableName("membership_record")
@Schema(description = "会员记录实体类")
public class MembershipRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "当前可用积分")
    private Integer points;

    @Schema(description = "累计获得积分")
    private Integer totalPointsEarned;

    @Schema(description = "累计消耗积分")
    private Integer totalPointsSpent;

    @Schema(description = "会员有效期截止")
    private LocalDateTime memberUntil;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 判断当前是否为有效会员
     */
    public boolean isCurrentlyMember() {
        return memberUntil != null && memberUntil.isAfter(LocalDateTime.now());
    }
}
