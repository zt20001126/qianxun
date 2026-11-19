/** R01/R02 小程序公开客房查询。 */
import { request } from './request'
import type { AppAvailabilityCheck, AppRoomAvailability, AppRoomDetail, AppRoomSummary } from '../types/room'

export function listRooms(checkIn?: string, checkOut?: string): Promise<AppRoomSummary[]> {
  const query = checkIn && checkOut
    ? `?check_in=${encodeURIComponent(checkIn)}&check_out=${encodeURIComponent(checkOut)}`
    : ''
  return request<{ list: AppRoomSummary[] }>({ url: `/api/app/rooms${query}`, withAuth: false }).then((result) => result.list)
}

export function getRoomDetail(id: number): Promise<AppRoomDetail> {
  return request<AppRoomDetail>({ url: `/api/app/rooms/${id}`, withAuth: false })
}

export function getRoomAvailability(id: number, month: string): Promise<AppRoomAvailability> {
  return request<AppRoomAvailability>({
    url: `/api/app/rooms/${id}/availability?month=${encodeURIComponent(month)}`,
    withAuth: false,
  })
}

/** 返回指定月份全店房间都不可订的住宿日，供日历标记满房提示。 */
export async function getFullyUnavailableDates(month: string): Promise<string[]> {
  const rooms = await listRooms()
  if (!rooms.length) return []

  const availability = await Promise.all(rooms.map((room) => getRoomAvailability(room.id, month)))
  const unavailableByRoom = availability.map((item) => new Set(item.unavailable_dates))
  return availability[0].unavailable_dates.filter((date) =>
    unavailableByRoom.every((dates) => dates.has(date)),
  )
}

export function checkRoomAvailability(
  roomId: number,
  checkIn: string,
  checkOut: string,
): Promise<AppAvailabilityCheck> {
  return request<AppAvailabilityCheck>({
    url: '/api/app/rooms/availability-check',
    method: 'POST',
    data: { room_id: roomId, check_in: checkIn, check_out: checkOut },
    withAuth: false,
  })
}
