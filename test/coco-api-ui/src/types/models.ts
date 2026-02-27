// Author types
export interface Author {
  id: string
  name: string
  aliases: string[]
  signature: string | null
  urls: string[]
  avatarBase64: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface AuthorCreateDTO {
  name: string
  aliases: string[]
  signature?: string | null
  urls?: string[]
  avatarBase64?: string | null
  remark?: string | null
}

export interface AuthorUpdateDTO {
  name?: string
  aliases?: string[]
  signature?: string | null
  urls?: string[]
  avatarBase64?: string | null
  remark?: string | null
}

// Work types
export interface Work {
  id: string
  name: string
  aliases: string[]
  urls: string[]
  avatarBase64: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface WorkCreateDTO {
  name: string
  aliases: string[]
  urls?: string[]
  avatarBase64?: string | null
  remark?: string | null
}

export interface WorkUpdateDTO {
  name?: string
  aliases?: string[]
  urls?: string[]
  avatarBase64?: string | null
  remark?: string | null
}

// Character types
export interface Character {
  id: string
  name: string
  aliases: string[]
  workId: string | null
  workName: string | null
  species: string
  avatarBase64: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

export interface CharacterCreateDTO {
  name: string
  aliases: string[]
  workId?: string | null
  species: string
  avatarBase64?: string | null
  remark?: string | null
}

export interface CharacterUpdateDTO {
  name?: string
  aliases?: string[]
  workId?: string | null
  species?: string
  avatarBase64?: string | null
  remark?: string | null
}

// Channel types
export interface Channel {
  id: string
  channelId: number
  channelUsername: string | null
  channelTitle: string
  monitoringStatus: boolean
  createTime: string
  updateTime: string
}

export interface ChannelCreateDTO {
  channelId: number
  channelUsername?: string | null
  channelTitle: string
  monitoringStatus?: boolean
}

export interface ChannelUpdateDTO {
  channelUsername?: string | null
  channelTitle?: string
  monitoringStatus?: boolean
}

export interface ChannelQueryParams {
  current: number
  size: number
  channelUsername?: string
  monitoringStatus?: boolean
}

// Telegram Channel types
export interface TgChannel {
  chatId: number
  title: string
  username: string | null
  type: string
  isChannel: boolean
  memberCount: number
  description: string | null
}

export interface TgChannelQueryParams {
  current: number
  size: number
  forceRefresh?: boolean
}

// Tag Filter Config types
export interface TagFilterConfig {
  id: string
  authorIds: string[]
  characterIds: string[]
  workIds: string[]
  customTags: Record<string, string>
  matchMode: 'whitelist' | 'blacklist'
  enabled: boolean
  createTime: string
  updateTime: string
}

export interface TagFilterConfigCreateDTO {
  authorIds: string[]
  characterIds: string[]
  workIds: string[]
  customTags: Record<string, string>
  matchMode: 'whitelist' | 'blacklist'
  enabled: boolean
}

export interface TagFilterConfigUpdateDTO {
  authorIds?: string[]
  characterIds?: string[]
  workIds?: string[]
  customTags?: Record<string, string>
  matchMode?: 'whitelist' | 'blacklist'
  enabled?: boolean
}

// Message Tracking types
export interface Message {
  id: string
  chatId: number
  messageId: number
  mediaAlbumId: number | null
  date: number
  rawJson: string
  createTime: string
  updateTime: string
}

export interface ChannelMessage {
  id: string
  messageId: number
  chatId: number
  channelUsername: string
  channelTitle: string
  date: number
  editDate: number | null
  contentType: string
  textContent: string | null
  mediaFiles: MediaFile[]
  webPage: WebPageInfo | null
  mediaAlbumId: number | null
  isMediaGroup: boolean
  mediaGroupItemCount: number | null
  mediaGroupMessageIds: number[] | null
  views: number | null
  forwards: number | null
  status: string
  createTime: string
  updateTime: string
}

export interface MediaFile {
  fileId: string
  fileType: string
  fileSize: number
  mimeType: string
  localPath: string | null
  downloaded: boolean
}

export interface WebPageInfo {
  url: string
  displayUrl: string
  type: string
  siteName: string | null
  title: string | null
  description: string | null
  author: string | null
  duration: number | null
  hasInstantView: boolean
  instantViewVersion: string | null
}

export interface ForwardQueue {
  id: string
  sourceChatId: number
  sourceMessageId: number
  mediaGroupMessageIds: number[] | null
  matchedTags: string[]
  status: string
  createTime: string
  updateTime: string
  forwardTime: string | null
  retryCount: number
  errorMessage: string | null
}

export interface ProcessedMessage {
  id: string
  chatId: number
  messageId: number
  messageType: string
  isRead: boolean
  isMatched: boolean
  matchedTags: string[]
  processTime: string
  readTime: string | null
  createTime: string
  updateTime: string
}

export interface UnreadBuffer {
  id: string
  chatId: number
  messageId: number
  fetchTime: string
  status: string
  errorMessage: string | null
  createTime: string
  updateTime: string
}

// Message Tracking Query Parameter types
export interface MessageQueryParams {
  current: number
  size: number
  chatId?: number
  startDate?: number
  endDate?: number
}

export interface ChannelMessageQueryParams {
  current: number
  size: number
  chatId?: number
  status?: string
  startDate?: number
  endDate?: number
}

export interface ForwardQueueQueryParams {
  current: number
  size: number
  sourceChatId?: number
  status?: string
}

export interface ProcessedMessageQueryParams {
  current: number
  size: number
  chatId?: number
  isRead?: boolean
  isMatched?: boolean
}

export interface UnreadBufferQueryParams {
  current: number
  size: number
  chatId?: number
  status?: string
}
