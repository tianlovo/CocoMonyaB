<template>
  <div class="processed-message-query-tab">
    <!-- Filter Form -->
    <div class="filter-form">
      <el-input
        v-model="filters.chatId"
        placeholder="聊天ID"
        clearable
        style="width: 200px"
      />
      <el-checkbox v-model="filters.isRead" label="已读" />
      <el-checkbox v-model="filters.isMatched" label="已匹配" />
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
      <template #isRead="{ row }">
        {{ formatBoolean(row.isRead) }}
      </template>

      <template #isMatched="{ row }">
        {{ formatBoolean(row.isMatched) }}
      </template>

      <template #processTime="{ row }">
        {{ formatDateTime(row.processTime) }}
      </template>
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { usePagination } from '@/composables/usePagination'
import { messageApi } from '@/api/message'
import type { ProcessedMessage } from '@/types/models'
import { formatBoolean, formatDateTime } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: '',
  isRead: false,
  isMatched: false
})

// Data state
const data = ref<ProcessedMessage[]>([])
const loading = ref(false)

// Computed
const hasFilters = computed(() => {
  return !!(filters.chatId || filters.isRead || filters.isMatched)
})

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'messageId',
    label: '消息ID',
    width: 200,
    hideOnMobile: true
  },
  {
    prop: 'chatId',
    label: '聊天ID',
    width: 150
  },
  {
    prop: 'isRead',
    label: '是否已读',
    width: 120,
    slot: 'isRead'
  },
  {
    prop: 'isMatched',
    label: '是否匹配',
    width: 120,
    slot: 'isMatched'
  },
  {
    prop: 'processTime',
    label: '处理时间',
    width: 180,
    slot: 'processTime',
    hideOnMobile: true
  }
]

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
    if (filters.isRead) {
      params.isRead = filters.isRead
    }
    if (filters.isMatched) {
      params.isMatched = filters.isMatched
    }

    const response = await messageApi.fetchProcessedMessages(params)
    data.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('Failed to load processed messages:', error)
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
  filters.chatId = ''
  filters.isRead = false
  filters.isMatched = false
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
.processed-message-query-tab {
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
