// 配置类型定义
export interface ServerConfig {
  // 服务器配置
  server: {
    port: number;
    javaBackendUrl: string;
    frontendToken: string;
  };
  // Bark通知配置
  bark: {
    enabled: boolean;
    key: string;
    server?: string;
  };
  // 监控配置
  monitor: {
    javaOfflineCheck: {
      enabled: boolean;
      intervalMinutes: number;
    };
    tgLoginCheck: {
      enabled: boolean;
      intervalMinutes: number;
      maxFailures: number;
    };
  };
}

// API响应类型
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T | null;
}

export interface PageResponse<T> {
  records: T[];
  current: number;
  size: number;
  total: number;
  pages: number;
}

// 系统状态类型
export interface SystemStatus {
  ready: boolean;
  status: string;
  reason: string | null;
  timestamp: number;
  progress: number;
  currentPhase: string;
}

// 频道查询参数
export interface TgChannelQueryParams {
  current?: number;
  size?: number;
  forceRefresh?: boolean;
}

// 监控状态
export interface MonitorStatus {
  javaBackend: {
    isOnline: boolean;
    lastCheck: string;
    offlineSince?: string;
    notified: boolean;
  };
  tgLogin: {
    isValid: boolean;
    lastCheck: string;
    consecutiveFailures: number;
    notified: boolean;
  };
}
