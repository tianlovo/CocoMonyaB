import { describe, it, expect, vi, beforeEach } from 'vitest'
import fc from 'fast-check'
import { authorApi } from './author'
import request from '@/utils/request'

// Mock the request module
vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Author API - Property-Based Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Feature: tag-management-frontend, Property 3: 搜索关键词传递
   * 
   * Validates: Requirements 4.2
   * 
   * Property: For any search keyword input, when a user searches for authors,
   * the system should pass the keyword as a query parameter to the API.
   */
  describe('Property 3: 搜索关键词传递', () => {
    it('should pass keyword as query parameter when keyword is provided', () => {
      fc.assert(
        fc.property(
          // Generate arbitrary pagination params
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.string({ minLength: 1, maxLength: 100 })
          }),
          (params) => {
            // Mock the response
            const mockResponse = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(request.get).mockResolvedValue(mockResponse)

            // Call the API
            authorApi.getPage(params)

            // Verify the keyword was passed as a query parameter
            expect(request.get).toHaveBeenCalledWith(
              '/config/tag/author/page',
              { params: expect.objectContaining({ keyword: params.keyword }) }
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should pass keyword parameter even when keyword is empty string', () => {
      fc.assert(
        fc.property(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.constant('')
          }),
          (params) => {
            const mockResponse = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(request.get).mockResolvedValue(mockResponse)

            authorApi.getPage(params)

            // Verify the keyword parameter is included even when empty
            expect(request.get).toHaveBeenCalledWith(
              '/config/tag/author/page',
              { params: expect.objectContaining({ keyword: '' }) }
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should not include keyword parameter when keyword is undefined', () => {
      fc.assert(
        fc.property(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 })
          }),
          (params) => {
            const mockResponse = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(request.get).mockResolvedValue(mockResponse)

            authorApi.getPage(params)

            // Verify the keyword parameter is not included when undefined
            const callArgs = vi.mocked(request.get).mock.calls[
              vi.mocked(request.get).mock.calls.length - 1
            ]
            expect(callArgs[1].params).toEqual(params)
            expect(callArgs[1].params).not.toHaveProperty('keyword')
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should preserve keyword parameter alongside pagination parameters', () => {
      fc.assert(
        fc.property(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.string({ minLength: 1, maxLength: 100 })
          }),
          (params) => {
            const mockResponse = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(request.get).mockResolvedValue(mockResponse)

            authorApi.getPage(params)

            // Verify all parameters are passed correctly
            expect(request.get).toHaveBeenCalledWith(
              '/config/tag/author/page',
              {
                params: {
                  current: params.current,
                  size: params.size,
                  keyword: params.keyword
                }
              }
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should handle special characters in keyword parameter', () => {
      fc.assert(
        fc.property(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.oneof(
              fc.constant('测试作者'),
              fc.constant('author@example.com'),
              fc.constant('作者 with spaces'),
              fc.constant('author-with-dash'),
              fc.constant('author_with_underscore'),
              fc.constant('作者123'),
              fc.constant('!@#$%^&*()')
            )
          }),
          (params) => {
            const mockResponse = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(request.get).mockResolvedValue(mockResponse)

            authorApi.getPage(params)

            // Verify special characters are preserved in the keyword
            expect(request.get).toHaveBeenCalledWith(
              '/config/tag/author/page',
              { params: expect.objectContaining({ keyword: params.keyword }) }
            )
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
