package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失物/招领物品抽象基类
 *
 * 包含两种物品共有的全部字段。
 * 具体表映射由子类通过 @TableName 声明。
 */
@Data
public abstract class BaseItem {

    @TableId(type = IdType.AUTO)
    @Schema(description = "物品ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "图片(多张用逗号分隔)")
    private String images;

    @Schema(description = "发布用户ID")
    private Long userId;

    @Schema(description = "状态(0待认领,1已认领,2已交接,3已关闭,4已过期)")
    private Integer status;

    @Schema(description = "是否置顶(0否,1是)")
    private Integer isPinned;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ===== 瞬态字段（仅用于前端展示，不持久化到数据库） =====

    @TableField(exist = false)
    @Schema(description = "分类名称")
    private String categoryName;

    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;

    @TableField(exist = false)
    @Schema(description = "发布者是否为会员")
    private Boolean isMemberItem;
}
