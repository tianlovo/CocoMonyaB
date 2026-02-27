<template>
  <div class="message-query-tab">
    <!-- Filter Form -->
    <div class="filter-form">
      <el-input
        v-model.number="filters.chatId"
        placeholder="频道ID"
        clearable
        type="number"
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
      <template #date="{ row }">
        {{ formatTimestamp(row.date) }}
      </template>

      <template #mediaAlbumId="{ row }">
        {{ row.mediaAlbumId || '-' }}
      </template>

      <template #rawJson="{ row }">
        <el-button
          type="primary"
          link
          size="small"
          @click="showJsonDialog(row.rawJson)"
        >
          查看
        </el-button>
      </template>

      <template #createTime="{ row }">
        {{ formatDateTime(row.createTime) }}
      </template>
    </DataTable>

    <!-- JSON Dialog -->
    <el-dialog
      v-model="jsonDialogVisible"
      title="消息原始数据"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="currentJson"
        type="textarea"
        :rows="20"
        readonly
        style="font-family: monospace"
      />
      <template #footer>
        <el-button @click="jsonDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyJson">复制</el-button>
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
import type { Message } from '@/types/models'
import { formatDateTime } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

// Data state
const data = ref<Message[]>([])
const loading = ref(false)

// JSON Dialog state
const jsonDialogVisible = ref(false)
const currentJson = ref('')

// Computed
const hasFilters = computed(() => {
  return !!(filters.chatId || filters.startDate || filters.endDate)
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
    width: 150
  },
  {
    prop: 'messageId',
    label: '消息ID',
    width: 150
  },
  {
    prop: 'mediaAlbumId',
    label: '媒体组ID',
    width: 150,
    slot: 'mediaAlbumId',
    hideOnMobile: true
  },
  {
    prop: 'date',
    label: '消息日期',
    width: 180,
    slot: 'date'
  },
  {
    prop: 'rawJson',
    label: '原始数据',
    width: 120,
    slot: 'rawJson'
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    slot: 'createTime',
    hideOnMobile: true
  }
]

// Format Unix timestamp to datetime string
const formatTimestamp = (timestamp: number | null | undefined): string => {
  if (!timestamp) return '-'
  try {
    const date = new Date(timestamp * 1000)
    return formatDateTime(date)
  } catch {
    return '-'
  }
}

// Show JSON dialog
const showJsonDialog = (json: string) => {
  try {
    // Pretty print JSON
    const parsed = JSON.parse(json)
    currentJson.value = JSON.stringify(parsed, null, 2)
  } catch {
    // If parsing fails, show raw string
    currentJson.value = json
  }
  jsonDialogVisible.value = true
}

// Copy JSON to clipboard
const copyJson = async () => {
  try {
    await navigator.clipboard.writeText(currentJson.value)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
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
  filters.chatId = undefined
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
