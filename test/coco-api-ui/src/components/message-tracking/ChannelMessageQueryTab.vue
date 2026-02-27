<template>
  <div class="channel-message-query-tab">
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
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
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
      <template #status="{ row }">
        <el-tag
          :type="getStatusType(row.status)"
          size="small"
        >
          {{ getStatusLabel(row.status) }}
        </el-tag>
      </template>

      <template #channelInfo="{ row }">
        <div class="channel-info">
          <div class="channel-title">{{ row.channelTitle || '-' }}</div>
          <div class="channel-username">@{{ row.channelUsername || '-' }}</div>
        </div>
      </template>

      <template #contentType="{ row }">
        <el-tag size="small" type="info">{{ formatContentType(row.contentType) }}</el-tag>
      </template>

      <template #textContent="{ row }">
        <el-tooltip
          v-if="row.textContent && row.textContent.length > 50"
          :content="row.textContent"
          placement="top"
        >
          <span>{{ truncateText(row.textContent, 50) }}</span>
        </el-tooltip>
        <span v-else>{{ row.textContent || '-' }}</span>
      </template>

      <template #mediaInfo="{ row }">
        <div v-if="row.isMediaGroup" class="media-info">
          <el-tag size="small" type="warning">媒体组 ({{ row.mediaGroupItemCount }})</el-tag>
        </div>
        <div v-else-if="row.mediaFiles && row.mediaFiles.length > 0" class="media-info">
          <el-tag size="small">{{ row.mediaFiles.length }} 个文件</el-tag>
        </div>
        <span v-else>-</span>
      </template>

      <template #stats="{ row }">
        <div class="stats-info">
          <span v-if="row.views">👁 {{ row.views }}</span>
          <span v-if="row.forwards">↗ {{ row.forwards }}</span>
          <span v-if="!row.views && !row.forwards">-</span>
        </div>
      </template>

      <template #date="{ row }">
        {{ formatTimestamp(row.date) }}
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
      title="消息详情"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="currentMessage" class="message-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="消息ID">{{ currentMessage.messageId }}</el-descriptions-item>
          <el-descriptions-item label="频道ID">{{ currentMessage.chatId }}</el-descriptions-item>
          <el-descriptions-item label="频道标题">{{ currentMessage.channelTitle }}</el-descriptions-item>
          <el-descriptions-item label="频道用户名">@{{ currentMessage.channelUsername }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentMessage.status)" size="small">
              {{ getStatusLabel(currentMessage.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="内容类型">{{ formatContentType(currentMessage.contentType) }}</el-descriptions-item>
          <el-descriptions-item label="消息日期">{{ formatTimestamp(currentMessage.date) }}</el-descriptions-item>
          <el-descriptions-item label="编辑日期">{{ currentMessage.editDate ? formatTimestamp(currentMessage.editDate) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="浏览次数">{{ currentMessage.views || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转发次数">{{ currentMessage.forwards || '-' }}</el-descriptions-item>
          <el-descriptions-item label="媒体组ID">{{ currentMessage.mediaAlbumId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="是否媒体组">{{ currentMessage.isMediaGroup ? '是' : '否' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentMessage.textContent" class="detail-section">
          <h4>文本内容</h4>
          <div class="text-content">{{ currentMessage.textContent }}</div>
        </div>

        <div v-if="currentMessage.mediaFiles && currentMessage.mediaFiles.length > 0" class="detail-section">
          <h4>媒体文件 ({{ currentMessage.mediaFiles.length }})</h4>
          <el-table :data="currentMessage.mediaFiles" border>
            <el-table-column prop="fileType" label="类型" width="100" />
            <el-table-column prop="mimeType" label="MIME类型" width="150" />
            <el-table-column prop="fileSize" label="大小" width="120">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="downloaded" label="已下载" width="100">
              <template #default="{ row }">
                <el-tag :type="row.downloaded ? 'success' : 'info'" size="small">
                  {{ row.downloaded ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="localPath" label="本地路径" min-width="200" />
          </el-table>
        </div>

        <div v-if="currentMessage.webPage" class="detail-section">
          <h4>网页信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="URL">
              <a :href="currentMessage.webPage.url" target="_blank">{{ currentMessage.webPage.url }}</a>
            </el-descriptions-item>
            <el-descriptions-item label="类型">{{ currentMessage.webPage.type }}</el-descriptions-item>
            <el-descriptions-item label="站点名称">{{ currentMessage.webPage.siteName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ currentMessage.webPage.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ currentMessage.webPage.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentMessage.webPage.author || '-' }}</el-descriptions-item>
            <el-descriptions-item label="即时预览">{{ currentMessage.webPage.hasInstantView ? '支持' : '不支持' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="currentMessage.isMediaGroup && currentMessage.mediaGroupMessageIds" class="detail-section">
          <h4>媒体组消息ID列表</h4>
          <div class="media-group-ids">
            <el-tag v-for="id in currentMessage.mediaGroupMessageIds" :key="id" style="margin: 4px">
              {{ id }}
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
import type { ChannelMessage } from '@/types/models'
import { formatDateTime, getStatusType, getStatusLabel, truncateText } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: undefined as number | undefined,
  status: '',
  startDate: '',
  endDate: ''
})

// Data state
const data = ref<ChannelMessage[]>([])
const loading = ref(false)

// Detail Dialog state
const detailDialogVisible = ref(false)
const currentMessage = ref<ChannelMessage | null>(null)

// Computed
const hasFilters = computed(() => {
  return !!(filters.chatId || filters.status || filters.startDate || filters.endDate)
})

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'messageId',
    label: '消息ID',
    width: 120
  },
  {
    prop: 'channelInfo',
    label: '频道信息',
    width: 200,
    slot: 'channelInfo'
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    slot: 'status'
  },
  {
    prop: 'contentType',
    label: '类型',
    width: 120,
    slot: 'contentType',
    hideOnMobile: true
  },
  {
    prop: 'textContent',
    label: '内容',
    minWidth: 200,
    slot: 'textContent'
  },
  {
    prop: 'mediaInfo',
    label: '媒体',
    width: 120,
    slot: 'mediaInfo',
    hideOnMobile: true
  },
  {
    prop: 'stats',
    label: '统计',
    width: 120,
    slot: 'stats',
    hideOnMobile: true
  },
  {
    prop: 'date',
    label: '日期',
    width: 180,
    slot: 'date'
  },
  {
    prop: 'actions',
    label: '操作',
    width: 100,
    slot: 'actions',
    fixed: 'right'
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

// Format content type
const formatContentType = (type: string): string => {
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

// Format file size
const formatFileSize = (bytes: number): string => {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

// Show detail dialog
const showDetailDialog = (message: ChannelMessage) => {
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
    if (filters.status) {
      params.status = filters.status
    }
    if (filters.startDate) {
      // Convert date string to timestamp (seconds)
      params.startDate = Math.floor(new Date(filters.startDate).getTime() / 1000)
    }
    if (filters.endDate) {
      // Convert date string to timestamp (seconds)
      params.endDate = Math.floor(new Date(filters.endDate).getTime() / 1000)
    }

    const response = await messageApi.fetchChannelMessages(params)
    data.value = response.records
    pagination.total = response.total
  } catch (error) {
    console.error('Failed to load channel messages:', error)
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
.channel-message-query-tab {
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

.channel-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.channel-title {
  font-weight: 600;
  color: var(--fluent-text-primary);
}

.channel-username {
  font-size: var(--font-size-xs);
  color: var(--fluent-text-secondary);
}

.media-info {
  display: flex;
  gap: 4px;
}

.stats-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: var(--font-size-xs);
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

.text-content {
  padding: var(--spacing-md);
  background-color: var(--fluent-bg-alt);
  border-radius: var(--radius-md);
  white-space: pre-wrap;
  word-break: break-word;
}

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
