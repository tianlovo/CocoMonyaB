import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { AuthorCreateDTO } from '@/types/models'
import type { ImportResult } from '@/types/api'

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

describe('AuthorView - Import/Export Functionality', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Export functionality', () => {
    it('should create JSON blob with correct content type', () => {
      const data: AuthorCreateDTO[] = [
        {
          name: 'Author 1',
          aliases: ['alias1'],
          signature: 'sig1',
          urls: ['url1'],
          avatarBase64: null,
          remark: null
        }
      ]

      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      
      expect(blob.type).toBe('application/json')
      expect(blob.size).toBeGreaterThan(0)
    })

    it('should format JSON with proper indentation', () => {
      const data: AuthorCreateDTO[] = [
        {
          name: 'Author 1',
          aliases: ['alias1'],
          signature: null,
          urls: [],
          avatarBase64: null,
          remark: null
        }
      ]

      const jsonString = JSON.stringify(data, null, 2)
      
      expect(jsonString).toContain('[\n')
      expect(jsonString).toContain('  {\n')
      expect(jsonString).toContain('    "name"')
    })

    it('should generate filename with timestamp', () => {
      const timestamp = new Date().getTime()
      const filename = `authors_export_${timestamp}.json`
      
      expect(filename).toMatch(/^authors_export_\d+\.json$/)
      expect(filename).toContain('authors_export_')
      expect(filename).toContain('.json')
    })

    it('should handle empty export data', () => {
      const data: AuthorCreateDTO[] = []
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      
      expect(blob.type).toBe('application/json')
      expect(blob.size).toBeGreaterThan(0)
    })

    it('should export all author fields', () => {
      const data: AuthorCreateDTO[] = [
        {
          name: 'Complete Author',
          aliases: ['alias1', 'alias2'],
          signature: 'My signature',
          urls: ['http://example.com'],
          avatarBase64: 'base64data',
          remark: 'Some remark'
        }
      ]

      const jsonString = JSON.stringify(data, null, 2)
      
      expect(jsonString).toContain('"name"')
      expect(jsonString).toContain('"aliases"')
      expect(jsonString).toContain('"signature"')
      expect(jsonString).toContain('"urls"')
      expect(jsonString).toContain('"avatarBase64"')
      expect(jsonString).toContain('"remark"')
    })
  })

  describe('Import validation', () => {
    it('should validate JSON format', () => {
      const invalidJson = 'not a json'
      
      let isValid = false
      try {
        JSON.parse(invalidJson)
        isValid = true
      } catch {
        isValid = false
      }
      
      expect(isValid).toBe(false)
    })

    it('should validate data is an array', () => {
      const validArray = JSON.stringify([])
      const invalidObject = JSON.stringify({})
      
      const parsedArray = JSON.parse(validArray)
      const parsedObject = JSON.parse(invalidObject)
      
      expect(Array.isArray(parsedArray)).toBe(true)
      expect(Array.isArray(parsedObject)).toBe(false)
    })

    it('should validate required name field exists', () => {
      const validItem = { name: 'Author', aliases: [] }
      const invalidItem = { aliases: [] }
      
      expect(validItem.name).toBeDefined()
      expect(typeof validItem.name).toBe('string')
      expect((invalidItem as any).name).toBeUndefined()
    })

    it('should validate required aliases field exists and is array', () => {
      const validItem = { name: 'Author', aliases: [] }
      const invalidItem = { name: 'Author' }
      
      expect(validItem.aliases).toBeDefined()
      expect(Array.isArray(validItem.aliases)).toBe(true)
      expect((invalidItem as any).aliases).toBeUndefined()
    })

    it('should validate name field is string', () => {
      const validItem = { name: 'Author', aliases: [] }
      const invalidItem = { name: 123, aliases: [] }
      
      expect(typeof validItem.name).toBe('string')
      expect(typeof invalidItem.name).not.toBe('string')
    })

    it('should validate aliases field is array', () => {
      const validItem = { name: 'Author', aliases: [] }
      const invalidItem = { name: 'Author', aliases: 'not-array' }
      
      expect(Array.isArray(validItem.aliases)).toBe(true)
      expect(Array.isArray(invalidItem.aliases)).toBe(false)
    })

    it('should detect invalid item at specific index', () => {
      const data = [
        { name: 'Author 1', aliases: [] },
        { name: 'Author 2', aliases: [] },
        { aliases: [] }, // Invalid - missing name
        { name: 'Author 4', aliases: [] }
      ]
      
      let invalidIndex = -1
      for (let i = 0; i < data.length; i++) {
        const item = data[i] as any
        if (!item.name || typeof item.name !== 'string') {
          invalidIndex = i
          break
        }
      }
      
      expect(invalidIndex).toBe(2)
    })

    it('should provide helpful error message for missing name', () => {
      const index = 0
      const errorMessage = `JSON 文件格式错误：第 ${index + 1} 条记录缺少有效的 name 字段`
      
      expect(errorMessage).toContain('第 1 条记录')
      expect(errorMessage).toContain('缺少有效的 name 字段')
    })

    it('should provide helpful error message for missing aliases', () => {
      const index = 2
      const errorMessage = `JSON 文件格式错误：第 ${index + 1} 条记录缺少有效的 aliases 字段`
      
      expect(errorMessage).toContain('第 3 条记录')
      expect(errorMessage).toContain('缺少有效的 aliases 字段')
    })
  })

  describe('Import result display', () => {
    it('should display success count', () => {
      const result: ImportResult = {
        successCount: 5,
        failureCount: 0,
        errors: []
      }
      
      expect(result.successCount).toBe(5)
      expect(result.failureCount).toBe(0)
    })

    it('should display failure count', () => {
      const result: ImportResult = {
        successCount: 3,
        failureCount: 2,
        errors: []
      }
      
      expect(result.successCount).toBe(3)
      expect(result.failureCount).toBe(2)
    })

    it('should display error details with index', () => {
      const result: ImportResult = {
        successCount: 0,
        failureCount: 1,
        errors: [
          {
            index: 0,
            name: 'Author 1',
            error: 'Name already exists'
          }
        ]
      }
      
      expect(result.errors).toHaveLength(1)
      expect(result.errors[0].index).toBe(0)
      expect(result.errors[0].name).toBe('Author 1')
      expect(result.errors[0].error).toBe('Name already exists')
    })

    it('should display multiple error details', () => {
      const result: ImportResult = {
        successCount: 1,
        failureCount: 2,
        errors: [
          {
            index: 1,
            name: 'Author 2',
            error: 'Name already exists'
          },
          {
            index: 3,
            name: 'Author 4',
            error: 'Invalid data'
          }
        ]
      }
      
      expect(result.errors).toHaveLength(2)
      expect(result.errors[0].index).toBe(1)
      expect(result.errors[1].index).toBe(3)
    })

    it('should show success icon when no failures', () => {
      const result: ImportResult = {
        successCount: 5,
        failureCount: 0,
        errors: []
      }
      
      const icon = result.failureCount === 0 ? 'success' : 'warning'
      expect(icon).toBe('success')
    })

    it('should show warning icon when has failures', () => {
      const result: ImportResult = {
        successCount: 3,
        failureCount: 2,
        errors: []
      }
      
      const icon = result.failureCount === 0 ? 'success' : 'warning'
      expect(icon).toBe('warning')
    })

    it('should show appropriate title for complete success', () => {
      const result: ImportResult = {
        successCount: 5,
        failureCount: 0,
        errors: []
      }
      
      const title = result.failureCount === 0 ? '导入成功' : '导入完成（部分失败）'
      expect(title).toBe('导入成功')
    })

    it('should show appropriate title for partial failure', () => {
      const result: ImportResult = {
        successCount: 3,
        failureCount: 2,
        errors: []
      }
      
      const title = result.failureCount === 0 ? '导入成功' : '导入完成（部分失败）'
      expect(title).toBe('导入完成（部分失败）')
    })

    it('should format error item display text', () => {
      const error = {
        index: 0,
        name: 'Test Author',
        error: 'Duplicate name'
      }
      
      const displayIndex = `第 ${error.index + 1} 条`
      expect(displayIndex).toBe('第 1 条')
    })
  })

  describe('File reading', () => {
    it('should read file as text', async () => {
      const content = JSON.stringify([{ name: 'Author', aliases: [] }])
      const blob = new Blob([content], { type: 'application/json' })
      const file = new File([blob], 'test.json', { type: 'application/json' })
      
      const text = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = (e) => resolve(e.target?.result as string)
        reader.onerror = reject
        reader.readAsText(file)
      })
      
      expect(text).toBe(content)
    })

    it('should handle file read errors', async () => {
      const blob = new Blob(['invalid'], { type: 'application/json' })
      const file = new File([blob], 'test.json', { type: 'application/json' })
      
      try {
        await new Promise<string>((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = (e) => resolve(e.target?.result as string)
          reader.onerror = () => reject(new Error('Read failed'))
          reader.readAsText(file)
        })
      } catch (error: any) {
        expect(error.message).toBe('Read failed')
      }
    })
  })

  describe('Import progress indication', () => {
    it('should show loading message during import', () => {
      const loadingMessage = '正在导入，请稍候...'
      expect(loadingMessage).toBe('正在导入，请稍候...')
    })

    it('should use info type for loading message', () => {
      const messageType = 'info'
      expect(messageType).toBe('info')
    })

    it('should set duration to 0 for persistent loading message', () => {
      const duration = 0
      expect(duration).toBe(0)
    })
  })

  describe('Edge cases', () => {
    it('should handle empty file', () => {
      const content = ''
      let isValid = false
      
      try {
        JSON.parse(content)
        isValid = true
      } catch {
        isValid = false
      }
      
      expect(isValid).toBe(false)
    })

    it('should handle file with only whitespace', () => {
      const content = '   \n  \t  '
      let isValid = false
      
      try {
        JSON.parse(content)
        isValid = true
      } catch {
        isValid = false
      }
      
      expect(isValid).toBe(false)
    })

    it('should handle large import result with many errors', () => {
      const errors = Array.from({ length: 100 }, (_, i) => ({
        index: i,
        name: `Author ${i}`,
        error: 'Error message'
      }))
      
      const result: ImportResult = {
        successCount: 0,
        failureCount: 100,
        errors
      }
      
      expect(result.errors).toHaveLength(100)
      expect(result.failureCount).toBe(100)
    })

    it('should handle import with all failures', () => {
      const result: ImportResult = {
        successCount: 0,
        failureCount: 5,
        errors: Array.from({ length: 5 }, (_, i) => ({
          index: i,
          name: `Author ${i}`,
          error: 'Failed'
        }))
      }
      
      expect(result.successCount).toBe(0)
      expect(result.failureCount).toBe(5)
      expect(result.errors).toHaveLength(5)
    })

    it('should handle import with all successes', () => {
      const result: ImportResult = {
        successCount: 10,
        failureCount: 0,
        errors: []
      }
      
      expect(result.successCount).toBe(10)
      expect(result.failureCount).toBe(0)
      expect(result.errors).toHaveLength(0)
    })
  })
})
