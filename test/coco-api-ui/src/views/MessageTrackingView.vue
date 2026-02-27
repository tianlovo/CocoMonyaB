<template>
  <div class="message-tracking-view">
    <!-- Page Header -->
    <div class="view-header fluent-card">
      <div class="header-content">
        <h1 class="view-title">消息跟踪</h1>
        <div class="header-actions">
          <AutoRefreshControl
            @toggle="handleAutoRefreshToggle"
            @interval-change="handleIntervalChange"
          />
        </div>
      </div>
    </div>

    <!-- Content Area -->
    <div class="view-content fluent-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="消息查询" name="message">
          <MessageQueryTab ref="messageTabRef" />
        </el-tab-pane>
        <el-tab-pane label="频道消息查询" name="channel-message">
          <ChannelMessageQueryTab ref="channelMessageTabRef" />
        </el-tab-pane>
        <el-tab-pane label="转发队列查询" name="forward-queue">
          <ForwardQueueQueryTab ref="forwardQueueTabRef" />
        </el-tab-pane>
        <el-tab-pane label="已处理消息查询" name="processed-message">
          <ProcessedMessageQueryTab ref="processedMessageTabRef" />
        </el-tab-pane>
        <el-tab-pane label="未读消息缓冲区查询" name="unread-buffer">
          <UnreadBufferQueryTab ref="unreadBufferTabRef" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'MessageTrackingView'
}
</script>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import AutoRefreshControl from '@/components/common/AutoRefreshControl.vue'
import MessageQueryTab from '@/components/message-tracking/MessageQueryTab.vue'
import ChannelMessageQueryTab from '@/components/message-tracking/ChannelMessageQueryTab.vue'
import ForwardQueueQueryTab from '@/components/message-tracking/ForwardQueueQueryTab.vue'
import ProcessedMessageQueryTab from '@/components/message-tracking/ProcessedMessageQueryTab.vue'
import UnreadBufferQueryTab from '@/components/message-tracking/UnreadBufferQueryTab.vue'

// Active tab state
const activeTab = ref('message')

// Tab refs
const messageTabRef = ref<InstanceType<typeof MessageQueryTab> | null>(null)
const channelMessageTabRef = ref<InstanceType<typeof ChannelMessageQueryTab> | null>(null)
const forwardQueueTabRef = ref<InstanceType<typeof ForwardQueueQueryTab> | null>(null)
const processedMessageTabRef = ref<InstanceType<typeof ProcessedMessageQueryTab> | null>(null)
const unreadBufferTabRef = ref<InstanceType<typeof UnreadBufferQueryTab> | null>(null)

// Auto-refresh state
const autoRefreshEnabled = ref(false)
const refreshInterval = ref(10000) // Default 10 seconds
const refreshTimer = ref<number | null>(null)

// Handle auto-refresh toggle
const handleAutoRefreshToggle = (enabled: boolean) => {
  autoRefreshEnabled.value = enabled
  
  if (enabled) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

// Handle interval change
const handleIntervalChange = (interval: number) => {
  refreshInterval.value = interval
  
  // Restart timer if auto-refresh is enabled
  if (autoRefreshEnabled.value) {
    stopAutoRefresh()
    startAutoRefresh()
  }
}

// Handle tab change
const handleTabChange = () => {
  // Restart auto-refresh for the new active tab
  if (autoRefreshEnabled.value) {
    stopAutoRefresh()
    startAutoRefresh()
  }
}

// Start auto-refresh
const startAutoRefresh = () => {
  refreshTimer.value = window.setInterval(() => {
    performRefresh()
  }, refreshInterval.value)
}

// Stop auto-refresh
const stopAutoRefresh = () => {
  if (refreshTimer.value !== null) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

// Perform refresh on current active tab
const performRefresh = () => {
  const tabRefMap: Record<string, any> = {
    'message': messageTabRef.value,
    'channel-message': channelMessageTabRef.value,
    'forward-queue': forwardQueueTabRef.value,
    'processed-message': processedMessageTabRef.value,
    'unread-buffer': unreadBufferTabRef.value
  }
  
  const currentTabRef = tabRefMap[activeTab.value]
  if (currentTabRef && typeof currentTabRef.loadData === 'function') {
    currentTabRef.loadData()
  }
}

// Cleanup on unmount
onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.message-tracking-view {
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

/* Responsive adjustments */
@media (max-width: 767px) {
  .message-tracking-view {
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
  
  .view-content {
    padding: var(--spacing-md);
  }
}
</style>
