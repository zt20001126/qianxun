import { http } from './http'
import type { ApiResponse } from '../types/api'
import type { AgentChatResult, AgentConversation, AgentMessage, AgentPage } from '../types/agent'

/** 管理员智能助手接口；模型调用时间可能超过普通业务请求。 */
export const agentApi = {
  async conversations(page: number): Promise<AgentPage<AgentConversation>> {
    const response = await http.get<ApiResponse<AgentPage<AgentConversation>>>('/api/admin/agent/conversations', { params: { page, size: 20 } })
    return response.data.data
  },
  async messages(id: number, page: number, size = 100): Promise<AgentPage<AgentMessage>> {
    const response = await http.get<ApiResponse<AgentPage<AgentMessage>>>(`/api/admin/agent/conversations/${id}/messages`, { params: { page, size } })
    return response.data.data
  },
  async chat(message: string, conversationId: number | null): Promise<AgentChatResult> {
    const response = await http.post<ApiResponse<AgentChatResult>>('/api/admin/agent/chat', {
      message, ...(conversationId === null ? {} : { conversation_id: conversationId }),
    }, { timeout: 75_000 })
    return response.data.data
  },
}
