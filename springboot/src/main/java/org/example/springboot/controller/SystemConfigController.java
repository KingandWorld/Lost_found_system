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
        configs.put("member.expire.days", String.valueOf(systemConfigService.getMemberExpireDays()));
        configs.put("points.publish.lost", systemConfigService.getConfigValue("points.publish.lost"));
        configs.put("points.publish.found", systemConfigService.getConfigValue("points.publish.found"));
        configs.put("points.publish.daily.max", systemConfigService.getConfigValue("points.publish.daily.max"));
        configs.put("points.item.completed", systemConfigService.getConfigValue("points.item.completed"));
        configs.put("points.claim.success", systemConfigService.getConfigValue("points.claim.success"));
        configs.put("points.exchange.cost", systemConfigService.getConfigValue("points.exchange.cost"));
        configs.put("points.exchange.days", systemConfigService.getConfigValue("points.exchange.days"));
        configs.put("captcha.enabled", systemConfigService.getConfigValue("captcha.enabled"));
        return Result.success(configs);
    }

    @Operation(summary = "获取会员相关配置")
    @GetMapping("/membership-configs")
    public Result<Map<String, String>> getMembershipConfigs() {
        Map<String, String> configs = new HashMap<>();
        configs.put("member.expire.days", String.valueOf(systemConfigService.getMemberExpireDays()));
        configs.put("points.publish.lost", systemConfigService.getConfigValue("points.publish.lost"));
        configs.put("points.publish.found", systemConfigService.getConfigValue("points.publish.found"));
        configs.put("points.publish.daily.max", systemConfigService.getConfigValue("points.publish.daily.max"));
        configs.put("points.item.completed", systemConfigService.getConfigValue("points.item.completed"));
        configs.put("points.claim.success", systemConfigService.getConfigValue("points.claim.success"));
        configs.put("points.exchange.cost", systemConfigService.getConfigValue("points.exchange.cost"));
        configs.put("points.exchange.days", systemConfigService.getConfigValue("points.exchange.days"));
        configs.put("captcha.enabled", systemConfigService.getConfigValue("captcha.enabled"));
        return Result.success(configs);
    }

    @Operation(summary = "批量更新会员配置")
    @PutMapping("/membership-configs")
    public Result<String> updateMembershipConfigs(@RequestBody Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // 校验数值类型配置
            if (key.startsWith("points.") || key.startsWith("member.")) {
                try {
                    int val = Integer.parseInt(value);
                    if (val < 0) {
                        return Result.error(key + " 不能为负数");
                    }
                } catch (NumberFormatException e) {
                    return Result.error(key + " 必须为整数");
                }
            }
            systemConfigService.setConfigValue(key, value);
        }
        return Result.success("会员配置已更新");
    }
}
