import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'
import { ElMessage } from 'element-plus'

export function useSystemReady() {
  const isReady = ref(false)
  const isChecking = ref(false)
  const reason = ref<string | null>(null)
  const status = ref<string>('NOT_STARTED')
  const progress = ref(0)
  const currentPhase = ref<string>('')

  const checkSystemStatus = async (): Promise<boolean> => {
    try {
      const res = await systemApi.getStatus()
      isReady.value = res.ready
      reason.value = res.reason
      status.value = res.status
      progress.value = res.progress
      currentPhase.value = res.currentPhase
      return res.ready
    } catch (error) {
      console.error('Failed to check system status:', error)
      isReady.value = false
      reason.value = '无法连接到服务器'
      status.value = 'NOT_STARTED'
      progress.value = 0
      currentPhase.value = ''
      return false
    }
  }

  const waitForSystemReady = async (maxRetries = 30, retryInterval = 2000): Promise<boolean> => {
    isChecking.value = true
    let retries = 0

    while (retries < maxRetries) {
      const ready = await checkSystemStatus()
      
      if (ready) {
        isChecking.value = false
        return true
      }

      retries++
      if (retries < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, retryInterval))
      }
    }

    isChecking.value = false
    ElMessage.error('系统启动超时，请检查后端服务')
    return false
  }

  onMounted(async () => {
    await checkSystemStatus()
  })

  return {
    isReady,
    isChecking,
    reason,
    status,
    progress,
    currentPhase,
    checkSystemStatus,
    waitForSystemReady
  }
}
