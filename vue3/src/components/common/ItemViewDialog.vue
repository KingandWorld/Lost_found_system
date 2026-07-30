<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="700px"
    center
    @close="handleClose"
  >
    <div class="view-dialog-content" v-loading="loading">
      <div class="view-item" v-for="field in fields" :key="field.key" :class="field.className">
        <div class="label">{{ field.label }}：</div>
        <div class="value" v-if="field.type === 'status'">
          <StatusTag :status="item[field.key]" />
        </div>
        <div class="value" v-else-if="field.type === 'datetime'">
          {{ formatDateTime(item[field.key]) }}
        </div>
        <div class="value" v-else-if="field.type === 'images' && item[field.key]">
          <div class="image-list">
            <el-image
              v-for="(img, index) in getImageArray(item[field.key])"
              :key="index"
              :src="getImageUrl(img)"
              :preview-src-list="getImageUrlList(item[field.key])"
              fit="cover"
              class="item-image"
            />
          </div>
        </div>
        <div class="value" v-else>{{ item[field.key] }}</div>
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, watch } from 'vue'
import StatusTag from './StatusTag.vue'
import { getImageUrl, getImageList } from '@/utils/imageUtils'

const props = defineProps({
  /** 弹窗是否可见 */
  modelValue: {
    type: Boolean,
    default: false
  },
  /** 弹窗标题 */
  title: {
    type: String,
    default: '查看详情'
  },
  /** 物品数据 */
  item: {
    type: Object,
    default: () => ({})
  },
  /** 加载状态 */
  loading: {
    type: Boolean,
    default: false
  },
  /** 要显示的字段列表 */
  fields: {
    type: Array,
    default: () => []
  },
  /** 日期格式化函数 */
  formatDateTime: {
    type: Function,
    default: (date) => {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN')
    }
  }
})

const emit = defineEmits(['update:modelValue', 'close'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleClose = () => {
  emit('close')
}

const getImageArray = (images) => {
  if (!images) return []
  return images.split(',').filter(Boolean)
}

const getImageUrlList = (images) => {
  return getImageList(images)
}
</script>

<style lang="scss" scoped>
.view-dialog-content {
  .view-item {
    display: flex;
    margin-bottom: 15px;

    .label {
      width: 100px;
      font-weight: bold;
      flex-shrink: 0;
    }

    .value {
      flex: 1;
    }

    &.description {
      .value {
        white-space: pre-wrap;
      }
    }

    &.images {
      .image-list {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .item-image {
          width: 120px;
          height: 120px;
          border-radius: 4px;
        }
      }
    }
  }
}
</style>
