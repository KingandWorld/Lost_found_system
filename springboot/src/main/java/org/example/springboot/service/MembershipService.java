package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.MembershipRecord;
import org.example.springboot.entity.PointsLog;
import org.example.springboot.entity.User;
import org.example.springboot.enumClass.PointsChangeType;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.MembershipRecordMapper;
import org.example.springboot.mapper.PointsLogMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会员服务 — 管理积分、会员状态、兑换
 */
@Service
public class MembershipService {
    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

    @Resource
    private MembershipRecordMapper membershipRecordMapper;

    @Resource
    private PointsLogMapper pointsLogMapper;

    @Resource
    private SystemConfigService systemConfigService;

    // ==================== 查询方法 ====================

    /**
     * 获取或惰性创建会员记录
     */
    public MembershipRecord getOrCreateRecord(Long userId) {
        LambdaQueryWrapper<MembershipRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MembershipRecord::getUserId, userId);
        MembershipRecord record = membershipRecordMapper.selectOne(wrapper);

        if (record == null) {
            record = new MembershipRecord();
            record.setUserId(userId);
            record.setPoints(0);
            record.setTotalPointsEarned(0);
            record.setTotalPointsSpent(0);
            record.setMemberUntil(null);
            membershipRecordMapper.insert(record);
            log.info("为用户 {} 创建会员记录", userId);
        }

