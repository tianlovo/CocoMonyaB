<template>
  <div class="app-layout">
    <Sidebar 
      :collapsed="sidebarCollapsed" 
      @toggle="toggleSidebar"
    />
    
    <div 
      class="main-container"
      :class="{ 'main-container-expanded': sidebarCollapsed }"
    >
      <Header @refresh="handleRefresh" />
      
      <main class="main-content fluent-fade-in">
        <div class="content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" :key="$route.path" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const router = useRouter()
const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleRefresh = () => {
  // Trigger a route refresh by navigating to the same route
  router.go(0)
}
</script>

<style scoped>
.app-layout {
  display: flex;
  width: 100%;
  min-height: 100vh;
  background-color: var(--fluent-bg-base);
}

.main-container {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  transition: margin-left var(--transition-normal);
  min-height: 100vh;
}

.main-container-expanded {
  margin-left: 64px;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background-color: var(--fluent-bg-base);
}

.content-wrapper {
  padding: var(--spacing-lg);
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* Page transition animations */
.page-enter-active,
.page-leave-active {
  transition: opacity var(--transition-page), transform var(--transition-page);
}

.page-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.page-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Mobile responsive */
@media (max-width: 767px) {
  .app-layout {
    flex-direction: column;
  }
  
  .main-container {
    margin-left: 0;
  }
  
  .main-container-expanded {
    margin-left: 0;
  }
  
  .content-wrapper {
    padding: var(--spacing-md);
  }
}

/* Tablet responsive */
@media (min-width: 768px) and (max-width: 1199px) {
  .content-wrapper {
    padding: var(--spacing-md);
  }
}
</style>
