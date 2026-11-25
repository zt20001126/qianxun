/** 智能助手会话和消息类型。 */
export interface AgentConversation { id: number; title: string | null; created_at: string; updated_at: string }
export interface AgentQuestion { id: string; type: 'text'; title: string }
export type AgentPart = { type: 'text'; text: string } | { type: 'questions'; questions: AgentQuestion[] }
export interface AgentMessage { id: number; role: 'user' | 'assistant'; content: { parts: AgentPart[] }; created_at: string }
export interface AgentPage<T> { list: T[]; total: number; page: number; size: number }
export interface AgentChatResult { conversation_id: number; status: 'completed' | 'waiting'; answer?: string; questions?: AgentQuestion[] }
