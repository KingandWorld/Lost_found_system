package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.MembershipRecord;
import org.example.springboot.entity.PointsLog;
import org.example.springboot.entity.User;
import org.example.springboot.service.MembershipService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 会员管理控制器
 */
@Tag(name = "会员管理")
@RestController
@RequestMapping("/membership")
public class MembershipController {
    private static final Logger log = LoggerFactory.getLogger(MembershipController.class);

    @Resource
    private MembershipService membershipService;

    // ==================== 用户端接口 ====================

    @Operation(summary = "获取当前用户会员信息")
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyMembership() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }

        MembershipRecord record = membershipService.getOrCreateRecord(currentUser.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("points", record.getPoints());
        result.put("totalPointsEarned", record.getTotalPointsEarned());
        result.put("totalPointsSpent", record.getTotalPointsSpent());
        result.put("memberUntil", record.getMemberUntil());
        result.put("isMember", record.isCurrentlyMember());

        return Result.success(result);
    }

    @Operation(summary = "获取积分变动历史")
    @GetMapping("/my/logs")
    public Result<Page<PointsLog>> getMyPointsLog(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }

        Page<PointsLog> page = membershipService.getPointsLogPage(currentUser.getId(), currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "积分兑换会员")
    @PostMapping("/exchange")
    public Result<String> exchangeMembership() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("未登录");
        }

        try {
            membershipService.exchangeMembership(currentUser.getId());
            MembershipRecord record = membershipService.getOrCreateRecord(currentUser.getId());
            String until = record.getMemberUntil() != null
                    ? record.getMemberUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "未知";
            return Result.success("兑换成功！会员有效期至 " + until);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 管理员接口 ====================

    @Operation(summary = "管理员：获取所有会员记录列表")
    @GetMapping("/admin/list")
    public Result<Page<MembershipRecord>> adminList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<MembershipRecord> page = membershipService.adminListMemberships(currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "管理员：获取某用户会员详情")
    @GetMapping("/admin/{userId}")
    public Result<Map<String, Object>> adminGetUserMembership(@PathVariable Long userId) {
        MembershipRecord record = membershipService.getOrCreateRecord(userId);
        Page<PointsLog> logs = membershipService.getPointsLogPage(userId, 1, 20);

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("recentLogs", logs.getRecords());
        return Result.success(result);
    }

    @Operation(summary = "管理员：发放积分")
    @PostMapping("/admin/{userId}/points/grant")
    public Result<String> adminGrantPoints(@PathVariable Long userId, @RequestBody Map<String, Object> params) {
        int points = (int) params.get("points");
        String reason = (String) params.getOrDefault("reason", "");
        membershipService.adminGrantPoints(userId, points, reason);
        return Result.success("已为用户发放 " + points + " 积分");
    }

    @Operation(summary = "管理员：扣除积分")
    @PostMapping("/admin/{userId}/points/revoke")
    public Result<String> adminRevokePoints(@PathVariable Long userId, @RequestBody Map<String, Object> params) {
        int points = (int) params.get("points");
        String reason = (String) params.getOrDefault("reason", "");
        membershipService.adminRevokePoints(userId, points, reason);
        return Result.success("已扣除用户 " + points + " 积分");
    }

    @Operation(summary = "管理员：设置会员有效期")
    @PutMapping("/admin/{userId}/membership/set")
    public Result<String> adminSetMembership(@PathVariable Long userId, @RequestBody Map<String, String> params) {
        try {
            String untilStr = params.get("memberUntil");
            if (untilStr == null || untilStr.isEmpty()) {
                return Result.error("请提供会员有效期");
            }
            LocalDateTime until = LocalDateTime.parse(untilStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            membershipService.adminSetMembership(userId, until);
            return Result.success("会员有效期已设置");
        } catch (Exception e) {
            log.error("设置会员有效期失败: {}", e.getMessage(), e);
            return Result.error("设置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "管理员：撤销会员")
    @PutMapping("/admin/{userId}/membership/revoke")
    public Result<String> adminRevokeMembership(@PathVariable Long userId) {
        try {
            membershipService.adminRevokeMembership(userId);
            return Result.success("会员资格已撤销");
        } catch (Exception e) {
            log.error("撤销会员失败: {}", e.getMessage(), e);
            return Result.error("撤销失败: " + e.getMessage());
        }
    }
}
