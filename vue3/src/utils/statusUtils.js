/**
 * 物品状态工具函数
 * 统一管理5种物品状态的文本、颜色、标签类型
 *
 * 状态值说明：
 *   0 - 待认领 (PENDING)
 *   1 - 已认领 (CLAIMED)
 *   2 - 已完成 (COMPLETED)
 *   3 - 已关闭 (CLOSED)
 *   4 - 已过期 (EXPIRED)
 */

/** 状态文本映射 */
export const STATUS_TEXT_MAP = {
  0: '待认领',
  1: '已认领',
  2: '已完成',
  3: '已关闭',
  4: '已过期'
}

/** 状态标签类型映射（用于 el-tag 的 type 属性） */
export const STATUS_TAG_TYPE_MAP = {
  0: 'warning',   // 待认领 - 橙色
  1: 'success',   // 已认领 - 绿色
  2: 'info',      // 已完成 - 蓝色
  3: 'info',      // 已关闭 - 灰色
  4: 'danger'     // 已过期 - 红色
}

/** 状态CSS类名映射 */
export const STATUS_CLASS_MAP = {
  0: 'status-pending',
  1: 'status-claimed',
  2: 'status-completed',
  3: 'status-closed',
  4: 'status-expired'
}

/** 状态颜色映射（用于自定义样式） */
export const STATUS_COLOR_MAP = {
  0: '#E6A23C',
  1: '#67C23A',
  2: '#409EFF',
  3: '#909399',
  4: '#F56C6C'
}

/**
 * 获取状态文本
 * @param {number} status - 状态值
 * @returns {string} 状态文本
 */
export const getStatusText = (status) => {
  return STATUS_TEXT_MAP[status] || '未知'
}

/**
 * 获取 el-tag 的 type 属性值
 * @param {number} status - 状态值
 * @returns {string} 标签类型
 */
export const getStatusTagType = (status) => {
  return STATUS_TAG_TYPE_MAP[status] || 'info'
}

/**
 * 获取状态CSS类名
 * @param {number} status - 状态值
 * @returns {string} CSS类名
 */
export const getStatusClass = (status) => {
  return STATUS_CLASS_MAP[status] || ''
}

/**
 * 获取状态颜色
 * @param {number} status - 状态值
 * @returns {string} 颜色值
 */
export const getStatusColor = (status) => {
  return STATUS_COLOR_MAP[status] || '#909399'
}

/**
 * 判断状态是否为终态（已完成、已关闭、已过期）
 * @param {number} status - 状态值
 * @returns {boolean}
 */
export const isFinalStatus = (status) => {
  return [2, 3, 4].includes(status)
}

/**
 * 判断物品是否可认领
 * @param {number} status - 状态值
 * @returns {boolean}
 */
export const canClaim = (status) => {
  return status === 0
}
