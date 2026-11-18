/** 校验第一阶段支持的中国大陆手机号格式。 */
const MAINLAND_PHONE_PATTERN = /^1[3-9]\d{9}$/

export function isValidMainlandPhone(phone: string): boolean {
  return MAINLAND_PHONE_PATTERN.test(phone)
}
