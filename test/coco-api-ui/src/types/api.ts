// API Response types
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T | null
}

export interface PageResponse<T> {
  records: T[]
  current: number
  size: number
  total: number
  pages: number
}

export interface PageParams {
  current: number
  size: number
}

export interface ImportResult {
  successCount: number
  failureCount: number
  errors: Array<{
    index: number
    name: string
    error: string
  }>
}

export interface SystemStatus {
  ready: boolean
  status: string
  reason: string | null
  timestamp: number
  progress: number
  currentPhase: string
}

export interface SystemInfo {
  projectName: string
  version: string
  group: string
  description: string
  buildTime: string
  javaVersion: string
  gradleVersion: string
  fullVersionInfo: string
}
