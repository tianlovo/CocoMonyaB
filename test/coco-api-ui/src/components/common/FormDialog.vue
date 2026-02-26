<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    :width="dialogWidth"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    @close="handleClose"
    class="fluent-scale-in"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      :disabled="loading"
      @submit.prevent="handleSubmit"
    >
      <slot :form-data="formData" :loading="loading" />
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel" :disabled="loading">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
          :disabled="loading"
        >
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

interface Props {
  visible: boolean
  title: string
  formData: Record<string, any>
  rules?: FormRules
  loading?: boolean
  width?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  width: '600px',
  rules: () => ({})
})

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'submit'): void
  (e: 'cancel'): void
}

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()

const dialogWidth = computed(() => {
  if (typeof props.width === 'number') {
    return `${props.width}px`
  }
  return props.width
})

const handleClose = () => {
  if (!props.loading) {
    emit('update:visible', false)
    emit('cancel')
  }
}

const handleCancel = () => {
  emit('update:visible', false)
  emit('cancel')
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    emit('submit')
  } catch (error) {
    // Validation failed, errors will be shown by Element Plus
    console.log('Form validation failed:', error)
  }
}

// Expose validate method for parent components
defineExpose({
  validate: () => formRef.value?.validate(),
  resetFields: () => formRef.value?.resetFields(),
  clearValidate: () => formRef.value?.clearValidate()
})
</script>

<style scoped>
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

  .dialog-footer {
    flex-direction: column-reverse;
  }

  .dialog-footer .el-button {
    width: 100%;
  }
}

@media (min-width: 768px) and (max-width: 1199px) {
  :deep(.el-dialog) {
    width: 80% !important;
  }
}
</style>
