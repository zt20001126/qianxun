import { request } from './request'
import type { InvitationCode, InvitationPage } from '../types/invitation'

export function getInvitationCode(): Promise<InvitationCode> {
  return request<InvitationCode>({ url: '/api/app/invitations/share-code', method: 'POST' })
}

export function getInvitations(page: number): Promise<InvitationPage> {
  return request<InvitationPage>({ url: '/api/app/invitations', data: { page, size: 20 } })
}
