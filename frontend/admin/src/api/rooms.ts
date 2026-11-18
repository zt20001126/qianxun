import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { Room, RoomSaveRequest } from '../types/catalog'

export const roomApi = {
  async list(): Promise<Room[]> {
    const response = await http.get<ApiResponse<{ list: Room[] }>>('/api/admin/rooms')
    return response.data.data.list
  },

  async create(request: RoomSaveRequest): Promise<Room> {
    const response = await http.post<ApiResponse<Room>>('/api/admin/rooms', request)
    return response.data.data
  },

  async update(id: number, request: RoomSaveRequest): Promise<Room> {
    const response = await http.put<ApiResponse<Room>>(`/api/admin/rooms/${id}`, request)
    return response.data.data
  },

  async setEnabled(id: number, enabled: boolean): Promise<Room> {
    const response = await http.put<ApiResponse<Room>>(`/api/admin/rooms/${id}/enabled`, { enabled })
    return response.data.data
  },
}
