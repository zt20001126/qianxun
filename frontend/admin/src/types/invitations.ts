/** 管理员查看的邀请关系与首单奖励。 */
export interface InvitationRecord {
  id: number
  inviter_id: number
  inviter_phone: string
  invitee_id: number
  invitee_phone: string
  bound_at: string
  order_no?: string
  paid_amount?: string
  reward_amount?: string
  reward_issued: boolean
}
export interface InvitationPage { list: InvitationRecord[]; total: number; page: number; size: number }

/** 按奖励是否产生及发放状态筛选。 */
export type InvitationRewardFilter = 'ALL' | 'NO_REWARD' | 'UNISSUED' | 'ISSUED'
