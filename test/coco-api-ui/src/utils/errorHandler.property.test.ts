import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import fc from 'fast-check'
import {
  parseConflictError,
  parseReferenceError,
  getEntityTypeName,
  handleConflictError,
  showSuccessMessage,
  showErrorMessage
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

describe('Error Handler Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Feature: tag-management-frontend, Property 7: 错误消息显示
   * **Validates: Requirements 3.4, 14.1, 14.2, 14.3**
   * 
   * For any API request failure, the system should use Element Plus Message component
   * to display error messages
   */
  describe('Property 7: Error Message Display', () => {
    it('should display error message using ElMessage.error for any error string', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }),
          (errorMessage) => {
            // Call showErrorMessage
            showErrorMessage(errorMessage)
            
            // Verify ElMessage.error was called with the message
            expect(ElMessage.error).toHaveBeenCalledWith(errorMessage)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should display success message using ElMessage.success for any success string', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }),
          (successMessage) => {
            // Call showSuccessMessage
            showSuccessMessage(successMessage)
            
            // Verify ElMessage.success was called with the message
            expect(ElMessage.success).toHaveBeenCalledWith(successMessage)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should display conflict error using ElMessageBox.alert for uniqueness conflicts', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('AUTHOR', 'WORK', 'CHARACTER'),
          fc.string({ minLength: 1 }).filter(s => !s.includes(',')), // Avoid commas in ID
          fc.string({ minLength: 1 }).filter(s => !s.includes(',')), // Avoid commas in name
          (entityType, entityId, name) => {
            // Create conflict error message
            const message = `名称已存在：冲突实体类型=${entityType}, ID=${entityId}, 名称=${name}`
            const error = new ApiError(-60003, message)
            
            // Call handleConflictError
            handleConflictError(error)
            
            // Verify ElMessageBox.alert was called
            expect(ElMessageBox.alert).toHaveBeenCalled()
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Test conflict error parsing
   */
  describe('Conflict Error Parsing', () => {
    it('should correctly parse conflict error details for any valid format', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('AUTHOR', 'WORK', 'CHARACTER'),
          fc.string({ minLength: 1 }).filter(s => !s.includes(',')), // Avoid commas in ID
          fc.string({ minLength: 1 }).filter(s => !s.includes(',')), // Avoid commas in name
          (entityType, entityId, name) => {
            const message = `名称已存在：冲突实体类型=${entityType}, ID=${entityId}, 名称=${name}`
            const error = new ApiError(-60003, message)
            
            const result = parseConflictError(error)
            
            expect(result).not.toBeNull()
            expect(result?.entityType).toBe(entityType)
            expect(result?.entityId).toBe(entityId)
            expect(result?.name).toBe(name)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should handle alias conflicts with same parsing logic', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('AUTHOR', 'WORK', 'CHARACTER'),
          fc.string({ minLength: 1 }).filter(s => !s.includes(',')), // Avoid commas in ID
          fc.string({ minLength: 1 }).filter(s => !s.includes(',')), // Avoid commas in alias
          (entityType, entityId, alias) => {
            const message = `别名已存在：冲突实体类型=${entityType}, ID=${entityId}, 别名=${alias}`
            const error = new ApiError(-60003, message)
            
            const result = parseConflictError(error)
            
            expect(result).not.toBeNull()
            expect(result?.entityType).toBe(entityType)
            expect(result?.entityId).toBe(entityId)
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Test reference error parsing
   */
  describe('Reference Error Parsing', () => {
    it('should correctly parse reference error details for any valid data', () => {
      fc.assert(
        fc.property(
          fc.array(fc.string(), { minLength: 0, maxLength: 10 }),
          fc.array(fc.string(), { minLength: 0, maxLength: 10 }),
          (characterIds, configIds) => {
            const error = new ApiError(-60004, '引用关系错误', {
              referencedByCharacters: characterIds,
              referencedByConfigs: configIds
            })
            
            const result = parseReferenceError(error)
            
            expect(result.characterCount).toBe(characterIds.length)
            expect(result.configCount).toBe(configIds.length)
            expect(result.referencedByCharacters).toEqual(characterIds)
            expect(result.referencedByConfigs).toEqual(configIds)
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should handle missing reference data gracefully', () => {
      fc.assert(
        fc.property(
          fc.string(),
          (message) => {
            // Create error without data
            const error = new ApiError(-60004, message)
            
            const result = parseReferenceError(error)
            
            // Should return empty arrays and zero counts
            expect(result.characterCount).toBe(0)
            expect(result.configCount).toBe(0)
            expect(result.referencedByCharacters).toEqual([])
            expect(result.referencedByConfigs).toEqual([])
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Test entity type name mapping
   */
  describe('Entity Type Name Mapping', () => {
    it('should return correct Chinese name for known entity types', () => {
      fc.assert(
        fc.property(
          fc.constantFrom('AUTHOR', 'WORK', 'CHARACTER', 'CONFIG'),
          (entityType) => {
            const result = getEntityTypeName(entityType)
            
            // Should return a non-empty Chinese name
            expect(result).toBeTruthy()
            expect(result.length).toBeGreaterThan(0)
            
            // Verify specific mappings
            const expectedMap: Record<string, string> = {
              'AUTHOR': '作者',
              'WORK': '原作',
              'CHARACTER': '角色',
              'CONFIG': '配置'
            }
            expect(result).toBe(expectedMap[entityType])
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should return original type for unknown entity types', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }).filter(s => 
            !['AUTHOR', 'WORK', 'CHARACTER', 'CONFIG'].includes(s) &&
            // Avoid object prototype property names
            !['toString', 'valueOf', 'hasOwnProperty', 'constructor', '__proto__'].includes(s)
          ),
          (unknownType) => {
            const result = getEntityTypeName(unknownType)
            
            // Should return the original type
            expect(result).toBe(unknownType)
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})
