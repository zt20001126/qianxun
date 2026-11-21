import { http } from './http'
import type { ApiResponse } from '../types/api'
import type {
  BookerPortraitFilters,
  BookerPortraitPage,
  BookerPortraitUpdateInput,
  PortraitTagOption,
} from '../types/portraits'

/** 管理端顾客画像和自定义标签接口。 */
export const portraitApi = {
  async list(filters: BookerPortraitFilters): Promise<BookerPortraitPage> {
    const response = await http.get<ApiResponse<BookerPortraitPage>>('/api/admin/portraits', { params: filters })
    return response.data.data
  },

  async update(profileId: number, request: BookerPortraitUpdateInput): Promise<void> {
    await http.put<ApiResponse<void>>('/api/admin/portraits/' + profileId, request)
  },

  async tags(): Promise<PortraitTagOption[]> {
    const response = await http.get<ApiResponse<PortraitTagOption[]>>('/api/admin/portrait-tags')
    return response.data.data
  },

  async createTag(name: string): Promise<PortraitTagOption> {
    const response = await http.post<ApiResponse<PortraitTagOption>>('/api/admin/portrait-tags', { name })
    return response.data.data
  },
}
