-- ===================================================
-- V2: 物品置顶 & 系统配置
-- 日期: 2026-07-24
-- ===================================================

-- 1. 物品表添加置顶字段
ALTER TABLE lost_item ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶(0否,1是)';
ALTER TABLE found_item ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶(0否,1是)';

-- 2. 创建系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 3. 插入默认配置
INSERT INTO system_config (config_key, config_value, description)
VALUES ('item.expire.days', '30', '物品过期天数，超过此天数的待认领物品将被标记为已过期');
