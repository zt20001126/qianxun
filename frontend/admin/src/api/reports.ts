import { http } from './http'
import type { ApiResponse } from '../types/api'
import type {
  Dashboard,
  ReportDimensionResult,
  ReportOverview,
  ReportScope,
} from '../types/operations'

export interface ReportFilters {
  month?: string
  scope?: ReportScope
}

export const reportApi = {
  async dashboard(filters: { date?: string; month?: string }): Promise<Dashboard> {
    const response = await http.get<ApiResponse<Dashboard>>('/api/admin/dashboard', { params: filters })
    return response.data.data
  },

  async overview(filters: ReportFilters): Promise<ReportOverview> {
    const response = await http.get<ApiResponse<ReportOverview>>('/api/admin/reports/overview', { params: filters })
    return response.data.data
  },

  async channels(filters: ReportFilters & { channel_id?: number }): Promise<ReportDimensionResult> {
    const response = await http.get<ApiResponse<ReportDimensionResult>>('/api/admin/reports/channels', {
      params: filters,
    })
    return response.data.data
  },

  async creators(filters: ReportFilters & { creator_id?: number }): Promise<ReportDimensionResult> {
    const response = await http.get<ApiResponse<ReportDimensionResult>>('/api/admin/reports/creators', {
      params: filters,
    })
    return response.data.data
  },

  async rooms(filters: ReportFilters & { room_id?: number }): Promise<ReportDimensionResult> {
    const response = await http.get<ApiResponse<ReportDimensionResult>>('/api/admin/reports/rooms', {
      params: filters,
    })
    return response.data.data
  },
}
