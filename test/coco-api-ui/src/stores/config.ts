import { defineStore } from 'pinia'
import { ref } from 'vue'
import { configApi } from '@/api/config'
import type { TagFilterConfig, TagFilterConfigCreateDTO, TagFilterConfigUpdateDTO } from '@/types/models'

export const useConfigStore = defineStore('config', () => {
  // State
  const config = ref<TagFilterConfig | null>(null)
  const loading = ref(false)

  // Actions
  const fetchGlobal = async () => {
    loading.value = true
    try {
      const response = await configApi.getGlobal()
      config.value = response
      return response
    } catch (error: any) {
      // If config doesn't exist (404 or -60002), set to null
      if (error?.code === -60002 || error?.response?.status === 404) {
        config.value = null
        return null
      }
      throw error
    } finally {
      loading.value = false
    }
  }

  const fetchById = async (id: string) => {
    loading.value = true
    try {
      const response = await configApi.getById(id)
      config.value = response
      return response
    } finally {
      loading.value = false
    }
  }

  const createOrUpdate = async (data: TagFilterConfigCreateDTO) => {
    loading.value = true
    try {
      const response = await configApi.createOrUpdate(data)
      config.value = response
      return response
    } finally {
      loading.value = false
    }
  }

  const update = async (id: string, data: TagFilterConfigUpdateDTO) => {
    loading.value = true
    try {
      const response = await configApi.update(id, data)
      config.value = response
      return response
    } finally {
      loading.value = false
    }
  }

  const expandTags = async (data: TagFilterConfigCreateDTO) => {
    loading.value = true
    try {
      return await configApi.expandTags(data)
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    config,
    loading,
    
    // Actions
    fetchGlobal,
    fetchById,
    createOrUpdate,
    update,
    expandTags
  }
})
