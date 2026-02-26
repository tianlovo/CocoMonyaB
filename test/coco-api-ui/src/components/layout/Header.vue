<template>
  <header class="header fluent-acrylic fluent-depth-4">
    <div class="header-content">
      <div class="header-left">
        <button 
          v-if="showMenuButton"
          class="header-action menu-button"
          @click="handleToggleSidebar"
        >
          <el-icon><Menu /></el-icon>
        </button>
        <h2 class="page-title">{{ pageTitle }}</h2>
      </div>
      
      <div class="header-right">
        <el-tooltip content="刷新" placement="bottom">
          <button class="header-action" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Menu } from '@element-plus/icons-vue'

interface Props {
  showMenuButton?: boolean
}

defineProps<Props>()

const route = useRoute()

const emit = defineEmits<{
  refresh: []
  toggleSidebar: []
}>()

const pageTitleMap: Record<string, string> = {
  '/authors': '作者库',
  '/works': '原作库',
  '/characters': '角色库',
  '/config': '标签过滤配置'
}

const pageTitle = computed(() => {
  return pageTitleMap[route.path] || '标签管理系统'
})

const handleRefresh = () => {
  emit('refresh')
}

const handleToggleSidebar = () => {
  emit('toggleSidebar')
}
</script>

<style scoped>
.header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--spacing-lg);
  border-bottom: 1px solid var(--fluent-border);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.header-content {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.menu-button {
  margin-right: var(--spacing-sm);
}

.page-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--fluent-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.header-action {
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
  color: var(--fluent-text-secondary);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.header-action:hover {
  background-color: var(--fluent-bg-alt);
  color: var(--fluent-text-primary);
  transform: scale(1.1);
}

.header-action:active {
  transform: scale(0.95);
}

/* Mobile responsive */
@media (max-width: 767px) {
  .header {
    padding: 0 var(--spacing-md);
  }
  
  .page-title {
    font-size: var(--font-size-lg);
  }
}
</style>
