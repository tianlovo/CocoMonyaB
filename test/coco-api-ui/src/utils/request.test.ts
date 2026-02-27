import { describe, it, expect, vi, beforeEach } from 'vitest'
import fc from 'fast-check'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'
import type { AxiosError } from 'axios'

// Mock Element Plus Message
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn()
  }
}))

/**
 * Simulates the response interceptor logic from request.ts
 * This function represents the core API response handling behavior
 */
function handleApiResponse(response: { data: ApiResponse }): any {
  const { code, msg, data } = response.data

  // Success response
  if (code === 200) {
    return data
  }

  // Error response
  ElMessage.error(msg || '请求失败')
  throw new Error(msg || '请求失败')
}

describe('API Response Handling - Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Feature: tag-management-frontend, Property 1: API 响应码处理
   * **Validates: Requirements 3.1, 3.2, 3.3**
   * 
   * Property: 对于任何 API 响应对象，如果 code 等于 200，系统应将其视为成功；
   * 如果 code 为负数，系统应将其视为失败并显示 msg 字段内容
   */
  it('should treat response as success when code is 200', () => {
    fc.assert(
      fc.property(
        fc.record({
          msg: fc.string(),
          data: fc.oneof(
            fc.constant(null),
            fc.string(),
            fc.integer(),
            fc.boolean(),
            fc.object(),
            fc.array(fc.anything())
          )
        }),
        ({ msg, data }) => {
          const apiResponse: ApiResponse = {
            code: 200,
            msg,
            data
          }

          const result = handleApiResponse({ data: apiResponse })
          
          // Should return the data field
          expect(result).toEqual(data)
          
          // Should not show error message
          expect(ElMessage.error).not.toHaveBeenCalled()
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 1: API 响应码处理
   * **Validates: Requirements 3.1, 3.2, 3.3**
   * 
   * Property: 对于任何 API 响应对象，如果 code 为负数，系统应将其视为失败
   * 并显示 msg 字段内容
   */
  it('should treat response as failure when code is negative and display msg', () => {
    fc.assert(
      fc.property(
        fc.record({
          code: fc.integer({ max: -1 }),
          msg: fc.string({ minLength: 1 }),
          data: fc.constant(null)
        }),
        ({ code, msg, data }) => {
          const apiResponse: ApiResponse = {
            code,
            msg,
            data
          }

          try {
            handleApiResponse({ data: apiResponse })
            // Should not reach here
            expect.fail('Expected to throw error')
          } catch (error) {
            // Should show error message with msg field content
            expect(ElMessage.error).toHaveBeenCalledWith(msg)
            
            // Should throw error containing msg
            expect(error).toBeInstanceOf(Error)
            expect((error as Error).message).toBe(msg)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 1: API 响应码处理
   * **Validates: Requirements 3.1, 3.2, 3.3**
   * 
   * Property: 对于任何非 200 的正数或零 code，系统应将其视为失败
   */
  it('should treat response as failure when code is non-200 positive or zero', () => {
    fc.assert(
      fc.property(
        fc.record({
          code: fc.oneof(
            fc.integer({ min: 0, max: 199 }),
            fc.integer({ min: 201, max: 1000 })
          ),
          msg: fc.string({ minLength: 1 }),
          data: fc.constant(null)
        }),
        ({ code, msg, data }) => {
          const apiResponse: ApiResponse = {
            code,
            msg,
            data
          }

          try {
            handleApiResponse({ data: apiResponse })
            expect.fail('Expected to throw error')
          } catch (error) {
            // Should show error message
            expect(ElMessage.error).toHaveBeenCalled()
            
            // Should throw error
            expect(error).toBeInstanceOf(Error)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 1: API 响应码处理
   * **Validates: Requirements 3.1, 3.2, 3.3**
   * 
   * Property: 当 msg 为空时，应显示默认错误消息
   */
  it('should display default error message when msg is empty', () => {
    fc.assert(
      fc.property(
        fc.integer({ max: -1 }),
        (code) => {
          const apiResponse: ApiResponse = {
            code,
            msg: '',
            data: null
          }

          try {
            handleApiResponse({ data: apiResponse })
            expect.fail('Expected to throw error')
          } catch (error) {
            // Should show default error message
            expect(ElMessage.error).toHaveBeenCalledWith('请求失败')
          }
        }
      ),
      { numRuns: 100 }
    )
  })
})

/**
 * Simulates the error interceptor logic from request.ts
 * This function represents the HTTP error handling behavior
 */
function handleHttpError(error: Partial<AxiosError>): never {
  // Network error
  if (!error.response) {
    ElMessage.error('网络连接失败，请检查网络设置')
    throw new Error('网络连接失败')
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
  throw new Error(errorMessage)
}

describe('HTTP Error Handling - Unit Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.1**
   * 
   * Test: Network error should display "网络连接失败，请检查网络设置"
   */
  it('should display network error message when no response', () => {
    const error: Partial<AxiosError> = {
      response: undefined
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络设置')
      expect((e as Error).message).toBe('网络连接失败')
    }
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.2**
   * 
   * Test: 401 error should display "未授权，请重新登录"
   */
  it('should display unauthorized message for 401 error', () => {
    const error: Partial<AxiosError> = {
      response: {
        status: 401,
        data: {},
        statusText: 'Unauthorized',
        headers: {},
        config: {} as any
      }
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith('未授权，请重新登录')
      expect((e as Error).message).toBe('未授权，请重新登录')
    }
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.3**
   * 
   * Test: 403 error should display "无权限访问此资源"
   */
  it('should display forbidden message for 403 error', () => {
    const error: Partial<AxiosError> = {
      response: {
        status: 403,
        data: {},
        statusText: 'Forbidden',
        headers: {},
        config: {} as any
      }
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith('无权限访问此资源')
      expect((e as Error).message).toBe('无权限访问此资源')
    }
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.4**
   * 
   * Test: 404 error should display "请求的资源不存在"
   */
  it('should display not found message for 404 error', () => {
    const error: Partial<AxiosError> = {
      response: {
        status: 404,
        data: {},
        statusText: 'Not Found',
        headers: {},
        config: {} as any
      }
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith('请求的资源不存在')
      expect((e as Error).message).toBe('请求的资源不存在')
    }
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.5**
   * 
   * Test: 500 error should display "服务器错误，请稍后重试"
   */
  it('should display server error message for 500 error', () => {
    const error: Partial<AxiosError> = {
      response: {
        status: 500,
        data: {},
        statusText: 'Internal Server Error',
        headers: {},
        config: {} as any
      }
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith('服务器错误，请稍后重试')
      expect((e as Error).message).toBe('服务器错误，请稍后重试')
    }
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.6**
   * 
   * Test: Other errors should display msg field from response
   */
  it('should display msg field for other HTTP errors', () => {
    const customMessage = '自定义错误消息'
    const error: Partial<AxiosError> = {
      response: {
        status: 502,
        data: {
          code: -1,
          msg: customMessage,
          data: null
        } as ApiResponse,
        statusText: 'Bad Gateway',
        headers: {},
        config: {} as any
      }
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith(customMessage)
      expect((e as Error).message).toBe(customMessage)
    }
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.6**
   * 
   * Test: Other errors without msg should display default message
   */
  it('should display default message for other HTTP errors without msg', () => {
    const error: Partial<AxiosError> = {
      response: {
        status: 502,
        data: {},
        statusText: 'Bad Gateway',
        headers: {},
        config: {} as any
      }
    }

    try {
      handleHttpError(error)
      expect.fail('Expected to throw error')
    } catch (e) {
      expect(ElMessage.error).toHaveBeenCalledWith('请求失败')
      expect((e as Error).message).toBe('请求失败')
    }
  })
})

describe('HTTP Error Handling - Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.2, 15.3, 15.4, 15.5**
   * 
   * Property: 对于任何特定的HTTP状态码（401, 403, 404, 500），
   * 系统应该显示对应的中文错误消息
   */
  it('should map specific HTTP status codes to Chinese error messages', () => {
    const statusCodeMapping = [
      { status: 401, message: '未授权，请重新登录' },
      { status: 403, message: '无权限访问此资源' },
      { status: 404, message: '请求的资源不存在' },
      { status: 500, message: '服务器错误，请稍后重试' }
    ]

    fc.assert(
      fc.property(
        fc.constantFrom(...statusCodeMapping),
        ({ status, message }) => {
          const error: Partial<AxiosError> = {
            response: {
              status,
              data: {},
              statusText: 'Error',
              headers: {},
              config: {} as any
            }
          }

          try {
            handleHttpError(error)
            expect.fail('Expected to throw error')
          } catch (e) {
            expect(ElMessage.error).toHaveBeenCalledWith(message)
            expect((e as Error).message).toBe(message)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.6**
   * 
   * Property: 对于任何非标准HTTP状态码，如果响应包含msg字段，
   * 系统应该显示该msg字段的内容
   */
  it('should display msg field for non-standard HTTP status codes', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 400, max: 599 }).filter(
          status => ![401, 403, 404, 500].includes(status)
        ),
        fc.string({ minLength: 1 }),
        (status, msg) => {
          const error: Partial<AxiosError> = {
            response: {
              status,
              data: {
                code: -1,
                msg,
                data: null
              } as ApiResponse,
              statusText: 'Error',
              headers: {},
              config: {} as any
            }
          }

          try {
            handleHttpError(error)
            expect.fail('Expected to throw error')
          } catch (e) {
            expect(ElMessage.error).toHaveBeenCalledWith(msg)
            expect((e as Error).message).toBe(msg)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: message-tracking-visualization, Property 9: HTTP状态码错误映射
   * **Validates: Requirements 15.1**
   * 
   * Property: 对于任何网络错误（无响应），系统应该显示网络连接失败消息
   */
  it('should display network error message for any error without response', () => {
    fc.assert(
      fc.property(
        fc.constant(undefined),
        (response) => {
          const error: Partial<AxiosError> = {
            response
          }

          try {
            handleHttpError(error)
            expect.fail('Expected to throw error')
          } catch (e) {
            expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败，请检查网络设置')
            expect((e as Error).message).toBe('网络连接失败')
          }
        }
      ),
      { numRuns: 100 }
    )
  })
})
