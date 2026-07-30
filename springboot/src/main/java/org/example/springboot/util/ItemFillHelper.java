package org.example.springboot.util;

import org.example.springboot.entity.BaseItem;
import org.example.springboot.entity.ItemCategory;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.ItemCategoryMapper;
import org.example.springboot.mapper.UserMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 物品关联信息填充工具
 *
 * 统一处理失物/招领列表的分类名称和用户名的批量填充，
 * 避免 LostItemService / FoundItemService 中重复的 N+1 查询逻辑。
 */
public final class ItemFillHelper {

    private ItemFillHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 批量填充分类名称、用户名和会员标识
     *
     * @param items          物品列表（BaseItem 的子类）
     * @param categoryMapper 分类 Mapper
     * @param userMapper     用户 Mapper
     * @param memberIds      当前会员用户ID集合
     * @param <T>            继承 BaseItem 的具体类型
     */
    public static <T extends BaseItem> void fillInfoBatch(
            List<T> items,
            ItemCategoryMapper categoryMapper,
            UserMapper userMapper,
            Set<Long> memberIds) {

        fillInfoBatch(items, categoryMapper, userMapper);
        fillMemberFlag(items, memberIds);
    }

    /**
     * 批量填充分类名称和用户名（不包含会员标识）
     *
     * @param items          物品列表（BaseItem 的子类）
     * @param categoryMapper 分类 Mapper
     * @param userMapper     用户 Mapper
     * @param <T>            继承 BaseItem 的具体类型
     */
    public static <T extends BaseItem> void fillInfoBatch(
            List<T> items,
            ItemCategoryMapper categoryMapper,
            UserMapper userMapper) {

        if (items == null || items.isEmpty()) {
            return;
        }

        // 收集去重的分类ID
        Set<Long> categoryIds = items.stream()
                .map(BaseItem::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 一次性批量加载分类
        Map<Long, ItemCategory> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<ItemCategory> categories = categoryMapper.selectBatchIds(categoryIds);
            for (ItemCategory c : categories) {
                categoryMap.put(c.getId(), c);
            }
        }

        // 收集去重的用户ID
        Set<Long> userIds = items.stream()
                .map(BaseItem::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 一次性批量加载用户
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        // 从内存 Map 填充
        for (T item : items) {
            if (item.getCategoryId() != null) {
                ItemCategory c = categoryMap.get(item.getCategoryId());
                if (c != null) {
                    item.setCategoryName(c.getName());
                }
            }
            if (item.getUserId() != null) {
                User u = userMap.get(item.getUserId());
                if (u != null) {
                    item.setUsername(u.getUsername());
                }
            }
        }
    }

    /**
     * 填充会员标识
     */
    private static <T extends BaseItem> void fillMemberFlag(List<T> items, Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty() || items == null) {
            return;
        }
        for (T item : items) {
            if (item.getUserId() != null) {
                item.setIsMemberItem(memberIds.contains(item.getUserId()));
            }
        }
    }
}
