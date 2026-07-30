<template>
  <div class="membership-management">
    <el-card class="data-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>会员管理</span>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%" v-loading="loading" border stripe>
        <el-table-column prop="userId" label="用户ID" width="100"></el-table-column>
        <el-table-column prop="points" label="积分" width="120" align="center">
          <template #default="scope">
            <span style="font-weight: bold; color: #e6a23c; font-size: 16px;">{{ scope.row.points }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="memberUntil" label="会员状态" width="200" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.isCurrentlyMember" type="warning" effect="dark">
              🌟 会员
            </el-tag>
            <el-tag v-else type="info">普通用户</el-tag>
            <div v-if="scope.row.memberUntil" style="font-size: 12px; margin-top: 4px; color: #909399;">
              至 {{ formatDate(scope.row.memberUntil) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalPointsEarned" label="累计获得" width="100" align="center"></el-table-column>
        <el-table-column prop="totalPointsSpent" label="累计消耗" width="100" align="center"></el-table-column>
        <el-table-column fixed="right" label="操作" width="200" align="center">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleManage(scope.row)">积分管理</el-button>
            <el-button link type="primary" size="small" @click="handleMembership(scope.row)">会员管理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 积分管理弹窗 -->
    <el-dialog v-model="pointsDialogVisible" title="积分管理" width="500px" append-to-body>
      <el-form :model="pointsForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input :model-value="currentRecord?.userId" disabled></el-input>
        </el-form-item>
        <el-form-item label="当前积分">
          <span style="font-size: 24px; font-weight: bold; color: #e6a23c;">{{ currentRecord?.points }}</span>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-radio-group v-model="pointsForm.operation">
            <el-radio value="grant">发放积分</el-radio>
            <el-radio value="revoke">扣除积分</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="积分数额">
          <el-input-number v-model="pointsForm.amount" :min="1" :max="99999"></el-input-number>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="pointsForm.reason" placeholder="请输入原因"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPointsOperation" :loading="submitLoading">确认</el-button>
      </template>
    </el-dialog>

    <!-- 会员管理弹窗 -->
    <el-dialog v-model="membershipDialogVisible" title="会员管理" width="500px" append-to-body>
      <el-form :model="membershipForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input :model-value="currentRecord?.userId" disabled></el-input>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag v-if="currentRecord?.isCurrentlyMember" type="warning">会员</el-tag>
          <el-tag v-else type="info">普通用户</el-tag>
        </el-form-item>
        <el-form-item v-if="currentRecord?.memberUntil" label="有效期至">
          {{ formatDate(currentRecord?.memberUntil) }}
        </el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="membershipForm.operation">
            <el-radio value="set">设置会员有效期</el-radio>
            <el-radio value="revoke">撤销会员资格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="membershipForm.operation === 'set'" label="有效期至">
          <el-date-picker
            v-model="membershipForm.memberUntil"
            type="datetime"
            placeholder="选择日期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          ></el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="membershipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMembershipOperation" :loading="submitLoading">确认</el-button>
      </template>
    </el-dialog>

    <!-- 积分历史弹窗 -->
    <el-dialog v-model="logsDialogVisible" title="积分历史" width="700px" append-to-body>
      <el-table :data="logsData" style="width: 100%" stripe size="small" max-height="400">
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="180"></el-table-column>
        <el-table-column prop="pointsChange" label="变动" width="100" align="center">
          <template #default="scope">
            <span :style="{ color: scope.row.pointsChange > 0 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
              {{ scope.row.pointsChange > 0 ? '+' : '' }}{{ scope.row.pointsChange }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="pointsAfter" label="余额" width="80" align="center"></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// 积分管理
const pointsDialogVisible = ref(false)
const currentRecord = ref(null)
const pointsForm = reactive({ operation: 'grant', amount: 10, reason: '' })

// 会员管理
const membershipDialogVisible = ref(false)
const membershipForm = reactive({ operation: 'set', memberUntil: '' })

// 积分历史
const logsDialogVisible = ref(false)
const logsData = ref([])

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/membership/admin/list', {
      currentPage: currentPage.value,
      size: pageSize.value,
    }, { showDefaultMsg: false })
    if (res) {
      // Mark currently member status
      tableData.value = (res.records || []).map(r => ({
        ...r,
        isCurrentlyMember: r.memberUntil && new Date(r.memberUntil) > new Date(),
      }))
      total.value = res.total || 0
    }
  } catch (error) {
    console.error('获取会员列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleManage = (row) => {
  currentRecord.value = row
  pointsForm.operation = 'grant'
  pointsForm.amount = 10
  pointsForm.reason = ''
  pointsDialogVisible.value = true
}

const handleMembership = (row) => {
  currentRecord.value = row
  membershipForm.operation = 'set'
  membershipForm.memberUntil = ''
  membershipDialogVisible.value = true
}

const submitPointsOperation = async () => {
  submitLoading.value = true
  try {
    const userId = currentRecord.value.userId
    const endpoint = pointsForm.operation === 'grant'
      ? `/membership/admin/${userId}/points/grant`
      : `/membership/admin/${userId}/points/revoke`

    await request.post(endpoint, {
      points: pointsForm.amount,
      reason: pointsForm.reason,
    }, {
      successMsg: '操作成功',
      onSuccess: () => {
        pointsDialogVisible.value = false
        fetchData()
      }
    })
  } catch (error) {
    console.error('积分操作失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const submitMembershipOperation = async () => {
  submitLoading.value = true
  try {
    const userId = currentRecord.value.userId
    if (membershipForm.operation === 'revoke') {
      await request.put(`/membership/admin/${userId}/membership/revoke`, {}, {
        successMsg: '会员资格已撤销',
        onSuccess: () => {
          membershipDialogVisible.value = false
          fetchData()
        }
      })
    } else {
      if (!membershipForm.memberUntil) {
        ElMessage.warning('请选择有效期')
        submitLoading.value = false
        return
      }
      await request.put(`/membership/admin/${userId}/membership/set`, {
        memberUntil: membershipForm.memberUntil,
      }, {
        successMsg: '会员有效期已设置',
        onSuccess: () => {
          membershipDialogVisible.value = false
          fetchData()
        }
      })
    }
  } catch (error) {
    console.error('会员操作失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  fetchData()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.membership-management {
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
