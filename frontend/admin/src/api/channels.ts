import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { Channel } from '../types/catalog'

export const channelApi = {
  async list(): Promise<Channel[]> {
    const response = await http.get<ApiResponse<{ list: Channel[] }>>('/api/admin/channels')
    return response.data.data.list
  },

  async setEnabled(id: number, enabled: boolean): Promise<Channel> {
    const response = await http.put<ApiResponse<Channel>>(`/api/admin/channels/${id}/enabled`, { enabled })
    return response.data.data
  },
}
