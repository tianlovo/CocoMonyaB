<template>
  <div class="telegram-channel-view">
    <div class="view-header fluent-card">
      <div class="header-content">
        <div class="header-title-section">
          <h1 class="view-title">Telegram 频道查询</h1>
          <div class="header-description">
            <el-text type="info">查询当前已登录 Telegram 账号的频道列表</el-text>
          </div>
        </div>
        <div class="header-actions">
          <el-button 
            :icon="Refresh" 
            :loading="channelStore.loading"
            @click="handleRefresh"
          >
            刷新数据
          </el-button>
        </div>
      </div>
    </div>

    <div class="view-content fluent-card">
      <DataTable
        :data="channelStore.tgChannels"
        :columns="columns"
        :loading="channelStore.loading"
        :pagination="pagination"
        :empty-type="'empty'"
        :empty-message="'暂无频道数据，请确保 Telegram 客户端已登录'"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #chatId="{ row }">
          <code class="channel-id">{{ row.chatId }}</code>
        </template>

        <template #username="{ row }">
          <span v-if="row.username">@{{ row.username }}</span>
          <span v-else class="text-secondary">-</span>
        </template>

        <template #isChannel="{ row }">
          <el-tag :type="row.isChannel ? 'success' : 'warning'" size="small">
            {{ row.isChannel ? '频道' : '超级群组' }}
          </el-tag>
        </template>

        <template #memberCount="{ row }">
          {{ formatNumber(row.memberCount) }}
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'TelegramChannelView'
}
</script>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { useChannelStore } from '@/stores/channel'
import { usePagination } from '@/composables/usePagination'

const channelStore = useChannelStore()
const { pagination } = usePagination(10)

// Refresh rate limiting
const lastRefreshTime = ref<number>(0)
const REFRESH_COOLDOWN = 60000 // 60 seconds cooldown

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'chatId',
    label: '频道ID',
    width: 150,
    slot: 'chatId'
  },
  {
    prop: 'username',
    label: '用户名',
    minWidth: 150,
    slot: 'username'
  },
  {
    prop: 'title',
    label: '频道标题',
    minWidth: 200
  },
  {
    prop: 'isChannel',
    label: '类型',
    width: 120,
    slot: 'isChannel'
  },
  {
    prop: 'memberCount',
    label: '成员数',
    width: 120,
    slot: 'memberCount'
  }
]

// Load Telegram channels data
const loadTgChannels = async (forceRefresh = false) => {
  try {
    const response = await channelStore.fetchTgChannels({
      current: pagination.current,
      size: pagination.size,
      forceRefresh
    })
    pagination.total = response.total
  } catch (error: any) {
    if (error?.code === -60001) {
      ElMessage.error('Telegram 客户端未就绪，请确保已登录')
    } else {
      ElMessage.error('加载频道列表失败')
    }
  }
}

// Handle page change
const handlePageChange = (page: number) => {
  pagination.current = page
  loadTgChannels()
}

// Handle page size change
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1 // Reset to first page
  loadTgChannels()
}

// Handle refresh button click
const handleRefresh = async () => {
  // Check cooldown period
  const now = Date.now()
  const timeSinceLastRefresh = now - lastRefreshTime.value
  const remainingCooldown = REFRESH_COOLDOWN - timeSinceLastRefresh
  
  if (lastRefreshTime.value > 0 && remainingCooldown > 0) {
    const remainingSeconds = Math.ceil(remainingCooldown / 1000)
    ElMessage.warning({
      message: `请稍后再试，距离下次刷新还需等待 ${remainingSeconds} 秒`,
      duration: 3000
    })
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '强制刷新将从 Telegram 服务器获取最新数据。频繁刷新可能导致以下风险：',
      '刷新确认',
      {
        confirmButtonText: '确定刷新',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true,
        message: `
          <div style="line-height: 1.6;">
            <p style="margin-bottom: 12px;">强制刷新将从 Telegram 服务器获取最新数据。</p>
            <p style="margin-bottom: 8px; font-weight: 600; color: #E6A23C;">⚠️ 频繁刷新可能导致以下风险：</p>
            <ul style="margin: 0; padding-left: 20px; color: #606266;">
              <li>Telegram 账号被临时限制或封禁</li>
              <li>API 请求被限流，导致功能异常</li>
              <li>增加服务器负载，影响系统性能</li>
            </ul>
            <p style="margin-top: 12px; color: #909399; font-size: 13px;">💡 建议：正常情况下使用缓存数据即可，仅在必要时刷新。</p>
            <p style="margin-top: 8px; color: #F56C6C; font-size: 13px; font-weight: 600;">🔒 刷新后需等待 60 秒才能再次刷新。</p>
          </div>
        `
      }
    )
    
    // Update last refresh time
    lastRefreshTime.value = Date.now()
    
    await loadTgChannels(true)
    ElMessage.success('数据已刷新，60 秒内无法再次刷新')
  } catch (error) {
    // User cancelled, do nothing
    if (error !== 'cancel') {
      console.error('Refresh failed:', error)
    }
  }
}

// Initialize
onMounted(() => {
  loadTgChannels()
})

// Utility function to format number
const formatNumber = (num: number) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}
</script>

<style scoped>
.telegram-channel-view {
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
  align-items: flex-start;
  gap: var(--spacing-md);
}

.header-title-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.view-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--fluent-text-primary);
}

.header-description {
  color: var(--fluent-text-secondary);
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

.channel-id {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: var(--font-size-sm);
  background: var(--fluent-bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
}

.text-secondary {
  color: var(--fluent-text-secondary);
  font-style: italic;
}

/* Responsive adjustments */
@media (max-width: 767px) {
  .telegram-channel-view {
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
    width: 100%;
  }
  
  .header-actions .el-button {
    width: 100%;
  }
  
  .view-content {
    padding: var(--spacing-md);
  }
}
</style>
