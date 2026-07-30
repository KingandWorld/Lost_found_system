<template>
  <el-tag
    :type="tagType"
    :size="size"
    :effect="effect"
    :round="round"
  >
    {{ statusText }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { getStatusText, getStatusTagType } from '@/utils/statusUtils'

const props = defineProps({
  /** 状态值 (0-4) */
  status: {
    type: Number,
    required: true
  },
  /** 标签大小 */
  size: {
    type: String,
    default: 'default',
    validator: (v) => ['large', 'default', 'small'].includes(v)
  },
  /** 标签主题 */
  effect: {
    type: String,
    default: 'light',
    validator: (v) => ['dark', 'light', 'plain'].includes(v)
  },
  /** 是否圆角 */
  round: {
    type: Boolean,
    default: false
  }
})

const statusText = computed(() => getStatusText(props.status))
const tagType = computed(() => getStatusTagType(props.status))
</script>
