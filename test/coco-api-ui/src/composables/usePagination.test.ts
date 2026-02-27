import { describe, it, expect } from 'vitest'
import fc from 'fast-check'
import { usePagination } from './usePagination'

describe('Pagination - Property Tests', () => {
  /**
   * Feature: tag-management-frontend, Property 2: 分页参数传递
   * **Validates: Requirements 4.1, 9.5**
   * 
   * Property: 对于任何分页查询操作，当用户改变页码或每页大小时，
   * 系统应使用新的分页参数重新请求数据
   */
  it('should update pagination state when page changes', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 10, max: 100 }), // initialSize
        fc.integer({ min: 1, max: 100 }),  // newPage
        (initialSize, newPage) => {
          const { pagination, handlePageChange } = usePagination(initialSize)
          
          // Initial state
          expect(pagination.current).toBe(1)
          expect(pagination.size).toBe(initialSize)
          
          // Change page
          handlePageChange(newPage)
          
          // Should update current page
          expect(pagination.current).toBe(newPage)
          // Size should remain unchanged
          expect(pagination.size).toBe(initialSize)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 2: 分页参数传递
   * **Validates: Requirements 4.1, 9.5**
   * 
   * Property: 当用户改变每页大小时，系统应更新 size 参数并重置页码为 1
   */
  it('should update size and reset page to 1 when size changes', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 10, max: 100 }), // initialSize
        fc.integer({ min: 10, max: 100 }), // newSize
        fc.integer({ min: 2, max: 10 }),   // currentPage (not 1)
        (initialSize, newSize, currentPage) => {
          const { pagination, handlePageChange, handleSizeChange } = usePagination(initialSize)
          
          // Set to a non-first page
          handlePageChange(currentPage)
          expect(pagination.current).toBe(currentPage)
          
          // Change size
          handleSizeChange(newSize)
          
          // Should update size
          expect(pagination.size).toBe(newSize)
          // Should reset to page 1
          expect(pagination.current).toBe(1)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 2: 分页参数传递
   * **Validates: Requirements 9.1, 9.2, 9.3, 9.4**
   * 
   * Property: reset 操作应将分页状态重置为初始值
   */
  it('should reset pagination state to initial values', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 10, max: 100 }), // initialSize
        fc.integer({ min: 2, max: 100 }),  // somePage
        fc.integer({ min: 1, max: 1000 }), // someTotal
        (initialSize, somePage, someTotal) => {
          const { pagination, handlePageChange, reset } = usePagination(initialSize)
          
          // Modify state
          handlePageChange(somePage)
          pagination.total = someTotal
          
          expect(pagination.current).toBe(somePage)
          expect(pagination.total).toBe(someTotal)
          
          // Reset
          reset()
          
          // Should reset to initial values
          expect(pagination.current).toBe(1)
          expect(pagination.total).toBe(0)
          // Size should remain unchanged
          expect(pagination.size).toBe(initialSize)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 2: 分页参数传递
   * **Validates: Requirements 9.1, 9.2**
   * 
   * Property: 分页状态应始终保持有效值（正整数）
   */
  it('should maintain valid pagination values', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 100 }),  // initialSize
        fc.integer({ min: 1, max: 1000 }), // page
        fc.integer({ min: 1, max: 100 }),  // size
        (initialSize, page, size) => {
          const { pagination, handlePageChange, handleSizeChange } = usePagination(initialSize)
          
          handlePageChange(page)
          handleSizeChange(size)
          
          // All values should be positive
          expect(pagination.current).toBeGreaterThan(0)
          expect(pagination.size).toBeGreaterThan(0)
          expect(pagination.total).toBeGreaterThanOrEqual(0)
          
          // Values should match what was set
          expect(pagination.current).toBe(1) // Reset to 1 after size change
          expect(pagination.size).toBe(size)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Feature: tag-management-frontend, Property 2: 分页参数传递
   * **Validates: Requirements 9.4**
   * 
   * Property: 支持的每页大小选项应包含 10、20、50、100
   */
  it('should support standard page size options', () => {
    const standardSizes = [10, 20, 50, 100]
    
    standardSizes.forEach(size => {
      const { pagination, handleSizeChange } = usePagination()
      
      handleSizeChange(size)
      
      expect(pagination.size).toBe(size)
      expect(pagination.current).toBe(1)
    })
  })
})
