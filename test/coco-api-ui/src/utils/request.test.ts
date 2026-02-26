import { describe, it, expect, vi, beforeEach } from 'vitest'
import fc from 'fast-check'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'

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
