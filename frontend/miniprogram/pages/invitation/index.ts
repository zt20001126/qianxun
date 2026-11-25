import { getInvitationCode, getInvitations } from '../../services/invitation'
import type { InvitationRecord } from '../../types/invitation'

interface DisplayRecord extends InvitationRecord { displayTime: string }

Page({
  data: { code: '', records: [] as DisplayRecord[], total: 0, page: 1, loading: false, loadingMore: false, error: '' },
  onLoad() {
    wx.hideShareMenu({ menus: ['shareAppMessage'] })
  },
  // 返回页面时刷新完成状态，避免停留期间订单完成后仍显示旧状态。
  onShow() { void this.load() },
  async load() {
    if (this.data.loading) return
    this.setData({ loading: true, error: '' })
    try {
      const [code, result] = await Promise.all([getInvitationCode(), getInvitations(1)])
      this.setData({ code: code.invite_code, records: this.format(result.list), total: result.total, page: 1 })
      wx.showShareMenu({ menus: ['shareAppMessage'] })
    } catch {
      this.setData({ error: '加载失败，请稍后重试' })
    } finally { this.setData({ loading: false }) }
  },
  format(records: InvitationRecord[]): DisplayRecord[] {
    return records.map(item => ({ ...item, displayTime: item.bound_at.replace('T', ' ').slice(0, 16) }))
  },
  async onReachBottom() {
    if (this.data.loadingMore || this.data.loading || this.data.error || this.data.records.length >= this.data.total) return
    this.setData({ loadingMore: true })
    try {
      const result = await getInvitations(this.data.page + 1)
      this.setData({ records: this.data.records.concat(this.format(result.list)), total: result.total, page: result.page })
    } catch { wx.showToast({ title: '加载失败，请稍后重试', icon: 'none' }) }
    finally { this.setData({ loadingMore: false }) }
  },
  onShareAppMessage() {
    return { title: '好友邀你来芊浔民宿', path: `/pages/invitation-entry/index?code=${this.data.code}`,
      imageUrl: '/assets/login-baijianlou-hero.jpg' }
  },
})
