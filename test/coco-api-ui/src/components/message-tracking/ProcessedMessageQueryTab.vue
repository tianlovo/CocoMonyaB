<template>
  <div class="processed-message-query-tab">
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
        v-model="filters.isRead"
        placeholder="是否已读"
        clearable
        style="width: 150px"
      >
        <el-option label="已读" :value="true" />
        <el-option label="未读" :value="false" />
      </el-select>
      <el-select
        v-model="filters.isMatched"
        placeholder="是否匹配"
        clearable
        style="width: 150px"
      >
        <el-option label="已匹配" :value="true" />
        <el-option label="未匹配" :value="false" />
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
      <template #messageType="{ row }">
        <el-tag size="small" type="info">{{ formatMessageType(row.messageType) }}</el-tag>
      </template>

      <template #isRead="{ row }">
        <el-tag :type="row.isRead ? 'success' : 'info'" size="small">
          {{ formatBoolean(row.isRead) }}
        </el-tag>
      </template>

      <template #isMatched="{ row }">
        <el-tag :type="row.isMatched ? 'success' : 'info'" size="small">
          {{ formatBoolean(row.isMatched) }}
        </el-tag>
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

      <template #processTime="{ row }">
        {{ formatDateTime(row.processTime) }}
      </template>

      <template #readTime="{ row }">
        {{ row.readTime ? formatDateTime(row.readTime) : '-' }}
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
      title="已处理消息详情"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-if="currentMessage" class="message-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentMessage.id }}</el-descriptions-item>
          <el-descriptions-item label="频道ID">{{ currentMessage.chatId }}</el-descriptions-item>
          <el-descriptions-item label="消息ID">{{ currentMessage.messageId }}</el-descriptions-item>
          <el-descriptions-item label="消息类型">
            <el-tag size="small" type="info">{{ formatMessageType(currentMessage.messageType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否已读">
            <el-tag :type="currentMessage.isRead ? 'success' : 'info'" size="small">
              {{ formatBoolean(currentMessage.isRead) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否匹配">
            <el-tag :type="currentMessage.isMatched ? 'success' : 'info'" size="small">
              {{ formatBoolean(currentMessage.isMatched) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ formatDateTime(currentMessage.processTime) }}</el-descriptions-item>
          <el-descriptions-item label="标记已读时间">{{ currentMessage.readTime ? formatDateTime(currentMessage.readTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentMessage.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(currentMessage.updateTime) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentMessage.matchedTags && currentMessage.matchedTags.length > 0" class="detail-section">
          <h4>匹配的标签 ({{ currentMessage.matchedTags.length }})</h4>
          <div class="tags-list">
            <el-tag
              v-for="(tag, index) in currentMessage.matchedTags"
              :key="index"
              size="small"
              style="margin: 4px"
            >
              {{ tag }}
            </el-tag>
          </div>
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
import type { ProcessedMessage } from '@/types/models'
import { formatBoolean, formatDateTime } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: undefined as number | undefined,
  isRead: undefined as boolean | undefined,
  isMatched: undefined as boolean | undefined
})

// Data state
const data = ref<ProcessedMessage[]>([])
const loading = ref(false)

// Detail Dialog state
const detailDialogVisible = ref(false)
const currentMessage = ref<ProcessedMessage | null>(null)

// Computed
const hasFilters = computed(() => {
  return !!(filters.chatId || filters.isRead !== undefined || filters.isMatched !== undefined)
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
    prop: 'messageType',
    label: '消息类型',
    width: 120,
    slot: 'messageType',
    hideOnMobile: true
  },
  {
    prop: 'isRead',
    label: '是否已读',
    width: 100,
    slot: 'isRead'
  },
  {
    prop: 'isMatched',
    label: '是否匹配',
    width: 100,
    slot: 'isMatched'
  },
  {
    prop: 'matchedTags',
    label: '匹配标签',
    minWidth: 200,
    slot: 'matchedTags'
  },
  {
    prop: 'processTime',
    label: '处理时间',
    width: 180,
    slot: 'processTime'
  },
  {
    prop: 'readTime',
    label: '已读时间',
    width: 180,
    slot: 'readTime',
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

// Format message type
const formatMessageType = (type: string): string => {
  const typeMap: Record<string, string> = {
    text: '文本',
    photo: '图片',
    video: '视频',
    audio: '音频',
    document: '文档',
    animation: '动画',
    voice: '语音',
    sticker: '贴纸',
    poll: '投票',
    location: '位置',
    contact: '联系人',
    venue: '地点',
    game: '游戏',
    invoice: '发票',
    web_page: '网页'
  }
  return typeMap[type] || type
}

// Show detail dialog
const showDetailDialog = (message: ProcessedMessage) => {
  currentMessage.value = message
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
    if (filters.isRead !== undefined) {
      params.isRead = filters.isRead
    }
    if (filters.isMatched !== undefined) {
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
  filters.chatId = undefined
  filters.isRead = undefined
  filters.isMatched = undefined
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

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.message-detail {
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

.tags-list {
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
