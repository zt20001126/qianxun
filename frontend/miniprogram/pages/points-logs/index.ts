/** T02 本人积分流水：按月分组展示积分变化、来源、时间和变更后余额。 */
import { getMyMember } from '../../services/member'
import { listMyPointsLogs } from '../../services/points'
import type { PointsLog } from '../../types/member'

interface DisplayLog extends PointsLog {
  month_label: string
  type_label: string
  points_label: string
  created_label: string
  source_label: string
  icon_name: string
  is_positive: boolean
}

interface MonthLogGroup {
  month: string
  items: DisplayLog[]
}

interface PointsLogsData {
  logs: DisplayLog[]
  monthGroups: MonthLogGroup[]
  availablePointsDisplay: string
  loading: boolean
  loadingMore: boolean
  errorMessage: string
  page: number
  size: number
  total: number
  ruleVisible: boolean
}

const typeLabels: Record<PointsLog['type'], string> = {
  ORDER_REWARD: '订单完成奖励',
  HISTORY_REWARD: '历史消费补发',
  GIFT: '赠送/补偿积分',
  REDEEM: '抵扣订单房费',
  RETURN: '取消订单返还',
  ADJUST: '积分调整',
}

function getLogIcon(type: PointsLog['type'], isPositive: boolean): string {
  if (type === 'ORDER_REWARD') return 'award-celadon'
  if (type === 'RETURN') return 'rotate-ccw-celadon'
  if (type === 'HISTORY_REWARD') return 'sparkles-celadon'
  if (type === 'GIFT') return isPositive ? 'gift-celadon' : 'gift'
  if (type === 'REDEEM') return 'ticket'
  return isPositive ? 'sparkles-celadon' : 'coins'
}

function formatMonthLabel(createdAt: string): string {
  const match = createdAt.match(/^(\d{4})-(\d{2})/)
  if (!match) return '历史记录'
  return `${match[1]}年${Number(match[2])}月`
}

function formatShortTime(createdAt: string): string {
  const match = createdAt.match(/^\d{4}-(\d{2})-(\d{2})[T ](\d{2}:\d{2})/)
  if (!match) return createdAt.replace('T', ' ').slice(0, 16)
  return `${Number(match[1])}月${Number(match[2])}日 ${match[3]}`
}

function groupByMonth(logs: DisplayLog[]): MonthLogGroup[] {
  const groups: MonthLogGroup[] = []
  const groupMap: Record<string, MonthLogGroup> = {}
  logs.forEach((item) => {
    let group = groupMap[item.month_label]
    if (!group) {
      group = { month: item.month_label, items: [] }
      groupMap[item.month_label] = group
      groups.push(group)
    }
    group.items.push(item)
  })
  return groups
}

function formatNumber(value: number): string {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

Page({
  data: {
    logs: [],
    monthGroups: [],
    availablePointsDisplay: '0',
    loading: true,
    loadingMore: false,
    errorMessage: '',
    page: 1,
    size: 20,
    total: 0,
    ruleVisible: false,
  } as PointsLogsData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '积分流水' })
    void this.loadInitial()
  },

  async onPullDownRefresh() {
    await this.loadInitial()
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    if (!this.data.loading && !this.data.loadingMore && this.data.logs.length < this.data.total) {
      void this.loadLogs(true)
    }
  },

  async loadInitial() {
    await Promise.all([this.loadMemberPoints(), this.loadLogs(false)])
  },

  async loadMemberPoints() {
    try {
      const member = await getMyMember()
      this.setData({ availablePointsDisplay: formatNumber(member.points) })
    } catch {
      // 积分流水仍可独立展示，不因会员余额读取失败阻断列表。
    }
  },

  async loadLogs(loadMore: boolean) {
    const nextPage = loadMore ? this.data.page + 1 : 1
    this.setData({
      loading: !loadMore,
      loadingMore: loadMore,
      errorMessage: '',
    })
    try {
      const result = await listMyPointsLogs(nextPage, this.data.size)
      const mapped: DisplayLog[] = result.list.map((log) => {
        const isPositive = log.points > 0
        return {
          ...log,
          month_label: formatMonthLabel(log.created_at),
          type_label: typeLabels[log.type] || '积分变动',
          points_label: `${isPositive ? '+' : ''}${log.points}`,
          created_label: formatShortTime(log.created_at),
          source_label: log.remark || (log.order_id ? `订单 #${log.order_id}` : '积分业务记录'),
          icon_name: getLogIcon(log.type, isPositive),
          is_positive: isPositive,
        }
      })
      const logs = loadMore ? this.data.logs.concat(mapped) : mapped
      this.setData({
        logs,
        monthGroups: groupByMonth(logs),
        page: result.page,
        total: result.total,
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '积分流水读取失败' })
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },

  onRetryTap() {
    void this.loadInitial()
  },

  onPointsRuleOpen() {
    this.setData({ ruleVisible: true })
  },

  onPointsRuleClose() {
    this.setData({ ruleVisible: false })
  },

  onBackMemberTap() {
    wx.switchTab({ url: '/pages/member-center/index' })
  },

  noop() {},
})
