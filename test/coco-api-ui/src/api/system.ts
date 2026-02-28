import request from '@/utils/request'
import type { SystemStatus, SystemInfo } from '@/types/api'

export const systemApi = {
  // Get system status - response is directly the data object, not wrapped in data field
  getStatus() {
    return request.get<any, SystemStatus>('/system/status', {
      transformResponse: [(data) => {
        // Parse JSON if needed
        if (typeof data === 'string') {
          try {
            return JSON.parse(data)
          } catch {
            return data
          }
        }
        return data
      }]
    })
  },

  // Health check
  healthCheck() {
    return request.get<any, string>('/system/health')
  },

  // Get system version info
  getInfo() {
    return request.get<any, SystemInfo>('/system/info')
  }
}
