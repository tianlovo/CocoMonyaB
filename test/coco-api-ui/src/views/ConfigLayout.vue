<template>
  <div class="config-layout">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="作者库" name="authors" />
      <el-tab-pane label="原作库" name="works" />
      <el-tab-pane label="角色库" name="characters" />
      <el-tab-pane label="标签过滤配置" name="filter" />
    </el-tabs>
    
    <div class="config-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const activeTab = ref('authors')

// Initialize active tab based on current route
watch(() => route.name, (newName) => {
  if (newName === 'config-authors') activeTab.value = 'authors'
  else if (newName === 'config-works') activeTab.value = 'works'
  else if (newName === 'config-characters') activeTab.value = 'characters'
  else if (newName === 'config-filter') activeTab.value = 'filter'
}, { immediate: true })

const handleTabChange = (tabName: string) => {
  const routeMap: Record<string, string> = {
    authors: '/config/authors',
    works: '/config/works',
    characters: '/config/characters',
    filter: '/config/filter'
  }
  
  router.push(routeMap[tabName])
}
</script>

<style scoped>
.config-layout {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.config-content {
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
