import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import fc from 'fast-check'
import MessageQueryTab from './MessageQueryTab.vue'
import { messageApi } from '@/api/message'
import type { PageResponse, Message } from '@/types/models'

/**
 * Property Tests: Loading States
 * 
 * These tests verify that loading states are correctly displayed across various scenarios
 */

// Mock the API
vi.mock('@/api/message', () => ({
  messageApi: {
    fetchMessages: vi.fn()
  }
}))

// Mock Element Plus components
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

// Helper function to create wrapper with common stubs
const createWrapper = () => {
  return mount(MessageQueryTab, {
    global: {
      stubs: {
        'el-input': true,
        'el-date-picker': true,
        'el-button': true,
        'el-tooltip': true,
        'DataTable': {
          name: 'DataTable',
          template: '<div class="data-table-stub"></div>',
          props: ['data', 'columns', 'loading', 'pagination', 'emptyType', 'emptyMessage']
        }
      }
    }
  })
}

describe('Loading States Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Feature: message-tracking-visualization, Property 7: Loading state display
   * **Validates: Requirements 10.1, 10.2, 10.3**
   * 
   * For any data fetching process, when data is loading and table has data,
   * system should display loading overlay
   */
  describe('Property 7: Loading state display', () => {
    it('should display skeleton loader on initial load (no existing data)', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            current: fc.integer({ min: 1, max: 10 }),
            size: fc.constantFrom(10, 20, 50, 100)
          }),
          async (paginationParams) => {
            // Create a promise that we control
            let resolvePromise: (value: PageResponse<Message>) => void
            const fetchPromise = new Promise<PageResponse<Message>>((resolve) => {
              resolvePromise = resolve
            })

            vi.mocked(messageApi.fetchMessages).mockReturnValue(fetchPromise)

            const wrapper = createWrapper()

            // Wait for component to mount and start loading
            await wrapper.vm.$nextTick()

            // At this point, data is empty and loading should be true
            const dataTableStub = wrapper.findComponent({ name: 'DataTable' })
            
            // Verify loading is true when data is empty (initial load)
            expect(dataTableStub.props('loading')).toBe(true)
            expect(dataTableStub.props('data')).toHaveLength(0)

            // Resolve the promise to complete loading
            resolvePromise!({
              records: [],
              current: paginationParams.current,
              size: paginationParams.size,
              total: 0,
              pages: 0
            })

            await wrapper.vm.$nextTick()
            await new Promise(resolve => setTimeout(resolve, 0))

            // After loading completes, loading should be false
            expect(dataTableStub.props('loading')).toBe(false)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should display loading overlay on refresh (existing data)', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            initialData: fc.array(
              fc.record({
                messageId: fc.uuid(),
                chatId: fc.string({ minLength: 1, maxLength: 50 }),
                content: fc.string({ maxLength: 200 }),
                timestamp: fc.date().map(d => d.toISOString()),
                sender: fc.string({ minLength: 1, maxLength: 50 })
              }),
              { minLength: 1, maxLength: 10 }
            ),
            refreshData: fc.array(
              fc.record({
                messageId: fc.uuid(),
                chatId: fc.string({ minLength: 1, maxLength: 50 }),
                content: fc.string({ maxLength: 200 }),
                timestamp: fc.date().map(d => d.toISOString()),
                sender: fc.string({ minLength: 1, maxLength: 50 })
              }),
              { minLength: 1, maxLength: 10 }
            )
          }),
          async ({ initialData, refreshData }) => {
            // First load returns initial data
            vi.mocked(messageApi.fetchMessages).mockResolvedValueOnce({
              records: initialData,
              current: 1,
              size: 10,
              total: initialData.length,
              pages: 1
            })

            const wrapper = createWrapper()

            // Wait for initial load to complete
            await wrapper.vm.$nextTick()
            await new Promise(resolve => setTimeout(resolve, 0))

            const dataTableStub = wrapper.findComponent({ name: 'DataTable' })
            
            // Verify initial data is loaded and loading is false
            expect(dataTableStub.props('data')).toHaveLength(initialData.length)
            expect(dataTableStub.props('loading')).toBe(false)

            // Now trigger a refresh with a delayed promise
            let resolveRefresh: (value: PageResponse<Message>) => void
            const refreshPromise = new Promise<PageResponse<Message>>((resolve) => {
              resolveRefresh = resolve
            })
            vi.mocked(messageApi.fetchMessages).mockReturnValue(refreshPromise)

            // Trigger refresh by calling loadData
            const componentInstance = wrapper.vm as any
            componentInstance.loadData()

            await wrapper.vm.$nextTick()

            // During refresh, data should still exist and loading should be true
            expect(dataTableStub.props('data').length).toBeGreaterThan(0)
            expect(dataTableStub.props('loading')).toBe(true)

            // Complete the refresh
            resolveRefresh!({
              records: refreshData,
              current: 1,
              size: 10,
              total: refreshData.length,
              pages: 1
            })

            await wrapper.vm.$nextTick()
            await new Promise(resolve => setTimeout(resolve, 0))

            // After refresh, loading should be false and data updated
            expect(dataTableStub.props('loading')).toBe(false)
            expect(dataTableStub.props('data')).toHaveLength(refreshData.length)
          }
        ),
        { numRuns: 50 }
      )
    })

    it('should maintain loading state throughout the entire fetch operation', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.record({
            delayMs: fc.integer({ min: 10, max: 100 }),
            dataSize: fc.integer({ min: 0, max: 20 })
          }),
          async ({ delayMs, dataSize }) => {
            const mockData = Array.from({ length: dataSize }, (_, i) => ({
              messageId: `msg-${i}`,
              chatId: `chat-${i}`,
              content: `content-${i}`,
              timestamp: new Date().toISOString(),
              sender: `sender-${i}`
            }))

            // Create a delayed promise
            const fetchPromise = new Promise<PageResponse<Message>>((resolve) => {
              setTimeout(() => {
                resolve({
                  records: mockData,
                  current: 1,
                  size: 10,
                  total: dataSize,
                  pages: Math.ceil(dataSize / 10)
                })
              }, delayMs)
            })

            vi.mocked(messageApi.fetchMessages).mockReturnValue(fetchPromise)

            const wrapper = createWrapper()

            await wrapper.vm.$nextTick()

            const dataTableStub = wrapper.findComponent({ name: 'DataTable' })
            
            // Loading should be true during fetch
            expect(dataTableStub.props('loading')).toBe(true)

            // Wait for the fetch to complete
            await new Promise(resolve => setTimeout(resolve, delayMs + 50))
            await wrapper.vm.$nextTick()

            // Loading should be false after fetch completes
            expect(dataTableStub.props('loading')).toBe(false)
            expect(dataTableStub.props('data')).toHaveLength(dataSize)
          }
        ),
        { numRuns: 50 }
      )
    })

    it('should set loading to false even when API call fails', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.constantFrom(
            new Error('Network error'),
            new Error('Timeout'),
            new Error('Server error')
          ),
          async (error) => {
            vi.mocked(messageApi.fetchMessages).mockRejectedValue(error)

            const wrapper = createWrapper()

            // Wait for component to mount and attempt to load
            await wrapper.vm.$nextTick()
            await new Promise(resolve => setTimeout(resolve, 0))

            const dataTableStub = wrapper.findComponent({ name: 'DataTable' })
            
            // Loading should be false after error
            expect(dataTableStub.props('loading')).toBe(false)
            expect(dataTableStub.props('data')).toHaveLength(0)
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
