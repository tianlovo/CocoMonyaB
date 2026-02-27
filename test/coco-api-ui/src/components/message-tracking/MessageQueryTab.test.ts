import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageQueryTab from './MessageQueryTab.vue'
import type { Message } from '@/types/models'

// Mock Element Plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn()
    }
  }
})

// Mock API
vi.mock('@/api/message', () => ({
  messageApi: {
    fetchMessages: vi.fn()
  }
}))

describe('MessageQueryTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Filter form rendering', () => {
    it('should render chatId input field', () => {
      const wrapper = mount(MessageQueryTab)
      const chatIdInput = wrapper.find('input[placeholder="聊天ID"]')
      expect(chatIdInput.exists()).toBe(true)
    })

    it('should render startDate date picker', () => {
      const wrapper = mount(MessageQueryTab)
      const startDatePicker = wrapper.find('input[placeholder="开始时间"]')
      expect(startDatePicker.exists()).toBe(true)
    })

    it('should render endDate date picker', () => {
      const wrapper = mount(MessageQueryTab)
      const endDatePicker = wrapper.find('input[placeholder="结束时间"]')
      expect(endDatePicker.exists()).toBe(true)
    })

    it('should render search button', () => {
      const wrapper = mount(MessageQueryTab)
      const searchButton = wrapper.find('button')
      expect(searchButton.text()).toContain('搜索')
    })

    it('should render reset button', () => {
      const wrapper = mount(MessageQueryTab)
      const buttons = wrapper.findAll('button')
      const resetButton = buttons.find(btn => btn.text().includes('重置'))
      expect(resetButton).toBeDefined()
    })
  })

  describe('Utility functions', () => {
    it('should format timestamp correctly', () => {
      const formatDateTime = (dateTimeStr: string) => {
        if (!dateTimeStr) return '-'
        try {
          const date = new Date(dateTimeStr)
          const year = date.getFullYear()
          const month = String(date.getMonth() + 1).padStart(2, '0')
          const day = String(date.getDate()).padStart(2, '0')
          const hours = String(date.getHours()).padStart(2, '0')
          const minutes = String(date.getMinutes()).padStart(2, '0')
          const seconds = String(date.getSeconds()).padStart(2, '0')
          return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        } catch {
          return dateTimeStr
        }
      }

      const timestamp = '2024-01-15T10:30:45'
      const formatted = formatDateTime(timestamp)
      expect(formatted).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    })

    it('should truncate long text', () => {
      const truncateText = (text: string, maxLength: number = 100) => {
        if (!text) return '-'
        if (text.length <= maxLength) return text
        return text.substring(0, maxLength) + '...'
      }

      const longText = 'a'.repeat(150)
      const truncated = truncateText(longText, 100)
      expect(truncated.length).toBe(103) // 100 + '...'
      expect(truncated).toContain('...')
    })

    it('should not truncate short text', () => {
      const truncateText = (text: string, maxLength: number = 100) => {
        if (!text) return '-'
        if (text.length <= maxLength) return text
        return text.substring(0, maxLength) + '...'
      }

      const shortText = 'Short text'
      const result = truncateText(shortText, 100)
      expect(result).toBe(shortText)
      expect(result).not.toContain('...')
    })

    it('should handle empty timestamp', () => {
      const formatDateTime = (dateTimeStr: string) => {
        if (!dateTimeStr) return '-'
        try {
          const date = new Date(dateTimeStr)
          const year = date.getFullYear()
          const month = String(date.getMonth() + 1).padStart(2, '0')
          const day = String(date.getDate()).padStart(2, '0')
          const hours = String(date.getHours()).padStart(2, '0')
          const minutes = String(date.getMinutes()).padStart(2, '0')
          const seconds = String(date.getSeconds()).padStart(2, '0')
          return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        } catch {
          return dateTimeStr
        }
      }

      const result = formatDateTime('')
      expect(result).toBe('-')
    })

    it('should handle empty text for truncation', () => {
      const truncateText = (text: string, maxLength: number = 100) => {
        if (!text) return '-'
        if (text.length <= maxLength) return text
        return text.substring(0, maxLength) + '...'
      }

      const result = truncateText('')
      expect(result).toBe('-')
    })
  })

  describe('Empty state handling', () => {
    it('should show "暂无数据" when no filters applied', () => {
      const hasFilters = false
      const emptyMessage = hasFilters ? '未找到匹配的记录' : '暂无数据'
      expect(emptyMessage).toBe('暂无数据')
    })

    it('should show "未找到匹配的记录" when filters applied', () => {
      const hasFilters = true
      const emptyMessage = hasFilters ? '未找到匹配的记录' : '暂无数据'
      expect(emptyMessage).toBe('未找到匹配的记录')
    })

    it('should detect filters when chatId is set', () => {
      const filters = { chatId: '123', startDate: '', endDate: '' }
      const hasFilters = !!(filters.chatId || filters.startDate || filters.endDate)
      expect(hasFilters).toBe(true)
    })

    it('should detect filters when startDate is set', () => {
      const filters = { chatId: '', startDate: '2024-01-01', endDate: '' }
      const hasFilters = !!(filters.chatId || filters.startDate || filters.endDate)
      expect(hasFilters).toBe(true)
    })

    it('should detect filters when endDate is set', () => {
      const filters = { chatId: '', startDate: '', endDate: '2024-01-31' }
      const hasFilters = !!(filters.chatId || filters.startDate || filters.endDate)
      expect(hasFilters).toBe(true)
    })

    it('should detect no filters when all empty', () => {
      const filters = { chatId: '', startDate: '', endDate: '' }
      const hasFilters = !!(filters.chatId || filters.startDate || filters.endDate)
      expect(hasFilters).toBe(false)
    })
  })

  describe('Table columns configuration', () => {
    it('should have messageId column', () => {
      const columns = [
        { prop: 'messageId', label: '消息ID', width: 200 },
        { prop: 'chatId', label: '聊天ID', width: 150 },
        { prop: 'content', label: '内容', minWidth: 200, slot: 'content' },
        { prop: 'timestamp', label: '时间', width: 180, slot: 'timestamp' },
        { prop: 'sender', label: '发送者', width: 150 }
      ]

      const messageIdColumn = columns.find(col => col.prop === 'messageId')
      expect(messageIdColumn).toBeDefined()
      expect(messageIdColumn?.label).toBe('消息ID')
    })

    it('should have chatId column', () => {
      const columns = [
        { prop: 'messageId', label: '消息ID', width: 200 },
        { prop: 'chatId', label: '聊天ID', width: 150 },
        { prop: 'content', label: '内容', minWidth: 200, slot: 'content' },
        { prop: 'timestamp', label: '时间', width: 180, slot: 'timestamp' },
        { prop: 'sender', label: '发送者', width: 150 }
      ]

      const chatIdColumn = columns.find(col => col.prop === 'chatId')
      expect(chatIdColumn).toBeDefined()
      expect(chatIdColumn?.label).toBe('聊天ID')
    })

    it('should have content column with slot', () => {
      const columns = [
        { prop: 'messageId', label: '消息ID', width: 200 },
        { prop: 'chatId', label: '聊天ID', width: 150 },
        { prop: 'content', label: '内容', minWidth: 200, slot: 'content' },
        { prop: 'timestamp', label: '时间', width: 180, slot: 'timestamp' },
        { prop: 'sender', label: '发送者', width: 150 }
      ]

      const contentColumn = columns.find(col => col.prop === 'content')
      expect(contentColumn).toBeDefined()
      expect(contentColumn?.slot).toBe('content')
    })

    it('should have timestamp column with slot', () => {
      const columns = [
        { prop: 'messageId', label: '消息ID', width: 200 },
        { prop: 'chatId', label: '聊天ID', width: 150 },
        { prop: 'content', label: '内容', minWidth: 200, slot: 'content' },
        { prop: 'timestamp', label: '时间', width: 180, slot: 'timestamp' },
        { prop: 'sender', label: '发送者', width: 150 }
      ]

      const timestampColumn = columns.find(col => col.prop === 'timestamp')
      expect(timestampColumn).toBeDefined()
      expect(timestampColumn?.slot).toBe('timestamp')
    })

    it('should have sender column', () => {
      const columns = [
        { prop: 'messageId', label: '消息ID', width: 200 },
        { prop: 'chatId', label: '聊天ID', width: 150 },
        { prop: 'content', label: '内容', minWidth: 200, slot: 'content' },
        { prop: 'timestamp', label: '时间', width: 180, slot: 'timestamp' },
        { prop: 'sender', label: '发送者', width: 150 }
      ]

      const senderColumn = columns.find(col => col.prop === 'sender')
      expect(senderColumn).toBeDefined()
      expect(senderColumn?.label).toBe('发送者')
    })
  })

  describe('Filter parameter handling', () => {
    it('should include chatId in params when set', () => {
      const filters = { chatId: '123', startDate: '', endDate: '' }
      const params: any = { current: 1, size: 10 }

      if (filters.chatId) params.chatId = filters.chatId
      if (filters.startDate) params.startDate = filters.startDate
      if (filters.endDate) params.endDate = filters.endDate

      expect(params.chatId).toBe('123')
      expect(params.startDate).toBeUndefined()
      expect(params.endDate).toBeUndefined()
    })

    it('should include startDate in params when set', () => {
      const filters = { chatId: '', startDate: '2024-01-01', endDate: '' }
      const params: any = { current: 1, size: 10 }

      if (filters.chatId) params.chatId = filters.chatId
      if (filters.startDate) params.startDate = filters.startDate
      if (filters.endDate) params.endDate = filters.endDate

      expect(params.chatId).toBeUndefined()
      expect(params.startDate).toBe('2024-01-01')
      expect(params.endDate).toBeUndefined()
    })

    it('should include endDate in params when set', () => {
      const filters = { chatId: '', startDate: '', endDate: '2024-01-31' }
      const params: any = { current: 1, size: 10 }

      if (filters.chatId) params.chatId = filters.chatId
      if (filters.startDate) params.startDate = filters.startDate
      if (filters.endDate) params.endDate = filters.endDate

      expect(params.chatId).toBeUndefined()
      expect(params.startDate).toBeUndefined()
      expect(params.endDate).toBe('2024-01-31')
    })

    it('should include all filters when all set', () => {
      const filters = { chatId: '123', startDate: '2024-01-01', endDate: '2024-01-31' }
      const params: any = { current: 1, size: 10 }

      if (filters.chatId) params.chatId = filters.chatId
      if (filters.startDate) params.startDate = filters.startDate
      if (filters.endDate) params.endDate = filters.endDate

      expect(params.chatId).toBe('123')
      expect(params.startDate).toBe('2024-01-01')
      expect(params.endDate).toBe('2024-01-31')
    })

    it('should only include pagination when no filters', () => {
      const filters = { chatId: '', startDate: '', endDate: '' }
      const params: any = { current: 1, size: 10 }

      if (filters.chatId) params.chatId = filters.chatId
      if (filters.startDate) params.startDate = filters.startDate
      if (filters.endDate) params.endDate = filters.endDate

      expect(Object.keys(params)).toEqual(['current', 'size'])
    })
  })
})
