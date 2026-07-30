package org.example.springboot.enumClass;

/**
 * 积分变动类型枚举
 */
public enum PointsChangeType {
    PUBLISH_LOST("PUBLISH_LOST", "发布失物"),
    PUBLISH_FOUND("PUBLISH_FOUND", "发布招领"),
    ITEM_COMPLETED("ITEM_COMPLETED", "物品交接完成"),
    CLAIM_SUCCESS("CLAIM_SUCCESS", "认领成功"),
    EXCHANGE_MEMBERSHIP("EXCHANGE_MEMBERSHIP", "兑换会员"),
    ADMIN_GRANT("ADMIN_GRANT", "管理员发放"),
    ADMIN_REVOKE("ADMIN_REVOKE", "管理员扣除");

    private final String value;
    private final String description;

    PointsChangeType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static PointsChangeType fromValue(String value) {
        for (PointsChangeType type : PointsChangeType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
