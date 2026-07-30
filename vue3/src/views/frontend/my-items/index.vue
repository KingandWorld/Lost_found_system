<template>
  <div class="my-items-page">
    <div class="page-header">
      <h1 class="page-title">我的发布</h1>
    </div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="失物信息" name="lost" />
      <el-tab-pane label="招领信息" name="found" />
    </el-tabs>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" @change="fetchItems">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">待认领</el-radio-button>
        <el-radio-button :value="1">已认领</el-radio-button>
        <el-radio-button :value="2">已完成</el-radio-button>
        <el-radio-button :value="3">已关闭</el-radio-button>
        <el-radio-button :value="4">已过期</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="!items.length" :description="emptyDescription" />

    <!-- 物品列表 -->
    <div v-else class="items-list">
      <el-row :gutter="20">
        <el-col v-for="item in items" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="item-card" shadow="hover" @click="viewDetail(item)">
            <div class="item-image">
              <el-image :src="getFirstImage(item.images)" fit="cover">
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="item-status-tag" :class="getStatusClass(item.status)">
                {{ getStatusText(item.status) }}
              </div>
              <div class="item-pin-badge" v-if="item.isPinned === 1">📌 置顶</div>
            </div>
            <div class="item-content">
              <h3 class="item-title">{{ item.title }}</h3>
              <p class="item-meta">
                <el-icon><Location /></el-icon>
                <span>{{ getLocationText(item) }}</span>
              </p>
              <p class="item-meta">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(item.createTime) }}</span>
              </p>
              <div class="item-footer">
                <span class="item-category">{{ item.categoryName || '未分类' }}</span>
                <el-button size="small" @click.stop="viewDetail(item)">查看详情</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { Picture, Location, Clock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('lost')
const statusFilter = ref(undefined)
const items = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

const baseAPI = import.meta.env.VITE_BASE_API || '/api'

const emptyDescription = computed(() => {
  const prefix = activeTab.value === 'lost' ? '失物' : '招领'
  const statusText = getStatusText(statusFilter.value)
  if (statusFilter.value !== undefined) {
    return `暂无"${statusText}"状态的${prefix}信息`
  }
  return `暂无${prefix}信息，快去发布吧！`
})

const fetchItems = async () => {
  if (!userStore.userInfo?.id) return

  loading.value = true
  try {
    const endpoint = activeTab.value === 'lost' ? '/lost-item/page' : '/found-item/page'
    const params = {
      userId: userStore.userInfo.id,
      currentPage: currentPage.value,
      size: pageSize.value
    }
    if (statusFilter.value !== undefined) {
      params.status = statusFilter.value
    }

    await request.get(endpoint, params, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        items.value = res.records || []
        total.value = res.total || 0
      }
    })
  } catch (error) {
    console.error('获取我的发布失败:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchItems()
}

const getFirstImage = (images) => {
  if (!images) return ''
  const imageArray = images.split(',').filter(img => img)
  if (imageArray.length === 0) return ''
  const firstImage = imageArray[0]
  return firstImage.startsWith('http') ? firstImage : baseAPI + firstImage
}

const getLocationText = (item) => {
  return item.lostPlace || item.foundPlace || '未知地点'
}

const formatTime = (timeStr) => {
  if (!timeStr) return '未知时间'
  const date = new Date(timeStr)
  return date.toLocaleDateString('zh-CN')
}

const getStatusText = (status) => {
  const textMap = {
    0: '待认领',
    1: '已认领',
    2: '已完成',
    3: '已关闭',
    4: '已过期'
  }
  return textMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    0: 'status-pending',
    1: 'status-claimed',
    2: 'status-completed',
    3: 'status-closed',
    4: 'status-expired'
  }
  return classMap[status] || ''
}

const viewDetail = (item) => {
  const prefix = activeTab.value === 'lost' ? '/lost' : '/found'
  router.push(`${prefix}/detail/${item.id}`)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  fetchItems()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchItems()
}

onMounted(() => {
  fetchItems()
})
</script>

<style lang="scss" scoped>
.my-items-page {
  .page-header {
    margin-bottom: 16px;

    .page-title {
      font-size: 24px;
      margin: 0;
    }
  }

  .filter-bar {
    margin-bottom: 20px;
    padding: 12px 0;
  }

  .loading-container {
    padding: 40px 0;
  }

  .items-list {
    .item-card {
      margin-bottom: 20px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
      }

      .item-image {
        position: relative;
        height: 180px;
        overflow: hidden;

        .el-image {
          width: 100%;
          height: 100%;
        }

        .image-placeholder {
          width: 100%;
          height: 100%;
          display: flex;
          justify-content: center;
          align-items: center;
          background-color: #f5f7fa;

          .el-icon {
            font-size: 40px;
            color: #c0c4cc;
          }
        }

        .item-status-tag {
          position: absolute;
          top: 8px;
          right: 8px;
          padding: 2px 8px;
          border-radius: 12px;
          font-size: 12px;
          color: #fff;

          &.status-pending { background-color: var(--status-pending); }
          &.status-claimed { background-color: var(--status-claimed); }
          &.status-completed { background-color: var(--status-completed); }
          &.status-closed { background-color: var(--status-closed); }
          &.status-expired { background-color: var(--status-expired); }
        }

        .item-pin-badge {
          position: absolute;
          top: 8px;
          left: 8px;
          background: linear-gradient(135deg, #f59e0b, #fbbf24);
          color: #fff;
          padding: 2px 8px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: 500;
        }
      }

      .item-content {
        padding: 8px 0;

        .item-title {
          margin: 0 0 8px 0;
          font-size: 16px;
          font-weight: 600;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .item-meta {
          margin: 4px 0;
          font-size: 13px;
          color: #666;
          display: flex;
          align-items: center;
          gap: 4px;

          span {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .item-footer {
          margin-top: 10px;
          display: flex;
          justify-content: space-between;
          align-items: center;

          .item-category {
            font-size: 12px;
            color: #999;
            background: #f5f7fa;
            padding: 2px 8px;
            border-radius: 4px;
          }
        }
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>
