export interface InvitationRecord { masked_phone: string; bound_at: string }
export interface InvitationPage { list: InvitationRecord[]; total: number; page: number; size: number }
export interface InvitationCode { invite_code: string }
