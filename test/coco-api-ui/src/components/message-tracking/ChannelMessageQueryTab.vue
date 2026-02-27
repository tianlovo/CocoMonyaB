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

    <!-- Tree Table -->
    <el-table
      :data="treeData"
      v-loading="loading"
      stripe
      style="width: 100%"
      row-key="id"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      :default-expand-all="false"
      class="fluent-card channel-message-table"
    >
      <!-- Message ID Column -->
      <el-table-column prop="messageId" label="消息ID" width="140">
        <template #default="{ row }">
          <div v-if="!row.isGroupHeader" class="message-id-cell">
            <span class="message-id-text">{{ row.messageId }}</span>
          </div>
          <div v-else class="group-header-cell">
            <el-icon><FolderOpened /></el-icon>
            <span class="group-header-text">媒体组</span>
          </div>
        </template>
      </el-table-column>

      <!-- Channel Info Column -->
      <el-table-column prop="channelInfo" label="频道信息" width="200">
        <template #default="{ row }">
          <div v-if="!row.isGroupHeader" class="channel-info">
            <div class="channel-title">{{ row.channelTitle || '-' }}</div>
            <div class="channel-username">@{{ row.channelUsername || '-' }}</div>
          </div>
          <div v-else class="group-header-info">
            <span class="group-id">#{{ row.mediaAlbumId }}</span>
          </div>
        </template>
      </el-table-column>

      <!-- Content Type Column -->
      <el-table-column prop="contentType" label="类型" width="120">
        <template #default="{ row }">
          <div v-if="!row.isGroupHeader">
            <el-tag size="small" :type="getContentTypeTagType(row.contentType)">
              {{ formatContentType(row.contentType) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <!-- Text Content Column -->
      <el-table-column prop="textContent" label="内容" min-width="200">
        <template #default="{ row }">
          <div v-if="!row.isGroupHeader">
            <el-tooltip
              v-if="row.textContent && row.textContent.length > 50"
              :content="row.textContent"
              placement="top"
            >
              <span>{{ truncateText(row.textContent, 50) }}</span>
            </el-tooltip>
            <span v-else>{{ row.textContent || '-' }}</span>
          </div>
          <span v-else class="group-summary">
            {{ getGroupSummary(row) }}
          </span>
        </template>
      </el-table-column>

      <!-- Media Info Column -->
      <el-table-column prop="mediaInfo" label="媒体" width="180">
        <template #default="{ row }">
          <div v-if="!row.isGroupHeader">
            <div v-if="row.mediaFiles && row.mediaFiles.length > 0" class="media-info">
              <el-tooltip :content="getMediaFilesInfo(row.mediaFiles)" placement="top">
                <div class="media-files-info">
                  <el-icon class="media-icon"><Document /></el-icon>
                  <span class="media-text">
                    {{ getMediaTypeIcon(row.mediaFiles[0].fileType) }}
                    <span v-if="row.mediaFiles.length > 1" class="media-count">×{{ row.mediaFiles.length }}</span>
                  </span>
                </div>
              </el-tooltip>
            </div>
            <span v-else class="no-media">无媒体</span>
          </div>
        </template>
      </el-table-column>

      <!-- Stats Column -->
      <el-table-column prop="stats" label="统计" width="120">
        <template #default="{ row }">
          <div v-if="!row.isGroupHeader" class="stats-info">
            <span v-if="row.views">👁 {{ row.views }}</span>
            <span v-if="row.forwards">↗ {{ row.forwards }}</span>
            <span v-if="!row.views && !row.forwards">-</span>
          </div>
        </template>
      </el-table-column>

      <!-- Date Column -->
      <el-table-column prop="date" label="日期" width="180">
        <template #default="{ row }">
          <span v-if="!row.isGroupHeader">{{ formatTimestamp(row.date) }}</span>
        </template>
      </el-table-column>

      <!-- Actions Column -->
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="!row.isGroupHeader"
            type="primary"
            link
            size="small"
            @click="showDetailDialog(row)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div v-if="pagination.total > 0" class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

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
          <el-descriptions-item label="内容类型">{{ formatContentType(currentMessage.contentType) }}</el-descriptions-item>
          <el-descriptions-item label="消息日期">{{ formatTimestamp(currentMessage.date) }}</el-descriptions-item>
          <el-descriptions-item label="编辑日期">{{ currentMessage.editDate ? formatTimestamp(currentMessage.editDate) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="浏览次数">{{ currentMessage.views || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转发次数">{{ currentMessage.forwards || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentMessage.textContent" class="detail-section">
          <h4>文本内容</h4>
          <div class="text-content">{{ currentMessage.textContent }}</div>
        </div>

        <!-- Media Group Section -->
        <div v-if="currentMessage.mediaAlbumId" class="detail-section">
          <h4>
            <el-icon><PictureFilled /></el-icon>
            媒体组信息
          </h4>
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>
              <div class="media-group-alert">
                <span>媒体组 ID: {{ currentMessage.mediaAlbumId }}</span>
                <el-tag type="warning" size="small">{{ getMediaGroupCountText(currentMessage) }}</el-tag>
              </div>
            </template>
          </el-alert>
          
          <div v-if="currentMessage.mediaGroupMessageIds && currentMessage.mediaGroupMessageIds.length > 0" class="media-group-messages">
            <div class="media-group-header">
              <span>关联消息ID列表 ({{ currentMessage.mediaGroupMessageIds.length }} 条)：</span>
            </div>
            <div class="media-group-ids">
              <el-tag 
                v-for="id in currentMessage.mediaGroupMessageIds" 
                :key="id" 
                :type="id === currentMessage.messageId ? 'primary' : 'info'"
                size="small"
              >
                {{ id }}
                <span v-if="id === currentMessage.messageId"> (当前)</span>
              </el-tag>
            </div>
          </div>
          
          <div v-else class="media-group-messages">
            <el-empty 
              description="暂无关联消息ID列表" 
              :image-size="60"
            />
          </div>
        </div>

        <div v-if="currentMessage.mediaFiles && currentMessage.mediaFiles.length > 0" class="detail-section">
          <h4>媒体文件 ({{ currentMessage.mediaFiles.length }})</h4>
          <el-table :data="currentMessage.mediaFiles" border>
            <el-table-column prop="fileType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="success">{{ getMediaTypeIcon(row.fileType) }}</el-tag>
              </template>
            </el-table-column>
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
            <el-table-column prop="localPath" label="本地路径" min-width="200">
              <template #default="{ row }">
                <span v-if="row.localPath" class="local-path">{{ row.localPath }}</span>
                <span v-else class="no-path">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="currentMessage.webPage" class="detail-section">
          <h4>网页信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="URL">
              <a :href="currentMessage.webPage.url" target="_blank" class="web-link">
                {{ currentMessage.webPage.url }}
                <el-icon><TopRight /></el-icon>
              </a>
            </el-descriptions-item>
            <el-descriptions-item label="类型">{{ currentMessage.webPage.type }}</el-descriptions-item>
            <el-descriptions-item label="站点名称">{{ currentMessage.webPage.siteName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ currentMessage.webPage.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ currentMessage.webPage.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentMessage.webPage.author || '-' }}</el-descriptions-item>
            <el-descriptions-item label="即时预览">{{ currentMessage.webPage.hasInstantView ? '支持' : '不支持' }}</el-descriptions-item>
          </el-descriptions>
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
import { PictureFilled, Document, TopRight, FolderOpened } from '@element-plus/icons-vue'
import { usePagination } from '@/composables/usePagination'
import { messageApi } from '@/api/message'
import type { ChannelMessage } from '@/types/models'
import { formatDateTime, truncateText } from '@/utils/formatters'

// Pagination
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

// Filter state
const filters = reactive({
  chatId: undefined as number | undefined,
  startDate: '',
  endDate: ''
})

// Data state
const data = ref<ChannelMessage[]>([])
const loading = ref(false)

// Detail Dialog state
const detailDialogVisible = ref(false)
const currentMessage = ref<ChannelMessage | null>(null)

// Tree data - group messages by mediaAlbumId
const treeData = computed(() => {
  const mediaGroups = new Map<number, ChannelMessage[]>()
  const standalone: ChannelMessage[] = []

  // Group messages by mediaAlbumId
  data.value.forEach(message => {
    // Add unique id for tree structure
    const messageWithId = { ...message, id: `msg-${message.messageId}` }
    
    if (message.mediaAlbumId) {
      if (!mediaGroups.has(message.mediaAlbumId)) {
        mediaGroups.set(message.mediaAlbumId, [])
      }
      mediaGroups.get(message.mediaAlbumId)!.push(messageWithId)
    } else {
      standalone.push(messageWithId)
    }
  })

  // Build tree structure
  const result: any[] = []

  // Add media groups
  mediaGroups.forEach((messages, albumId) => {
    if (messages.length > 1) {
      // Create group header
      result.push({
        id: `group-${albumId}`,
        isGroupHeader: true,
        mediaAlbumId: albumId,
        children: messages,
        hasChildren: true
      })
    } else {
      // Single message in group, treat as standalone
      result.push(messages[0])
    }
  })

  // Add standalone messages
  result.push(...standalone)

  // Sort by date (newest first)
  result.sort((a, b) => {
    const dateA = a.isGroupHeader ? (a.children[0]?.date || 0) : (a.date || 0)
    const dateB = b.isGroupHeader ? (b.children[0]?.date || 0) : (b.date || 0)
    return dateB - dateA
  })

  return result
})

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

// Get content type tag color
const getContentTypeTagType = (type: string): string => {
  const typeColorMap: Record<string, string> = {
    text: 'info',
    photo: 'success',
    video: 'warning',
    audio: 'primary',
    document: '',
    animation: 'warning',
    voice: 'primary',
    sticker: 'success',
    poll: 'info',
    location: 'danger',
    contact: 'info',
    venue: 'danger',
    game: 'success',
    invoice: 'warning',
    web_page: 'primary'
  }
  return typeColorMap[type] || 'info'
}

// Get media type icon text
const getMediaTypeIcon = (fileType: string): string => {
  const iconMap: Record<string, string> = {
    photo: '图片',
    video: '视频',
    audio: '音频',
    document: '文档',
    animation: '动画',
    voice: '语音'
  }
  return iconMap[fileType] || '文件'
}

// Get media files info for tooltip
const getMediaFilesInfo = (mediaFiles: any[]): string => {
  if (!mediaFiles || mediaFiles.length === 0) return '无媒体文件'
  
  const fileTypes = mediaFiles.map(f => getMediaTypeIcon(f.fileType))
  const uniqueTypes = [...new Set(fileTypes)]
  
  return `包含 ${mediaFiles.length} 个文件: ${uniqueTypes.join(', ')}`
}

// Get media group count text for detail dialog
const getMediaGroupCountText = (message: ChannelMessage): string => {
  if (message.mediaGroupMessageIds && message.mediaGroupMessageIds.length > 0) {
    return `包含 ${message.mediaGroupMessageIds.length} 条消息`
  }
  
  if (message.mediaGroupItemCount && message.mediaGroupItemCount > 0) {
    return `包含 ${message.mediaGroupItemCount} 项`
  }
  
  if (message.mediaFiles && message.mediaFiles.length > 0) {
    return `包含 ${message.mediaFiles.length} 个文件`
  }
  
  return '媒体组 (数量未知)'
}

// Generate consistent color for media group ID
const getMediaGroupColor = (mediaAlbumId: number | null): string => {
  if (!mediaAlbumId) return '#909399'
  
  const colors = [
    '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9C27B0', '#00BCD4',
    '#FF9800', '#4CAF50', '#2196F3', '#FF5722', '#673AB7', '#009688'
  ]
  
  const index = Math.abs(mediaAlbumId) % colors.length
  return colors[index]
}

// Get group summary
const getGroupSummary = (row: any): string => {
  if (!row.children || row.children.length === 0) return ''
  
  const types = row.children.map((m: any) => formatContentType(m.contentType))
  const uniqueTypes = [...new Set(types)]
  
  return `包含 ${uniqueTypes.join('、')} 等内容`
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

    if (filters.chatId) {
      params.chatId = filters.chatId
    }
    if (filters.startDate) {
      params.startDate = Math.floor(new Date(filters.startDate).getTime() / 1000)
    }
    if (filters.endDate) {
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

.channel-message-table {
  margin-top: var(--spacing-md);
}

.message-id-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.message-id-text {
  font-weight: 500;
  color: var(--fluent-text-primary);
}

.group-header-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-weight: 600;
  color: var(--fluent-primary);
}

.group-header-text {
  font-size: var(--font-size-base);
}

.group-header-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.group-id {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: var(--font-size-sm);
  color: var(--fluent-text-secondary);
  font-weight: 600;
}

.group-summary {
  color: var(--fluent-text-secondary);
  font-size: var(--font-size-sm);
  font-style: italic;
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

.content-type-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.media-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.media-files-info {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: var(--fluent-bg-alt);
  border-radius: var(--radius-sm);
  cursor: help;
  transition: all var(--transition-fast);
}

.media-files-info:hover {
  background-color: var(--fluent-bg-hover);
  transform: translateX(2px);
}

.media-icon {
  font-size: 14px;
  color: var(--fluent-primary);
}

.media-text {
  font-size: var(--font-size-xs);
  color: var(--fluent-text-primary);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.media-count {
  font-size: var(--font-size-xs);
  color: var(--fluent-text-secondary);
  font-weight: normal;
}

.no-media {
  color: var(--fluent-text-tertiary);
  font-size: var(--font-size-sm);
}

.stats-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: var(--font-size-xs);
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
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
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.text-content {
  padding: var(--spacing-md);
  background-color: var(--fluent-bg-alt);
  border-radius: var(--radius-md);
  white-space: pre-wrap;
  word-break: break-word;
}

.media-group-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
}

.media-group-messages {
  margin-top: var(--spacing-md);
  padding: var(--spacing-md);
  background-color: var(--fluent-bg-alt);
  border-radius: var(--radius-md);
}

.media-group-header {
  font-size: var(--font-size-sm);
  color: var(--fluent-text-secondary);
  margin-bottom: var(--spacing-sm);
  font-weight: 500;
}

.media-group-ids {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.local-path {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: var(--font-size-xs);
  color: var(--fluent-text-secondary);
}

.no-path {
  color: var(--fluent-text-tertiary);
}

.web-link {
  color: var(--fluent-primary);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.web-link:hover {
  text-decoration: underline;
}

/* Tree table styles */
:deep(.el-table__expand-icon) {
  color: var(--fluent-primary);
}

:deep(.el-table__row--level-1) {
  background-color: rgba(64, 158, 255, 0.02);
}

:deep(.el-table__row--level-1:hover) {
  background-color: rgba(64, 158, 255, 0.05) !important;
}

/* Remove number input spinner */
:deep(input[type="number"]::-webkit-inner-spin-button),
:deep(input[type="number"]::-webkit-outer-spin-button) {
  -webkit-appearance: none;
  appearance: none;
  margin: 0;
}

:deep(input[type="number"]) {
  -moz-appearance: textfield;
  appearance: textfield;
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
