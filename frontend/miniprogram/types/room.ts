/** R01/R02 小程序客房资料，与 V3.1 接口字段保持一致。 */
export interface AppRoomSummary {
  id: number
  number: string
  name: string
  price: string
  capacity: number
  bed_info: string | null
  images: string[]
  available?: boolean
}

export interface AppRoomDetail extends AppRoomSummary {
  floor: number
  area: number | null
  facilities: string[]
  description: string | null
}

/** R03 小程序月度房态，仅列出不可订住宿夜晚。 */
export interface AppRoomAvailability {
  room_id: number
  month: string
  unavailable_dates: string[]
}

/** R04 日期区间复核结果。 */
export interface AppAvailabilityCheck {
  available: boolean
  nights: number
  room_amount: string
}
