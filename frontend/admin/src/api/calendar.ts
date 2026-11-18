import { http } from './http'
import type { ApiResponse } from '../types/api'

/** K01 后台月度房态单元格状态。 */
export type CalendarDayStatus =
  | 'AVAILABLE'
  | 'BLOCKED'
  | 'PENDING_CONFIRM'
  | 'PENDING_CHECKIN'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'OCCUPIED'

export interface CalendarDay {
  date: string
  status: CalendarDayStatus
  order_id: number | null
  block_id: number | null
}

export interface CalendarRoom {
  room_id: number
  room_number: string
  days: CalendarDay[]
}

export interface RoomCalendar {
  month: string
  rooms: CalendarRoom[]
}

/** K01 房态日历接口，与 V3.1 返回字段一致。 */
export const calendarApi = {
  async month(month: string): Promise<RoomCalendar> {
    const response = await http.get<ApiResponse<RoomCalendar>>('/api/admin/room-calendar', {
      params: { month },
    })
    return response.data.data
  },
}
