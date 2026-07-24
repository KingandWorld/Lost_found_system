package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置控制器
 */
@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/system-config")
public class SystemConfigController {
    private static final Logger log = LoggerFactory.getLogger(SystemConfigController.class);

    @Resource
    private SystemConfigService systemConfigService;

    @Operation(summary = "获取过期天数配置")
    @GetMapping("/expire-days")
    public Result<Integer> getExpireDays() {
        int days = systemConfigService.getExpireDays();
        return Result.success(days);
    }

    @Operation(summary = "更新过期天数配置")
    @PutMapping("/expire-days")
    public Result<String> updateExpireDays(@RequestBody Map<String, Integer> params) {
        Integer value = params.get("value");
        if (value == null || value < 1 || value > 365) {
            return Result.error("过期天数必须在1-365之间");
        }
        systemConfigService.setConfigValue("item.expire.days", String.valueOf(value));
        log.info("管理员更新物品过期天数: {}天", value);
        return Result.success("保存成功，物品过期天数已更新为 " + value + " 天");
    }

    @Operation(summary = "获取所有系统配置")
    @GetMapping
    public Result<Map<String, String>> getAllConfigs() {
        Map<String, String> configs = new HashMap<>();
        configs.put("item.expire.days", String.valueOf(systemConfigService.getExpireDays()));
        return Result.success(configs);
    }
}
