import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Author } from '@/types/models'

export const useAuthorStore = defineStore('author', () => {
  const authors = ref<Author[]>([])
  const loading = ref(false)

  return {
    authors,
    loading
  }
})
