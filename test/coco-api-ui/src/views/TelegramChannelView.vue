<template>
  <div class="telegram-channel-view">
    <div class="view-header fluent-card">
      <div class="header-content">
        <h1 class="view-title">Telegram 频道查询</h1>
        <div class="header-description">
          <el-text type="info">查询当前已登录 Telegram 账号的频道列表</el-text>
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
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn } from '@/components/common/DataTable.vue'
import { useChannelStore } from '@/stores/channel'
import { usePagination } from '@/composables/usePagination'

const channelStore = useChannelStore()
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

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
const loadTgChannels = async () => {
  try {
    const response = await channelStore.fetchTgChannels({
      current: pagination.current,
      size: pagination.size
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
  
  .view-content {
    padding: var(--spacing-md);
  }
}
</style>
