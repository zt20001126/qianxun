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
  /** 所选住宿日期的房费合计，金额为十进制字符串。 */
  room_amount?: string
}

export interface AppRoomDetail extends AppRoomSummary {
  floor: number
  area: number | null
  facilities: string[]
  description: string | null
}

/** R03 小程序单房月历。 */
export interface AppRoomAvailability {
  room_id: number
  month: string
  unavailable_dates: string[]
  /** 按日期索引的可订夜价，金额为两位小数的十进制字符串。 */
  nightly_prices: Record<string, string>
}

/** 日历组件使用的月度房态，全店无上架房间时单独标记。 */
export type CalendarAvailability = Pick<AppRoomAvailability, 'unavailable_dates' | 'nightly_prices'> & {
  noRooms?: boolean
}

/** R04 日期区间复核结果。 */
export interface AppAvailabilityCheck {
  available: boolean
  nights: number
  room_amount: string
}
