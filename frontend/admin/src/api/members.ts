/** M03–M05 后台会员查询、预建与积分发放接口。 */
import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { MemberDetail, MemberPage, PointsGrantRequest, PointsGrantResult } from '../types/members'

export const memberApi = {
  async list(params: { phone?: string; page: number; size: number }): Promise<MemberPage> {
    const response = await http.get<ApiResponse<MemberPage>>('/api/admin/members', { params })
    return response.data.data
  },

  async detail(id: number): Promise<MemberDetail> {
    const response = await http.get<ApiResponse<MemberDetail>>(`/api/admin/members/${id}`)
    return response.data.data
  },

  async create(phone: string): Promise<{ id: number; phone: string }> {
    const response = await http.post<ApiResponse<{ id: number; phone: string }>>('/api/admin/members', { phone })
    return response.data.data
  },

  async grant(id: number, request: PointsGrantRequest): Promise<PointsGrantResult> {
    const response = await http.post<ApiResponse<PointsGrantResult>>(
      `/api/admin/members/${id}/points/grant`, request,
    )
    return response.data.data
  },
}
