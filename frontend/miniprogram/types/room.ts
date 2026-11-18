/** R01/R02 小程序客房资料，与 V3.1 接口字段保持一致。 */
export interface AppRoomSummary {
  id: number
  number: string
  name: string
  price: string
  capacity: number
  bed_info: string | null
  images: string[]
}

export interface AppRoomDetail extends AppRoomSummary {
  floor: number
  area: number | null
  facilities: string[]
  description: string | null
}
