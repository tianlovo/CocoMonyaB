<template>
  <div v-if="!isSystemReady" class="system-loading">
    <div class="loading-content">
      <el-icon class="loading-icon" :size="48">
        <Loading />
      </el-icon>
      <h2>系统启动中...</h2>
      <p v-if="systemReason">{{ systemReason }}</p>
      <p class="loading-hint">请稍候，正在初始化系统组件</p>
    </div>
  </div>
  <router-view v-else v-slot="{ Component }">
    <keep-alive :include="['AuthorView', 'WorkView', 'CharacterView']">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSystemReady } from '@/composables/useSystemReady'
import { Loading } from '@element-plus/icons-vue'

const { isReady, reason, waitForSystemReady } = useSystemReady()
const isSystemReady = ref(false)
const systemReason = ref<string | null>(null)

onMounted(async () => {
  // Wait for system to be ready before showing the app
  const ready = await waitForSystemReady()
  isSystemReady.value = ready
  systemReason.value = reason.value
})
</script>

<style scoped>
#app {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
}

.system-loading {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.loading-content {
  text-align: center;
  color: white;
  padding: 2rem;
}

.loading-icon {
  animation: rotate 2s linear infinite;
  margin-bottom: 1rem;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-content h2 {
  font-size: 1.5rem;
  margin: 1rem 0;
  font-weight: 500;
}

.loading-content p {
  font-size: 1rem;
  margin: 0.5rem 0;
  opacity: 0.9;
}

.loading-hint {
  font-size: 0.875rem;
  opacity: 0.7;
}
</style>
