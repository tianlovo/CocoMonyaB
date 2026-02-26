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
  baseURL: 'http://127.0.0.1:10721',
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
      return data
    }

    // Error response - preserve error details for special handling
    // Don't show message for reference errors (-60004) as they need custom handling
    if (code !== -60004) {
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

    // HTTP error
    ElMessage.error('服务器错误，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
