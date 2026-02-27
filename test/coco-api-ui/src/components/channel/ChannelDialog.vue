<template>
  <el-dialog
    :model-value="dialogVisible"
    :title="isEdit ? '编辑频道' : '新建频道'"
    width="600px"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    @close="handleCancel"
    class="fluent-scale-in"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      label-position="left"
      :disabled="loading"
    >
      <el-form-item label="频道ID" prop="channelId">
        <el-input
          v-model.number="formData.channelId"
          placeholder="请输入 Telegram 频道 ID"
          :disabled="isEdit"
          type="number"
        />
        <el-text type="info" size="small" style="margin-top: 4px">
          Telegram 频道的唯一标识符（数字）
        </el-text>
      </el-form-item>

      <el-form-item label="频道用户名" prop="channelUsername">
        <el-input
          v-model="formData.channelUsername"
          placeholder="请输入频道用户名（可选）"
          maxlength="100"
          show-word-limit
        >
          <template #prepend>@</template>
        </el-input>
        <el-text type="info" size="small" style="margin-top: 4px">
          频道的公开用户名，不含 @ 符号
        </el-text>
      </el-form-item>

      <el-form-item label="频道标题" prop="channelTitle">
        <el-input
          v-model="formData.channelTitle"
          placeholder="请输入频道标题"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="监控状态" prop="monitoringStatus">
        <el-switch
          v-model="formData.monitoringStatus"
          active-text="启用"
          inactive-text="停止"
        />
        <el-text type="info" size="small" style="margin-top: 4px; display: block">
          启用后将监控该频道的消息
        </el-text>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel" :disabled="loading">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleConfirm"
          :loading="loading"
          :disabled="loading"
        >
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts">
export default {
  name: 'ChannelDialog'
}
</script>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useChannelStore } from '@/stores/channel'
import type { Channel, ChannelCreateDTO, ChannelUpdateDTO } from '@/types/models'
import { handleConflictError, showSuccessMessage } from '@/utils/errorHandler'
import { ApiError } from '@/utils/request'

interface Props {
  visible: boolean
  channel: Channel | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const channelStore = useChannelStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const isEdit = computed(() => !!props.channel)

// Form data
const formData = ref<ChannelCreateDTO & { id?: string }>({
  channelId: 0,
  channelUsername: '',
  channelTitle: '',
  monitoringStatus: true
})

// Form validation rules
const rules: FormRules = {
  channelId: [
    { required: true, message: '请输入频道ID', trigger: 'blur' },
    { type: 'number', message: '频道ID必须是数字', trigger: 'blur' }
  ],
  channelUsername: [
    { max: 100, message: '频道用户名长度不能超过100个字符', trigger: 'blur' }
  ],
  channelTitle: [
    { required: true, message: '请输入频道标题', trigger: 'blur' },
    { min: 1, max: 200, message: '频道标题长度必须在1-200个字符之间', trigger: 'blur' }
  ]
}

// Reset form
const resetForm = () => {
  formData.value = {
    channelId: 0,
    channelUsername: '',
    channelTitle: '',
    monitoringStatus: true
  }
  formRef.value?.clearValidate()
}

// Watch for channel prop changes
watch(() => props.channel, (newChannel) => {
  if (newChannel) {
    formData.value = {
      id: newChannel.id,
      channelId: newChannel.channelId,
      channelUsername: newChannel.channelUsername || '',
      channelTitle: newChannel.channelTitle,
      monitoringStatus: newChannel.monitoringStatus
    }
  } else {
    resetForm()
  }
}, { immediate: true })

// Handle confirm
const handleConfirm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isEdit.value && formData.value.id) {
      // Update
      const updateData: ChannelUpdateDTO = {
        channelUsername: formData.value.channelUsername || null,
        channelTitle: formData.value.channelTitle,
        monitoringStatus: formData.value.monitoringStatus
      }
      await channelStore.updateChannel(formData.value.id, updateData)
      showSuccessMessage('更新成功')
    } else {
      // Create
      const createData: ChannelCreateDTO = {
        channelId: formData.value.channelId,
        channelUsername: formData.value.channelUsername || null,
        channelTitle: formData.value.channelTitle,
        monitoringStatus: formData.value.monitoringStatus
      }
      await channelStore.createChannel(createData)
      showSuccessMessage('创建成功')
    }

    dialogVisible.value = false
    emit('success')
  } catch (error: any) {
    if (error instanceof ApiError && error.code === -60003) {
      handleConflictError(error)
    }
  } finally {
    loading.value = false
  }
}

// Handle cancel
const handleCancel = () => {
  dialogVisible.value = false
}
</script>

<style scoped>
:deep(.el-form-item__content) {
  flex-direction: column;
  align-items: flex-start;
}

:deep(.el-input-group__prepend) {
  padding: 0 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .dialog-footer {
    flex-direction: column-reverse;
  }

  .dialog-footer .el-button {
    width: 100%;
  }
}
</style>
