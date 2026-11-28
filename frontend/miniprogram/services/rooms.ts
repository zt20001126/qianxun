/** R01/R02 小程序公开客房查询。 */
import { request } from './request'
import type { AppAvailabilityCheck, AppRoomAvailability, AppRoomDetail, AppRoomSummary, CalendarAvailability } from '../types/room'

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

/** 汇总全店逐日最低可订夜价，以及所有房间均不可订的日期。 */
export async function getStoreCalendarAvailability(month: string, roomIds: number[]): Promise<CalendarAvailability> {
  if (!roomIds.length) return { unavailable_dates: [], nightly_prices: {}, noRooms: true }

  const availability = await Promise.all(roomIds.map((id) => getRoomAvailability(id, month)))
  const unavailableByRoom = availability.map((item) => new Set(item.unavailable_dates))
  const unavailableDates = availability[0].unavailable_dates.filter((date) =>
    unavailableByRoom.every((dates) => dates.has(date)),
  )
  const nightlyPrices: Record<string, string> = {}
  for (const room of availability) {
    // 用整数分比较十进制字符串，避免将金额换算成浮点元。
    for (const [date, price] of Object.entries(room.nightly_prices ?? {})) {
      const lowest = nightlyPrices[date]
      if (lowest === undefined || Number(price.replace('.', '')) < Number(lowest.replace('.', ''))) {
        nightlyPrices[date] = price
      }
    }
  }
  return { unavailable_dates: unavailableDates, nightly_prices: nightlyPrices }
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
