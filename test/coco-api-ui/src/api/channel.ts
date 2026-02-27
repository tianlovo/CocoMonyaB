import request from '@/utils/request'
import type { Channel, ChannelCreateDTO, ChannelUpdateDTO, ChannelQueryParams, TgChannel, TgChannelQueryParams } from '@/types/models'
import type { PageResponse } from '@/types/api'

export const channelApi = {
  // Get paginated list
  getPage(params: ChannelQueryParams) {
    return request.get<any, PageResponse<Channel>>('/channel/page', { params })
  },

  // Get all channels
  getList() {
    return request.get<any, Channel[]>('/channel/list')
  },

  // Get by ID
  getById(id: string) {
    return request.get<any, Channel>(`/channel/${id}`)
  },

  // Create
  create(data: ChannelCreateDTO) {
    return request.post<any, Channel>('/channel', data)
  },

  // Update
  update(id: string, data: ChannelUpdateDTO) {
    return request.put<any, Channel>(`/channel/${id}`, data)
  },

  // Delete
  delete(id: string) {
    return request.delete<any, void>(`/channel/${id}`)
  },

  // Get Telegram channels (logged-in account)
  getTgChannels(params: TgChannelQueryParams) {
    return request.get<any, PageResponse<TgChannel>>('/channel/tg/logged-in', { params })
  }
}
