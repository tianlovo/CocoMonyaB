import { ref } from 'vue'

export interface UseApiOptions {
  immediate?: boolean
  onSuccess?: (data: any) => void
  onError?: (error: Error) => void
}

export function useApi<T>(
  apiFunc: (...args: any[]) => Promise<T>,
  options?: UseApiOptions
) {
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const data = ref<T | null>(null)

  const execute = async (...args: any[]) => {
    loading.value = true
    error.value = null
    try {
      data.value = await apiFunc(...args)
      options?.onSuccess?.(data.value)
      return data.value
    } catch (e) {
      error.value = e as Error
      options?.onError?.(e as Error)
      throw e
    } finally {
      loading.value = false
    }
  }

  return { loading, error, data, execute }
}
