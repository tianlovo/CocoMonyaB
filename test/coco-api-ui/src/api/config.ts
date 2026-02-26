import request from '@/utils/request'
import type { TagFilterConfig, TagFilterConfigCreateDTO, TagFilterConfigUpdateDTO } from '@/types/models'

export const configApi = {
  // Get global config
  getGlobal() {
    return request.get<any, TagFilterConfig>('/config/tag/filter/global')
  },

  // Create or update
  createOrUpdate(data: TagFilterConfigCreateDTO) {
    return request.post<any, TagFilterConfig>('/config/tag/filter', data)
  },

  // Update
  update(id: string, data: TagFilterConfigUpdateDTO) {
    return request.put<any, TagFilterConfig>(`/config/tag/filter/${id}`, data)
  },

  // Get by ID
  getById(id: string) {
    return request.get<any, TagFilterConfig>(`/config/tag/filter/${id}`)
  },

  // Expand tags test
  expandTags(data: TagFilterConfigCreateDTO) {
    return request.post<any, string[]>('/config/tag/filter/expand', data)
  }
}
