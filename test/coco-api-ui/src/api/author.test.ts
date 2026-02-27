import { describe, it, expect, vi, beforeEach } from 'vitest'
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

describe('Author API Service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getPage', () => {
    it('should call GET /config/tag/author/page with pagination params', async () => {
      const mockResponse = {
        records: [],
        current: 1,
        size: 10,
        total: 0,
        pages: 0
      }
      vi.mocked(request.get).mockResolvedValue(mockResponse)

      const params = { current: 1, size: 10 }
      const result = await authorApi.getPage(params)

      expect(request.get).toHaveBeenCalledWith('/config/tag/author/page', { params })
      expect(result).toEqual(mockResponse)
    })

    it('should support keyword search parameter', async () => {
      const mockResponse = {
        records: [],
        current: 1,
        size: 10,
        total: 0,
        pages: 0
      }
      vi.mocked(request.get).mockResolvedValue(mockResponse)

      const params = { current: 1, size: 10, keyword: 'test' }
      await authorApi.getPage(params)

      expect(request.get).toHaveBeenCalledWith('/config/tag/author/page', { params })
    })
  })

  describe('getById', () => {
    it('should call GET /config/tag/author/:id', async () => {
      const mockAuthor = {
        id: 'test-id',
        name: 'Test Author',
        aliases: [],
        signature: null,
        urls: [],
        avatarBase64: null,
        remark: null,
        createTime: '2024-01-01',
        updateTime: '2024-01-01'
      }
      vi.mocked(request.get).mockResolvedValue(mockAuthor)

      const result = await authorApi.getById('test-id')

      expect(request.get).toHaveBeenCalledWith('/config/tag/author/test-id')
      expect(result).toEqual(mockAuthor)
    })
  })

  describe('getByName', () => {
    it('should call GET /config/tag/author/name/:name', async () => {
      const mockAuthor = {
        id: 'test-id',
        name: 'Test Author',
        aliases: [],
        signature: null,
        urls: [],
        avatarBase64: null,
        remark: null,
        createTime: '2024-01-01',
        updateTime: '2024-01-01'
      }
      vi.mocked(request.get).mockResolvedValue(mockAuthor)

      const result = await authorApi.getByName('Test Author')

      expect(request.get).toHaveBeenCalledWith('/config/tag/author/name/Test Author')
      expect(result).toEqual(mockAuthor)
    })
  })

  describe('create', () => {
    it('should call POST /config/tag/author with author data', async () => {
      const createData = {
        name: 'New Author',
        aliases: ['Alias1'],
        signature: 'Test signature',
        urls: ['http://example.com'],
        avatarBase64: null,
        remark: 'Test remark'
      }
      const mockResponse = {
        id: 'new-id',
        ...createData,
        createTime: '2024-01-01',
        updateTime: '2024-01-01'
      }
      vi.mocked(request.post).mockResolvedValue(mockResponse)

      const result = await authorApi.create(createData)

      expect(request.post).toHaveBeenCalledWith('/config/tag/author', createData)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('update', () => {
    it('should call PUT /config/tag/author/:id with update data', async () => {
      const updateData = {
        name: 'Updated Author',
        aliases: ['NewAlias']
      }
      const mockResponse = {
        id: 'test-id',
        name: 'Updated Author',
        aliases: ['NewAlias'],
        signature: null,
        urls: [],
        avatarBase64: null,
        remark: null,
        createTime: '2024-01-01',
        updateTime: '2024-01-02'
      }
      vi.mocked(request.put).mockResolvedValue(mockResponse)

      const result = await authorApi.update('test-id', updateData)

      expect(request.put).toHaveBeenCalledWith('/config/tag/author/test-id', updateData)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('delete', () => {
    it('should call DELETE /config/tag/author/:id without force parameter', async () => {
      vi.mocked(request.delete).mockResolvedValue(undefined)

      await authorApi.delete('test-id')

      expect(request.delete).toHaveBeenCalledWith('/config/tag/author/test-id', { 
        params: { force: undefined } 
      })
    })

    it('should call DELETE /config/tag/author/:id with force=true', async () => {
      vi.mocked(request.delete).mockResolvedValue(undefined)

      await authorApi.delete('test-id', true)

      expect(request.delete).toHaveBeenCalledWith('/config/tag/author/test-id', { 
        params: { force: true } 
      })
    })

    it('should call DELETE /config/tag/author/:id with force=false', async () => {
      vi.mocked(request.delete).mockResolvedValue(undefined)

      await authorApi.delete('test-id', false)

      expect(request.delete).toHaveBeenCalledWith('/config/tag/author/test-id', { 
        params: { force: false } 
      })
    })
  })

  describe('import', () => {
    it('should call POST /config/tag/author/import with author array', async () => {
      const importData = [
        { name: 'Author1', aliases: [] },
        { name: 'Author2', aliases: ['Alias'] }
      ]
      const mockResponse = {
        successCount: 2,
        failureCount: 0,
        errors: []
      }
      vi.mocked(request.post).mockResolvedValue(mockResponse)

      const result = await authorApi.import(importData)

      expect(request.post).toHaveBeenCalledWith('/config/tag/author/import', importData)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('export', () => {
    it('should call GET /config/tag/author/export', async () => {
      const mockResponse = [
        { name: 'Author1', aliases: [] },
        { name: 'Author2', aliases: ['Alias'] }
      ]
      vi.mocked(request.get).mockResolvedValue(mockResponse)

      const result = await authorApi.export()

      expect(request.get).toHaveBeenCalledWith('/config/tag/author/export')
      expect(result).toEqual(mockResponse)
    })
  })
})
