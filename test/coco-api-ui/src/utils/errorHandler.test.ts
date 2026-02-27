import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  parseConflictError,
  parseReferenceError,
  getEntityTypeName,
  handleConflictError,
  handleReferenceError,
  showSuccessMessage,
  showErrorMessage,
  confirmDangerousOperation
} from './errorHandler'
import { ApiError } from './request'
import { ElMessage, ElMessageBox } from 'element-plus'

// Mock Element Plus components
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  ElMessageBox: {
    alert: vi.fn(),
    confirm: vi.fn()
  }
}))

describe('Error Handler Unit Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Test uniqueness conflict error parsing
   * **Validates: Requirement 14.7**
   */
  describe('Uniqueness Conflict Error Parsing', () => {
    it('should parse author name conflict correctly', () => {
      const message = '名称已存在：冲突实体类型=AUTHOR, ID=abc123, 名称=张三'
      const error = new ApiError(-60003, message)
      
      const result = parseConflictError(error)
      
      expect(result).not.toBeNull()
      expect(result?.entityType).toBe('AUTHOR')
      expect(result?.entityId).toBe('abc123')
      expect(result?.name).toBe('张三')
    })

    it('should parse work alias conflict correctly', () => {
      const message = '别名已存在：冲突实体类型=WORK, ID=work456, 别名=原作别名'
      const error = new ApiError(-60003, message)
      
      const result = parseConflictError(error)
      
      expect(result).not.toBeNull()
      expect(result?.entityType).toBe('WORK')
      expect(result?.entityId).toBe('work456')
    })

    it('should parse character conflict correctly', () => {
      const message = '名称已存在：冲突实体类型=CHARACTER, ID=char789, 名称=角色名'
      const error = new ApiError(-60003, message)
      
      const result = parseConflictError(error)
      
      expect(result).not.toBeNull()
      expect(result?.entityType).toBe('CHARACTER')
      expect(result?.entityId).toBe('char789')
      expect(result?.name).toBe('角色名')
    })

    it('should return null for invalid format', () => {
      const message = '无效的错误消息格式'
      const error = new ApiError(-60003, message)
      
      const result = parseConflictError(error)
      
      expect(result).toBeNull()
    })

    it('should handle conflict message without name field', () => {
      const message = '名称已存在：冲突实体类型=AUTHOR, ID=abc123'
      const error = new ApiError(-60003, message)
      
      const result = parseConflictError(error)
      
      expect(result).not.toBeNull()
      expect(result?.entityType).toBe('AUTHOR')
      expect(result?.entityId).toBe('abc123')
      expect(result?.name).toBe('')
    })
  })

  /**
   * Test reference integrity error parsing
   * **Validates: Requirement 14.7**
   */
  describe('Reference Integrity Error Parsing', () => {
    it('should parse reference error with character references', () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: ['char1', 'char2', 'char3'],
        referencedByConfigs: []
      })
      
      const result = parseReferenceError(error)
      
      expect(result.characterCount).toBe(3)
      expect(result.configCount).toBe(0)
      expect(result.referencedByCharacters).toEqual(['char1', 'char2', 'char3'])
      expect(result.referencedByConfigs).toEqual([])
    })

    it('should parse reference error with config references', () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: [],
        referencedByConfigs: ['config1']
      })
      
      const result = parseReferenceError(error)
      
      expect(result.characterCount).toBe(0)
      expect(result.configCount).toBe(1)
      expect(result.referencedByCharacters).toEqual([])
      expect(result.referencedByConfigs).toEqual(['config1'])
    })

    it('should parse reference error with both types of references', () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: ['char1', 'char2'],
        referencedByConfigs: ['config1', 'config2']
      })
      
      const result = parseReferenceError(error)
      
      expect(result.characterCount).toBe(2)
      expect(result.configCount).toBe(2)
    })

    it('should handle missing data gracefully', () => {
      const error = new ApiError(-60004, '引用关系错误')
      
      const result = parseReferenceError(error)
      
      expect(result.characterCount).toBe(0)
      expect(result.configCount).toBe(0)
      expect(result.referencedByCharacters).toEqual([])
      expect(result.referencedByConfigs).toEqual([])
    })

    it('should handle partial data', () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: ['char1']
      })
      
      const result = parseReferenceError(error)
      
      expect(result.characterCount).toBe(1)
      expect(result.configCount).toBe(0)
      expect(result.referencedByCharacters).toEqual(['char1'])
      expect(result.referencedByConfigs).toEqual([])
    })
  })

  /**
   * Test entity type name mapping
   */
  describe('Entity Type Name Mapping', () => {
    it('should return correct Chinese name for AUTHOR', () => {
      expect(getEntityTypeName('AUTHOR')).toBe('作者')
    })

    it('should return correct Chinese name for WORK', () => {
      expect(getEntityTypeName('WORK')).toBe('原作')
    })

    it('should return correct Chinese name for CHARACTER', () => {
      expect(getEntityTypeName('CHARACTER')).toBe('角色')
    })

    it('should return correct Chinese name for CONFIG', () => {
      expect(getEntityTypeName('CONFIG')).toBe('配置')
    })

    it('should return original type for unknown types', () => {
      expect(getEntityTypeName('UNKNOWN')).toBe('UNKNOWN')
      expect(getEntityTypeName('CUSTOM_TYPE')).toBe('CUSTOM_TYPE')
    })
  })

  /**
   * Test conflict error handling
   */
  describe('Conflict Error Handling', () => {
    it('should display detailed conflict message for valid error', () => {
      const message = '名称已存在：冲突实体类型=AUTHOR, ID=abc123, 名称=张三'
      const error = new ApiError(-60003, message)
      
      handleConflictError(error)
      
      expect(ElMessageBox.alert).toHaveBeenCalled()
      const callArgs = (ElMessageBox.alert as any).mock.calls[0]
      expect(callArgs[0]).toContain('作者')
      expect(callArgs[0]).toContain('abc123')
      expect(callArgs[1]).toBe('唯一性冲突')
    })

    it('should fallback to error message for invalid format', () => {
      const message = '无效的错误消息'
      const error = new ApiError(-60003, message)
      
      handleConflictError(error)
      
      expect(ElMessage.error).toHaveBeenCalledWith(message)
    })
  })

  /**
   * Test reference error handling
   */
  describe('Reference Error Handling', () => {
    it('should show reference details and call force delete on confirm', async () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: ['char1', 'char2'],
        referencedByConfigs: ['config1']
      })
      
      const forceDeleteFn = vi.fn().mockResolvedValue(undefined)
      
      // Mock confirm to resolve (user confirms)
      ;(ElMessageBox.confirm as any).mockResolvedValue(undefined)
      
      const result = await handleReferenceError(error, '测试实体', forceDeleteFn)
      
      expect(ElMessageBox.confirm).toHaveBeenCalled()
      const callArgs = (ElMessageBox.confirm as any).mock.calls[0]
      expect(callArgs[0]).toContain('测试实体')
      expect(callArgs[0]).toContain('角色: 2 个')
      expect(callArgs[0]).toContain('标签过滤配置: 1 个')
      
      expect(forceDeleteFn).toHaveBeenCalled()
      expect(ElMessage.success).toHaveBeenCalledWith('强制删除成功')
      expect(result).toBe(true)
    })

    it('should not call force delete on cancel', async () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: ['char1']
      })
      
      const forceDeleteFn = vi.fn()
      
      // Mock confirm to reject (user cancels)
      ;(ElMessageBox.confirm as any).mockRejectedValue(new Error('cancel'))
      
      const result = await handleReferenceError(error, '测试实体', forceDeleteFn)
      
      expect(ElMessageBox.confirm).toHaveBeenCalled()
      expect(forceDeleteFn).not.toHaveBeenCalled()
      expect(result).toBe(false)
    })

    it('should handle force delete failure', async () => {
      const error = new ApiError(-60004, '引用关系错误', {
        referencedByCharacters: ['char1']
      })
      
      const forceDeleteFn = vi.fn().mockRejectedValue(new Error('Delete failed'))
      
      // Mock confirm to resolve
      ;(ElMessageBox.confirm as any).mockResolvedValue(undefined)
      
      const result = await handleReferenceError(error, '测试实体', forceDeleteFn)
      
      expect(forceDeleteFn).toHaveBeenCalled()
      expect(ElMessage.error).toHaveBeenCalledWith('强制删除失败')
      expect(result).toBe(false)
    })
  })

  /**
   * Test message display functions
   */
  describe('Message Display Functions', () => {
    it('should display success message', () => {
      showSuccessMessage('操作成功')
      
      expect(ElMessage.success).toHaveBeenCalledWith('操作成功')
    })

    it('should display error message', () => {
      showErrorMessage('操作失败')
      
      expect(ElMessage.error).toHaveBeenCalledWith('操作失败')
    })
  })

  /**
   * Test confirmation dialog
   */
  describe('Confirmation Dialog', () => {
    it('should return true when user confirms', async () => {
      ;(ElMessageBox.confirm as any).mockResolvedValue(undefined)
      
      const result = await confirmDangerousOperation('确定删除吗？', '删除确认')
      
      expect(ElMessageBox.confirm).toHaveBeenCalled()
      expect(result).toBe(true)
    })

    it('should return false when user cancels', async () => {
      ;(ElMessageBox.confirm as any).mockRejectedValue(new Error('cancel'))
      
      const result = await confirmDangerousOperation('确定删除吗？', '删除确认')
      
      expect(ElMessageBox.confirm).toHaveBeenCalled()
      expect(result).toBe(false)
    })
  })
})
