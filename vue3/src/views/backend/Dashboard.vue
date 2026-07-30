<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <template #header>
        <div class="welcome-header">
          <el-avatar :size="64" :src="avatarUrl">
            {{ userInfo?.name?.charAt(0) }}
          </el-avatar>
          <div class="welcome-info">
            <h2>欢迎回来, {{ userInfo?.name || userInfo?.username }}</h2>
            <p>{{ currentTime }}</p>
          </div>
        </div>
      </template>
      <div class="role-info">
        <el-tag>{{ roleLabel }}</el-tag>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="statsLoading">
      <!-- 用户统计 -->
      <el-card class="stat-card stat-card--users" shadow="hover">
        <div class="stat-card__inner">
          <div class="stat-card__icon">
            <el-icon :size="32"><UserFilled /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__number">{{ statistics.totalUsers || 0 }}</div>
            <div class="stat-card__label">总用户数</div>
            <div class="stat-card__sub">活跃 {{ statistics.activeUsers || 0 }} 人</div>
          </div>
        </div>
      </el-card>

      <!-- 物品总数 -->
      <el-card class="stat-card stat-card--items" shadow="hover">
        <div class="stat-card__inner">
          <div class="stat-card__icon">
            <el-icon :size="32"><Box /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__number">{{ statistics.totalItems || 0 }}</div>
            <div class="stat-card__label">物品总数</div>
            <div class="stat-card__sub">失物 {{ statistics.totalLostItems || 0 }} / 招领 {{ statistics.totalFoundItems || 0 }}</div>
          </div>
        </div>
      </el-card>

      <!-- 待认领 -->
      <el-card class="stat-card stat-card--pending" shadow="hover">
        <div class="stat-card__inner">
          <div class="stat-card__icon">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__number">{{ (statistics.pendingLostItems || 0) + (statistics.pendingFoundItems || 0) }}</div>
            <div class="stat-card__label">待认领</div>
            <div class="stat-card__sub">失物 {{ statistics.pendingLostItems || 0 }} / 招领 {{ statistics.pendingFoundItems || 0 }}</div>
          </div>
        </div>
      </el-card>

      <!-- 认领申请 -->
      <el-card class="stat-card stat-card--claims" shadow="hover">
        <div class="stat-card__inner">
          <div class="stat-card__icon">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__number">{{ statistics.totalClaims || 0 }}</div>
            <div class="stat-card__label">认领申请</div>
            <div class="stat-card__sub">待审核 {{ statistics.pendingClaims || 0 }} / 已通过 {{ statistics.approvedClaims || 0 }}</div>
          </div>
        </div>
      </el-card>

      <!-- 分类数量 -->
      <el-card class="stat-card stat-card--categories" shadow="hover">
        <div class="stat-card__inner">
          <div class="stat-card__icon">
            <el-icon :size="32"><Grid /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__number">{{ statistics.totalCategories || 0 }}</div>
            <div class="stat-card__label">物品分类</div>
          </div>
        </div>
      </el-card>

      <!-- 已过期 -->
      <el-card class="stat-card stat-card--expired" shadow="hover">
        <div class="stat-card__inner">
          <div class="stat-card__icon">
            <el-icon :size="32"><WarningFilled /></el-icon>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__number">{{ (statistics.expiredLostItems || 0) + (statistics.expiredFoundItems || 0) }}</div>
            <div class="stat-card__label">已过期</div>
            <div class="stat-card__sub">失物 {{ statistics.expiredLostItems || 0 }} / 招领 {{ statistics.expiredFoundItems || 0 }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 物品状态分布 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="section-title">失物状态分布</span>
          </template>
          <div class="status-distribution">
            <div class="status-bar">
              <div class="status-bar__label">待认领</div>
              <el-progress :percentage="calcPercent(statistics.pendingLostItems, statistics.totalLostItems)" color="#E6A23C" />
              <span class="status-bar__count">{{ statistics.pendingLostItems || 0 }}</span>
            </div>
            <div class="status-bar">
              <div class="status-bar__label">已认领</div>
              <el-progress :percentage="calcPercent(statistics.claimedLostItems, statistics.totalLostItems)" color="#67C23A" />
              <span class="status-bar__count">{{ statistics.claimedLostItems || 0 }}</span>
            </div>
            <div class="status-bar">
              <div class="status-bar__label">已完成</div>
              <el-progress :percentage="calcPercent(statistics.completedLostItems, statistics.totalLostItems)" color="#409EFF" />
              <span class="status-bar__count">{{ statistics.completedLostItems || 0 }}</span>
            </div>
            <div class="status-bar">
              <div class="status-bar__label">已过期</div>
              <el-progress :percentage="calcPercent(statistics.expiredLostItems, statistics.totalLostItems)" color="#F56C6C" />
              <span class="status-bar__count">{{ statistics.expiredLostItems || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card" shadow="never">
          <template #header>
            <span class="section-title">招领状态分布</span>
          </template>
          <div class="status-distribution">
            <div class="status-bar">
              <div class="status-bar__label">待认领</div>
              <el-progress :percentage="calcPercent(statistics.pendingFoundItems, statistics.totalFoundItems)" color="#E6A23C" />
              <span class="status-bar__count">{{ statistics.pendingFoundItems || 0 }}</span>
            </div>
            <div class="status-bar">
              <div class="status-bar__label">已认领</div>
              <el-progress :percentage="calcPercent(statistics.claimedFoundItems, statistics.totalFoundItems)" color="#67C23A" />
              <span class="status-bar__count">{{ statistics.claimedFoundItems || 0 }}</span>
            </div>
            <div class="status-bar">
              <div class="status-bar__label">已完成</div>
              <el-progress :percentage="calcPercent(statistics.completedFoundItems, statistics.totalFoundItems)" color="#409EFF" />
              <span class="status-bar__count">{{ statistics.completedFoundItems || 0 }}</span>
            </div>
            <div class="status-bar">
              <div class="status-bar__label">已过期</div>
              <el-progress :percentage="calcPercent(statistics.expiredFoundItems, statistics.totalFoundItems)" color="#F56C6C" />
              <span class="status-bar__count">{{ statistics.expiredFoundItems || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { UserFilled, Box, Clock, Document, Grid, WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 角色标签
const roleLabel = computed(() => {
  const roleMap = {
    'ADMIN': '系统管理员',
    'USER': '普通用户'
  }
  return roleMap[userInfo.value?.roleCode] || '未知角色'
})

const avatarUrl = computed(() => {
  return userInfo.value?.avatar ? baseAPI + userInfo.value.avatar : '';
})

// 当前时间
const currentTime = ref('')
let timeInterval = null

const updateTime = () => {
  const now = new Date()
  const options = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  }
  currentTime.value = now.toLocaleDateString('zh-CN', options)
}

// 统计数据
const statsLoading = ref(false)
const statistics = ref({})

const fetchStatistics = async () => {
  statsLoading.value = true
  try {
    await request.get('/dashboard/statistics', null, {
      showDefaultMsg: false,
      onSuccess: (data) => {
        statistics.value = data || {}
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    statsLoading.value = false
  }
}

// 计算百分比
const calcPercent = (value, total) => {
  if (!total || total === 0) return 0
  return Math.round((value / total) * 100)
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 60000)
  fetchStatistics()
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
    timeInterval = null
  }
})
</script>

<style lang="scss" scoped>
.dashboard {
  .welcome-card {
    margin-bottom: 20px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .welcome-header {
      display: flex;
      align-items: center;
      gap: 20px;

      .el-avatar {
        transition: transform 0.3s ease;

        &:hover {
          transform: scale(1.1);
        }
      }

      .welcome-info {
        h2 {
          margin: 0 0 8px 0;
          font-size: 24px;
          background: linear-gradient(to right, #409eff, #67c23a);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
        p {
          margin: 0;
          color: #666;
        }
      }
    }

    .role-info {
      margin-top: 16px;
    }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }

  .stat-card {
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    }

    &__inner {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    &__icon {
      width: 64px;
      height: 64px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    &__content {
      flex: 1;
      min-width: 0;
    }

    &__number {
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
    }

    &__label {
      font-size: 14px;
      color: #666;
      margin-top: 2px;
    }

    &__sub {
      font-size: 12px;
      color: #999;
      margin-top: 2px;
    }

    // 不同卡片配色
    &--users .stat-card__icon {
      background: #e8f4fd;
      color: #409EFF;
    }
    &--users .stat-card__number {
      color: #409EFF;
    }

    &--items .stat-card__icon {
      background: #e8f8e8;
      color: #67C23A;
    }
    &--items .stat-card__number {
      color: #67C23A;
    }

    &--pending .stat-card__icon {
      background: #fdf6ec;
      color: #E6A23C;
    }
    &--pending .stat-card__number {
      color: #E6A23C;
    }

    &--claims .stat-card__icon {
      background: #f4e8fd;
      color: #9B59B6;
    }
    &--claims .stat-card__number {
      color: #9B59B6;
    }

    &--categories .stat-card__icon {
      background: #e8faf0;
      color: #1ABC9C;
    }
    &--categories .stat-card__number {
      color: #1ABC9C;
    }

    &--expired .stat-card__icon {
      background: #fef0f0;
      color: #F56C6C;
    }
    &--expired .stat-card__number {
      color: #F56C6C;
    }
  }

  .section-card {
    height: 100%;

    .section-title {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .status-distribution {
    .status-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }

      &__label {
        width: 60px;
        font-size: 14px;
        color: #666;
        flex-shrink: 0;
      }

      .el-progress {
        flex: 1;
      }

      &__count {
        width: 40px;
        text-align: right;
        font-size: 14px;
        font-weight: 600;
        color: #333;
        flex-shrink: 0;
      }
    }
  }
}

@media (max-width: 768px) {
  .dashboard {
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
    }
  }
}

@media (max-width: 480px) {
  .dashboard {
    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
