import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'
import { ElMessage } from 'element-plus'

export function useSystemReady() {
  const isReady = ref(false)
  const isChecking = ref(false)
  const reason = ref<string | null>(null)

  const checkSystemStatus = async (): Promise<boolean> => {
    try {
      const status = await systemApi.getStatus()
      isReady.value = status.ready
      reason.value = status.reason
      return status.ready
    } catch (error) {
      console.error('Failed to check system status:', error)
      isReady.value = false
      reason.value = '无法连接到服务器'
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
    checkSystemStatus,
    waitForSystemReady
  }
}
