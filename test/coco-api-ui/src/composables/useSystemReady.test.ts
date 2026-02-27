import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useSystemReady } from './useSystemReady'
import { systemApi } from '@/api/system'

vi.mock('@/api/system')
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

describe('useSystemReady', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('checkSystemStatus', () => {
    it('should update isReady to true when system is ready', async () => {
      vi.mocked(systemApi.getStatus).mockResolvedValue({
        ready: true,
        reason: null,
        timestamp: 1708588800000
      })

      const { isReady, reason, checkSystemStatus } = useSystemReady()
      
      const result = await checkSystemStatus()

      expect(result).toBe(true)
      expect(isReady.value).toBe(true)
      expect(reason.value).toBeNull()
    })

    it('should update isReady to false when system is not ready', async () => {
      vi.mocked(systemApi.getStatus).mockResolvedValue({
        ready: false,
        reason: '系统正在启动中...',
        timestamp: 1708588800000
      })

      const { isReady, reason, checkSystemStatus } = useSystemReady()
      
      const result = await checkSystemStatus()

      expect(result).toBe(false)
      expect(isReady.value).toBe(false)
      expect(reason.value).toBe('系统正在启动中...')
    })

    it('should handle API errors gracefully', async () => {
      vi.mocked(systemApi.getStatus).mockRejectedValue(new Error('Network error'))

      const { isReady, reason, checkSystemStatus } = useSystemReady()
      
      const result = await checkSystemStatus()

      expect(result).toBe(false)
      expect(isReady.value).toBe(false)
      expect(reason.value).toBe('无法连接到服务器')
    })
  })

  describe('waitForSystemReady', () => {
    it('should return true immediately if system is ready', async () => {
      vi.mocked(systemApi.getStatus).mockResolvedValue({
        ready: true,
        reason: null,
        timestamp: 1708588800000
      })

      const { isChecking, waitForSystemReady } = useSystemReady()
      
      const result = await waitForSystemReady(3, 100)

      expect(result).toBe(true)
      expect(isChecking.value).toBe(false)
      expect(systemApi.getStatus).toHaveBeenCalledTimes(1)
    })

    it('should retry until system is ready', async () => {
      vi.mocked(systemApi.getStatus)
        .mockResolvedValueOnce({
          ready: false,
          reason: '启动中...',
          timestamp: 1708588800000
        })
        .mockResolvedValueOnce({
          ready: false,
          reason: '启动中...',
          timestamp: 1708588801000
        })
        .mockResolvedValueOnce({
          ready: true,
          reason: null,
          timestamp: 1708588802000
        })

      const { waitForSystemReady } = useSystemReady()
      
      const result = await waitForSystemReady(5, 10)

      expect(result).toBe(true)
      expect(systemApi.getStatus).toHaveBeenCalledTimes(3)
    })

    it('should return false after max retries', async () => {
      vi.mocked(systemApi.getStatus).mockResolvedValue({
        ready: false,
        reason: '启动中...',
        timestamp: 1708588800000
      })

      const { waitForSystemReady } = useSystemReady()
      
      const result = await waitForSystemReady(3, 10)

      expect(result).toBe(false)
      expect(systemApi.getStatus).toHaveBeenCalledTimes(3)
    })
  })
})
