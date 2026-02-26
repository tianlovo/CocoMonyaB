import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TagFilterConfig } from '@/types/models'

export const useConfigStore = defineStore('config', () => {
  const config = ref<TagFilterConfig | null>(null)
  const loading = ref(false)

  return {
    config,
    loading
  }
})
