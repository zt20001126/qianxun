/** T04/T06 后台积分流水与订单奖励重试接口。 */
import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { PointsLogPage, PointsLogType, PointsRewardRetryResult } from '../types/members'

export interface PointsLogFilters {
  member_id?: number
  order_id?: number
  type?: PointsLogType
  page: number
  size: number
}

export const pointsApi = {
  async list(filters: PointsLogFilters): Promise<PointsLogPage> {
    const response = await http.get<ApiResponse<PointsLogPage>>('/api/admin/points/logs', { params: filters })
    return response.data.data
  },

  async retryReward(orderId: number): Promise<PointsRewardRetryResult> {
    const response = await http.post<ApiResponse<PointsRewardRetryResult>>(
      `/api/admin/points/rewards/${orderId}/retry`,
    )
    return response.data.data
  },
}
