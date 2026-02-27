<template>
  <div class="channel-layout">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="频道管理" name="management" />
      <el-tab-pane label="Telegram 频道查询" name="telegram" />
    </el-tabs>
    
    <div class="channel-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const activeTab = ref('management')

// Initialize active tab based on current route
watch(() => route.name, (newName) => {
  if (newName === 'channel-management') activeTab.value = 'management'
  else if (newName === 'channel-telegram') activeTab.value = 'telegram'
}, { immediate: true })

const handleTabChange = (tabName: string) => {
  const routeMap: Record<string, string> = {
    management: '/channel/management',
    telegram: '/channel/telegram'
  }
  
  router.push(routeMap[tabName])
}
</script>

<style scoped>
.channel-layout {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.channel-content {
  flex: 1;
}

:deep(.el-tabs__header) {
  margin-bottom: 0;
}

:deep(.el-tabs__nav-wrap) {
  padding: 0 var(--spacing-md);
}

/* Mobile responsive */
@media (max-width: 767px) {
  :deep(.el-tabs__nav-wrap) {
    padding: 0;
  }
}
</style>
