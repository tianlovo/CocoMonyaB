<template>
  <div class="message-query-tab">
    <!-- Filter Form -->
    <div class="filter-form">
      <el-input
        v-model="filters.chatId"
        placeholder="聊天ID"
        clearable
        style="width: 200px"
      />
      <el-date-picker
        v-model="filters.startDate"
        type="datetime"
        placeholder="开始时间"
        style="width: 200px"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
      <el-date-picker
        v-model="filters.endDate"
        type="datetime"
        placeholder="结束时间"
        style="width: 200px"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
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
      <template #content="{ row }">
        <el-tooltip
          v-if="row.content && row.content.length > 100"
          :content="row.content"
          placement="top"
        >
          <span>{{ truncateText(row.content, 100) }}</span>
        </el-tooltip>
        <span v-else>{{ row.content || '-' }}</span>
      </template>

      <template #timestamp="{ row }">
        {{ formatDateTime(row.timestamp) }}
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
import type { Message } from '@/types/models'
import { formatDateTime, truncateText } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: '',
  startDate: '',
  endDate: ''
})

// Data state
const data = ref<Message[]>([])
const loading = ref(false)

// Computed
const hasFilters = computed(() => {
  return !!(filters.chatId || filters.startDate || filters.endDate)
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
    prop: 'content',
    label: '内容',
    minWidth: 200,
    slot: 'content'
  },
  {
    prop: 'timestamp',
    label: '时间',
    width: 180,
    slot: 'timestamp'
  },
  {
    prop: 'sender',
    label: '发送者',
    width: 150,
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
    if (filters.startDate) {
      // Convert date string to timestamp (seconds)
      params.startDate = Math.floor(new Date(filters.startDate).getTime() / 1000)
    }
    if (filters.endDate) {
      // Convert date string to timestamp (seconds)
      params.endDate = Math.floor(new Date(filters.endDate).getTime() / 1000)
    }

    const response = await messageApi.fetchMessages(params)
    data.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('Failed to load messages:', error)
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
  filters.startDate = ''
  filters.endDate = ''
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
.message-query-tab {
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
