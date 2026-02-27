import { defineStore } from 'pinia'
import { ref } from 'vue'
import { workApi } from '@/api/work'
import type { Work, WorkCreateDTO, WorkUpdateDTO } from '@/types/models'
import type { PageParams, PageResponse } from '@/types/api'

export const useWorkStore = defineStore('work', () => {
  // State
  const works = ref<Work[]>([])
  const workCache = ref<Map<string, Work>>(new Map())
  const loading = ref(false)
  const currentPage = ref<PageResponse<Work> | null>(null)

  // Actions
  const fetchPage = async (params: PageParams & { keyword?: string }) => {
    loading.value = true
    try {
      const response = await workApi.getPage(params)
      currentPage.value = response
      works.value = response.records
      
      // Update cache
      response.records.forEach(work => {
        workCache.value.set(work.id, work)
      })
      
      return response
    } finally {
      loading.value = false
    }
  }

  const fetchById = async (id: string) => {
    // Check cache first
    if (workCache.value.has(id)) {
      return workCache.value.get(id)!
    }

    loading.value = true
    try {
      const work = await workApi.getById(id)
      workCache.value.set(work.id, work)
      return work
    } finally {
      loading.value = false
    }
  }

  const fetchByName = async (name: string) => {
    loading.value = true
    try {
      const work = await workApi.getByName(name)
      workCache.value.set(work.id, work)
      return work
    } finally {
      loading.value = false
    }
  }

  const createWork = async (data: WorkCreateDTO) => {
    loading.value = true
    try {
      const work = await workApi.create(data)
      workCache.value.set(work.id, work)
      
      // Refresh current page if exists
      if (currentPage.value) {
        await fetchPage({
          current: currentPage.value.current,
          size: currentPage.value.size
        })
      }
      
      return work
    } finally {
      loading.value = false
    }
  }

  const updateWork = async (id: string, data: WorkUpdateDTO) => {
    loading.value = true
    try {
      const work = await workApi.update(id, data)
      workCache.value.set(work.id, work)
      
      // Update in current list
      const index = works.value.findIndex(w => w.id === id)
      if (index !== -1) {
        works.value[index] = work
      }
      
      return work
    } finally {
      loading.value = false
    }
  }

  const deleteWork = async (id: string, force?: boolean) => {
    loading.value = true
    try {
      await workApi.delete(id, force)
      workCache.value.delete(id)
      
      // Remove from current list
      works.value = works.value.filter(w => w.id !== id)
      
      // Update total count
      if (currentPage.value) {
        currentPage.value.total -= 1
      }
    } finally {
      loading.value = false
    }
  }

  const importWorks = async (data: WorkCreateDTO[]) => {
    loading.value = true
    try {
      const result = await workApi.import(data)
      
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

  const exportWorks = async () => {
    loading.value = true
    try {
      return await workApi.export()
    } finally {
      loading.value = false
    }
  }

  const clearCache = () => {
    workCache.value.clear()
  }

  const getCachedWork = (id: string) => {
    return workCache.value.get(id)
  }

  return {
    // State
    works,
    workCache,
    loading,
    currentPage,
    
    // Actions
    fetchPage,
    fetchById,
    fetchByName,
    createWork,
    updateWork,
    deleteWork,
    importWorks,
    exportWorks,
    clearCache,
    getCachedWork
  }
})
