<template>
  <aside 
    class="sidebar fluent-acrylic fluent-depth-8"
    :class="{ 
      'sidebar-collapsed': collapsed,
      'sidebar-mobile-hidden': isMobile && collapsed 
    }"
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
      <template v-for="item in navItems" :key="item.path">
        <!-- Parent item with children -->
        <div v-if="item.children" class="nav-group">
          <router-link
            :to="item.path"
            class="nav-item fluent-transition"
            :class="{ 'nav-item-active': isActive(item.path) }"
            @click="handleNavClick"
          >
            <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
            <span class="nav-text" v-if="!collapsed">{{ item.label }}</span>
          </router-link>
          
          <!-- Sub items (only show when not collapsed and parent is active) -->
          <div v-if="!collapsed && isActive(item.path)" class="nav-sub-items">
            <router-link
              v-for="child in item.children"
              :key="child.path"
              :to="child.path"
              class="nav-sub-item fluent-transition"
              :class="{ 'nav-sub-item-active': route.path === child.path }"
              @click="handleNavClick"
            >
              <el-icon class="nav-icon"><component :is="child.icon" /></el-icon>
              <span class="nav-text">{{ child.label }}</span>
            </router-link>
          </div>
        </div>
        
        <!-- Simple item without children -->
        <router-link
          v-else
          :to="item.path"
          class="nav-item fluent-transition"
          :class="{ 'nav-item-active': isActive(item.path) }"
          @click="handleNavClick"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-text" v-if="!collapsed">{{ item.label }}</span>
        </router-link>
      </template>
    </nav>
  </aside>
  
  <!-- Mobile overlay -->
  <div 
    v-if="isMobile && !collapsed" 
    class="sidebar-overlay"
    @click="$emit('toggle')"
  ></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { User, Document, Avatar, Setting, ChatDotRound, Connection, InfoFilled } from '@element-plus/icons-vue'

interface Props {
  collapsed?: boolean
}

defineProps<Props>()

const emit = defineEmits<{
  toggle: []
}>()

const route = useRoute()
const isMobile = ref(false)

const navItems = [
  { path: '/message-tracking', label: '消息跟踪', icon: ChatDotRound },
  { path: '/channel', label: '频道管理', icon: Connection, children: [
    { path: '/channel/management', label: '频道管理', icon: Setting },
    { path: '/channel/telegram', label: 'Telegram 频道查询', icon: Connection }
  ]},
  { path: '/config', label: '配置', icon: Setting, children: [
    { path: '/config/authors', label: '作者库', icon: User },
    { path: '/config/works', label: '原作库', icon: Document },
    { path: '/config/characters', label: '角色库', icon: Avatar },
    { path: '/config/filter', label: '标签过滤配置', icon: Setting }
  ]},
  { path: '/system-info', label: '系统信息', icon: InfoFilled }
]

const isActive = (path: string) => {
  // Check if current route starts with the path (for parent items)
  if (route.path.startsWith(path)) {
    return true
  }
  return route.path === path
}

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const handleNavClick = () => {
  // Auto-collapse sidebar on mobile after navigation
  if (isMobile.value) {
    emit('toggle')
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
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

.nav-group {
  display: flex;
  flex-direction: column;
}

.nav-sub-items {
  display: flex;
  flex-direction: column;
  padding-left: var(--spacing-lg);
  margin-top: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.nav-sub-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-md);
  margin-bottom: var(--spacing-xs);
  border-radius: var(--radius-md);
  color: var(--fluent-text-secondary);
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
}

.nav-sub-item:hover {
  background-color: var(--fluent-bg-alt);
  color: var(--fluent-text-primary);
  transform: translateX(4px);
}

.nav-sub-item-active {
  background-color: var(--fluent-primary-light);
  color: var(--fluent-primary);
  font-weight: 600;
}

.nav-sub-item-active:hover {
  background-color: var(--fluent-primary-light);
  transform: translateX(4px);
}

.nav-sub-item .nav-icon {
  font-size: 16px;
  min-width: 16px;
}

.nav-sub-item .nav-text {
  margin-left: var(--spacing-sm);
}

/* Mobile responsive */
@media (max-width: 767px) {
  .sidebar {
    width: 280px;
    height: 100vh;
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
    transform: translateX(0);
    transition: transform var(--transition-normal);
  }
  
  .sidebar-mobile-hidden {
    transform: translateX(-100%);
  }
  
  .sidebar-nav {
    display: flex;
    flex-direction: column;
  }
  
  .nav-item {
    width: 100%;
  }
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  animation: fadeIn var(--transition-fast);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
