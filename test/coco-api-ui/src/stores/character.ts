import { defineStore } from 'pinia'
import { ref } from 'vue'
import { characterApi } from '@/api/character'
import type { Character, CharacterCreateDTO, CharacterUpdateDTO } from '@/types/models'
import type { PageParams, PageResponse } from '@/types/api'

export const useCharacterStore = defineStore('character', () => {
  // State
  const characters = ref<Character[]>([])
  const characterCache = ref<Map<string, Character>>(new Map())
  const loading = ref(false)
  const currentPage = ref<PageResponse<Character> | null>(null)

  // Actions
  const fetchPage = async (params: PageParams & { keyword?: string; workId?: string; species?: string }) => {
    loading.value = true
    try {
      const response = await characterApi.getPage(params)
      currentPage.value = response
      characters.value = response.records
      
      // Update cache
      response.records.forEach(character => {
        characterCache.value.set(character.id, character)
      })
      
      return response
    } finally {
      loading.value = false
    }
  }

  const fetchById = async (id: string) => {
    // Check cache first
    if (characterCache.value.has(id)) {
      return characterCache.value.get(id)!
    }

    loading.value = true
    try {
      const character = await characterApi.getById(id)
      characterCache.value.set(character.id, character)
      return character
    } finally {
      loading.value = false
    }
  }

  const fetchByName = async (name: string) => {
    loading.value = true
    try {
      const character = await characterApi.getByName(name)
      characterCache.value.set(character.id, character)
      return character
    } finally {
      loading.value = false
    }
  }

  const fetchByWork = async (workId: string) => {
    loading.value = true
    try {
      const characters = await characterApi.getByWork(workId)
      characters.forEach(character => {
        characterCache.value.set(character.id, character)
      })
      return characters
    } finally {
      loading.value = false
    }
  }

  const createCharacter = async (data: CharacterCreateDTO) => {
    loading.value = true
    try {
      const character = await characterApi.create(data)
      characterCache.value.set(character.id, character)
      
      // Refresh current page if exists
      if (currentPage.value) {
        await fetchPage({
          current: currentPage.value.current,
          size: currentPage.value.size
        })
      }
      
      return character
    } finally {
      loading.value = false
    }
  }

  const updateCharacter = async (id: string, data: CharacterUpdateDTO) => {
    loading.value = true
    try {
      const character = await characterApi.update(id, data)
      characterCache.value.set(character.id, character)
      
      // Update in current list
      const index = characters.value.findIndex(c => c.id === id)
      if (index !== -1) {
        characters.value[index] = character
      }
      
      return character
    } finally {
      loading.value = false
    }
  }

  const deleteCharacter = async (id: string, force?: boolean) => {
    loading.value = true
    try {
      await characterApi.delete(id, force)
      characterCache.value.delete(id)
      
      // Remove from current list
      characters.value = characters.value.filter(c => c.id !== id)
      
      // Update total count
      if (currentPage.value) {
        currentPage.value.total -= 1
      }
    } finally {
      loading.value = false
    }
  }

  const importCharacters = async (data: CharacterCreateDTO[]) => {
    loading.value = true
    try {
      const result = await characterApi.import(data)
      
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

  const exportCharacters = async () => {
    loading.value = true
    try {
      return await characterApi.export()
    } finally {
      loading.value = false
    }
  }

  const clearCache = () => {
    characterCache.value.clear()
  }

  const getCachedCharacter = (id: string) => {
    return characterCache.value.get(id)
  }

  return {
    // State
    characters,
    characterCache,
    loading,
    currentPage,
    
    // Actions
    fetchPage,
    fetchById,
    fetchByName,
    fetchByWork,
    createCharacter,
    updateCharacter,
    deleteCharacter,
    importCharacters,
    exportCharacters,
    clearCache,
    getCachedCharacter
  }
})
