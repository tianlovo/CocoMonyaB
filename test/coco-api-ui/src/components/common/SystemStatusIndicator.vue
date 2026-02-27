<template>
  <el-tooltip :content="tooltipContent" placement="bottom">
    <el-tag :type="statusType" size="small" class="status-indicator">
      <el-icon class="status-icon">
        <component :is="statusIcon" />
      </el-icon>
      {{ statusText }}
    </el-tag>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { CircleCheck, CircleClose, Loading } from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'

const props = defineProps<{
  autoRefresh?: boolean
  refreshInterval?: number
}>()

const isReady = ref(false)
const isChecking = ref(true)
const reason = ref<string | null>(null)
let intervalId: number | null = null

const statusType = computed(() => {
  if (isChecking.value) return 'info'
  return isReady.value ? 'success' : 'danger'
})

const statusIcon = computed(() => {
  if (isChecking.value) return Loading
  return isReady.value ? CircleCheck : CircleClose
})

const statusText = computed(() => {
  if (isChecking.value) return '检查中'
  return isReady.value ? '系统就绪' : '系统未就绪'
})

const tooltipContent = computed(() => {
  if (isChecking.value) return '正在检查系统状态...'
  if (isReady.value) return '系统已就绪，所有服务正常运行'
  return reason.value || '系统未就绪，请稍候'
})

const checkStatus = async () => {
  try {
    isChecking.value = true
    const status = await systemApi.getStatus()
    isReady.value = status.ready
    reason.value = status.reason
  } catch (error) {
    isReady.value = false
    reason.value = '无法连接到服务器'
  } finally {
    isChecking.value = false
  }
}

onMounted(async () => {
  await checkStatus()
  
  if (props.autoRefresh) {
    const interval = props.refreshInterval || 30000 // Default 30 seconds
    intervalId = window.setInterval(checkStatus, interval)
  }
})

onUnmounted(() => {
  if (intervalId !== null) {
    clearInterval(intervalId)
  }
})
</script>

<style scoped>
.status-indicator {
  cursor: pointer;
  user-select: none;
}

.status-icon {
  margin-right: 4px;
  vertical-align: middle;
}
</style>
