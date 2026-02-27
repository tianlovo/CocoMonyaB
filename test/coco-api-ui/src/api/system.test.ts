import { describe, it, expect, vi, beforeEach } from 'vitest'
import { systemApi } from './system'
import request from '@/utils/request'

vi.mock('@/utils/request')

describe('systemApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getStatus', () => {
    it('should call GET /system/status', async () => {
      const mockStatus = {
        ready: true,
        reason: null,
        timestamp: 1708588800000
      }
      
      vi.mocked(request.get).mockResolvedValue(mockStatus)

      const result = await systemApi.getStatus()

      expect(request.get).toHaveBeenCalledWith('/system/status')
      expect(result).toEqual(mockStatus)
    })

    it('should return not ready status', async () => {
      const mockStatus = {
        ready: false,
        reason: '系统正在启动中...',
        timestamp: 1708588800000
      }
      
      vi.mocked(request.get).mockResolvedValue(mockStatus)

      const result = await systemApi.getStatus()

      expect(result.ready).toBe(false)
      expect(result.reason).toBe('系统正在启动中...')
    })
  })

  describe('healthCheck', () => {
    it('should call GET /system/health', async () => {
      const mockResponse = 'OK'
      
      vi.mocked(request.get).mockResolvedValue(mockResponse)

      const result = await systemApi.healthCheck()

      expect(request.get).toHaveBeenCalledWith('/system/health')
      expect(result).toBe('OK')
    })
  })

  describe('getInfo', () => {
    it('should call GET /system/info', async () => {
      const mockInfo = {
        projectName: 'CocoMonyaB',
        version: '1.0.0',
        artifact: 'cocomonyab',
        group: 'org.xlyo',
        description: '测试项目',
        buildTime: '2024-03-20T10:30:00.000Z',
        javaVersion: '21.0.1',
        gradleVersion: '8.5',
        fullVersionInfo: 'CocoMonyaB v1.0.0 (Built: 2024-03-20T10:30:00.000Z, Java: 21.0.1)'
      }
      
      vi.mocked(request.get).mockResolvedValue(mockInfo)

      const result = await systemApi.getInfo()

      expect(request.get).toHaveBeenCalledWith('/system/info')
      expect(result).toEqual(mockInfo)
      expect(result.projectName).toBe('CocoMonyaB')
      expect(result.version).toBe('1.0.0')
    })
  })
})
