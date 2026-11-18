/** 客房、渠道和达人接口数据结构，字段与 V3.1 snake_case 契约一致。 */
export interface Room {
  id: number
  number: string
  name: string
  floor: number
  price: string
  capacity: number
  bed_info: string | null
  area: number | null
  facilities: string[]
  images: string[]
  description: string | null
  enabled: boolean
  created_at: string
  updated_at: string
}

export interface RoomSaveRequest {
  number: string
  name: string
  floor: number
  price: string
  capacity: number
  bed_info: string | null
  area: number | null
  facilities: string[]
  images: string[]
  description: string | null
}

export interface Channel {
  id: number
  name: string
  enabled: boolean
  created_at: string
  updated_at: string
}

export interface Creator {
  id: number
  name: string
  channel_id: number
  enabled: boolean
  created_at: string
  updated_at: string
}

export interface CreatorSaveRequest {
  name: string
  channel_id: number
}
