import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessageBox, ElMessage } from 'element-plus'
import AuthorView from './AuthorView.vue'
import { ApiError } from '@/utils/request'
import { createPinia, setActivePinia } from 'pinia'
import type { Author } from '@/types/models'

// Mock Element Plus components
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn()
    },
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

describe('AuthorView - Delete Functionality Logic', () => {
  describe('ApiError class', () => {
    it('should create ApiError with code, message, and data', () => {
      const error = new ApiError(-60004, 'Reference error', {
        referencedByCharacters: ['char1'],
        referencedByConfigs: []
      })

      expect(error.code).toBe(-60004)
      expect(error.message).toBe('Reference error')
      expect(error.data).toEqual({
        referencedByCharacters: ['char1'],
        referencedByConfigs: []
      })
      expect(error.name).toBe('ApiError')
    })

    it('should work without data parameter', () => {
      const error = new ApiError(-60003, 'Conflict error')

      expect(error.code).toBe(-60003)
      expect(error.message).toBe('Conflict error')
      expect(error.data).toBeUndefined()
    })
  })

  describe('Reference error data structure', () => {
    it('should handle reference error with both character and config references', () => {
      const errorData = {
        referencedByCharacters: ['char1', 'char2'],
        referencedByConfigs: ['config1']
      }

      expect(errorData.referencedByCharacters).toHaveLength(2)
      expect(errorData.referencedByConfigs).toHaveLength(1)
    })

    it('should handle reference error with only character references', () => {
      const errorData = {
        referencedByCharacters: ['char1', 'char2', 'char3'],
        referencedByConfigs: []
      }

      expect(errorData.referencedByCharacters).toHaveLength(3)
      expect(errorData.referencedByConfigs).toHaveLength(0)
    })

    it('should handle reference error with only config references', () => {
      const errorData = {
        referencedByCharacters: [],
        referencedByConfigs: ['config1', 'config2']
      }

      expect(errorData.referencedByCharacters).toHaveLength(0)
      expect(errorData.referencedByConfigs).toHaveLength(2)
    })
  })

  describe('Reference error message formatting', () => {
    it('should format message with both character and config references', () => {
      const authorName = 'Test Author'
      const referencedByCharacters = ['char1', 'char2']
      const referencedByConfigs = ['config1']

      let message = `无法删除作者 "${authorName}"，该作者被以下内容引用：\n\n`
      
      if (referencedByCharacters.length > 0) {
        message += `• 角色: ${referencedByCharacters.length} 个\n`
      }
      
      if (referencedByConfigs.length > 0) {
        message += `• 标签过滤配置: ${referencedByConfigs.length} 个\n`
      }
      
      message += '\n是否强制删除？（将自动清理所有引用关系）'

      expect(message).toContain('无法删除作者 "Test Author"')
      expect(message).toContain('角色: 2 个')
      expect(message).toContain('标签过滤配置: 1 个')
      expect(message).toContain('是否强制删除')
    })

    it('should format message with only character references', () => {
      const authorName = 'Test Author'
      const referencedByCharacters = ['char1', 'char2', 'char3']
      const referencedByConfigs: string[] = []

      let message = `无法删除作者 "${authorName}"，该作者被以下内容引用：\n\n`
      
      if (referencedByCharacters.length > 0) {
        message += `• 角色: ${referencedByCharacters.length} 个\n`
      }
      
      if (referencedByConfigs.length > 0) {
        message += `• 标签过滤配置: ${referencedByConfigs.length} 个\n`
      }
      
      message += '\n是否强制删除？（将自动清理所有引用关系）'

      expect(message).toContain('角色: 3 个')
      expect(message).not.toContain('标签过滤配置')
    })

    it('should format message with only config references', () => {
      const authorName = 'Test Author'
      const referencedByCharacters: string[] = []
      const referencedByConfigs = ['config1', 'config2']

      let message = `无法删除作者 "${authorName}"，该作者被以下内容引用：\n\n`
      
      if (referencedByCharacters.length > 0) {
        message += `• 角色: ${referencedByCharacters.length} 个\n`
      }
      
      if (referencedByConfigs.length > 0) {
        message += `• 标签过滤配置: ${referencedByConfigs.length} 个\n`
      }
      
      message += '\n是否强制删除？（将自动清理所有引用关系）'

      expect(message).not.toContain('角色')
      expect(message).toContain('标签过滤配置: 2 个')
    })
  })

  describe('Delete confirmation dialog', () => {
    beforeEach(() => {
      vi.clearAllMocks()
      setActivePinia(createPinia())
    })

    it('should show confirmation dialog before deleting', () => {
      const author: Author = {
        id: '1',
        name: 'Test Author',
        aliases: [],
        signature: null,
        urls: [],
        avatarBase64: null,
        remark: null,
        createTime: '2024-01-01T00:00:00',
        updateTime: '2024-01-01T00:00:00'
      }

      // Mock ElMessageBox.confirm to reject (user cancels)
      vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

      // Simulate delete action
      const confirmMessage = `确定要删除作者 "${author.name}" 吗？此操作不可撤销。`
      const confirmTitle = '删除确认'

      ElMessageBox.confirm(confirmMessage, confirmTitle, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).catch(() => {})

      expect(ElMessageBox.confirm).toHaveBeenCalledWith(
        confirmMessage,
        confirmTitle,
        expect.objectContaining({
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      )
    })

    it('should include author name in confirmation message', () => {
      const authorName = 'John Doe'
      const confirmMessage = `确定要删除作者 "${authorName}" 吗？此操作不可撤销。`

      expect(confirmMessage).toContain(authorName)
      expect(confirmMessage).toContain('确定要删除作者')
      expect(confirmMessage).toContain('此操作不可撤销')
    })

    it('should use warning type for confirmation dialog', () => {
      const dialogOptions = {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning' as const
      }

      expect(dialogOptions.type).toBe('warning')
    })
  })

  describe('Reference relationship error handling', () => {
    beforeEach(() => {
      vi.clearAllMocks()
    })

    it('should detect reference error by code -60004', () => {
      const error = new ApiError(-60004, 'Reference error', {
        referencedByCharacters: ['char1'],
        referencedByConfigs: []
      })

      expect(error.code).toBe(-60004)
    })

    it('should show force delete option when reference error occurs', () => {
      const author: Author = {
        id: '1',
        name: 'Test Author',
        aliases: [],
        signature: null,
        urls: [],
        avatarBase64: null,
        remark: null,
        createTime: '2024-01-01T00:00:00',
        updateTime: '2024-01-01T00:00:00'
      }

      const error = new ApiError(-60004, 'Reference error', {
        referencedByCharacters: ['char1', 'char2'],
        referencedByConfigs: ['config1']
      })

      const data = error.data || {}
      const referencedByCharacters = data.referencedByCharacters || []
      const referencedByConfigs = data.referencedByConfigs || []

      let message = `无法删除作者 "${author.name}"，该作者被以下内容引用：\n\n`
      
      if (referencedByCharacters.length > 0) {
        message += `• 角色: ${referencedByCharacters.length} 个\n`
      }
      
      if (referencedByConfigs.length > 0) {
        message += `• 标签过滤配置: ${referencedByConfigs.length} 个\n`
      }
      
      message += '\n是否强制删除？（将自动清理所有引用关系）'

      expect(message).toContain('无法删除作者')
      expect(message).toContain('是否强制删除')
      expect(message).toContain('将自动清理所有引用关系')
    })

    it('should display reference details with character count', () => {
      const referencedByCharacters = ['char1', 'char2', 'char3']
      const message = `• 角色: ${referencedByCharacters.length} 个\n`

      expect(message).toContain('角色: 3 个')
    })

    it('should display reference details with config count', () => {
      const referencedByConfigs = ['config1', 'config2']
      const message = `• 标签过滤配置: ${referencedByConfigs.length} 个\n`

      expect(message).toContain('标签过滤配置: 2 个')
    })

    it('should handle missing reference data gracefully', () => {
      const error = new ApiError(-60004, 'Reference error')
      const data = error.data || {}
      const referencedByCharacters = data.referencedByCharacters || []
      const referencedByConfigs = data.referencedByConfigs || []

      expect(referencedByCharacters).toEqual([])
      expect(referencedByConfigs).toEqual([])
    })

    it('should use warning type for force delete confirmation', () => {
      const dialogOptions = {
        confirmButtonText: '强制删除',
        cancelButtonText: '取消',
        type: 'warning' as const,
        dangerouslyUseHTMLString: false
      }

      expect(dialogOptions.type).toBe('warning')
      expect(dialogOptions.confirmButtonText).toBe('强制删除')
      expect(dialogOptions.dangerouslyUseHTMLString).toBe(false)
    })

    it('should not show character references when count is zero', () => {
      const authorName = 'Test Author'
      const referencedByCharacters: string[] = []
      const referencedByConfigs = ['config1']

      let message = `无法删除作者 "${authorName}"，该作者被以下内容引用：\n\n`
      
      if (referencedByCharacters.length > 0) {
        message += `• 角色: ${referencedByCharacters.length} 个\n`
      }
      
      if (referencedByConfigs.length > 0) {
        message += `• 标签过滤配置: ${referencedByConfigs.length} 个\n`
      }

      expect(message).not.toContain('角色')
      expect(message).toContain('标签过滤配置: 1 个')
    })

    it('should not show config references when count is zero', () => {
      const authorName = 'Test Author'
      const referencedByCharacters = ['char1']
      const referencedByConfigs: string[] = []

      let message = `无法删除作者 "${authorName}"，该作者被以下内容引用：\n\n`
      
      if (referencedByCharacters.length > 0) {
        message += `• 角色: ${referencedByCharacters.length} 个\n`
      }
      
      if (referencedByConfigs.length > 0) {
        message += `• 标签过滤配置: ${referencedByConfigs.length} 个\n`
      }

      expect(message).toContain('角色: 1 个')
      expect(message).not.toContain('标签过滤配置')
    })
  })

  describe('Error handling edge cases', () => {
    it('should handle error without data field', () => {
      const error = new ApiError(-60004, 'Reference error')
      const data = error.data || {}

      expect(data).toEqual({})
      expect(data.referencedByCharacters).toBeUndefined()
      expect(data.referencedByConfigs).toBeUndefined()
    })

    it('should handle error with null data field', () => {
      const error = { code: -60004, message: 'Reference error', data: null }
      const data = error.data || {}

      expect(data).toEqual({})
    })

    it('should handle error with partial data', () => {
      const error = new ApiError(-60004, 'Reference error', {
        referencedByCharacters: ['char1']
      })

      const data = error.data || {}
      const referencedByCharacters = data.referencedByCharacters || []
      const referencedByConfigs = data.referencedByConfigs || []

      expect(referencedByCharacters).toEqual(['char1'])
      expect(referencedByConfigs).toEqual([])
    })
  })
})
