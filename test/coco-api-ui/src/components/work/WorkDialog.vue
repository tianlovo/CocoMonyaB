<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑原作' : '新建原作'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="left"
    >
      <!-- 名称 -->
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入原作名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <!-- 别名列表 -->
      <el-form-item label="别名" prop="aliases">
        <div class="dynamic-list">
          <div
            v-for="(alias, index) in formData.aliases"
            :key="index"
            class="dynamic-list-item"
          >
            <el-input
              v-model="formData.aliases[index]"
              placeholder="请输入别名"
              maxlength="100"
              show-word-limit
            />
            <el-button
              type="danger"
              :icon="Delete"
              circle
              @click="removeAlias(index)"
            />
          </div>
          <el-button
            type="primary"
            :icon="Plus"
            plain
            @click="addAlias"
          >
            添加别名
          </el-button>
        </div>
      </el-form-item>

      <!-- 网址列表 -->
      <el-form-item label="网址" prop="urls">
        <div class="dynamic-list">
          <div
            v-for="(url, index) in formData.urls"
            :key="index"
            class="dynamic-list-item"
          >
            <el-input
              v-model="formData.urls[index]"
              placeholder="请输入网址"
              maxlength="500"
              show-word-limit
            />
            <el-button
              type="danger"
              :icon="Delete"
              circle
              @click="removeUrl(index)"
            />
          </div>
          <el-button
            type="primary"
            :icon="Plus"
            plain
            @click="addUrl"
          >
            添加网址
          </el-button>
        </div>
      </el-form-item>

      <!-- 头像 -->
      <el-form-item label="头像" prop="avatarBase64">
        <div class="avatar-upload">
          <div v-if="formData.avatarBase64" class="avatar-preview">
            <img :src="formData.avatarBase64" alt="头像预览" />
            <div class="avatar-actions">
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                @click="removeAvatar"
              >
                删除
              </el-button>
            </div>
          </div>
          <el-upload
            v-else
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleAvatarChange"
          >
            <el-button type="primary" :icon="Upload">
              上传头像
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 JPG、PNG 格式，建议尺寸 200x200
              </div>
            </template>
          </el-upload>
        </div>
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="4"
          placeholder="请输入备注"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, type FormInstance, type UploadFile } from 'element-plus'
import { Plus, Delete, Upload } from '@element-plus/icons-vue'
import { workApi } from '@/api/work'
import type { Work, WorkCreateDTO, WorkUpdateDTO } from '@/types/models'
import {
  workNameRules,
  aliasListRules,
  urlListRules,
  remarkRules
} from '@/utils/validators'
import { ApiError } from '@/utils/request'
import { handleConflictError, showSuccessMessage } from '@/utils/errorHandler'

interface Props {
  visible: boolean
  work?: Work | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  work: null
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const loading = ref(false)

// Computed property to determine if in edit mode
const isEdit = computed(() => !!props.work)

// Dialog visibility (two-way binding)
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// Form data with explicit types
interface FormData {
  id?: string
  name: string
  aliases: string[]
  urls: string[]
  avatarBase64: string | null
  remark: string | null
}

const formData = reactive<FormData>({
  name: '',
  aliases: [],
  urls: [],
  avatarBase64: null,
  remark: null
})

// Form validation rules
const rules = {
  name: workNameRules,
  aliases: aliasListRules,
  urls: urlListRules,
  remark: remarkRules
}

// Reset form
const resetForm = () => {
  formData.id = undefined
  formData.name = ''
  formData.aliases = []
  formData.urls = []
  formData.avatarBase64 = null
  formData.remark = null
  formRef.value?.clearValidate()
}

// Watch for work prop changes to populate form
watch(
  () => props.work,
  (work) => {
    if (work) {
      formData.id = work.id
      formData.name = work.name
      formData.aliases = [...work.aliases]
      formData.urls = [...work.urls]
      formData.avatarBase64 = work.avatarBase64
      formData.remark = work.remark
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

// Watch for dialog visibility to repopulate form when opening
watch(
  () => props.visible,
  (visible) => {
    if (visible && props.work) {
      formData.id = props.work.id
      formData.name = props.work.name
      formData.aliases = [...props.work.aliases]
      formData.urls = [...props.work.urls]
      formData.avatarBase64 = props.work.avatarBase64
      formData.remark = props.work.remark
    }
  }
)

// Add alias
const addAlias = () => {
  formData.aliases.push('')
}

// Remove alias
const removeAlias = (index: number) => {
  formData.aliases.splice(index, 1)
}

// Add URL
const addUrl = () => {
  formData.urls.push('')
}

// Remove URL
const removeUrl = (index: number) => {
  formData.urls.splice(index, 1)
}

// Handle avatar upload
const handleAvatarChange = (file: UploadFile) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    formData.avatarBase64 = e.target?.result as string
  }
  if (file.raw) {
    reader.readAsDataURL(file.raw)
  }
}

// Remove avatar
const removeAvatar = () => {
  formData.avatarBase64 = null
}

// Handle close
const handleClose = () => {
  dialogVisible.value = false
  resetForm()
}

// Handle submit
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    // Validate form
    await formRef.value.validate()

    loading.value = true

    // Filter out empty aliases and URLs
    const submitData: WorkCreateDTO | WorkUpdateDTO = {
      name: formData.name,
      aliases: formData.aliases.filter(alias => alias.trim() !== ''),
      urls: formData.urls.filter(url => url.trim() !== ''),
      avatarBase64: formData.avatarBase64 || null,
      remark: formData.remark || null
    }

    if (isEdit.value && formData.id) {
      // Update existing work
      await workApi.update(formData.id, submitData)
      showSuccessMessage('原作更新成功')
    } else {
      // Create new work
      await workApi.create(submitData as WorkCreateDTO)
      showSuccessMessage('原作创建成功')
    }

    emit('success')
    handleClose()
  } catch (error: any) {
    // Handle uniqueness conflict error
    if (error instanceof ApiError && error.code === -60003) {
      handleConflictError(error)
    } else {
      // Error message already shown by axios interceptor
      console.error('Submit failed:', error)
    }
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
.dynamic-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.dynamic-list-item {
  display: flex;
  gap: var(--spacing-sm);
  align-items: flex-start;
}

.dynamic-list-item .el-input {
  flex: 1;
}

.avatar-upload {
  width: 100%;
}

.avatar-preview {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  align-items: flex-start;
}

.avatar-preview img {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border-radius: var(--border-radius-md);
  border: 1px solid var(--fluent-border-color);
}

.avatar-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.el-upload__tip {
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--fluent-text-secondary);
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
    margin: 5vh auto;
  }
  
  .avatar-preview img {
    width: 150px;
    height: 150px;
  }
}
</style>
