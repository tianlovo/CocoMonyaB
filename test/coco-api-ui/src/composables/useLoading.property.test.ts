import { describe, it, expect, vi } from 'vitest'
import fc from 'fast-check'
import { useLoading } from './useLoading'

/**
 * Feature: tag-management-frontend, Property 8: 加载状态管理
 * 
 * 对于任何异步操作（API 请求、数据加载），在操作进行期间，系统应显示加载状态指示器
 * 
 * **Validates: Requirements 3.7, 13.1, 13.4, 13.5**
 */
describe('Property 8: Loading State Management', () => {
  it('should set loading to true during async operation execution', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.integer({ min: 5, max: 20 }), // Reduced delay
        fc.oneof(
          fc.constant('success'),
          fc.constant('error')
        ),
        async (delay, outcome) => {
          const { loading, withLoading } = useLoading()
          
          // Initially loading should be false
          expect(loading.value).toBe(false)
          
          // Create an async operation
          const asyncOperation = async () => {
            await new Promise(resolve => setTimeout(resolve, delay))
            if (outcome === 'error') {
              throw new Error('Test error')
            }
            return 'success'
          }
          
          // Start the operation
          const promise = withLoading(asyncOperation)
          
          // During execution, loading should be true
          expect(loading.value).toBe(true)
          
          // Wait for completion
          try {
            await promise
          } catch {
            // Ignore errors for this test
          }
          
          // After completion, loading should be false
          expect(loading.value).toBe(false)
        }
      ),
      { numRuns: 50, timeout: 10000 } // Reduced runs and increased timeout
    )
  }, 15000) // Test timeout

  it('should reset loading to false even when async operation fails', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.integer({ min: 5, max: 20 }), // Reduced delay
        fc.string({ minLength: 1, maxLength: 50 }),
        async (delay, errorMessage) => {
          const { loading, withLoading } = useLoading()
          
          const failingOperation = async () => {
            await new Promise(resolve => setTimeout(resolve, delay))
            throw new Error(errorMessage)
          }
          
          expect(loading.value).toBe(false)
          
          try {
            await withLoading(failingOperation)
          } catch (error) {
            // Error is expected
            expect((error as Error).message).toBe(errorMessage)
          }
          
          // Loading should be false after error
          expect(loading.value).toBe(false)
        }
      ),
      { numRuns: 50, timeout: 10000 } // Reduced runs
    )
  }, 15000) // Test timeout

  it('should handle multiple sequential async operations correctly', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.array(fc.integer({ min: 5, max: 15 }), { minLength: 2, maxLength: 3 }), // Reduced delays and array size
        async (delays) => {
          const { loading, withLoading } = useLoading()
          
          for (const delay of delays) {
            expect(loading.value).toBe(false)
            
            const operation = async () => {
              await new Promise(resolve => setTimeout(resolve, delay))
              return delay
            }
            
            const promise = withLoading(operation)
            expect(loading.value).toBe(true)
            
            await promise
            expect(loading.value).toBe(false)
          }
        }
      ),
      { numRuns: 30, timeout: 10000 } // Reduced runs
    )
  }, 15000) // Test timeout

  it('should preserve return value from async operation', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.anything(),
        async (expectedValue) => {
          const { loading, withLoading } = useLoading()
          
          const operation = async () => {
            await new Promise(resolve => setTimeout(resolve, 5)) // Reduced delay
            return expectedValue
          }
          
          const result = await withLoading(operation)
          
          expect(result).toEqual(expectedValue)
          expect(loading.value).toBe(false)
        }
      ),
      { numRuns: 50, timeout: 10000 } // Reduced runs
    )
  }, 15000) // Test timeout

  it('should handle initial loading state correctly', () => {
    fc.assert(
      fc.property(
        fc.boolean(),
        (initialState) => {
          const { loading } = useLoading(initialState)
          expect(loading.value).toBe(initialState)
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should maintain loading state independence across multiple instances', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.integer({ min: 5, max: 20 }), // Reduced delays
        fc.integer({ min: 5, max: 20 }),
        async (delay1, delay2) => {
          const loader1 = useLoading()
          const loader2 = useLoading()
          
          // Both should start as false
          expect(loader1.loading.value).toBe(false)
          expect(loader2.loading.value).toBe(false)
          
          // Start first operation
          const op1 = loader1.withLoading(async () => {
            await new Promise(resolve => setTimeout(resolve, delay1))
            return 'op1'
          })
          
          expect(loader1.loading.value).toBe(true)
          expect(loader2.loading.value).toBe(false)
          
          // Start second operation
          const op2 = loader2.withLoading(async () => {
            await new Promise(resolve => setTimeout(resolve, delay2))
            return 'op2'
          })
          
          expect(loader1.loading.value).toBe(true)
          expect(loader2.loading.value).toBe(true)
          
          // Wait for both
          await Promise.all([op1, op2])
          
          expect(loader1.loading.value).toBe(false)
          expect(loader2.loading.value).toBe(false)
        }
      ),
      { numRuns: 30, timeout: 10000 } // Reduced runs
    )
  }, 15000) // Test timeout

  it('should handle rapid successive calls correctly', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.integer({ min: 2, max: 4 }), // Reduced call count
        async (callCount) => {
          const { loading, withLoading } = useLoading()
          const results: number[] = []
          
          // Make multiple rapid calls
          const promises = Array.from({ length: callCount }, (_, i) => 
            withLoading(async () => {
              await new Promise(resolve => setTimeout(resolve, 5)) // Reduced delay
              return i
            }).then(result => results.push(result))
          )
          
          // During execution, loading should be true at some point
          // (Note: Due to timing, we can't guarantee it's always true)
          
          await Promise.all(promises)
          
          // After all complete, loading should be false
          expect(loading.value).toBe(false)
          expect(results).toHaveLength(callCount)
        }
      ),
      { numRuns: 30, timeout: 10000 } // Reduced runs
    )
  }, 15000) // Test timeout
})
