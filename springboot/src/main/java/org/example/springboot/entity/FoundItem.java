package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 招领信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("found_item")
@Schema(description = "招领信息实体类")
public class FoundItem extends BaseItem {

    @Schema(description = "拾取地点")
    private String foundPlace;

    @Schema(description = "拾取时间")
    private LocalDateTime foundTime;
}
