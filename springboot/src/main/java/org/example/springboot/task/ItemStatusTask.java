package org.example.springboot.task;

import jakarta.annotation.Resource;
import org.example.springboot.service.ItemStatusService;
import org.example.springboot.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 物品状态定时任务
 */
@Component
public class ItemStatusTask {
    private static final Logger log = LoggerFactory.getLogger(ItemStatusTask.class);

    @Resource
    private ItemStatusService itemStatusService;

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 每天凌晨2点处理过期物品
     * 将超过配置天数的待认领物品标记为过期（默认30天，可在系统设置中修改）
     * 置顶物品永不过期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processExpiredItems() {
        try {
            int expireDays = systemConfigService.getExpireDays();
            log.info("开始处理过期物品, 过期天数={}", expireDays);
            itemStatusService.processExpiredItems(expireDays);
            log.info("过期物品处理完成");
        } catch (Exception e) {
            log.error("处理过期物品失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每小时统计物品状态（用于监控）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void logStatusStatistics() {
        try {
            ItemStatusService.ItemStatusStatistics statistics = itemStatusService.getStatusStatistics();
            log.info("物品状态统计 - 失物: 待认领={}, 已认领={}, 已完成={}, 已关闭={}, 已过期={}", 
                    statistics.getPendingLost(), statistics.getClaimedLost(), 
                    statistics.getCompletedLost(), statistics.getClosedLost(), statistics.getExpiredLost());
            log.info("物品状态统计 - 招领: 待认领={}, 已认领={}, 已完成={}, 已关闭={}, 已过期={}", 
                    statistics.getPendingFound(), statistics.getClaimedFound(), 
                    statistics.getCompletedFound(), statistics.getClosedFound(), statistics.getExpiredFound());
        } catch (Exception e) {
            log.error("统计物品状态失败: {}", e.getMessage(), e);
        }
    }
}
