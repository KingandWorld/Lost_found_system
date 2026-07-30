-- ===================================================
-- 失物招领系统 — 数据库迁移脚本（兼容版）
-- 适用场景：现有 lost_found_db 数据库的增量更新
-- 包含：add_version_fields + V2 + V3
-- 执行方式：宝塔面板 → 数据库 → 导入
-- ===================================================

-- ===================================================
-- 第一部分：字段优化与索引（add_version_fields.sql）
-- ===================================================

-- 删除可能存在的重复数据（为唯一约束做准备）
DELETE t1 FROM `claim_application` t1
INNER JOIN `claim_application` t2
WHERE t1.id > t2.id
  AND t1.item_id = t2.item_id
  AND t1.item_type = t2.item_type
  AND t1.user_id = t2.user_id
  AND t1.status = t2.status
  AND t1.status = 0;

-- 添加唯一约束
ALTER TABLE `claim_application`
    ADD UNIQUE KEY `uk_item_user_pending`
    (`item_id`, `item_type`, `user_id`);

-- 更新用户状态字段
ALTER TABLE `user`
    MODIFY COLUMN `status` int NOT NULL DEFAULT 0;

-- 更新认领申请表状态字段
ALTER TABLE `claim_application`
    MODIFY COLUMN `status` int NOT NULL DEFAULT 0;

-- 更新失物状态字段
ALTER TABLE `lost_item`
    MODIFY COLUMN `status` int NOT NULL DEFAULT 0;
-- 更新招领状态字段
ALTER TABLE `found_item`
    MODIFY COLUMN `status` int NOT NULL DEFAULT 0;

-- 添加通知表索引
ALTER TABLE `notification` ADD INDEX `idx_user_read` (`user_id`, `is_read`);
ALTER TABLE `notification` ADD INDEX `idx_create_time` (`create_time`);

-- 添加物品表索引
ALTER TABLE `lost_item` ADD INDEX `idx_status_create_time` (`status`, `create_time`);
ALTER TABLE `lost_item` ADD INDEX `idx_category_status` (`category_id`, `status`);
ALTER TABLE `found_item` ADD INDEX `idx_status_create_time` (`status`, `create_time`);
ALTER TABLE `found_item` ADD INDEX `idx_category_status` (`category_id`, `status`);

-- ===================================================
-- 第二部分：物品置顶 & 系统配置（V2）
-- ===================================================

-- 物品表添加置顶字段
ALTER TABLE lost_item
    ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE found_item
    ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0;

-- 创建系统配置表
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

-- 插入默认过期天数配置
INSERT INTO system_config (config_key, config_value, description)
SELECT 'item.expire.days', '30', '物品过期天数，超过此天数的待认领物品将被标记为已过期'
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'item.expire.days');

-- ===================================================
-- 第三部分：会员积分系统（V3）
-- ===================================================

-- 会员记录表
CREATE TABLE IF NOT EXISTS membership_record (
    id                  BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    points              INT NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    total_points_earned INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    total_points_spent  INT NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
    member_until        DATETIME DEFAULT NULL COMMENT '会员有效期截止（NULL=非会员/已过期）',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员记录表';

-- 积分变动日志表
CREATE TABLE IF NOT EXISTS points_log (
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id        BIGINT NOT NULL COMMENT '用户ID',
    points_change  INT NOT NULL COMMENT '积分变动（正数=获得，负数=消耗）',
    points_after   INT NOT NULL COMMENT '变动后余额',
    change_type    VARCHAR(30) NOT NULL COMMENT '变动类型',
    related_id     BIGINT DEFAULT NULL COMMENT '关联ID（物品ID等）',
    description    VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_change_type (change_type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志表';

-- 插入会员系统配置（仅当不存在时插入）
INSERT INTO system_config (config_key, config_value, description)
SELECT * FROM (
    SELECT 'points.publish.lost' AS k, '2' AS v, '发布失物获得积分' AS d
    UNION ALL SELECT 'points.publish.found', '2', '发布招领获得积分'
    UNION ALL SELECT 'points.publish.daily.max', '10', '每日发布获取积分上限'
    UNION ALL SELECT 'points.item.completed', '20', '物品交接完成发布者获得积分'
    UNION ALL SELECT 'points.claim.success', '15', '认领成功认领人获得积分'
    UNION ALL SELECT 'points.exchange.cost', '100', '兑换会员所需积分'
    UNION ALL SELECT 'points.exchange.days', '30', '兑换会员天数'
    UNION ALL SELECT 'member.expire.days', '60', '会员物品过期天数'
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM system_config WHERE config_key = tmp.k
);
