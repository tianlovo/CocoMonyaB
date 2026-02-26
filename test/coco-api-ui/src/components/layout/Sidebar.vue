<template>
  <aside 
    class="sidebar fluent-acrylic fluent-depth-8"
    :class="{ 'sidebar-collapsed': collapsed }"
  >
    <div class="sidebar-header">
      <h1 class="sidebar-title" v-if="!collapsed">标签管理系统</h1>
      <h1 class="sidebar-title-short" v-else>标签</h1>
      <button 
        class="sidebar-toggle"
        @click="$emit('toggle')"
        :title="collapsed ? '展开侧边栏' : '折叠侧边栏'"
      >
        <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
      </button>
    </div>
    
    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item fluent-transition"
        :class="{ 'nav-item-active': isActive(item.path) }"
      >
        <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
        <span class="nav-text" v-if="!collapsed">{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { User, Document, Avatar, Setting } from '@element-plus/icons-vue'

interface Props {
  collapsed?: boolean
}

defineProps<Props>()

defineEmits<{
  toggle: []
}>()

const route = useRoute()

const navItems = [
  { path: '/authors', label: '作者库', icon: User },
  { path: '/works', label: '原作库', icon: Document },
  { path: '/characters', label: '角色库', icon: Avatar },
  { path: '/config', label: '标签过滤配置', icon: Setting }
]

const isActive = (path: string) => {
  return route.path === path
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  position: fixed;
  left: 0;
  top: 0;
  z-index: var(--z-fixed);
}

.sidebar-collapsed {
  width: 64px;
}

.sidebar-header {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--fluent-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
}

.sidebar-title {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--fluent-text-primary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-title-short {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--fluent-text-primary);
  margin: 0;
}

.sidebar-toggle {
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
}

.sidebar-toggle:hover {
  background-color: var(--fluent-bg-alt);
  color: var(--fluent-text-primary);
}

.sidebar-nav {
  flex: 1;
  padding: var(--spacing-md);
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  border-radius: var(--radius-md);
  color: var(--fluent-text-secondary);
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background-color: var(--fluent-bg-alt);
  color: var(--fluent-text-primary);
  transform: translateX(4px);
}

.nav-item-active {
  background-color: var(--fluent-primary);
  color: var(--fluent-text-on-accent);
  box-shadow: var(--fluent-shadow-4);
}

.nav-item-active:hover {
  background-color: var(--fluent-primary-hover);
  transform: translateX(4px);
}

.nav-icon {
  font-size: 20px;
  min-width: 20px;
}

.nav-text {
  margin-left: var(--spacing-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-collapsed .nav-item {
  justify-content: center;
  padding: var(--spacing-md) var(--spacing-sm);
}

.sidebar-collapsed .nav-text {
  display: none;
}

/* Mobile responsive */
@media (max-width: 767px) {
  .sidebar {
    width: 100%;
    height: auto;
    position: relative;
  }
  
  .sidebar-collapsed {
    width: 100%;
  }
  
  .sidebar-nav {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }
  
  .nav-item {
    flex: 1;
    min-width: calc(50% - var(--spacing-sm));
  }
}
</style>
