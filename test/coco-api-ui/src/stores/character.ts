import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Character } from '@/types/models'

export const useCharacterStore = defineStore('character', () => {
  const characters = ref<Character[]>([])
  const loading = ref(false)

  return {
    characters,
    loading
  }
})
