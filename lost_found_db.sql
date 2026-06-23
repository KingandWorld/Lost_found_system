-- phpMyAdmin SQL Dump
-- version 5.0.4
-- https://www.phpmyadmin.net/
--
-- 主机： localhost
-- 生成日期： 2026-07-05 13:04:14
-- 服务器版本： 8.0.45
-- PHP 版本： 7.4.33

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 数据库： `lost_found_db`
--

-- --------------------------------------------------------

--
-- 表的结构 `claim_application`
--

CREATE TABLE `claim_application` (
  `id` bigint NOT NULL COMMENT '申请ID',
  `item_id` bigint NOT NULL COMMENT '物品ID',
  `item_type` tinyint NOT NULL COMMENT '物品类型(0招领信息,1失物信息)',
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '申请说明',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态(0待审核,1已通过,2已拒绝,3已取消)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审核备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认领申请表' ROW_FORMAT=DYNAMIC;

--
-- 转存表中的数据 `claim_application`
--

INSERT INTO `claim_application` (`id`, `item_id`, `item_type`, `user_id`, `description`, `status`, `create_time`, `update_time`, `audit_user_id`, `audit_time`, `audit_remark`) VALUES
(1, 1, 1, 3, '俺滴学生证！！！！！！！！！！', 3, '2025-06-23 17:34:22', '2025-06-23 17:51:34', NULL, '2025-06-23 17:39:49', '用户主动取消申请'),
(2, 1, 1, 3, '在我这！！！！！！！！', 1, '2025-06-23 17:40:01', '2025-06-23 17:51:37', 2, '2025-06-23 17:43:54', '谢谢泥'),
(3, 5, 1, 3, '111111111111111', 0, '2025-06-25 11:39:25', '2025-06-25 11:39:25', NULL, NULL, NULL),
(4, 7, 1, 2, '电饭锅fdgf大概的方式的方式的', 0, '2026-07-01 15:39:45', '2026-07-01 15:39:45', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- 表的结构 `found_item`
--

CREATE TABLE `found_item` (
  `id` bigint NOT NULL COMMENT '招领ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '描述',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `found_place` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '拾取地点',
  `found_time` datetime DEFAULT NULL COMMENT '拾取时间',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `images` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片(多张用逗号分隔)',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态(0待认领,1已认领,2已交接,3已关闭,4已过期)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招领信息表' ROW_FORMAT=DYNAMIC;

--
-- 转存表中的数据 `found_item`
--

INSERT INTO `found_item` (`id`, `title`, `description`, `category_id`, `found_place`, `found_time`, `contact_name`, `contact_phone`, `images`, `user_id`, `status`, `create_time`, `update_time`) VALUES
(1, '捡到一部手机', '在教学楼捡到一部iPhone手机，黑色外壳', 2, '教学楼三楼', '2023-05-17 09:15:00', '李四', '13900139000', NULL, 2, 0, '2026-06-10 03:49:40', '2026-06-10 03:49:40'),
(2, '捡到一串钥匙', '在操场捡到一串钥匙，约5把', 4, '操场跑道', '2023-05-18 16:20:00', '李四', '13900139001', NULL, 2, 0, '2026-06-10 03:49:40', '2026-06-10 03:49:40'),
(3, '捡到一本660', '里面写了2页左右，封面没有姓名', 5, '图书馆楼下共享单车', '2025-06-19 00:00:00', 'jx', '13123456789', '/img/1750670457832.png', 3, 0, '2026-06-10 03:49:40', '2026-06-10 03:49:40'),
(4, '测试测试', '测试测试测试测试测试', 2, '测试', '2025-06-26 00:00:00', 'user1', '13800138001', '/img/1751073473254.jpg', 2, 3, '2026-06-10 03:49:40', '2026-06-10 03:49:40'),
(5, '捡到一个计算器', '在教学楼A栋303教室捡到一个科学计算器，品牌卡西欧，请失主联系认领。', 2, '教学楼A栋303', '2026-06-28 09:00:00', '李四', '13900139001', NULL, 2, 0, '2026-07-05 12:48:39', '2026-07-05 12:48:39'),
(6, '捡到一个计算器', '在教学楼A栋303教室捡到一个科学计算器，品牌卡西欧，请失主联系认领。', 2, '教学楼A栋303', '2026-06-28 09:00:00', '李四', '13900139001', NULL, 2, 0, '2026-07-05 12:57:51', '2026-07-05 12:57:51');

-- --------------------------------------------------------

--
-- 表的结构 `item_category`
--

CREATE TABLE `item_category` (
  `id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `sort` int DEFAULT '0' COMMENT '排序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物品分类表' ROW_FORMAT=DYNAMIC;

--
-- 转存表中的数据 `item_category`
--

INSERT INTO `item_category` (`id`, `name`, `sort`, `create_time`, `update_time`) VALUES
(1, '证件类(更新)', 1, '2025-06-23 16:59:00', '2026-07-05 12:39:28'),
(2, '电子产品', 2, '2025-06-23 16:59:00', '2025-06-23 16:59:00'),
(3, '现金/卡类', 3, '2025-06-23 16:59:00', '2025-06-23 16:59:00'),
(4, '生活用品', 4, '2025-06-23 16:59:00', '2025-06-23 16:59:00'),
(5, '书籍资料', 5, '2025-06-23 16:59:00', '2025-06-23 16:59:00'),
(6, '衣物饰品', 6, '2025-06-23 16:59:00', '2025-06-23 16:59:00'),
(7, '其他', 99, '2025-06-23 16:59:00', '2025-06-23 16:59:00'),
(9, '测试分类_1783226365', 99, '2026-07-05 12:39:27', '2026-07-05 12:39:27'),
(10, '测试分类_1783226384', 99, '2026-07-05 12:39:46', '2026-07-05 12:39:46'),
(11, '测试分类_1783226916', 99, '2026-07-05 12:48:38', '2026-07-05 12:48:38'),
(12, '测试分类_1783227467', 99, '2026-07-05 12:57:50', '2026-07-05 12:57:50');

-- --------------------------------------------------------

--
-- 表的结构 `lost_item`
--

CREATE TABLE `lost_item` (
  `id` bigint NOT NULL COMMENT '失物ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '描述',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `lost_place` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '丢失地点',
  `lost_time` datetime DEFAULT NULL COMMENT '丢失时间',
  `contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `images` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片(多张用逗号分隔)',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态(0待认领,1已认领,2已交接,3已关闭,4已过期)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失物信息表' ROW_FORMAT=DYNAMIC;

--
-- 转存表中的数据 `lost_item`
--

INSERT INTO `lost_item` (`id`, `title`, `description`, `category_id`, `lost_place`, `lost_time`, `contact_name`, `contact_phone`, `images`, `user_id`, `status`, `create_time`, `update_time`) VALUES
(2, '丢失黑色钱包', '内有身份证、银行卡等重要证件', 3, '食堂门口', '2023-05-16 12:00:00', '张三', '13800138001', '/img/1750829121743.jpg,/img/1750829130466.jpg', 2, 0, '2026-06-10 03:49:39', '2026-06-10 03:49:39'),
(3, '学生证丢了', '有我照片，学号为2006051039', 1, '图书馆2楼', '2025-06-17 00:00:00', 'test', '13123456789', '/img/1750671050359.jpeg', 3, 0, '2026-06-10 03:49:39', '2026-06-10 03:49:39'),
(4, '照片丢了', '照片如下，一个小房子', 4, '南食堂', '2025-06-20 00:00:00', 'test', '13123456789', '/img/1750671103600.jpeg', 3, 0, '2026-06-10 03:49:39', '2026-06-10 03:49:39'),
(5, 'test0000', 'test0000111', 1, 'test0000', '2025-06-17 00:00:00', 'user1', '13800138001', '', 2, 2, '2025-06-25 11:38:16', '2025-06-25 11:40:14'),
(6, '第三方的身份', '发热感发热Greg废物废物废物发', 2, '的发生打撒的', '2026-06-10 00:00:00', 'user1', '13800138001', '/img/1781032270339.jpg', 2, 0, '2026-06-10 03:49:39', '2026-06-10 03:49:39'),
(65, 'test', 'test test test test test', 1, 'place', '2026-06-28 14:30:00', 'me', '13800138001', NULL, 1, 0, '2026-07-05 12:47:35', '2026-07-05 12:47:35'),
(66, '测试丢失的笔记本', '在图书馆自习室丢失一台银色笔记本电脑，配有黑色充电器，如有捡到请联系。', 2, '图书馆四楼自习室', '2026-06-28 14:30:00', '管理员', '13800138001', NULL, 1, 0, '2026-07-05 12:48:38', '2026-07-05 12:48:38'),
(67, '用户丢失的背包', '在操场丢失黑色双肩背包一个，内有课本和文具，请拾到者联系。', 4, '操场主席台', '2026-06-27 10:00:00', '张三', '13900139001', NULL, 2, 0, '2026-07-05 12:48:38', '2026-07-05 12:48:38');

-- --------------------------------------------------------

--
-- 表的结构 `notification`
--

CREATE TABLE `notification` (
  `id` bigint NOT NULL COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型(0系统消息,1申请消息,2审核消息)',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读(0未读,1已读)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知消息表' ROW_FORMAT=DYNAMIC;

--
-- 转存表中的数据 `notification`
--

INSERT INTO `notification` (`id`, `user_id`, `title`, `content`, `type`, `related_id`, `is_read`, `create_time`) VALUES
(1, 2, '新的认领申请', '用户 jx 申请认领您发布的物品：test0000，请及时处理。', 1, 3, 1, '2025-06-25 11:39:25'),
(2, 3, '物品状态变更', '您发布的物品：照片丢了，状态已从 待认领 变更为 已过期。', 0, 4, 0, '2026-06-10 02:00:00'),
(3, 2, '物品状态变更', '您发布的物品：丢失黑色钱包，状态已从 待认领 变更为 已过期。', 0, 2, 1, '2026-06-10 02:00:00'),
(4, 3, '物品状态变更', '您发布的物品：学生证丢了，状态已从 待认领 变更为 已过期。', 0, 3, 0, '2026-06-10 02:00:00'),
(5, 2, '物品状态变更', '您发布的物品：捡到一串钥匙，状态已从 待认领 变更为 已过期。', 0, 2, 1, '2026-06-10 02:00:00'),
(6, 3, '物品状态变更', '您发布的物品：捡到一本660，状态已从 待认领 变更为 已过期。', 0, 3, 0, '2026-06-10 02:00:00'),
(7, 2, '物品状态变更', '您发布的物品：捡到一部手机，状态已从 待认领 变更为 已过期。', 0, 1, 1, '2026-06-10 02:00:00'),
(8, 2, '物品状态变更', '您发布的物品：测试测试，状态已从 待认领 变更为 已过期。', 0, 4, 1, '2026-06-10 02:00:00'),
(9, 3, '新的认领申请', '用户 张三 申请认领您发布的物品：gdfgfdgdfg，请及时处理。', 1, 4, 0, '2026-07-01 15:39:45'),
(10, 2, '物品状态变更', '您发布的物品：测试测试，状态已从 待认领 变更为 已关闭。', 0, 4, 1, '2026-07-05 12:48:39');

-- --------------------------------------------------------

--
-- 表的结构 `user`
--

CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `role_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'USER' COMMENT '角色编码(ADMIN/USER)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态(0待审核,1正常,2审核失败,3已禁用,4已锁定,5已过期)',
  `sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表' ROW_FORMAT=DYNAMIC;

--
-- 转存表中的数据 `user`
--

INSERT INTO `user` (`id`, `username`, `password`, `name`, `phone`, `email`, `avatar`, `role_code`, `create_time`, `update_time`, `status`, `sex`) VALUES
(1, 'admin', '$2a$10$S7fPW1PZXknUl1cZJlcOK.xcSrMV1w68kcB7JB4sknKJpti/HOrnC', '管理员-已更新', NULL, NULL, NULL, 'ADMIN', '2025-06-23 16:59:00', '2026-07-05 12:37:28', 1, ''),
(2, 'user1', '$2a$10$S7fPW1PZXknUl1cZJlcOK.xcSrMV1w68kcB7JB4sknKJpti/HOrnC', '张三', '13800138001', 'zhangsan@example.com', NULL, 'USER', '2025-06-23 16:59:00', '2025-06-23 17:08:04', 1, ''),
(3, 'test', '$2a$10$S7fPW1PZXknUl1cZJlcOK.xcSrMV1w68kcB7JB4sknKJpti/HOrnC', 'jx', '13123456789', '1796145602@qq.com', NULL, 'USER', '2025-06-23 17:07:52', '2025-06-23 17:07:52', 1, NULL),
(5, 'test1111', '$2a$10$j8iABY5QTClh8VveiXWQjeGenMUaftitg9NKmBy1GfXI.di/rJJv2', 'jx11', '13123456778', '133456789@qq.com', NULL, 'USER', '2025-06-23 23:49:54', '2025-06-23 23:49:54', 0, NULL),
(6, 'test000', '$2a$10$yAMRe7rkHTQSOXgNhreyB.pIc5i4b9ma7.2v64jwgVUGDR6Qbj9Q.', '1515', '13123456789', '154165@163.com', NULL, 'USER', '2025-07-01 16:01:10', '2025-07-01 16:01:10', 0, NULL),
(7, 'test_1783226244', '$2a$10$axbDgD8YqjuNxzx1Z3njx.NPE97B6IO/uaMANkGQe9BqKHBYZ.oc.', '测试用户', '13800138002', 'test_1783226244@example.com', NULL, 'USER', '2026-07-05 12:37:27', '2026-07-05 12:37:27', 0, NULL),
(8, 'test_1783226365', '$2a$10$t8hRNbcafsxmq8WHFmdnKO0psGZiffa2L7laI4Ip050qKMgDmJnsq', '测试用户', '13800138002', 'test_1783226365@example.com', NULL, 'USER', '2026-07-05 12:39:27', '2026-07-05 12:39:27', 0, NULL),
(9, 'test_1783226384', '$2a$10$IJ9oxgpr8FzgXkhDcirqGe5Ba58AMQ/pUFcXYdbWtliV.Qc6SOJDa', '测试用户', '13800138002', 'test_1783226384@example.com', NULL, 'USER', '2026-07-05 12:39:45', '2026-07-05 12:39:45', 0, NULL),
(10, 'test_1783226916', '$2a$10$Q8i3sNOTfXrrq33iKcqd3uzuRnRh6nhQkfxghspfZ6k5Np4l3LGeO', '测试用户', '13800138002', 'test_1783226916@example.com', NULL, 'USER', '2026-07-05 12:48:37', '2026-07-05 12:48:37', 0, NULL),
(11, 'test_1508', '$2a$10$LTRKuwOIgUv19E63q6JoIORGAamN7ouj2ONLIvMUsZ9eddZRqAiTm', 'testuser', NULL, NULL, NULL, 'USER', '2026-07-05 12:52:36', '2026-07-05 12:52:36', 0, NULL),
(12, 'test_1783227467', '$2a$10$HMvjeY9nElY6I4zNQQzQmuoh6B8cuWpMfnOILWOKPVGrNPYHONlZe', '测试用户', '13800138002', 'test_1783227467@example.com', NULL, 'USER', '2026-07-05 12:57:49', '2026-07-05 12:57:49', 0, NULL);

--
-- 转储表的索引
--

--
-- 表的索引 `claim_application`
--
ALTER TABLE `claim_application`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD KEY `idx_item` (`item_id`,`item_type`) USING BTREE,
  ADD KEY `idx_user_id` (`user_id`) USING BTREE,
  ADD KEY `idx_status` (`status`) USING BTREE;

--
-- 表的索引 `found_item`
--
ALTER TABLE `found_item`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD KEY `idx_category_id` (`category_id`) USING BTREE,
  ADD KEY `idx_user_id` (`user_id`) USING BTREE,
  ADD KEY `idx_status` (`status`) USING BTREE,
  ADD KEY `idx_status_create_time` (`status`,`create_time`) USING BTREE,
  ADD KEY `idx_category_status` (`category_id`,`status`) USING BTREE;

--
-- 表的索引 `item_category`
--
ALTER TABLE `item_category`
  ADD PRIMARY KEY (`id`) USING BTREE;

--
-- 表的索引 `lost_item`
--
ALTER TABLE `lost_item`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD KEY `idx_category_id` (`category_id`) USING BTREE,
  ADD KEY `idx_user_id` (`user_id`) USING BTREE,
  ADD KEY `idx_status` (`status`) USING BTREE,
  ADD KEY `idx_status_create_time` (`status`,`create_time`) USING BTREE,
  ADD KEY `idx_category_status` (`category_id`,`status`) USING BTREE;

--
-- 表的索引 `notification`
--
ALTER TABLE `notification`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD KEY `idx_user_id` (`user_id`) USING BTREE,
  ADD KEY `idx_is_read` (`is_read`) USING BTREE,
  ADD KEY `idx_user_read` (`user_id`,`is_read`) USING BTREE,
  ADD KEY `idx_create_time` (`create_time`) USING BTREE;

--
-- 表的索引 `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`) USING BTREE,
  ADD UNIQUE KEY `uk_username` (`username`) USING BTREE;

--
-- 在导出的表使用AUTO_INCREMENT
--

--
-- 使用表AUTO_INCREMENT `claim_application`
--
ALTER TABLE `claim_application`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID', AUTO_INCREMENT=5;

--
-- 使用表AUTO_INCREMENT `found_item`
--
ALTER TABLE `found_item`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招领ID', AUTO_INCREMENT=7;

--
-- 使用表AUTO_INCREMENT `item_category`
--
ALTER TABLE `item_category`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID', AUTO_INCREMENT=13;

--
-- 使用表AUTO_INCREMENT `lost_item`
--
ALTER TABLE `lost_item`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '失物ID', AUTO_INCREMENT=71;

--
-- 使用表AUTO_INCREMENT `notification`
--
ALTER TABLE `notification`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID', AUTO_INCREMENT=11;

--
-- 使用表AUTO_INCREMENT `user`
--
ALTER TABLE `user`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID', AUTO_INCREMENT=13;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
