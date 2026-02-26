<template>
  <div class="author-view">
    <div class="view-header fluent-card">
      <div class="header-content">
        <h1 class="view-title">作者库管理</h1>
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索作者名称或别名"
            :prefix-icon="Search"
            clearable
            style="width: 300px"
            @input="handleSearch"
          />
          <el-button :icon="Download" @click="handleExport">
            导出
          </el-button>
          <el-button :icon="Upload" @click="handleImportClick">
            导入
          </el-button>
          <el-button type="primary" :icon="Plus" @click="handleCreate">
            新建作者
          </el-button>
        </div>
      </div>
    </div>

    <div class="view-content fluent-card">
      <DataTable
        :data="authorStore.authors"
        :columns="columns"
        :loading="authorStore.loading"
        :pagination="pagination"
        :actions="actions"
        :empty-type="searchKeyword ? 'no-result' : 'empty'"
        :empty-message="searchKeyword ? '未找到匹配的作者' : '暂无作者数据'"
        :empty-action-text="searchKeyword ? '' : '新建作者'"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
        @action="handleAction"
        @empty-action="handleEmptyAction"
      >
        <template #aliases="{ row }">
          <el-tag
            v-for="(alias, index) in row.aliases"
            :key="index"
            size="small"
            style="margin-right: 4px"
          >
            {{ alias }}
          </el-tag>
          <span v-if="row.aliases.length === 0" class="text-secondary">无</span>
        </template>

        <template #signature="{ row }">
          <span v-if="row.signature" class="text-ellipsis">{{ row.signature }}</span>
          <span v-else class="text-secondary">-</span>
        </template>

        <template #createTime="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </DataTable>
    </div>

    <!-- Author Dialog -->
    <AuthorDialog
      v-model:visible="dialogVisible"
      :author="currentAuthor"
      @success="handleDialogSuccess"
    />

    <!-- Hidden file input for import -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".json"
      style="display: none"
      @change="handleFileChange"
    />

    <!-- Import Result Dialog -->
    <el-dialog
      v-model="importDialogVisible"
      title="导入结果"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="import-result">
        <div class="result-summary">
          <el-result
            :icon="importResult.failureCount === 0 ? 'success' : 'warning'"
            :title="importResult.failureCount === 0 ? '导入成功' : '导入完成（部分失败）'"
          >
            <template #sub-title>
              <div class="result-stats">
                <div class="stat-item success">
                  <span class="stat-label">成功:</span>
                  <span class="stat-value">{{ importResult.successCount }}</span>
                </div>
                <div class="stat-item failure">
                  <span class="stat-label">失败:</span>
                  <span class="stat-value">{{ importResult.failureCount }}</span>
                </div>
              </div>
            </template>
          </el-result>
        </div>

        <div v-if="importResult.errors.length > 0" class="error-details">
          <h4>错误详情</h4>
          <el-scrollbar max-height="300px">
            <div
              v-for="(error, index) in importResult.errors"
              :key="index"
              class="error-item"
            >
              <div class="error-header">
                <el-tag type="danger" size="small">第 {{ error.index + 1 }} 条</el-tag>
                <span class="error-name">{{ error.name }}</span>
              </div>
              <div class="error-message">{{ error.error }}</div>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" @click="importDialogVisible = false">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
export default {
  name: 'AuthorView'
}
</script>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Plus, Download, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn, TableAction } from '@/components/common/DataTable.vue'
import AuthorDialog from '@/components/author/AuthorDialog.vue'
import { useAuthorStore } from '@/stores/author'
import { usePagination } from '@/composables/usePagination'
import type { Author } from '@/types/models'
import { ApiError } from '@/utils/request'
import {
  handleConflictError,
  handleReferenceError,
  showSuccessMessage,
  showErrorMessage,
  confirmDangerousOperation
} from '@/utils/errorHandler'

const authorStore = useAuthorStore()
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

const searchKeyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

// Dialog state
const dialogVisible = ref(false)
const currentAuthor = ref<Author | null>(null)

