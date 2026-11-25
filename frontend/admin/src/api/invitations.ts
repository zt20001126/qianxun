/** 邀请奖励查询及人工发放标记接口。 */
import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { InvitationPage, InvitationRewardFilter } from '../types/invitations'

export const invitationApi = {
  async list(page: number, size: number, phone: string, rewardStatus: InvitationRewardFilter): Promise<InvitationPage> {
    const response = await http.get<ApiResponse<InvitationPage>>('/api/admin/invitations', { params: { page, size, phone: phone || undefined, reward_status: rewardStatus } })
    return response.data.data
  },
  async setIssued(id: number): Promise<void> {
    await http.put(`/api/admin/invitations/${id}/reward-issued`, { reward_issued: true })
  },
}
