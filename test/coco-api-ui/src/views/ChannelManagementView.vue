<template>
  <div class="channel-management-view">
    <div class="view-header fluent-card">
      <div class="header-content">
        <h1 class="view-title">频道管理</h1>
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索频道用户名"
            :prefix-icon="Search"
            clearable
            style="width: 300px"
            @input="handleSearch"
          />
          <el-select
            v-model="monitoringFilter"
            placeholder="监控状态"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option label="全部" :value="undefined" />
            <el-option label="监控中" :value="true" />
            <el-option label="已停止" :value="false" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="handleCreate">
            新建频道
          </el-button>
        </div>
      </div>
    </div>

    <div class="view-content fluent-card">
      <DataTable
        :data="channelStore.channels"
        :columns="columns"
        :loading="channelStore.loading"
        :pagination="pagination"
        :actions="actions"
        :empty-type="searchKeyword || monitoringFilter !== undefined ? 'no-result' : 'empty'"
        :empty-message="searchKeyword || monitoringFilter !== undefined ? '未找到匹配的频道' : '暂无频道数据'"
        :empty-action-text="searchKeyword || monitoringFilter !== undefined ? '' : '新建频道'"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
        @action="handleAction"
        @empty-action="handleEmptyAction"
      >
        <template #channelId="{ row }">
          <code class="channel-id">{{ row.channelId }}</code>
        </template>

        <template #channelUsername="{ row }">
          <span v-if="row.channelUsername">@{{ row.channelUsername }}</span>
          <span v-else class="text-secondary">-</span>
        </template>

        <template #monitoringStatus="{ row }">
          <el-tag :type="row.monitoringStatus ? 'success' : 'info'" size="small">
            {{ row.monitoringStatus ? '监控中' : '已停止' }}
          </el-tag>
        </template>

        <template #createTime="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </DataTable>
    </div>

    <!-- Channel Dialog -->
    <ChannelDialog
      v-model:visible="dialogVisible"
      :channel="currentChannel"
      @success="handleDialogSuccess"
    />
  </div>
</template>

<script lang="ts">
export default {
  name: 'ChannelManagementView'
}
</script>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '@/components/common/DataTable.vue'
import type { TableColumn, TableAction } from '@/components/common/DataTable.vue'
import ChannelDialog from '@/components/channel/ChannelDialog.vue'
import { useChannelStore } from '@/stores/channel'
import { usePagination } from '@/composables/usePagination'
import type { Channel } from '@/types/models'
import { showSuccessMessage } from '@/utils/errorHandler'

const channelStore = useChannelStore()
const { pagination, handlePageChange, handleSizeChange } = usePagination(10)

const searchKeyword = ref('')
const monitoringFilter = ref<boolean | undefined>(undefined)
let searchTimer: ReturnType<typeof setTimeout> | null = null

// Dialog state
const dialogVisible = ref(false)
const currentChannel = ref<Channel | null>(null)

// Table columns configuration
const columns: TableColumn[] = [
  {
    prop: 'channelId',
    label: '频道ID',
    width: 150,
    slot: 'channelId'
  },
  {
    prop: 'channelUsername',
    label: '用户名',
    minWidth: 150,
    slot: 'channelUsername'
  },
  {
    prop: 'channelTitle',
    label: '频道标题',
    minWidth: 200
  },
  {
    prop: 'monitoringStatus',
    label: '监控状态',
    width: 100,
    slot: 'monitoringStatus'
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    slot: 'createTime'
  }
]

// Table actions configuration
const actions: TableAction[] = [
  {
    name: 'edit',
    label: '编辑',
    type: 'primary'
  },
  {
    name: 'delete',
    label: '删除',
    type: 'danger'
  }
]

// Load channels data
const loadChannels = async () => {
  try {
    const response = await channelStore.fetchPage({
      current: pagination.current,
      size: pagination.size,
      channelUsername: searchKeyword.value || undefined,
      monitoringStatus: monitoringFilter.value
    })
    pagination.total = response.total
  } catch (error) {
    ElMessage.error('加载频道列表失败')
  }
}

// Search with debounce (300ms)
const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  
  searchTimer = setTimeout(() => {
    pagination.current = 1
    loadChannels()
  }, 300)
}

// Handle table actions
const handleAction = (actionName: string, row: Channel) => {
  if (actionName === 'edit') {
    handleEdit(row)
  } else if (actionName === 'delete') {
    handleDelete(row)
  }
}

// Handle create
const handleCreate = () => {
  currentChannel.value = null
  dialogVisible.value = true
}

// Handle edit
const handleEdit = (channel: Channel) => {
  currentChannel.value = channel
  dialogVisible.value = true
}

// Handle delete
const handleDelete = async (channel: Channel) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除频道 "${channel.channelTitle}" 吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await channelStore.deleteChannel(channel.id)
    showSuccessMessage('删除成功')
    loadChannels()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}

// Handle empty action
const handleEmptyAction = () => {
  handleCreate()
}

// Handle dialog success
const handleDialogSuccess = () => {
  loadChannels()
}

// Initialize
onMounted(() => {
  loadChannels()
})

// Utility function to format date time
const formatDateTime = (dateTimeStr: string) => {
  if (!dateTimeStr) return '-'
  
  try {
    const date = new Date(dateTimeStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateTimeStr
  }
}
</script>

<style scoped>
.channel-management-view {
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
  align-items: center;
  gap: var(--spacing-md);
}

.view-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--fluent-text-primary);
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
  .channel-management-view {
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
    flex-direction: column;
  }
  
  .header-actions .el-input,
  .header-actions .el-select {
    width: 100% !important;
  }
  
  .view-content {
    padding: var(--spacing-md);
  }
}
</style>