        return record;
    }

    /**
     * 判断用户是否为有效会员
     */
    public boolean isMember(Long userId) {
        MembershipRecord record = getOrCreateRecord(userId);
        return record.isCurrentlyMember();
    }

    /**
     * 获取所有当前有效会员的用户ID集合（用于排序和标识）
     */
    public Set<Long> getCurrentMemberUserIds() {
        LambdaQueryWrapper<MembershipRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(MembershipRecord::getMemberUntil, LocalDateTime.now());
        List<MembershipRecord> records = membershipRecordMapper.selectList(wrapper);
        return records.stream()
                .map(MembershipRecord::getUserId)
                .collect(Collectors.toSet());
    }

    // ==================== 积分操作 ====================

    /**
     * 发放积分（正向变动）
     */
    @Transactional(rollbackFor = Exception.class)
    public void awardPoints(Long userId, int points, PointsChangeType type, Long relatedId, String description) {
        if (points <= 0) {
            return;
        }

        MembershipRecord record = getOrCreateRecord(userId);

        record.setPoints(record.getPoints() + points);
        record.setTotalPointsEarned(record.getTotalPointsEarned() + points);
        membershipRecordMapper.updateById(record);

        insertPointsLog(userId, points, record.getPoints(), type.getValue(), relatedId, description);

        log.info("用户 {} 获得 {} 积分（类型: {}），当前余额: {}", userId, points, type.getDescription(), record.getPoints());
    }

    /**
     * 消耗积分（负向变动）
     */
    @Transactional(rollbackFor = Exception.class)
    public void spendPoints(Long userId, int points, PointsChangeType type, String description) {
        if (points <= 0) {
            return;
        }

        MembershipRecord record = getOrCreateRecord(userId);

        if (record.getPoints() < points) {
            throw new ServiceException("积分不足，当前积分：" + record.getPoints() + "，需要：" + points);
        }

        record.setPoints(record.getPoints() - points);
        record.setTotalPointsSpent(record.getTotalPointsSpent() + points);
        membershipRecordMapper.updateById(record);

        insertPointsLog(userId, -points, record.getPoints(), type.getValue(), null, description);

        log.info("用户 {} 消耗 {} 积分（类型: {}），当前余额: {}", userId, points, type.getDescription(), record.getPoints());
    }

    /**
     * 检查今日发布积分是否已达上限
     */
    public boolean canAwardPublishPoints(Long userId) {
        int dailyMax = systemConfigService.getIntConfig("points.publish.daily.max", 10);

        // 查询今日发布类积分日志
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsLog::getUserId, userId)
               .in(PointsLog::getChangeType, PointsChangeType.PUBLISH_LOST.getValue(), PointsChangeType.PUBLISH_FOUND.getValue())
               .ge(PointsLog::getCreateTime, todayStart)
               .le(PointsLog::getCreateTime, todayEnd);

        List<PointsLog> todayLogs = pointsLogMapper.selectList(wrapper);
        int todayEarned = todayLogs.stream().mapToInt(PointsLog::getPointsChange).sum();

        return todayEarned < dailyMax;
    }

    // ==================== 会员兑换 ====================

    /**
     * 积分兑换会员天数
     */
    @Transactional(rollbackFor = Exception.class)
    public void exchangeMembership(Long userId) {
        int cost = systemConfigService.getIntConfig("points.exchange.cost", 100);
        int days = systemConfigService.getIntConfig("points.exchange.days", 30);

        MembershipRecord record = getOrCreateRecord(userId);

        if (record.getPoints() < cost) {
            throw new ServiceException("积分不足，兑换需要 " + cost + " 积分，当前积分：" + record.getPoints());
        }

        // 扣积分
        spendPoints(userId, cost, PointsChangeType.EXCHANGE_MEMBERSHIP, "兑换 " + days + " 天会员");

        // 延长会员有效期
        LocalDateTime now = LocalDateTime.now();
        if (record.getMemberUntil() != null && record.getMemberUntil().isAfter(now)) {
            // 当前是会员，从现有截止时间延长
            record.setMemberUntil(record.getMemberUntil().plusDays(days));
        } else {
            // 非会员或已过期，从现在开始
            record.setMemberUntil(now.plusDays(days));
        }
        membershipRecordMapper.updateById(record);

        log.info("用户 {} 兑换 {} 天会员，会员有效期至 {}", userId, days, record.getMemberUntil());
    }

    // ==================== 积分日志 ====================

    /**
     * 获取用户积分变动日志（分页）
     */
    public Page<PointsLog> getPointsLogPage(Long userId, int currentPage, int size) {
        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsLog::getUserId, userId)
               .orderByDesc(PointsLog::getCreateTime);

        return pointsLogMapper.selectPage(new Page<>(currentPage, size), wrapper);
    }

    // ==================== 管理员操作 ====================

    /**
     * 管理员发放积分
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminGrantPoints(Long userId, int points, String reason) {
        checkAdmin();
        if (points <= 0) {
            throw new ServiceException("发放积分必须大于0");
        }
        String desc = "管理员发放：" + (reason != null ? reason : "无说明");
        awardPoints(userId, points, PointsChangeType.ADMIN_GRANT, null, desc);
    }

    /**
     * 管理员扣除积分
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminRevokePoints(Long userId, int points, String reason) {
        checkAdmin();
        if (points <= 0) {
            throw new ServiceException("扣除积分必须大于0");
        }
        String desc = "管理员扣除：" + (reason != null ? reason : "无说明");
        spendPoints(userId, points, PointsChangeType.ADMIN_REVOKE, desc);
    }

    /**
     * 管理员设置会员有效期
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminSetMembership(Long userId, LocalDateTime memberUntil) {
        checkAdmin();
        MembershipRecord record = getOrCreateRecord(userId);
        record.setMemberUntil(memberUntil);
        membershipRecordMapper.updateById(record);
        log.info("管理员设置用户 {} 会员有效期至 {}", userId, memberUntil);
    }

    /**
     * 管理员撤销会员
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminRevokeMembership(Long userId) {
        checkAdmin();
        MembershipRecord record = getOrCreateRecord(userId);
        record.setMemberUntil(null);
        membershipRecordMapper.updateById(record);
        log.info("管理员撤销用户 {} 会员资格", userId);
    }

    /**
     * 管理员分页查询所有会员记录
     */
    public Page<MembershipRecord> adminListMemberships(int currentPage, int size) {
        checkAdmin();
        LambdaQueryWrapper<MembershipRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(MembershipRecord::getPoints);
        return membershipRecordMapper.selectPage(new Page<>(currentPage, size), wrapper);
    }

    // ==================== 内部方法 ====================

    private void insertPointsLog(Long userId, int pointsChange, int pointsAfter,
                                  String changeType, Long relatedId, String description) {
        PointsLog logEntry = new PointsLog();
        logEntry.setUserId(userId);
        logEntry.setPointsChange(pointsChange);
        logEntry.setPointsAfter(pointsAfter);
        logEntry.setChangeType(changeType);
        logEntry.setRelatedId(relatedId);
        logEntry.setDescription(description);
        pointsLogMapper.insert(logEntry);
    }

    private void checkAdmin() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("无权限，仅管理员可执行此操作");
        }
    }
}
