/** 会员中心：展示会员等级、积分数据、积分入口与等级规则。 */
import { getMyMember } from '../../services/member'
import { getPointsRules } from '../../services/points'
import type { AppMember, PointsLevelRule, PointsRules } from '../../types/member'

interface TierDisplay extends PointsLevelRule {
  min_points: number
  matrix_name: string
  member_name: string
  range_label: string
  modal_range_label: string
  is_current: boolean
}

interface MemberCenterData {
  member: AppMember | null
  rules: PointsRules | null
  tierRows: TierDisplay[]
  loading: boolean
  errorMessage: string
  growthPointsDisplay: string
  availablePointsDisplay: string
  currentTierName: string
  currentRate: number
  upgradeTitle: string
  upgradeHint: string
  progressWidth: string
  maxDiscountRateLabel: string
  showPointsRuleModal: boolean
}

function normalizeTierName(name: string): string {
  return name.replace(/会员/g, '').replace(/卡/g, '')
}

function getTierBaseName(name: string): string {
  const normalizedName = normalizeTierName(name)
  const displayNames: Record<string, string> = {
    普通: '普通',
    银: '银卡',
    金: '金卡',
    钻石: '钻石',
  }
  return displayNames[normalizedName] || normalizedName
}

function getTierRows(levels: PointsLevelRule[], currentLevel: string): TierDisplay[] {
  const orderedLevels = levels.slice().sort((left, right) => left.growth_points - right.growth_points)
  return orderedLevels.map((level, index) => {
    const nextLevel = orderedLevels[index + 1]
    const maxPoints = nextLevel ? nextLevel.growth_points - 1 : null
    const matrixName = getTierBaseName(level.name)
    const rangeLabel = maxPoints === null
      ? `≥${level.growth_points}`
      : `${level.growth_points}-${maxPoints}`
    const modalRangeLabel = maxPoints === null
      ? `≥ ${level.growth_points}`
      : `${level.growth_points} - ${maxPoints}`

    return {
      ...level,
      min_points: level.growth_points,
      matrix_name: matrixName,
      member_name: `${matrixName}会员`,
      range_label: rangeLabel,
      modal_range_label: modalRangeLabel,
      is_current: normalizeTierName(level.name) === normalizeTierName(currentLevel),
    }
  })
}

function formatPoints(value: number): string {
  return value.toLocaleString('en-US')
}

Page({
  data: {
    member: null,
    rules: null,
    tierRows: [],
    loading: true,
    errorMessage: '',
    growthPointsDisplay: '0',
    availablePointsDisplay: '0',
    currentTierName: '',
    currentRate: 0,
    upgradeTitle: '',
    upgradeHint: '',
    progressWidth: '0%',
    maxDiscountRateLabel: '20%',
    showPointsRuleModal: false,
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
      const [member, rules] = await Promise.all([getMyMember(), getPointsRules()])
      const tierRows = getTierRows(rules.levels, member.level)
      const currentIndex = tierRows.findIndex((tier) => tier.is_current)
      const currentTier = currentIndex >= 0 ? tierRows[currentIndex] : undefined
      const nextTier = currentIndex >= 0 ? tierRows[currentIndex + 1] : undefined
      const hasNextTier = Boolean(nextTier && member.next_level_points > 0)

      this.setData({
        member,
        rules,
        tierRows,
        growthPointsDisplay: formatPoints(member.growth_points),
        availablePointsDisplay: formatPoints(member.points),
        currentTierName: currentTier?.member_name || `${getTierBaseName(member.level)}会员`,
        currentRate: currentTier?.points_per_yuan || 0,
        upgradeTitle: hasNextTier ? `升级至${nextTier?.member_name}` : '已达最高尊享等级',
        upgradeHint: hasNextTier
          ? `距离升级还差 ${member.next_level_points} 成长积分`
          : '尊享全店最高抵扣比例',
        progressWidth: `${Math.max(0, Math.min(100, member.progress_percent))}%`,
        maxDiscountRateLabel: `${Math.round(rules.max_discount_rate * 100)}%`,
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

  onPointsRuleTap() {
    this.setData({ showPointsRuleModal: true })
  },

  onPointsRuleClose() {
    this.setData({ showPointsRuleModal: false })
  },
})
