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
