/** R01/R02 小程序公开客房查询。 */
import { request } from './request'
import type { AppRoomDetail, AppRoomSummary } from '../types/room'

export function listRooms(): Promise<AppRoomSummary[]> {
  return request<{ list: AppRoomSummary[] }>({ url: '/api/app/rooms', withAuth: false }).then((result) => result.list)
}

export function getRoomDetail(id: number): Promise<AppRoomDetail> {
  return request<AppRoomDetail>({ url: `/api/app/rooms/${id}`, withAuth: false })
}
