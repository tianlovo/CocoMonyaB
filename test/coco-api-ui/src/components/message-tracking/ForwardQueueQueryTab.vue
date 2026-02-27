<template>
  <div class="forward-queue-query-tab">
    <!-- Filter Form -->
    <div class="filter-form">
      <el-input
        v-model.number="filters.sourceChatId"
        placeholder="源频道ID"
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

      <template #mediaGroupInfo="{ row }">
        <div v-if="row.mediaGroupMessageIds && row.mediaGroupMessageIds.length > 0">
          <el-tag size="small" type="warning">
            媒体组 ({{ row.mediaGroupMessageIds.length }})
          </el-tag>
        </div>
        <span v-else>单条消息</span>
      </template>

      <template #matchedTags="{ row }">
        <div v-if="row.matchedTags && row.matchedTags.length > 0" class="tags-container">
          <el-tag
            v-for="(tag, index) in row.matchedTags.slice(0, 3)"
            :key="index"
            size="small"
            style="margin: 2px"
          >
            {{ tag }}
          </el-tag>
          <el-tag v-if="row.matchedTags.length > 3" size="small" type="info" style="margin: 2px">
            +{{ row.matchedTags.length - 3 }}
          </el-tag>
        </div>
        <span v-else>-</span>
      </template>

      <template #retryCount="{ row }">
        <el-tag v-if="row.retryCount > 0" size="small" :type="row.retryCount > 2 ? 'danger' : 'warning'">
          {{ row.retryCount }}
        </el-tag>
        <span v-else>0</span>
      </template>

      <template #createTime="{ row }">
        {{ formatDateTime(row.createTime) }}
      </template>

      <template #forwardTime="{ row }">
        {{ row.forwardTime ? formatDateTime(row.forwardTime) : '-' }}
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
      title="转发队列详情"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-if="currentQueue" class="queue-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentQueue.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentQueue.status)" size="small">
              {{ getStatusLabel(currentQueue.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="源频道ID">{{ currentQueue.sourceChatId }}</el-descriptions-item>
          <el-descriptions-item label="源消息ID">{{ currentQueue.sourceMessageId }}</el-descriptions-item>
          <el-descriptions-item label="重试次数">
            <el-tag :type="currentQueue.retryCount > 2 ? 'danger' : (currentQueue.retryCount > 0 ? 'warning' : 'info')" size="small">
              {{ currentQueue.retryCount }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentQueue.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(currentQueue.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="转发时间">{{ currentQueue.forwardTime ? formatDateTime(currentQueue.forwardTime) : '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentQueue.matchedTags && currentQueue.matchedTags.length > 0" class="detail-section">
          <h4>匹配的标签 ({{ currentQueue.matchedTags.length }})</h4>
          <div class="tags-list">
            <el-tag
              v-for="(tag, index) in currentQueue.matchedTags"
              :key="index"
              size="small"
              style="margin: 4px"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <div v-if="currentQueue.mediaGroupMessageIds && currentQueue.mediaGroupMessageIds.length > 0" class="detail-section">
          <h4>媒体组消息ID列表 ({{ currentQueue.mediaGroupMessageIds.length }})</h4>
          <div class="media-group-ids">
            <el-tag
              v-for="id in currentQueue.mediaGroupMessageIds"
              :key="id"
              size="small"
              type="info"
              style="margin: 4px"
            >
              {{ id }}
            </el-tag>
          </div>
        </div>

        <div v-if="currentQueue.errorMessage" class="detail-section">
          <h4>错误信息</h4>
          <el-alert
            :title="currentQueue.errorMessage"
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
import type { ForwardQueue } from '@/types/models'
import { formatDateTime, getStatusType, getStatusLabel } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  sourceChatId: undefined as number | undefined,
  status: ''
})

// Data state
const data = ref<ForwardQueue[]>([])
const loading = ref(false)

// Detail Dialog state
const detailDialogVisible = ref(false)
const currentQueue = ref<ForwardQueue | null>(null)

// Computed
const hasFilters = computed(() => {
  return !!(filters.sourceChatId || filters.status)
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
    prop: 'sourceChatId',
    label: '源频道ID',
    width: 120
  },
  {
    prop: 'sourceMessageId',
    label: '源消息ID',
    width: 120
  },
  {
    prop: 'mediaGroupInfo',
    label: '消息类型',
    width: 120,
    slot: 'mediaGroupInfo',
    hideOnMobile: true
  },
  {
    prop: 'matchedTags',
    label: '匹配标签',
    minWidth: 200,
    slot: 'matchedTags'
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    slot: 'status'
  },
  {
    prop: 'retryCount',
    label: '重试次数',
    width: 100,
    slot: 'retryCount',
    hideOnMobile: true
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    slot: 'createTime'
  },
  {
    prop: 'forwardTime',
    label: '转发时间',
    width: 180,
    slot: 'forwardTime',
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
const showDetailDialog = (queue: ForwardQueue) => {
  currentQueue.value = queue
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
  filters.sourceChatId = undefined
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

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.queue-detail {
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

.tags-list,
.media-group-ids {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
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
