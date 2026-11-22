/** 商城购买表单；商品价格和积分抵扣以后端试算及支付快照为准。 */
import { getMallProduct, getMallShipping, previewMall, prepayMall, mockPayMall, createMallWechatPrepay, getMallPaymentStatus,
  type MallProduct, type MallPreview, type MallPrepay } from '../../services/mall'

Page({
  data: { product: null as MallProduct | null, preview: null as MallPreview | null, pending: null as MallPrepay | null,
    quantity: 1, recipientName: '', recipientPhone: '', address: '', usePoints: true, pointsTierLabel: '',
    loading: true, previewLoading: false, submitting: false, error: '' },
  async onLoad(options: Record<string, string | undefined>) {
    const id = Number(options.id)
    if (!Number.isInteger(id) || id <= 0) { this.setData({ loading: false, error: '商品信息无效' }); return }
    try {
      const [product, shipping] = await Promise.all([getMallProduct(id), getMallShipping()])
      this.setData({ product, recipientName: shipping.recipient_name || '', recipientPhone: shipping.recipient_phone || '',
        address: shipping.shipping_address || '' })
      // 默认勾选积分抵扣，具体可用额度及金额始终以后端试算为准。
      await this.updatePreview()
    } catch (error) { this.setData({ error: error instanceof Error ? error.message : '商品加载失败' }) }
    finally { this.setData({ loading: false }) }
  },
  async updatePreview(): Promise<MallPreview | null> {
    if (!this.data.product || this.data.previewLoading) return null
    this.setData({ previewLoading: true, error: '' })
    try {
      const base = await previewMall(this.data.product.id, this.data.quantity, 0)
      const preview = this.data.usePoints && base.max_points_usable > 0
        ? await previewMall(this.data.product.id, this.data.quantity, base.max_points_usable) : base
      this.setData({ preview, pointsTierLabel: formatPointsTier(preview.points_per_yuan) })
      return preview
    } catch (error) {
      this.setData({ preview: null, pointsTierLabel: '', error: error instanceof Error ? error.message : '金额试算失败' })
      return null
    }
    finally { this.setData({ previewLoading: false }) }
  },
  onQuantity(event: WechatMiniprogram.TouchEvent) {
    if (this.data.previewLoading || this.data.pending) return
    const quantity = Math.max(1, Math.min(100000, this.data.quantity + Number(event.currentTarget.dataset.delta)))
    this.setData({ quantity }, () => { void this.updatePreview() })
  },
  onPointsToggle() {
    if (this.data.previewLoading || this.data.pending || !this.data.preview || this.data.preview.max_points_usable <= 0) return
    this.setData({ usePoints: !this.data.usePoints }, () => { void this.updatePreview() })
  },
  onName(event: WechatMiniprogram.Input) { this.setData({ recipientName: event.detail.value }) },
  onPhone(event: WechatMiniprogram.Input) { this.setData({ recipientPhone: event.detail.value }) },
  onAddress(event: WechatMiniprogram.Input) { this.setData({ address: event.detail.value }) },
  async onCheckPayment() {
    const pending = this.data.pending
    if (!pending || this.data.submitting) return
    this.setData({ submitting: true, error: '' })
    try {
      const state = await getMallPaymentStatus(pending.payment_no)
      if (state.status === 'SUCCESS' && state.order_id && state.order_no) {
        this.goToSuccess(pending.payment_no, state.order_id, state.order_no)
        return
      }
      if (state.status === 'CLOSED' || state.status === 'FAILED') {
        this.setData({ pending: null, error: '该交易已关闭，请重新提交订单。' })
        return
      }
      this.setData({ error: '交易仍在处理中，请稍后再次查询。' })
    } catch (error) { this.setData({ error: error instanceof Error ? error.message : '查询支付状态失败' }) }
    finally { this.setData({ submitting: false }) }
  },
  async onSubmit() {
    if (this.data.submitting || this.data.previewLoading || !this.data.product || !this.data.preview) return
    if (!this.data.recipientName.trim() || !/^1[3-9]\d{9}$/.test(this.data.recipientPhone) || !this.data.address.trim()) {
      wx.showToast({ title: '请填写完整收货信息', icon: 'none' }); return
    }
    this.setData({ submitting: true, error: '' })
    try {
      const payment = this.data.pending || await prepayMall({ product_id: this.data.product.id,
        quantity: this.data.quantity, points_used: this.data.preview.points_used,
        recipient_name: this.data.recipientName.trim(), recipient_phone: this.data.recipientPhone,
        shipping_address: this.data.address.trim() })
      // 预支付重新核价后，用服务端锁定的金额更新页面，避免用户按过期试算确认支付。
      this.setData({ pending: payment, preview: { ...this.data.preview, total_amount: payment.total_amount,
        points_used: payment.points_used, discount_amount: payment.discount_amount, amount: payment.amount } })
      if (payment.payment_method === 'WECHAT') return
      const confirmed = await new Promise<boolean>((resolve) => wx.showModal({ title: '确认模拟支付',
        content: `商品金额 ¥${payment.total_amount}，积分抵扣 ¥${payment.discount_amount}，应付 ¥${payment.amount}。`,
        confirmText: '确认支付', success: (result) => resolve(result.confirm), fail: () => resolve(false) }))
      if (!confirmed) return
      if (payment.payment_method !== 'MOCK') throw new Error('该支付方式暂不可用')
      const result = await mockPayMall(payment.payment_no)
      if (result.status !== 'SUCCESS' || !result.order_id || !result.order_no) {
        throw new Error('支付结果正在确认中，请点击查询支付状态')
      }
      this.goToSuccess(payment.payment_no, result.order_id, result.order_no)
    } catch (error) { this.setData({ error: error instanceof Error ? error.message : '支付失败，请稍后重试' }) }
    finally { this.setData({ submitting: false }) }
  },
  async onPhoneAuthorized(event: { detail: { code?: string } }) {
    const pending = this.data.pending
    if (!pending || pending.payment_method !== 'WECHAT' || this.data.submitting) return
    if (!event.detail.code) { wx.showToast({ title: '请先授权手机号', icon: 'none' }); return }
    this.setData({ submitting: true, error: '' })
    try {
      const loginCode = await new Promise<string>((resolve, reject) => wx.login({
        success: (result) => result.code ? resolve(result.code) : reject(new Error('获取微信登录凭证失败')),
        fail: () => reject(new Error('获取微信登录凭证失败')),
      }))
      const payment = await createMallWechatPrepay(pending.payment_no, loginCode, event.detail.code)
      await new Promise<void>((resolve, reject) => wx.requestPayment({ timeStamp: payment.time_stamp,
        nonceStr: payment.nonce_str, package: payment.package, signType: payment.sign_type as 'RSA',
        paySign: payment.pay_sign, success: () => resolve(),
        fail: (error) => reject(new Error(error.errMsg?.includes('cancel') ? '已取消支付' : '微信支付未完成')) }))
      for (let attempt = 0; attempt < 10; attempt += 1) {
        const state = await getMallPaymentStatus(pending.payment_no)
        if (state.status === 'SUCCESS' && state.order_id && state.order_no) {
          this.goToSuccess(pending.payment_no, state.order_id, state.order_no)
          return
        }
        if (state.status === 'FAILED' || state.status === 'CLOSED') break
        await new Promise((resolve) => setTimeout(resolve, 1000))
      }
      this.setData({ error: '支付结果仍在确认中，请稍后查询订单。' })
    } catch (error) {
      // 客户端取消或网络错误也可能与服务端通知交错；先查询状态，避免误报已付款订单。
      try {
        const state = await getMallPaymentStatus(pending.payment_no)
        if (state.status === 'SUCCESS' && state.order_id && state.order_no) {
          this.goToSuccess(pending.payment_no, state.order_id, state.order_no)
          return
        }
      } catch { /* 保留原错误，用户可稍后查询订单。 */ }
      this.setData({ error: error instanceof Error ? error.message : '微信支付未完成，请稍后查询订单' })
    }
    finally { this.setData({ submitting: false }) }
  },
  goToSuccess(paymentNo: string, orderId: number, orderNo: string) {
    wx.redirectTo({
      url: '/pages/payment-success/index?source=mall&payment_no=' + encodeURIComponent(paymentNo)
        + '&order_id=' + orderId + '&order_no=' + encodeURIComponent(orderNo),
    })
  },
})

function formatPointsTier(pointsPerYuan: number): string {
  const tierNames: Record<number, string> = { 30: '普通会员', 25: '银卡会员', 20: '金卡会员', 15: '钻石会员' }
  const tierName = tierNames[pointsPerYuan]
  return tierName ? `${tierName}（${pointsPerYuan}积分=¥1）` : `${pointsPerYuan}积分=¥1`
}
