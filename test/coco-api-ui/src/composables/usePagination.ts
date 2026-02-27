import { reactive } from 'vue'
import type { PaginationState } from '@/types/common'

export function usePagination(initialSize = 10) {
  const pagination = reactive<PaginationState>({
    current: 1,
    size: initialSize,
    total: 0
  })

  const handlePageChange = (page: number) => {
    pagination.current = page
  }

  const handleSizeChange = (size: number) => {
    pagination.size = size
    pagination.current = 1
  }

  const reset = () => {
    pagination.current = 1
    pagination.total = 0
  }

  return {
    pagination,
    handlePageChange,
    handleSizeChange,
    reset
  }
}
