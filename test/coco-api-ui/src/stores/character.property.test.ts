import { describe, it, expect, beforeEach, vi } from 'vitest'
import fc from 'fast-check'
import { setActivePinia, createPinia } from 'pinia'
import { useCharacterStore } from './character'
import { characterApi } from '@/api/character'
import type { PageResponse, Character } from '@/types/models'

vi.mock('@/api/character')

describe('Character Store - Property-Based Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  /**
   * Feature: tag-management-frontend, Property 3: 搜索关键词传递
   * 
   * Validates: Requirements 6.2
   * 
   * Property: For any search keyword input, when fetching characters through the store,
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
            const mockResponse: PageResponse<Character> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(characterApi.getPage).mockResolvedValue(mockResponse)

            const store = useCharacterStore()
            await store.fetchPage(params)

            // Verify the keyword was passed to the API
            expect(characterApi.getPage).toHaveBeenCalledWith(
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
            const mockResponse: PageResponse<Character> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(characterApi.getPage).mockResolvedValue(mockResponse)

            const store = useCharacterStore()
            await store.fetchPage(params)

            // Verify the API was called without keyword parameter
            const callArgs = vi.mocked(characterApi.getPage).mock.calls[
              vi.mocked(characterApi.getPage).mock.calls.length - 1
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
            const store = useCharacterStore()

            for (const params of paramsList) {
              const mockResponse: PageResponse<Character> = {
                records: [],
                current: params.current,
                size: params.size,
                total: 0,
                pages: 0
              }
              vi.mocked(characterApi.getPage).mockResolvedValue(mockResponse)

              await store.fetchPage(params)

              // Verify each call preserves the keyword parameter correctly
              const lastCall = vi.mocked(characterApi.getPage).mock.calls[
                vi.mocked(characterApi.getPage).mock.calls.length - 1
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
              fc.constant('角色名'),
              fc.constant('人物 (别名)'),
              fc.constant('Character@example'),
              fc.constant('测试-角色_123'),
              fc.constant('Character with 中文')
            )
          }),
          async (params) => {
            const mockResponse: PageResponse<Character> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(characterApi.getPage).mockResolvedValue(mockResponse)

            const store = useCharacterStore()
            await store.fetchPage(params)

            // Verify special characters are preserved
            expect(characterApi.getPage).toHaveBeenCalledWith(
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
            const mockResponse: PageResponse<Character> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(characterApi.getPage).mockResolvedValue(mockResponse)

            const store = useCharacterStore()
            await store.fetchPage(params)

            // Verify empty string is passed as-is
            expect(characterApi.getPage).toHaveBeenCalledWith(
              expect.objectContaining({ keyword: '' })
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should pass keyword along with workId and species filters', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            current: fc.integer({ min: 1, max: 100 }),
            size: fc.integer({ min: 10, max: 100 }),
            keyword: fc.string({ minLength: 1, maxLength: 100 }),
            workId: fc.option(fc.uuid(), { nil: undefined }),
            species: fc.option(fc.constantFrom('人类', '精灵', '兽人', '龙族'), { nil: undefined })
          }),
          async (params) => {
            const mockResponse: PageResponse<Character> = {
              records: [],
              current: params.current,
              size: params.size,
              total: 0,
              pages: 0
            }
            vi.mocked(characterApi.getPage).mockResolvedValue(mockResponse)

            const store = useCharacterStore()
            await store.fetchPage(params)

            // Verify all parameters are passed correctly
            const expectedParams: any = {
              current: params.current,
              size: params.size,
              keyword: params.keyword
            }
            
            if (params.workId !== undefined) {
              expectedParams.workId = params.workId
            }
            
            if (params.species !== undefined) {
              expectedParams.species = params.species
            }

            expect(characterApi.getPage).toHaveBeenCalledWith(
              expect.objectContaining(expectedParams)
            )
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