// Import/Export state
const fileInputRef = ref<HTMLInputElement | null>(null)
const importDialogVisible = ref(false)
const importResult = ref({
  successCount: 0,
  failureCount: 0,
  errors: [] as Array<{ index: number; name: string; error: string }>
})

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'name',
    label: '名称',
    minWidth: 150
  },
  {
    prop: 'aliases',
    label: '别名',
    minWidth: 200,
    slot: 'aliases'
  },
  {
    prop: 'signature',
    label: '个性签名',
    minWidth: 200,
    slot: 'signature'
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    slot: 'createTime'
  }
]

// Table actions configuration
const actions: TableAction[] = [
  {
    name: 'edit',
    label: '编辑',
    type: 'primary'
  },
  {
    name: 'delete',
    label: '删除',
    type: 'danger'
  }
]

// Load authors data
const loadAuthors = async () => {
  try {
    const response = await authorStore.fetchPage({
      current: pagination.current,
      size: pagination.size,
      keyword: searchKeyword.value || undefined
    })
    pagination.total = response.total
  } catch (error) {
    ElMessage.error('加载作者列表失败')
  }
}

// Search with debounce (300ms)
const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  
  searchTimer = setTimeout(() => {
    pagination.current = 1 // Reset to first page when searching
    loadAuthors()
  }, 300)
}

// Handle table actions
const handleAction = (actionName: string, row: Author) => {
  if (actionName === 'edit') {
    handleEdit(row)
  } else if (actionName === 'delete') {
    handleDelete(row)
  }
}

// Handle create
const handleCreate = () => {
  currentAuthor.value = null
  dialogVisible.value = true
}

// Handle edit
const handleEdit = (author: Author) => {
  currentAuthor.value = author
  dialogVisible.value = true
}

// Placeholder for delete action (will be implemented in task 8.7)
const handleDelete = async (author: Author) => {
  try {
    // First confirmation dialog
    const confirmed = await confirmDangerousOperation(
      `确定要删除作者 "${author.name}" 吗？此操作不可撤销。`,
      '删除确认'
    )
    
    if (!confirmed) return

    // Attempt to delete
    await authorStore.deleteAuthor(author.id, false)
    showSuccessMessage('删除成功')
    loadAuthors()
  } catch (error: any) {
    // Check if it's a reference error (code -60004)
    if (error instanceof ApiError && error.code === -60004) {
      await handleReferenceError(error, author.name, async () => {
        await authorStore.deleteAuthor(author.id, true)
      })
      loadAuthors()
    } else if (error instanceof ApiError && error.code === -60003) {
      // Handle uniqueness conflict (shouldn't happen in delete, but just in case)
      handleConflictError(error)
    } else if (error !== 'cancel') {
      // Other errors are already handled by the interceptor
      console.error('Delete failed:', error)
    }
  }
}

// Handle empty action (create new author)
const handleEmptyAction = () => {
  handleCreate()
}

// Handle dialog success (reload list)
const handleDialogSuccess = () => {
  loadAuthors()
}

// Handle export
const handleExport = async () => {
  try {
    const data = await authorStore.exportAuthors()
    
    // Parse if data is a string (backend returns JSON string)
    const jsonData = typeof data === 'string' ? JSON.parse(data) : data
    
    // Create JSON blob
    const blob = new Blob([JSON.stringify(jsonData, null, 2)], { type: 'application/json' })
    
    // Create download link
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `authors_export_${new Date().getTime()}.json`
    
    // Trigger download
    document.body.appendChild(link)
    link.click()
    
    // Cleanup
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    
    showSuccessMessage('导出成功')
  } catch (error) {
    showErrorMessage('导出失败')
    console.error('Export failed:', error)
  }
}

// Handle import click (trigger file input)
const handleImportClick = () => {
  fileInputRef.value?.click()
}

