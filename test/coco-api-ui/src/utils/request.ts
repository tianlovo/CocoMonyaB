import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'

// Custom API Error class to preserve error details
export class ApiError extends Error {
  code: number
  data: any

  constructor(code: number, message: string, data?: any) {
    super(message)
    this.code = code
    this.data = data
    this.name = 'ApiError'
  }
}

const request = axios.create({
  baseURL: 'http://127.0.0.1:10721/api',
  timeout: 10000
})

// Request interceptor
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  (response) => {
    const apiResponse = response.data as ApiResponse
    const { code, msg, data } = apiResponse

    // Success response
    if (code === 200) {
      // Parse data if it's a JSON string (backend sometimes returns stringified JSON)
      if (typeof data === 'string' && data.length > 0) {
        try {
          return JSON.parse(data)
        } catch {
          // If parsing fails, return as is
          return data
        }
      }
      return data
    }

    // Error response - preserve error details for special handling
    // Don't show message for reference errors (-60004) and uniqueness conflicts (-60003)
    // as they need custom handling in the UI
    if (code !== -60004 && code !== -60003) {
      ElMessage.error(msg || '请求失败')
    }
    return Promise.reject(new ApiError(code, msg || '请求失败', data))
  },
  (error: AxiosError) => {
    // Network error
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络设置')
      return Promise.reject(new Error('网络连接失败'))
    }

    // HTTP status code error handling
    const status = error.response.status
    const apiResponse = error.response.data as ApiResponse | undefined
    
    let errorMessage: string
    
    switch (status) {
      case 401:
        errorMessage = '未授权，请重新登录'
        break
      case 403:
        errorMessage = '无权限访问此资源'
        break
      case 404:
        errorMessage = '请求的资源不存在'
        break
      case 500:
        errorMessage = '服务器错误，请稍后重试'
        break
      default:
        // Use msg field from response for other errors
        errorMessage = apiResponse?.msg || '请求失败'
    }
    
    ElMessage.error(errorMessage)
    return Promise.reject(new Error(errorMessage))
  }
)

export default request
