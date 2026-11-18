import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { Creator, CreatorSaveRequest } from '../types/catalog'

export interface CreatorFilters {
  channel_id?: number
  enabled?: boolean
  name?: string
}

export const creatorApi = {
  async list(filters: CreatorFilters): Promise<Creator[]> {
    const response = await http.get<ApiResponse<{ list: Creator[] }>>('/api/admin/creators', { params: filters })
    return response.data.data.list
  },

  async create(request: CreatorSaveRequest): Promise<Creator> {
    const response = await http.post<ApiResponse<Creator>>('/api/admin/creators', request)
    return response.data.data
  },

  async update(id: number, request: CreatorSaveRequest): Promise<Creator> {
    const response = await http.put<ApiResponse<Creator>>(`/api/admin/creators/${id}`, request)
    return response.data.data
  },

  async setEnabled(id: number, enabled: boolean): Promise<Creator> {
    const response = await http.put<ApiResponse<Creator>>(`/api/admin/creators/${id}/enabled`, { enabled })
    return response.data.data
  },
}
