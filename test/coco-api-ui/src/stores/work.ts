import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Work } from '@/types/models'

export const useWorkStore = defineStore('work', () => {
  const works = ref<Work[]>([])
  const loading = ref(false)

  return {
    works,
    loading
  }
})
