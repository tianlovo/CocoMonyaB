import request from '@/utils/request'
import type { Author, AuthorCreateDTO, AuthorUpdateDTO } from '@/types/models'
import type { PageParams, PageResponse, ImportResult } from '@/types/api'

export const authorApi = {
  // Get paginated list
  getPage(params: PageParams & { keyword?: string }) {
    return request.get<any, PageResponse<Author>>('/config/tag/author/page', { params })
  },

  // Get by ID
  getById(id: string) {
    return request.get<any, Author>(`/config/tag/author/${id}`)
  },

  // Get by name
  getByName(name: string) {
    return request.get<any, Author>(`/config/tag/author/name/${name}`)
  },

  // Create
  create(data: AuthorCreateDTO) {
    return request.post<any, Author>('/config/tag/author', data)
  },

  // Update
  update(id: string, data: AuthorUpdateDTO) {
    return request.put<any, Author>(`/config/tag/author/${id}`, data)
  },

  // Delete
  delete(id: string, force?: boolean) {
    return request.delete<any, void>(`/config/tag/author/${id}`, { params: { force } })
  },

  // Import
  import(data: AuthorCreateDTO[]) {
    return request.post<any, ImportResult>('/config/tag/author/import', data)
  },

  // Export
  export() {
    return request.get<any, AuthorCreateDTO[]>('/config/tag/author/export')
  }
}
