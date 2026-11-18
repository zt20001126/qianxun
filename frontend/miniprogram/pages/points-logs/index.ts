/** T02 本人积分流水：展示积分变化、来源、时间和变更后余额。 */
import { listMyPointsLogs } from '../../services/points'
import type { PointsLog } from '../../types/member'

interface DisplayLog extends PointsLog {
  type_label: string
  points_label: string
  created_label: string
  source_label: string
}

interface PointsLogsData {
  logs: DisplayLog[]
  loading: boolean
  loadingMore: boolean
  errorMessage: string
  page: number
  size: number
  total: number
}

const typeLabels: Record<PointsLog['type'], string> = {
  ORDER_REWARD: '订单奖励',
  HISTORY_REWARD: '历史消费补发',
  GIFT: '赠送/补偿',
  REDEEM: '积分抵扣',
  RETURN: '退款返还',
  ADJUST: '积分调整',
}

Page({
  data: { logs: [], loading: true, loadingMore: false, errorMessage: '', page: 1, size: 20, total: 0 } as PointsLogsData,

  onLoad() {
    wx.setNavigationBarTitle({ title: '积分流水' })
    void this.loadLogs(false)
  },

  async onPullDownRefresh() {
    await this.loadLogs(false)
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    if (!this.data.loading && !this.data.loadingMore && this.data.logs.length < this.data.total) {
      void this.loadLogs(true)
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
      const mapped = result.list.map((log) => ({
        ...log,
        type_label: typeLabels[log.type],
        points_label: `${log.points > 0 ? '+' : ''}${log.points}`,
        created_label: log.created_at.replace('T', ' ').slice(0, 16),
        source_label: log.remark || (log.order_id ? `订单 #${log.order_id}` : '积分业务记录'),
      }))
      this.setData({
        logs: loadMore ? this.data.logs.concat(mapped) : mapped,
        page: result.page,
        total: result.total,
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '积分流水读取失败' })
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },
})
