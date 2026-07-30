package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.mapper.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Tag(name = "管理仪表盘接口")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private LostItemMapper lostItemMapper;

    @Resource
    private FoundItemMapper foundItemMapper;

    @Resource
    private ClaimApplicationMapper claimApplicationMapper;

    @Resource
    private ItemCategoryMapper itemCategoryMapper;

    @Operation(summary = "获取管理后台统计数据")
    @GetMapping("/statistics")
    public Result<?> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("activeUsers", userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.User>()
                        .eq(org.example.springboot.entity.User::getStatus, 1)));

        // 失物统计
        long totalLostItems = lostItemMapper.selectCount(null);
        stats.put("totalLostItems", totalLostItems);
        stats.put("pendingLostItems", lostItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.LostItem>()
                        .eq(org.example.springboot.entity.LostItem::getStatus, 0)));
        stats.put("claimedLostItems", lostItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.LostItem>()
                        .eq(org.example.springboot.entity.LostItem::getStatus, 1)));
        stats.put("completedLostItems", lostItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.LostItem>()
                        .eq(org.example.springboot.entity.LostItem::getStatus, 2)));
        stats.put("expiredLostItems", lostItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.LostItem>()
                        .eq(org.example.springboot.entity.LostItem::getStatus, 4)));

        // 招领统计
        long totalFoundItems = foundItemMapper.selectCount(null);
        stats.put("totalFoundItems", totalFoundItems);
        stats.put("pendingFoundItems", foundItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.FoundItem>()
                        .eq(org.example.springboot.entity.FoundItem::getStatus, 0)));
        stats.put("claimedFoundItems", foundItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.FoundItem>()
                        .eq(org.example.springboot.entity.FoundItem::getStatus, 1)));
        stats.put("completedFoundItems", foundItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.FoundItem>()
                        .eq(org.example.springboot.entity.FoundItem::getStatus, 2)));
        stats.put("expiredFoundItems", foundItemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.FoundItem>()
                        .eq(org.example.springboot.entity.FoundItem::getStatus, 4)));

        // 物品总数
        stats.put("totalItems", totalLostItems + totalFoundItems);

        // 认领统计
        stats.put("totalClaims", claimApplicationMapper.selectCount(null));
        stats.put("pendingClaims", claimApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.ClaimApplication>()
                        .eq(org.example.springboot.entity.ClaimApplication::getStatus, 0)));
        stats.put("approvedClaims", claimApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.example.springboot.entity.ClaimApplication>()
                        .eq(org.example.springboot.entity.ClaimApplication::getStatus, 1)));

        // 分类统计
        stats.put("totalCategories", itemCategoryMapper.selectCount(null));

        return Result.success(stats);
    }
}
