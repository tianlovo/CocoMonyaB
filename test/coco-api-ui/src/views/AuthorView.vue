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
          <el-button type="primary" :icon="Plus">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn, TableAction } from '@/components/common/DataTable.vue'
import { useAuthorStore } from '@/stores/author'
import { usePagination } from '@/composables/usePagination'
import type { Author } from '@/types/models'

const authorStore = useAuthorStore()
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

const searchKeyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

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

// Placeholder for edit action (will be implemented in task 8.5)
const handleEdit = (author: Author) => {
  ElMessage.info(`编辑功能将在后续任务中实现 (作者: ${author.name})`)
}

// Placeholder for delete action (will be implemented in task 8.7)
const handleDelete = (author: Author) => {
  ElMessageBox.confirm(
    `删除功能将在后续任务中实现。确定要删除作者 "${author.name}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.info('删除功能将在后续任务中实现')
  }).catch(() => {
    // User cancelled
  })
}

// Handle empty action (create new author)
const handleEmptyAction = () => {
  ElMessage.info('新建作者功能将在后续任务中实现')
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
}
</style>
