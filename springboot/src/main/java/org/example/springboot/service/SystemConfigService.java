package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.springboot.entity.SystemConfig;
import org.example.springboot.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {
    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    @Resource
    private SystemConfigMapper systemConfigMapper;

    /**
     * 默认配置值（数据库不存在时回退）
     */
    private static final Map<String, String> DEFAULTS = new ConcurrentHashMap<>();
    static {
        DEFAULTS.put("item.expire.days", "30");
        DEFAULTS.put("points.publish.lost", "2");
        DEFAULTS.put("points.publish.found", "2");
        DEFAULTS.put("points.publish.daily.max", "10");
        DEFAULTS.put("points.item.completed", "20");
        DEFAULTS.put("points.claim.success", "15");
        DEFAULTS.put("points.exchange.cost", "100");
        DEFAULTS.put("points.exchange.days", "30");
        DEFAULTS.put("member.expire.days", "60");
        DEFAULTS.put("captcha.enabled", "false");
        DEFAULTS.put("captcha.expire.seconds", "300");
    }

    /**
     * 配置缓存（减少DB查询）
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 获取配置值
     */
    public String getConfigValue(String key) {
        // 先查缓存
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // 查数据库
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config != null) {
            cache.put(key, config.getConfigValue());
            return config.getConfigValue();
        }

        // 返回默认值
        String defaultVal = DEFAULTS.getOrDefault(key, "");
        log.info("配置 {} 不存在，使用默认值: {}", key, defaultVal);
        return defaultVal;
    }

    /**
     * 获取过期天数配置
     */
    public int getExpireDays() {
        String value = getConfigValue("item.expire.days");
        try {
            int days = Integer.parseInt(value);
            return Math.max(1, Math.min(days, 365)); // 限制1-365天
        } catch (NumberFormatException e) {
            log.warn("过期天数配置无效: {}, 使用默认值30", value);
            return 30;
        }
    }

    /**
     * 设置配置值
     */
    public void setConfigValue(String key, String value) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config != null) {
            config.setConfigValue(value);
            systemConfigMapper.updateById(config);
        } else {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription("");
            systemConfigMapper.insert(config);
        }

        // 更新缓存
        cache.put(key, value);
        log.info("系统配置已更新: {} = {}", key, value);
    }

    /**
     * 获取整数配置值
     */
    public int getIntConfig(String key, int defaultVal) {
        String value = getConfigValue(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置 {} 值无效: {}, 使用默认值 {}", key, value, defaultVal);
            return defaultVal;
        }
    }

    /**
     * 获取会员物品过期天数
     */
    public int getMemberExpireDays() {
        String value = getConfigValue("member.expire.days");
        try {
            int days = Integer.parseInt(value);
            return Math.max(1, Math.min(days, 365));
        } catch (NumberFormatException e) {
            log.warn("会员过期天数配置无效: {}, 使用默认值60", value);
            return 60;
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
    }
}
