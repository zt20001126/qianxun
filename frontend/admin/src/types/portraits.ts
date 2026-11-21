/** 管理端按手机号维护的顾客画像字段与订单备注类型。 */
export type BookerPartyType =
  | 'SOLO'
  | 'COUPLE'
  | 'PARENT_CHILD'
  | 'FAMILY_FRIENDS'
  | 'FRIENDS'
  | 'BUSINESS'
  | 'GROUP'
  | 'OTHER'

export const bookerPartyTypeLabels: Record<BookerPartyType, string> = {
  SOLO: '独自出行',
  COUPLE: '情侣/夫妻',
  PARENT_CHILD: '亲子家庭',
  FAMILY_FRIENDS: '家庭/亲友',
  FRIENDS: '朋友结伴',
  BUSINESS: '商务/差旅',
  GROUP: '团队/团体',
  OTHER: '其他',
}

/** 后端可保存的画像字段；不包含身份证号。 */
export interface BookerPortraitInput {
  age: number | null
  party_type: BookerPartyType | null
  inferred_region: string | null
  tags: string[]
}

export interface BookerPortraitUpdateInput extends BookerPortraitInput {
  admin_remark: string | null
  order_remarks: BookerPortraitOrderRemarkInput[]
}

export interface PortraitTagOption {
  id: number
  name: string
}

export interface BookerPortraitOrderRemarkInput {
  order_id: number
  remark: string | null
  admin_remark: string | null
}

/** 订单详情回显使用的画像字段。 */
export interface BookerPortrait extends BookerPortraitInput {
  id: number
}

/** 顾客画像中的一笔已完成订单。 */
export interface BookerPortraitOrder {
  order_id: number
  order_no: string
  check_in: string
  paid_amount: string
  remark: string | null
  admin_remark: string | null
}

/** 画像编辑页持续维护的订单备注；订单状态不限制编辑。 */
export interface BookerPortraitEditableOrder {
  order_id: number
  order_no: string
  check_in: string
  status: string
  remark: string | null
  admin_remark: string | null
}

/** 顾客画像列表的一行，按手机号归并订单。 */
export interface BookerPortraitListItem {
  id: number
  guest_name: string | null
  phone: string | null
  age: number | null
  party_type: BookerPartyType | null
  inferred_region: string | null
  member: boolean
  tags: string[]
  admin_remark: string | null
  completed_order_count: number
  cumulative_paid_amount: string
  completed_orders: BookerPortraitOrder[]
  orders: BookerPortraitEditableOrder[]
}

export interface BookerPortraitPage {
  list: BookerPortraitListItem[]
  total: number
  page: number
  size: number
}

export interface BookerPortraitFilters {
  guest_name?: string
  phone?: string
  page: number
  size: number
}

