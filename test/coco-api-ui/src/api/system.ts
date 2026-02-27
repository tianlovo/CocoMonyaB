import request from '@/utils/request'
import type { SystemStatus } from '@/types/api'

export const systemApi = {
  // Get system status
  getStatus() {
    return request.get<any, SystemStatus>('/system/status')
  },

  // Health check
  healthCheck() {
    return request.get<any, string>('/system/health')
  }
}
