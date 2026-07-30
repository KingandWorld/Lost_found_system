-- V3: 会员系统
-- 日期: 2026-07-24

-- 1. 会员记录表（一用户一记录，惰性创建）
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

-- 2. 积分变动日志表（审计追踪，不可变）
CREATE TABLE IF NOT EXISTS points_log (
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id        BIGINT NOT NULL COMMENT '用户ID',
    points_change  INT NOT NULL COMMENT '积分变动（正数=获得，负数=消耗）',
    points_after   INT NOT NULL COMMENT '变动后余额',
    change_type    VARCHAR(30) NOT NULL COMMENT '变动类型: PUBLISH_LOST,PUBLISH_FOUND,ITEM_COMPLETED,CLAIM_SUCCESS,EXCHANGE_MEMBERSHIP,ADMIN_GRANT,ADMIN_REVOKE',
    related_id     BIGINT DEFAULT NULL COMMENT '关联ID（物品ID等）',
    description    VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_change_type (change_type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志表';

-- 3. 插入会员系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('points.publish.lost', '2', '发布失物获得积分'),
('points.publish.found', '2', '发布招领获得积分'),
('points.publish.daily.max', '10', '每日发布获取积分上限'),
('points.item.completed', '20', '物品交接完成发布者获得积分'),
('points.claim.success', '15', '认领成功认领人获得积分'),
('points.exchange.cost', '100', '兑换会员所需积分'),
('points.exchange.days', '30', '兑换会员天数'),
('member.expire.days', '60', '会员物品过期天数')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
