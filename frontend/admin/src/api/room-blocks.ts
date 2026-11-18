import { http } from './http'
import type { ApiResponse } from '../types/api'

/** 后台封房接口数据，与 V3.1 R10-R13 一致。 */
export interface RoomBlock {
  id: number
  room_id: number
  start_date: string
  end_date: string
  reason: string | null
  created_at: string
  updated_at: string
}

export interface RoomBlockSaveRequest {
  room_id: number
  start_date: string
  end_date: string
  reason: string | null
}

export const roomBlockApi = {
  async list(roomId?: number, month?: string): Promise<RoomBlock[]> {
    const response = await http.get<ApiResponse<{ list: RoomBlock[] }>>('/api/admin/room-blocks', {
      params: { room_id: roomId || undefined, month: month || undefined },
    })
    return response.data.data.list
  },

  async create(request: RoomBlockSaveRequest): Promise<RoomBlock> {
    const response = await http.post<ApiResponse<RoomBlock>>('/api/admin/room-blocks', request)
    return response.data.data
  },

  async update(id: number, request: RoomBlockSaveRequest): Promise<RoomBlock> {
    const response = await http.put<ApiResponse<RoomBlock>>(`/api/admin/room-blocks/${id}`, request)
    return response.data.data
  },

  async remove(id: number): Promise<void> {
    await http.delete<ApiResponse<void>>(`/api/admin/room-blocks/${id}`)
  },
}
