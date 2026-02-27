<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    :width="dialogWidth"
    :close-on-click-modal="false"
    @close="handleClose"
    class="fluent-scale-in"
  >
    <div class="confirm-content">
      <div class="confirm-icon">
        <el-icon :size="48" :color="iconColor">
          <component :is="iconComponent" />
        </el-icon>
      </div>
      <div class="confirm-message" v-html="message"></div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">
          {{ cancelText }}
        </el-button>
        <el-button
          :type="confirmButtonType"
          @click="handleConfirm"
        >
          {{ confirmText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { InfoFilled, WarningFilled, CircleCloseFilled } from '@element-plus/icons-vue'

interface Props {
  visible: boolean
  title: string
  message: string
  type?: 'info' | 'warning' | 'danger'
  confirmText?: string
  cancelText?: string
  width?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'warning',
  confirmText: '确定',
  cancelText: '取消',
  width: '500px'
})

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}

const emit = defineEmits<Emits>()

const dialogWidth = computed(() => {
  if (typeof props.width === 'number') {
    return `${props.width}px`
  }
  return props.width
})

const iconComponent = computed(() => {
  switch (props.type) {
    case 'danger':
      return CircleCloseFilled
    case 'warning':
      return WarningFilled
    case 'info':
    default:
      return InfoFilled
  }
})

const iconColor = computed(() => {
  switch (props.type) {
    case 'danger':
      return 'var(--fluent-danger)'
    case 'warning':
      return 'var(--fluent-warning)'
    case 'info':
    default:
      return 'var(--fluent-primary)'
  }
})

const confirmButtonType = computed(() => {
  switch (props.type) {
    case 'danger':
      return 'danger'
    case 'warning':
      return 'warning'
    case 'info':
    default:
      return 'primary'
  }
})

const handleClose = () => {
  emit('update:visible', false)
  emit('cancel')
}

const handleCancel = () => {
  emit('update:visible', false)
  emit('cancel')
}

const handleConfirm = () => {
  emit('update:visible', false)
  emit('confirm')
}
</script>

<style scoped>
.confirm-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg) 0;
}

.confirm-icon {
  margin-bottom: var(--spacing-lg);
  opacity: 0.9;
}

.confirm-message {
  font-size: var(--font-size-md);
  color: var(--fluent-text-primary);
  text-align: center;
  line-height: 1.6;
  white-space: pre-line;
  max-width: 100%;
  word-wrap: break-word;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

/* Responsive adjustments */
@media (max-width: 767px) {
  :deep(.el-dialog) {
    width: 90% !important;
    margin: var(--spacing-md);
  }

  .confirm-content {
    padding: var(--spacing-md) 0;
  }

  .confirm-icon :deep(.el-icon) {
    font-size: 36px !important;
  }

  .confirm-message {
    font-size: var(--font-size-sm);
  }

  .dialog-footer {
    flex-direction: column-reverse;
  }

  .dialog-footer .el-button {
    width: 100%;
  }
}

@media (min-width: 768px) and (max-width: 1199px) {
  :deep(.el-dialog) {
    width: 70% !important;
  }
}
</style>
