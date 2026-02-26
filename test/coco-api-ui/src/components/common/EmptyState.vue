<template>
  <div class="empty-state fluent-fade-in">
    <div class="empty-state__icon">
      <el-icon :size="64" :color="iconColor">
        <component :is="iconComponent" />
      </el-icon>
    </div>
    <div class="empty-state__message">
      {{ displayMessage }}
    </div>
    <div v-if="actionText" class="empty-state__action">
      <el-button type="primary" @click="handleAction">
        {{ actionText }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Document, Search, WarningFilled } from '@element-plus/icons-vue'

interface Props {
  type?: 'empty' | 'no-result' | 'error'
  message?: string
  actionText?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'empty',
  message: '',
  actionText: ''
})

interface Emits {
  (e: 'action'): void
}

const emit = defineEmits<Emits>()

const iconComponent = computed(() => {
  switch (props.type) {
    case 'no-result':
      return Search
    case 'error':
      return WarningFilled
    case 'empty':
    default:
      return Document
  }
})

const iconColor = computed(() => {
  switch (props.type) {
    case 'error':
      return 'var(--fluent-danger)'
    case 'no-result':
      return 'var(--fluent-text-tertiary)'
    case 'empty':
    default:
      return 'var(--fluent-text-tertiary)'
  }
})

const displayMessage = computed(() => {
  if (props.message) {
    return props.message
  }
  
  switch (props.type) {
    case 'no-result':
      return '无搜索结果'
    case 'error':
      return '加载失败，请重试'
    case 'empty':
    default:
      return '暂无数据'
  }
})

const handleAction = () => {
  emit('action')
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xxl) var(--spacing-lg);
  min-height: 300px;
  color: var(--fluent-text-secondary);
}

.empty-state__icon {
  margin-bottom: var(--spacing-lg);
  opacity: 0.6;
}

.empty-state__message {
  font-size: var(--font-size-md);
  color: var(--fluent-text-secondary);
  margin-bottom: var(--spacing-lg);
  text-align: center;
  line-height: 1.6;
}

.empty-state__action {
  margin-top: var(--spacing-sm);
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .empty-state {
    min-height: 200px;
    padding: var(--spacing-lg) var(--spacing-md);
  }
  
  .empty-state__icon :deep(.el-icon) {
    font-size: 48px !important;
  }
  
  .empty-state__message {
    font-size: var(--font-size-sm);
  }
}
</style>
