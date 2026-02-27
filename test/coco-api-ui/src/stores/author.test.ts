import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthorStore } from './author'
import { authorApi } from '@/api/author'
import type { Author, AuthorCreateDTO } from '@/types/models'
import type { PageResponse } from '@/types/api'

vi.mock('@/api/author')

describe('Author Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const mockAuthor: Author = {
    id: '1',
    name: 'Test Author',
    aliases: ['Alias1'],
    signature: 'Test signature',
    urls: ['https://example.com'],
    avatarBase64: null,
    remark: 'Test remark',
    createTime: '2024-01-01T00:00:00Z',
    updateTime: '2024-01-01T00:00:00Z'
  }

  const mockPageResponse: PageResponse<Author> = {
    records: [mockAuthor],
    current: 1,
    size: 10,
    total: 1,
    pages: 1
  }

  describe('fetchPage', () => {
    it('should fetch authors and update state', async () => {
      vi.mocked(authorApi.getPage).mockResolvedValue(mockPageResponse)
      
      const store = useAuthorStore()
      const result = await store.fetchPage({ current: 1, size: 10 })

      expect(result).toEqual(mockPageResponse)
      expect(store.authors).toEqual([mockAuthor])
      expect(store.currentPage).toEqual(mockPageResponse)
      expect(store.loading).toBe(false)
    })

    it('should update cache when fetching page', async () => {
      vi.mocked(authorApi.getPage).mockResolvedValue(mockPageResponse)
      
      const store = useAuthorStore()
      await store.fetchPage({ current: 1, size: 10 })

      expect(store.getCachedAuthor('1')).toEqual(mockAuthor)
    })

    it('should set loading state during fetch', async () => {
      vi.mocked(authorApi.getPage).mockImplementation(() => {
        const store = useAuthorStore()
        expect(store.loading).toBe(true)
        return Promise.resolve(mockPageResponse)
      })
      
      const store = useAuthorStore()
      await store.fetchPage({ current: 1, size: 10 })
    })
  })

  describe('fetchById', () => {
    it('should return cached author if available', async () => {
      const store = useAuthorStore()
      store.authorCache.set('1', mockAuthor)

      const result = await store.fetchById('1')

      expect(result).toEqual(mockAuthor)
      expect(authorApi.getById).not.toHaveBeenCalled()
    })

    it('should fetch from API if not cached', async () => {
      vi.mocked(authorApi.getById).mockResolvedValue(mockAuthor)
      
      const store = useAuthorStore()
      const result = await store.fetchById('1')

      expect(result).toEqual(mockAuthor)
      expect(authorApi.getById).toHaveBeenCalledWith('1')
      expect(store.getCachedAuthor('1')).toEqual(mockAuthor)
    })
  })

  describe('createAuthor', () => {
    it('should create author and update cache', async () => {
      const createData: AuthorCreateDTO = {
        name: 'New Author',
        aliases: []
      }
      vi.mocked(authorApi.create).mockResolvedValue(mockAuthor)
      
      const store = useAuthorStore()
      const result = await store.createAuthor(createData)

      expect(result).toEqual(mockAuthor)
      expect(store.getCachedAuthor('1')).toEqual(mockAuthor)
    })

    it('should refresh current page after creation', async () => {
      const createData: AuthorCreateDTO = {
        name: 'New Author',
        aliases: []
      }
      vi.mocked(authorApi.create).mockResolvedValue(mockAuthor)
      vi.mocked(authorApi.getPage).mockResolvedValue(mockPageResponse)
      
      const store = useAuthorStore()
      store.currentPage = mockPageResponse
      
      await store.createAuthor(createData)

      expect(authorApi.getPage).toHaveBeenCalledWith({
        current: 1,
        size: 10
      })
    })
  })

  describe('updateAuthor', () => {
    it('should update author and cache', async () => {
      const updateData = { name: 'Updated Name' }
      const updatedAuthor = { ...mockAuthor, name: 'Updated Name' }
      vi.mocked(authorApi.update).mockResolvedValue(updatedAuthor)
      
      const store = useAuthorStore()
      store.authors = [mockAuthor]
      
      const result = await store.updateAuthor('1', updateData)

      expect(result).toEqual(updatedAuthor)
      expect(store.getCachedAuthor('1')).toEqual(updatedAuthor)
      expect(store.authors[0]).toEqual(updatedAuthor)
    })
  })

  describe('deleteAuthor', () => {
    it('should delete author and remove from cache', async () => {
      vi.mocked(authorApi.delete).mockResolvedValue(undefined)
      
      const store = useAuthorStore()
      store.authors = [mockAuthor]
      store.authorCache.set('1', mockAuthor)
      store.currentPage = mockPageResponse
      
      await store.deleteAuthor('1')

      expect(store.authors).toEqual([])
      expect(store.getCachedAuthor('1')).toBeUndefined()
      expect(store.currentPage?.total).toBe(0)
    })

    it('should pass force parameter to API', async () => {
      vi.mocked(authorApi.delete).mockResolvedValue(undefined)
      
      const store = useAuthorStore()
      await store.deleteAuthor('1', true)

      expect(authorApi.delete).toHaveBeenCalledWith('1', true)
    })
  })

  describe('importAuthors', () => {
    it('should import authors and refresh page', async () => {
      const importData: AuthorCreateDTO[] = [
        { name: 'Author 1', aliases: [] },
        { name: 'Author 2', aliases: [] }
      ]
      const importResult = {
        successCount: 2,
        failureCount: 0,
        errors: []
      }
      vi.mocked(authorApi.import).mockResolvedValue(importResult)
      vi.mocked(authorApi.getPage).mockResolvedValue(mockPageResponse)
      
      const store = useAuthorStore()
      store.currentPage = mockPageResponse
      
      const result = await store.importAuthors(importData)

      expect(result).toEqual(importResult)
      expect(authorApi.getPage).toHaveBeenCalled()
    })
  })

  describe('exportAuthors', () => {
    it('should export authors', async () => {
      const exportData: AuthorCreateDTO[] = [
        { name: 'Author 1', aliases: [] }
      ]
      vi.mocked(authorApi.export).mockResolvedValue(exportData)
      
      const store = useAuthorStore()
      const result = await store.exportAuthors()

      expect(result).toEqual(exportData)
    })
  })

  describe('clearCache', () => {
    it('should clear all cached authors', () => {
      const store = useAuthorStore()
      store.authorCache.set('1', mockAuthor)
      store.authorCache.set('2', { ...mockAuthor, id: '2' })

      store.clearCache()

      expect(store.authorCache.size).toBe(0)
    })
  })
})
