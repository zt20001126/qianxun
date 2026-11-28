import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { HolidayPriceDates, HolidayPriceDatesUpdate, Room, RoomSaveRequest } from '../types/catalog'

const HOLIDAY_PRICE_DATES_URL = '/api/admin/holiday-price-dates'

export const roomApi = {
  async listHolidayPriceDates(month: string): Promise<string[]> {
    const response = await http.get<ApiResponse<HolidayPriceDates>>(HOLIDAY_PRICE_DATES_URL, { params: { month } })
    return response.data.data.dates
  },

  async updateHolidayPriceDates(request: HolidayPriceDatesUpdate): Promise<string[]> {
    const response = await http.put<ApiResponse<HolidayPriceDates>>(HOLIDAY_PRICE_DATES_URL, request)
    return response.data.data.dates
  },

  async list(): Promise<Room[]> {
    const response = await http.get<ApiResponse<{ list: Room[] }>>('/api/admin/rooms')
    return response.data.data.list
  },

  async create(request: RoomSaveRequest): Promise<Room> {
    const response = await http.post<ApiResponse<Room>>('/api/admin/rooms', request)
    return response.data.data
  },

  async update(id: number, request: RoomSaveRequest): Promise<Room> {
    const response = await http.put<ApiResponse<Room>>(`/api/admin/rooms/${id}`, request)
    return response.data.data
  },

  async setEnabled(id: number, enabled: boolean): Promise<Room> {
    const response = await http.put<ApiResponse<Room>>(`/api/admin/rooms/${id}/enabled`, { enabled })
    return response.data.data
  },
}
