import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

export interface UseFormOptions<T> {
  initialData: T
  rules: FormRules
  onSubmit: (data: T) => Promise<void>
}

export function useForm<T extends Record<string, any>>(
  options: UseFormOptions<T>
) {
  const formRef = ref<FormInstance>()
  const formData = reactive<T>({ ...options.initialData })
  const loading = ref(false)

  const validate = async () => {
    if (!formRef.value) return false
    try {
      await formRef.value.validate()
      return true
    } catch {
      return false
    }
  }

  const submit = async () => {
    if (!(await validate())) return
    loading.value = true
    try {
      await options.onSubmit(formData)
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    Object.assign(formData, options.initialData)
    formRef.value?.resetFields()
  }

  return {
    formRef,
    formData,
    loading,
    validate,
    submit,
    reset
  }
}
