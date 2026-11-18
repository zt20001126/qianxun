import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { PaymentListItem, RefundListItem, TransactionFilters, TransactionPage } from '../types/transactions'

/** P04/P05 管理后台支付及退款流水查询。 */
export const transactionApi = {
  async payments(filters: TransactionFilters): Promise<TransactionPage<PaymentListItem>> {
    const response = await http.get<ApiResponse<TransactionPage<PaymentListItem>>>(
      '/api/admin/payments', { params: filters },
    )
    return response.data.data
  },

  async refunds(filters: TransactionFilters): Promise<TransactionPage<RefundListItem>> {
    const response = await http.get<ApiResponse<TransactionPage<RefundListItem>>>(
      '/api/admin/refunds', { params: filters },
    )
    return response.data.data
  },
}
