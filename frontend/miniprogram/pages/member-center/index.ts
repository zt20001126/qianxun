/** 独立会员中心：展示等级进度、积分规则和本人消费订单。 */
import { DEFAULT_AVATAR_PATH } from '../../config'
import { getMyMember, listMyMemberOrders } from '../../services/member'
import { getPointsRules } from '../../services/points'
import type { AppOrder } from '../../types/booking'
import type { AppMember, PointsLevelRule, PointsRules } from '../../types/member'
import { getOrderStatusLabel } from '../../types/booking'
import { STORE_CONTACT_PHONE } from '../../config'
import { callBusinessPhone } from '../../utils/business-contact'
import { clearSession } from '../../utils/session'

interface RecentOrder extends AppOrder {
  status_label: string
}

interface MemberCenterData {
  member: AppMember | null
  rules: PointsRules | null
  levels: PointsLevelRule[]
  recentOrders: RecentOrder[]
  loading: boolean
  errorMessage: string
  avatarSource: string
  storePhone: string
  progressWidth: string
  hasMoreOrders: boolean
}

Page({
  data: {
    member: null,
    rules: null,
    levels: [],
    recentOrders: [],
    loading: true,
    errorMessage: '',
    avatarSource: DEFAULT_AVATAR_PATH,
    storePhone: STORE_CONTACT_PHONE,
    progressWidth: '0%',
    hasMoreOrders: false,
  } as MemberCenterData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '会员中心' })
  },

  onShow() {
    void this.loadCenter()
  },

  async onPullDownRefresh() {
    await this.loadCenter()
    wx.stopPullDownRefresh()
  },

  async loadCenter() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const [member, rules, orderPage] = await Promise.all([
        getMyMember(), getPointsRules(), listMyMemberOrders(1, 3),
      ])
      this.setData({
        member,
        rules,
        levels: rules.levels,
        recentOrders: orderPage.list.map((order) => ({
          ...order,
          status_label: getOrderStatusLabel(order.status),
        })),
        hasMoreOrders: orderPage.total > 3,
        progressWidth: `${Math.max(0, Math.min(100, member.progress_percent))}%`,
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '会员资料读取失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onPointsLogsTap() {
    wx.navigateTo({ url: '/pages/points-logs/index' })
  },

  onOrdersTap() {
    wx.switchTab({ url: '/pages/orders/index' })
  },

  onOrderTap(event: WechatMiniprogram.TouchEvent) {
    const orderId = Number(event.currentTarget.dataset.orderId)
    if (Number.isInteger(orderId) && orderId > 0) {
      wx.navigateTo({ url: `/pages/order-detail/index?id=${orderId}` })
    }
  },

  onBusinessPhoneTap() {
    callBusinessPhone()
  },

  onLogoutTap() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需要重新输入手机号登录。',
      success: (result) => {
        if (!result.confirm) return
        clearSession()
        wx.reLaunch({ url: '/pages/login/index' })
      },
    })
  },
})
