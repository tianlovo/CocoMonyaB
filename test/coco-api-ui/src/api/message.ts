import request from '@/utils/request'
import type { PageResponse } from '@/types/api'
import type {
  Message,
  ChannelMessage,
  ForwardQueue,
  ProcessedMessage,
  UnreadBuffer,
  MessageQueryParams,
  ChannelMessageQueryParams,
  ForwardQueueQueryParams,
  ProcessedMessageQueryParams,
  UnreadBufferQueryParams
} from '@/types/models'

export const messageApi = {
  // Get messages page
  fetchMessages(params: MessageQueryParams) {
    return request.get<any, PageResponse<Message>>('/message/page', { params })
  },

  // Get channel messages page
  fetchChannelMessages(params: ChannelMessageQueryParams) {
    return request.get<any, PageResponse<ChannelMessage>>('/channel-message/page', { params })
  },

  // Get forward queue page
  fetchForwardQueue(params: ForwardQueueQueryParams) {
    return request.get<any, PageResponse<ForwardQueue>>('/forward-queue/page', { params })
  },

  // Get processed messages page
  fetchProcessedMessages(params: ProcessedMessageQueryParams) {
    return request.get<any, PageResponse<ProcessedMessage>>('/processed-message/page', { params })
  },

  // Get unread buffer page
  fetchUnreadBuffer(params: UnreadBufferQueryParams) {
    return request.get<any, PageResponse<UnreadBuffer>>('/unread-buffer/page', { params })
  }
}
