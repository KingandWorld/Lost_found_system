<template>
  <div class="settings-page">
    <el-card class="settings-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>

      <el-form label-width="160px" class="settings-form">
        <el-form-item label="物品过期天数">
          <el-input-number
            v-model="expireDays"
            :min="1"
            :max="365"
            :step="1"
          />
          <span class="form-tip">超过此天数的待认领物品将被自动标记为"已过期"，置顶物品不受影响</span>
        </el-form-item>

        <el-form-item label="启用验证码">
          <el-switch v-model="captchaEnabled" @change="saveCaptchaSetting" :loading="savingCaptcha" />
          <span class="form-tip">开启后，登录和注册需要输入数学验证码</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveSettings" :loading="saving">
            保存设置
          </el-button>
          <el-button @click="fetchSettings">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>会员积分设置</span>
        </div>
      </template>

      <el-form label-width="200px" class="settings-form">
        <el-form-item label="发布失物获得积分">
          <el-input-number v-model="membershipConfigs['points.publish.lost']" :min="0" :max="100" />
          <span class="form-tip">发布一条失物获得的积分（每日上限见下方）</span>
        </el-form-item>

        <el-form-item label="发布招领获得积分">
          <el-input-number v-model="membershipConfigs['points.publish.found']" :min="0" :max="100" />
          <span class="form-tip">发布一条招领获得的积分</span>
        </el-form-item>

        <el-form-item label="每日发布积分上限">
          <el-input-number v-model="membershipConfigs['points.publish.daily.max']" :min="0" :max="1000" />
          <span class="form-tip">每天通过发布能获得的最大积分（防刷分）</span>
        </el-form-item>

        <el-form-item label="交接完成积分（发布者）">
          <el-input-number v-model="membershipConfigs['points.item.completed']" :min="0" :max="500" />
          <span class="form-tip">物品成功交接后，发布者获得的积分</span>
        </el-form-item>

        <el-form-item label="认领成功积分（认领人）">
          <el-input-number v-model="membershipConfigs['points.claim.success']" :min="0" :max="500" />
          <span class="form-tip">成功认领物品后，认领人获得的积分</span>
        </el-form-item>

        <el-form-item label="兑换会员所需积分">
          <el-input-number v-model="membershipConfigs['points.exchange.cost']" :min="1" :max="99999" />
          <span class="form-tip">用户兑换会员需要消耗的积分</span>
        </el-form-item>

        <el-form-item label="兑换会员天数">
          <el-input-number v-model="membershipConfigs['points.exchange.days']" :min="1" :max="3650" />
          <span class="form-tip">每次兑换获得多少天会员</span>
        </el-form-item>

        <el-form-item label="会员物品过期天数">
          <el-input-number v-model="membershipConfigs['member.expire.days']" :min="1" :max="365" />
          <span class="form-tip">会员发布的物品过期天数（应大于普通过期天数）</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveMembershipSettings" :loading="savingMembership">
            保存会员设置
          </el-button>
          <el-button @click="fetchSettings">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>功能说明</span>
        </div>
      </template>
      <div class="info-content">
        <el-alert
          title="物品过期机制"
          type="info"
          :closable="false"
          show-icon
        >
          <p>系统每天凌晨2点自动检查：如果物品状态为"待认领"且发布时间超过配置天数，则自动标记为"已过期"。</p>
          <p>置顶物品永远不会被自动过期。管理员可在失物/招领管理页面将重要物品设为置顶。</p>
          <p>会员物品使用独立的过期天数，通常比普通物品更长。</p>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const expireDays = ref(30)
const saving = ref(false)
const savingMembership = ref(false)
const captchaEnabled = ref(false)
const savingCaptcha = ref(false)

const membershipConfigs = reactive({
  'points.publish.lost': 2,
  'points.publish.found': 2,
  'points.publish.daily.max': 10,
  'points.item.completed': 20,
  'points.claim.success': 15,
  'points.exchange.cost': 100,
  'points.exchange.days': 30,
  'member.expire.days': 60,
})

const fetchSettings = async () => {
  try {
    // 并行获取所有配置
    const [expireRes, membershipRes] = await Promise.all([
      request.get('/system-config/expire-days', {}, { showDefaultMsg: false }).catch(() => 30),
      request.get('/system-config/membership-configs', {}, { showDefaultMsg: false }).catch(() => ({})),
    ])
    expireDays.value = expireRes || 30
    // 验证码开关
    captchaEnabled.value = membershipRes?.['captcha.enabled'] === 'true'
    if (membershipRes) {
      Object.keys(membershipConfigs).forEach(key => {
        if (membershipRes[key] !== undefined) {
          membershipConfigs[key] = parseInt(membershipRes[key]) || 0
        }
      })
    }
  } catch (error) {
    console.error('获取系统设置失败:', error)
  }
}

const saveSettings = async () => {
  saving.value = true
  try {
    await request.put('/system-config/expire-days', { value: expireDays.value }, {
      successMsg: '保存成功',
      onSuccess: () => {
        saving.value = false
      }
    })
  } catch (error) {
    console.error('保存系统设置失败:', error)
    saving.value = false
  }
}

const saveMembershipSettings = async () => {
  savingMembership.value = true
  try {
    const configs = {}
    Object.keys(membershipConfigs).forEach(key => {
      configs[key] = String(membershipConfigs[key])
    })
    await request.put('/system-config/membership-configs', configs, {
      successMsg: '会员设置已保存',
      onSuccess: () => {
        savingMembership.value = false
      }
    })
  } catch (error) {
    console.error('保存会员设置失败:', error)
    savingMembership.value = false
  }
}

const saveCaptchaSetting = async () => {
  savingCaptcha.value = true
  try {
    await request.put('/system-config/membership-configs', {
      'captcha.enabled': captchaEnabled.value ? 'true' : 'false'
    }, {
      successMsg: captchaEnabled.value ? '验证码已启用' : '验证码已关闭',
      onSuccess: () => { savingCaptcha.value = false }
    })
  } catch (error) {
    console.error('保存验证码设置失败:', error)
    savingCaptcha.value = false
  }
}

onMounted(() => {
  fetchSettings()
})
</script>

<style lang="scss" scoped>
.settings-page {
  .settings-card {
    margin-bottom: 20px;
  }

  .settings-form {
    max-width: 600px;

    .form-tip {
      margin-left: 12px;
      color: #909399;
      font-size: 13px;
    }
  }

  .info-card {
    .info-content {
      .el-alert {
        p {
          margin: 4px 0;
          line-height: 1.6;
        }
      }
    }
  }
}
</style>
