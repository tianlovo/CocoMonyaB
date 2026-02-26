import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authorApi } from '@/api/author'
import type { Author, AuthorCreateDTO, AuthorUpdateDTO } from '@/types/models'
import type { PageParams, PageResponse } from '@/types/api'

export const useAuthorStore = defineStore('author', () => {
  // State
  const authors = ref<Author[]>([])
  const authorCache = ref<Map<string, Author>>(new Map())
  const loading = ref(false)
  const currentPage = ref<PageResponse<Author> | null>(null)

  // Actions
  const fetchPage = async (params: PageParams & { keyword?: string }) => {
    loading.value = true
    try {
      const response = await authorApi.getPage(params)
      currentPage.value = response
      authors.value = response.records
      
      // Update cache
      response.records.forEach(author => {
        authorCache.value.set(author.id, author)
      })
      
      return response
    } finally {
      loading.value = false
    }
  }

  const fetchById = async (id: string) => {
    // Check cache first
    if (authorCache.value.has(id)) {
      return authorCache.value.get(id)!
    }

    loading.value = true
    try {
      const author = await authorApi.getById(id)
      authorCache.value.set(author.id, author)
      return author
    } finally {
      loading.value = false
    }
  }

  const fetchByName = async (name: string) => {
    loading.value = true
    try {
      const author = await authorApi.getByName(name)
      authorCache.value.set(author.id, author)
      return author
    } finally {
      loading.value = false
    }
  }

  const createAuthor = async (data: AuthorCreateDTO) => {
    loading.value = true
    try {
      const author = await authorApi.create(data)
      authorCache.value.set(author.id, author)
      
      // Refresh current page if exists
      if (currentPage.value) {
        await fetchPage({
          current: currentPage.value.current,
          size: currentPage.value.size
        })
      }
      
      return author
    } finally {
      loading.value = false
    }
  }

  const updateAuthor = async (id: string, data: AuthorUpdateDTO) => {
    loading.value = true
    try {
      const author = await authorApi.update(id, data)
      authorCache.value.set(author.id, author)
      
      // Update in current list
      const index = authors.value.findIndex(a => a.id === id)
      if (index !== -1) {
        authors.value[index] = author
      }
      
      return author
    } finally {
      loading.value = false
    }
  }

  const deleteAuthor = async (id: string, force?: boolean) => {
    loading.value = true
    try {
      await authorApi.delete(id, force)
      authorCache.value.delete(id)
      
      // Remove from current list
      authors.value = authors.value.filter(a => a.id !== id)
      
      // Update total count
      if (currentPage.value) {
        currentPage.value.total -= 1
      }
    } finally {
      loading.value = false
    }
  }

  const importAuthors = async (data: AuthorCreateDTO[]) => {
    loading.value = true
    try {
      const result = await authorApi.import(data)
      
      // Refresh current page after import
      if (currentPage.value) {
        await fetchPage({
          current: currentPage.value.current,
          size: currentPage.value.size
        })
      }
      
      return result
    } finally {
      loading.value = false
    }
  }

  const exportAuthors = async () => {
    loading.value = true
    try {
      return await authorApi.export()
    } finally {
      loading.value = false
    }
  }

  const clearCache = () => {
    authorCache.value.clear()
  }

  const getCachedAuthor = (id: string) => {
    return authorCache.value.get(id)
  }

  return {
    // State
    authors,
    authorCache,
    loading,
    currentPage,
    
    // Actions
    fetchPage,
    fetchById,
    fetchByName,
    createAuthor,
    updateAuthor,
    deleteAuthor,
    importAuthors,
    exportAuthors,
    clearCache,
    getCachedAuthor
  }
})
