<template>
  <div class="unread-buffer-query-tab">
    <!-- Filter Form -->
    <div class="filter-form">
      <el-input
        v-model.number="filters.chatId"
        placeholder="频道ID"
        clearable
        type="number"
        style="width: 200px"
      />
      <el-select
        v-model="filters.status"
        placeholder="状态"
        clearable
        style="width: 150px"
      >
        <el-option label="待处理" value="PENDING" />
        <el-option label="已处理" value="PROCESSED" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- Data Table -->
    <DataTable
      :data="data"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :empty-type="hasFilters ? 'no-result' : 'empty'"
      :empty-message="hasFilters ? '未找到匹配的记录' : '暂无数据'"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
    >
      <template #status="{ row }">
        <el-tag
          :type="getStatusType(row.status)"
          size="small"
        >
          {{ getStatusLabel(row.status) }}
        </el-tag>
      </template>

      <template #fetchTime="{ row }">
        {{ formatDateTime(row.fetchTime) }}
      </template>

      <template #updateTime="{ row }">
        {{ formatDateTime(row.updateTime) }}
      </template>

      <template #hasError="{ row }">
        <el-tag v-if="row.errorMessage" type="danger" size="small">
          有错误
        </el-tag>
        <span v-else>-</span>
      </template>

      <template #actions="{ row }">
        <el-button
          type="primary"
          link
          size="small"
          @click="showDetailDialog(row)"
        >
          详情
        </el-button>
      </template>
    </DataTable>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      title="未读消息缓冲区详情"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-if="currentBuffer" class="buffer-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentBuffer.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentBuffer.status)" size="small">
              {{ getStatusLabel(currentBuffer.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="频道ID">{{ currentBuffer.chatId }}</el-descriptions-item>
          <el-descriptions-item label="消息ID">{{ currentBuffer.messageId }}</el-descriptions-item>
          <el-descriptions-item label="获取时间">{{ formatDateTime(currentBuffer.fetchTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentBuffer.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">{{ formatDateTime(currentBuffer.updateTime) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentBuffer.errorMessage" class="detail-section">
          <h4>错误信息</h4>
          <el-alert
            :title="currentBuffer.errorMessage"
            type="error"
            :closable="false"
            show-icon
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { usePagination } from '@/composables/usePagination'
import { messageApi } from '@/api/message'
import type { UnreadBuffer } from '@/types/models'
import { formatDateTime, getStatusType, getStatusLabel } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: undefined as number | undefined,
  status: ''
})

// Data state
const data = ref<UnreadBuffer[]>([])
const loading = ref(false)

// Detail Dialog state
const detailDialogVisible = ref(false)
const currentBuffer = ref<UnreadBuffer | null>(null)

// Computed
const hasFilters = computed(() => {
  return !!(filters.chatId || filters.status)
})

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'id',
    label: 'ID',
    width: 200,
    hideOnMobile: true
  },
  {
    prop: 'chatId',
    label: '频道ID',
    width: 120
  },
  {
    prop: 'messageId',
    label: '消息ID',
    width: 120
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    slot: 'status'
  },
  {
    prop: 'fetchTime',
    label: '获取时间',
    width: 180,
    slot: 'fetchTime'
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 180,
    slot: 'updateTime',
    hideOnMobile: true
  },
  {
    prop: 'hasError',
    label: '错误',
    width: 100,
    slot: 'hasError',
    hideOnMobile: true
  },
  {
    prop: 'actions',
    label: '操作',
    width: 100,
    slot: 'actions',
    fixed: 'right'
  }
]

// Show detail dialog
const showDetailDialog = (buffer: UnreadBuffer) => {
  currentBuffer.value = buffer
  detailDialogVisible.value = true
}

// Load data function
const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      current: pagination.current,
      size: pagination.size
    }

    // Only include filter parameters if they have valid values
    if (filters.chatId) {
      params.chatId = filters.chatId
    }
    if (filters.status) {
      params.status = filters.status
    }

    const response = await messageApi.fetchUnreadBuffer(params)
    data.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('Failed to load unread buffer:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// Search with debounce
let searchTimer: ReturnType<typeof setTimeout> | null = null
const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = setTimeout(() => {
    pagination.current = 1
    loadData()
  }, 300)
}

// Reset filters
const handleReset = () => {
  filters.chatId = undefined
  filters.status = ''
  pagination.current = 1
  loadData()
}

// Expose loadData for parent component to call (for auto-refresh)
defineExpose({
  loadData
})

// Initialize
onMounted(() => {
  loadData()
})

// Watch pagination changes to reload data
watch(() => pagination.current, () => {
  loadData()
})

watch(() => pagination.size, () => {
  loadData()
})
</script>

<style scoped>
.unread-buffer-query-tab {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.filter-form {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
  flex-wrap: wrap;
}

.buffer-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.detail-section {
  margin-top: var(--spacing-md);
}

.detail-section h4 {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--fluent-text-primary);
  font-size: var(--font-size-md);
}

/* Remove number input spinner */
:deep(input[type="number"]::-webkit-inner-spin-button),
:deep(input[type="number"]::-webkit-outer-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}

:deep(input[type="number"]) {
  -moz-appearance: textfield;
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-form > * {
    width: 100% !important;
  }
}
</style>
