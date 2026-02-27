<template>
  <div class="forward-queue-query-tab">
    <!-- Filter Form -->
    <div class="filter-form">
      <el-input
        v-model="filters.sourceChatId"
        placeholder="源聊天ID"
        clearable
        style="width: 200px"
      />
      <el-select
        v-model="filters.status"
        placeholder="状态"
        clearable
        style="width: 150px"
      >
        <el-option label="待处理" value="PENDING" />
        <el-option label="转发成功" value="SUCCESS" />
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

      <template #createTime="{ row }">
        {{ formatDateTime(row.createTime) }}
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
import type { ForwardQueue } from '@/types/models'
import { formatDateTime, getStatusType, getStatusLabel } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  sourceChatId: '',
  status: ''
})

// Data state
const data = ref<ForwardQueue[]>([])
const loading = ref(false)

// Computed
const hasFilters = computed(() => {
  return !!(filters.sourceChatId || filters.status)
})

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'queueId',
    label: '队列ID',
    width: 200,
    hideOnMobile: true
  },
  {
    prop: 'sourceChatId',
    label: '源聊天ID',
    width: 150
  },
  {
    prop: 'targetChatId',
    label: '目标聊天ID',
    width: 150
  },
  {
    prop: 'status',
    label: '状态',
    width: 120,
    slot: 'status'
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    slot: 'createTime',
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
    if (filters.sourceChatId) {
      params.sourceChatId = filters.sourceChatId
    }
    if (filters.status) {
      params.status = filters.status
    }

    const response = await messageApi.fetchForwardQueue(params)
    data.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('Failed to load forward queue:', error)
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
  filters.sourceChatId = ''
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
.forward-queue-query-tab {
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