// Handle file change (file selected)
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  // Reset file input
  target.value = ''
  
  try {
    // Read file content
    const content = await readFileAsText(file)
    
    // Validate JSON format
    let data: any
    try {
      data = JSON.parse(content)
    } catch (parseError) {
      ElMessage.error('JSON 格式无效，请检查文件格式')
      return
    }
    
    // Validate data is an array
    if (!Array.isArray(data)) {
      ElMessage.error('JSON 文件格式错误：期望一个数组')
      return
    }
    
    // Validate each item has required fields
    for (let i = 0; i < data.length; i++) {
      const item = data[i]
      if (!item.name || typeof item.name !== 'string') {
        ElMessage.error(`JSON 文件格式错误：第 ${i + 1} 条记录缺少有效的 name 字段`)
        return
      }
      if (!item.aliases || !Array.isArray(item.aliases)) {
        ElMessage.error(`JSON 文件格式错误：第 ${i + 1} 条记录缺少有效的 aliases 字段`)
        return
      }
    }
    
    // Show loading message
    const loadingMessage = ElMessage({
      message: '正在导入，请稍候...',
      type: 'info',
      duration: 0
    })
    
    try {
      // Call import API
      console.log('Calling import API with data:', data)
      const result = await authorStore.importAuthors(data)
      console.log('Import API returned:', result)
      
      // Close loading message
      loadingMessage.close()
      
      // Validate result
      if (!result || typeof result.successCount === 'undefined') {
        console.error('Invalid import result:', result)
        ElMessage.error('导入失败：服务器返回数据格式错误')
        return
      }
      
      // Show result dialog
      importResult.value = result
      importDialogVisible.value = true
      
      // Reload list
      loadAuthors()
    } catch (error) {
      console.error('Import failed with error:', error)
      loadingMessage.close()
      
      // Show error message if not already shown by interceptor
      if (error instanceof Error) {
        ElMessage.error(`导入失败: ${error.message}`)
      } else {
        ElMessage.error('导入失败，请检查控制台日志')
      }
    }
  } catch (error) {
    ElMessage.error('读取文件失败')
    console.error('File read failed:', error)
  }
}

// Utility function to read file as text
const readFileAsText = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      resolve(e.target?.result as string)
    }
    reader.onerror = reject
    reader.readAsText(file)
  })
}

// Initialize
onMounted(() => {
  loadAuthors()
})

// Utility function to format date time
const formatDateTime = (dateTimeStr: string) => {
  if (!dateTimeStr) return '-'
  
  try {
    const date = new Date(dateTimeStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateTimeStr
  }
}
</script>

<style scoped>
.author-view {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.view-header {
  padding: var(--spacing-lg);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-md);
}

.view-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--fluent-text-primary);
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.view-content {
  padding: var(--spacing-lg);
  flex: 1;
}

.text-secondary {
  color: var(--fluent-text-secondary);
  font-style: italic;
}

.text-ellipsis {
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

/* Import result dialog styles */
.import-result {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.result-summary {
  text-align: center;
}

.result-stats {
  display: flex;
  justify-content: center;
  gap: var(--spacing-xl);
  margin-top: var(--spacing-md);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-lg);
}

.stat-item.success .stat-value {
  color: var(--el-color-success);
  font-weight: 600;
}

.stat-item.failure .stat-value {
  color: var(--el-color-danger);
  font-weight: 600;
}

.stat-label {
  color: var(--fluent-text-secondary);
}

.error-details {
  border-top: 1px solid var(--fluent-border-color);
  padding-top: var(--spacing-md);
}

.error-details h4 {
  margin: 0 0 var(--spacing-md) 0;
  font-size: var(--font-size-md);
  color: var(--fluent-text-primary);
}

.error-item {
  padding: var(--spacing-md);
  background: var(--fluent-bg-secondary);
  border-radius: var(--border-radius-md);
  margin-bottom: var(--spacing-sm);
}

.error-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xs);
}

.error-name {
  font-weight: 500;
  color: var(--fluent-text-primary);
}

.error-message {
  color: var(--fluent-text-secondary);
  font-size: var(--font-size-sm);
  padding-left: calc(var(--spacing-sm) + 60px);
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .author-view {
    padding: var(--spacing-md);
  }
  
  .view-header {
    padding: var(--spacing-md);
  }
  
  .header-content {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions {
    flex-direction: column;
  }
  
  .header-actions .el-input {
    width: 100% !important;
  }
  
  .view-content {
    padding: var(--spacing-md);
  }
  
  .result-stats {
    flex-direction: column;
    gap: var(--spacing-sm);
  }
}
</style>
