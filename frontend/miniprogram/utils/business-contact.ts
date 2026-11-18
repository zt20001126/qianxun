/** 统一使用已确认的商家联系电话发起拨号。 */
import { STORE_CONTACT_PHONE } from '../config'

export function callBusinessPhone(): void {
  wx.makePhoneCall({ phoneNumber: STORE_CONTACT_PHONE })
}
