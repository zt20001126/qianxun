import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { HolidayPriceDates, HolidayPriceDatesUpdate, Room, RoomSaveRequest } from '../types/catalog'

const HOLIDAY_PRICE_DATES_URL = '/api/admin/holiday-price-dates'
const ROOM_IMAGE_UPLOAD_TIMEOUT_MS = 120_000

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

  /** 上传一张客房图片，返回客房保存时使用的地址。 */
  async uploadImage(file: File): Promise<string> {
    const form = new FormData()
    form.append('file', file)
    const response = await http.post<ApiResponse<{ url: string }>>('/api/admin/rooms/images', form, {
      timeout: ROOM_IMAGE_UPLOAD_TIMEOUT_MS,
    })
    return response.data.data.url
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
