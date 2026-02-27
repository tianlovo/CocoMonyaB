import { defineStore } from 'pinia'
import { ref } from 'vue'
import { channelApi } from '@/api/channel'
import type { Channel, ChannelCreateDTO, ChannelUpdateDTO, ChannelQueryParams, TgChannel, TgChannelQueryParams } from '@/types/models'
import type { PageResponse } from '@/types/api'

export const useChannelStore = defineStore('channel', () => {
  const channels = ref<Channel[]>([])
  const tgChannels = ref<TgChannel[]>([])
  const loading = ref(false)

  const fetchPage = async (params: ChannelQueryParams): Promise<PageResponse<Channel>> => {
    loading.value = true
    try {
      const response = await channelApi.getPage(params)
      channels.value = response.records
      return response
    } finally {
      loading.value = false
    }
  }

  const fetchList = async (): Promise<Channel[]> => {
    loading.value = true
    try {
      const response = await channelApi.getList()
      channels.value = response
      return response
    } finally {
      loading.value = false
    }
  }

  const fetchById = async (id: string): Promise<Channel> => {
    return await channelApi.getById(id)
  }

  const createChannel = async (data: ChannelCreateDTO): Promise<Channel> => {
    return await channelApi.create(data)
  }

  const updateChannel = async (id: string, data: ChannelUpdateDTO): Promise<Channel> => {
    return await channelApi.update(id, data)
  }

  const deleteChannel = async (id: string): Promise<void> => {
    await channelApi.delete(id)
  }

  const fetchTgChannels = async (params: TgChannelQueryParams): Promise<PageResponse<TgChannel>> => {
    loading.value = true
    try {
      const response = await channelApi.getTgChannels(params)
      tgChannels.value = response.records
      return response
    } finally {
      loading.value = false
    }
  }

  const refreshTgChannels = async (params: Omit<TgChannelQueryParams, 'forceRefresh'>): Promise<PageResponse<TgChannel>> => {
    return fetchTgChannels({ ...params, forceRefresh: true })
  }

  return {
    channels,
    tgChannels,
    loading,
    fetchPage,
    fetchList,
    fetchById,
    createChannel,
    updateChannel,
    deleteChannel,
    fetchTgChannels,
    refreshTgChannels
  }
})
