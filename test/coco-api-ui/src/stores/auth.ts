import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { LoginResult } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref<string>(localStorage.getItem('coco_api_ui_token') || '')
  const isAuthenticated = ref(false)
  const isLoading = ref(false)

  // Getters
  const getToken = computed(() => token.value)
  const getIsAuthenticated = computed(() => isAuthenticated.value)

  // Actions
  async function login(accessToken: string): Promise<boolean> {
    isLoading.value = true
    try {
      const result = await authApi.login(accessToken)
      token.value = result.token
      localStorage.setItem('coco_api_ui_token', result.token)
      isAuthenticated.value = true
      return true
    } catch (error) {
      console.error('登录失败:', error)
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function checkAuth(): Promise<boolean> {
    const storedToken = localStorage.getItem('coco_api_ui_token')
    if (!storedToken) {
      isAuthenticated.value = false
      return false
    }

    try {
      const result = await authApi.verify(storedToken)
      if (result.valid) {
        token.value = storedToken
        isAuthenticated.value = true
        return true
      } else {
        logout()
        return false
      }
    } catch (error) {
      console.error('验证登录状态失败:', error)
      logout()
      return false
    }
  }

  function logout() {
    token.value = ''
    isAuthenticated.value = false
    localStorage.removeItem('coco_api_ui_token')
  }

  return {
    token,
    isAuthenticated,
    isLoading,
    getToken,
    getIsAuthenticated,
    login,
    checkAuth,
    logout
  }
})
