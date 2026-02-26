import request from '@/utils/request'
import type { Work, WorkCreateDTO, WorkUpdateDTO } from '@/types/models'
import type { PageParams, PageResponse, ImportResult } from '@/types/api'

export const workApi = {
  // Get paginated list
  getPage(params: PageParams & { keyword?: string }) {
    return request.get<any, PageResponse<Work>>('/config/tag/work/page', { params })
  },

  // Get by ID
  getById(id: string) {
    return request.get<any, Work>(`/config/tag/work/${id}`)
  },

  // Get by name
  getByName(name: string) {
    return request.get<any, Work>(`/config/tag/work/name/${name}`)
  },

  // Create
  create(data: WorkCreateDTO) {
    return request.post<any, Work>('/config/tag/work', data)
  },

  // Update
  update(id: string, data: WorkUpdateDTO) {
    return request.put<any, Work>(`/config/tag/work/${id}`, data)
  },

  // Delete
  delete(id: string, force?: boolean) {
    return request.delete<any, void>(`/config/tag/work/${id}`, { params: { force } })
  },

  // Import
  import(data: WorkCreateDTO[]) {
    return request.post<any, ImportResult>('/config/tag/work/import', data)
  },

  // Export
  export() {
    return request.get<any, WorkCreateDTO[]>('/config/tag/work/export')
  }
}
