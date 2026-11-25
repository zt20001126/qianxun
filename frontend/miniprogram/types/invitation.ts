export interface InvitationRecord { masked_phone: string; bound_at: string; has_completed_order: boolean }
export interface InvitationPage { list: InvitationRecord[]; total: number; page: number; size: number }
export interface InvitationCode { invite_code: string }
