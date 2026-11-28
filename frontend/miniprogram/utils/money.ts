/** 以整数分计算客房平均夜价，四舍五入到分。 */
export function averageRoomNightlyPrice(roomAmount: string | null, nights: number): string | null {
  if (roomAmount === null || !/^\d+(?:\.\d{1,2})?$/.test(roomAmount)
    || !Number.isSafeInteger(nights) || nights <= 0) return null

  const [yuan, fraction = ''] = roomAmount.split('.')
  const cents = Number(yuan + fraction.padEnd(2, '0'))
  if (!Number.isSafeInteger(cents)) return null
  const quotient = Math.floor(cents / nights)
  const average = cents % nights >= Math.ceil(nights / 2) ? quotient + 1 : quotient
  const digits = String(average).padStart(3, '0')
  return `${digits.slice(0, -2)}.${digits.slice(-2)}`
}
