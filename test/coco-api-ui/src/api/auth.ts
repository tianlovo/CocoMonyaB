import request from '@/utils/request'

export interface LoginResult {
  token: string
  expiresIn: number
}

export const authApi = {
  // 登录
  login(token: string) {
    return request.post<any, LoginResult>('/auth/login', { token })
  },

  // 验证token
  verify(token: string) {
    return request.post<any, { valid: boolean }>('/auth/verify', { token })
  }
}
