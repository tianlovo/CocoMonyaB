import request from '@/utils/request'

export interface ServerConfig {
  server: {
    javaBackendUrl: string
  }
  bark: {
    enabled: boolean
    key: string
    server?: string
  }
  monitor: {
    javaOfflineCheck: {
      enabled: boolean
      intervalMinutes: number
    }
    tgLoginCheck: {
      enabled: boolean
      intervalMinutes: number
      maxFailures: number
    }
  }
}

export interface MonitorStatus {
  javaBackend: {
    isOnline: boolean
    lastCheck: string
    offlineSince?: string
    notified: boolean
  }
  tgLogin: {
    isValid: boolean
    lastCheck: string
    consecutiveFailures: number
    notified: boolean
  }
}

export interface JavaConnectionTestResult {
  connected: boolean
  status: number
  responseTime: string
  message?: string
  error?: string
}

// 固定端口常量（与后端保持一致）
export const FIXED_PORT = 15088

export const serverConfigApi = {
  // 获取服务器配置
  getConfig() {
    return request.get<any, ServerConfig>('/config')
  },

  // 更新服务器配置
  updateConfig(config: Partial<ServerConfig>) {
    return request.put<any, void>('/config', config)
  },

  // 测试Bark通知
  testBark() {
    return request.post<any, void>('/config/bark/test')
  },

  // 测试Java后端连接
  testJavaConnection(javaBackendUrl: string) {
    return request.post<any, JavaConnectionTestResult>('/config/test-java-connection', {
      javaBackendUrl
    })
  },

  // 获取监控状态
  getMonitorStatus() {
    return request.get<any, MonitorStatus>('/monitor/status')
  },

  // 重启监控服务
  restartMonitor() {
    return request.post<any, void>('/monitor/restart')
  }
}
