import request from '@/utils/request'
import type { Character, CharacterCreateDTO, CharacterUpdateDTO } from '@/types/models'
import type { PageParams, PageResponse, ImportResult } from '@/types/api'

export const characterApi = {
  // Get paginated list
  getPage(params: PageParams & { keyword?: string; workId?: string; species?: string }) {
    return request.get<any, PageResponse<Character>>('/config/tag/character/page', { params })
  },

  // Get by ID
  getById(id: string) {
    return request.get<any, Character>(`/config/tag/character/${id}`)
  },

  // Get by name
  getByName(name: string) {
    return request.get<any, Character>(`/config/tag/character/name/${name}`)
  },

  // Get by work
  getByWork(workId: string) {
    return request.get<any, Character[]>(`/config/tag/character/work/${workId}`)
  },

  // Create
  create(data: CharacterCreateDTO) {
    return request.post<any, Character>('/config/tag/character', data)
  },

  // Update
  update(id: string, data: CharacterUpdateDTO) {
    return request.put<any, Character>(`/config/tag/character/${id}`, data)
  },

  // Delete
  delete(id: string, force?: boolean) {
    return request.delete<any, void>(`/config/tag/character/${id}`, { params: { force } })
  },

  // Import
  import(data: CharacterCreateDTO[]) {
    return request.post<any, ImportResult>('/config/tag/character/import', data)
  },

  // Export
  export() {
    return request.get<any, CharacterCreateDTO[]>('/config/tag/character/export')
  }
}
