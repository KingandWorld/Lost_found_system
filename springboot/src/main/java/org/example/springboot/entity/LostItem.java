package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 失物信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lost_item")
@Schema(description = "失物信息实体类")
public class LostItem extends BaseItem {

    @Schema(description = "丢失地点")
    private String lostPlace;

    @Schema(description = "丢失时间")
    private LocalDateTime lostTime;
}
