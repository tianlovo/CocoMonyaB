import { describe, it, expect, beforeEach, vi } from 'vitest'
import fc from 'fast-check'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthorStore } from './author'
import { authorApi } from '@/api/author'
import type { PageResponse, Author } from '@/types/models'

vi.mock('@/api/author')

describe('Author Store - Property-Based Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  /**
   * Feature: tag-management-frontend, Property 3: 搜索关键词传递
   * 
   * Validates: Requirements 4.2
   * 
   * Property: For any search keyword input, when fetching authors through the store,
   * the system should pass the keyword parameter to the API service.
   */
  describe('Property 3: 搜索关键词传递', () => {
    it('should pass keyword parameter to API when fetching page with keyword', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.string({ minLength: 1, maxLength: 100 })
          }),
          async (params) => {
            const mockResponse: PageResponse<Author> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(authorApi.getPage).mockResolvedValue(mockResponse)

            const store = useAuthorStore()
            await store.fetchPage(params)

            // Verify the keyword was passed to the API
            expect(authorApi.getPage).toHaveBeenCalledWith(
              expect.objectContaining({
                current: params.current,
                size: params.size,
                keyword: params.keyword
              })
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should pass undefined keyword when keyword is not provided', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 })
          }),
          async (params) => {
            const mockResponse: PageResponse<Author> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(authorApi.getPage).mockResolvedValue(mockResponse)

            const store = useAuthorStore()
            await store.fetchPage(params)

            // Verify the API was called without keyword parameter
            const callArgs = vi.mocked(authorApi.getPage).mock.calls[
              vi.mocked(authorApi.getPage).mock.calls.length - 1
            ][0]
            expect(callArgs).toEqual(params)
            expect(callArgs).not.toHaveProperty('keyword')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should preserve keyword parameter through multiple fetch operations', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.array(
            fc.record({
              current: fc.integer({ min: 1, max: 100 }),
              size: fc.integer({ min: 10, max: 100 }),
              keyword: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined })
            }),
            { minLength: 1, maxLength: 5 }
          ),
          async (paramsList) => {
            const store = useAuthorStore()

            for (const params of paramsList) {
              const mockResponse: PageResponse<Author> = {
                records: [],
                current: params.current,
                size: params.size,
                total: 0,
                pages: 0
              }
              vi.mocked(authorApi.getPage).mockResolvedValue(mockResponse)

              await store.fetchPage(params)

              // Verify each call preserves the keyword parameter correctly
              const lastCall = vi.mocked(authorApi.getPage).mock.calls[
                vi.mocked(authorApi.getPage).mock.calls.length - 1
              ][0]

              // Check that the params match (keyword may be present as undefined)
              expect(lastCall.current).toBe(params.current)
              expect(lastCall.size).toBe(params.size)
              
              if (params.keyword !== undefined) {
                expect(lastCall.keyword).toBe(params.keyword)
              }
              // Note: when keyword is undefined, it may still be present as a property
              // This is acceptable behavior as the API will ignore undefined values
            }
          }
        ),
        { numRuns: 50 }
      )
    })

    it('should handle Chinese characters and special characters in keyword', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.oneof(
              fc.constant('张三'),
              fc.constant('李四 (别名)'),
              fc.constant('作者@example'),
              fc.constant('测试-作者_123'),
              fc.constant('Author with 中文')
            )
          }),
          async (params) => {
            const mockResponse: PageResponse<Author> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(authorApi.getPage).mockResolvedValue(mockResponse)

            const store = useAuthorStore()
            await store.fetchPage(params)

            // Verify special characters are preserved
            expect(authorApi.getPage).toHaveBeenCalledWith(
              expect.objectContaining({ keyword: params.keyword })
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should pass empty string keyword when explicitly provided', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.constant('')
          }),
          async (params) => {
            const mockResponse: PageResponse<Author> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(authorApi.getPage).mockResolvedValue(mockResponse)

            const store = useAuthorStore()
            await store.fetchPage(params)

            // Verify empty string is passed as-is
            expect(authorApi.getPage).toHaveBeenCalledWith(
              expect.objectContaining({ keyword: '' })
            )
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
