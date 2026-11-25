/** 智能助手会话和消息类型。 */
export interface AgentConversation { id: number; title: string | null; created_at: string; updated_at: string }
export interface AgentQuestionOption { label: string; description: string }
export type AgentQuestion = { id: string; type: 'text'; title: string } | { id: string; type: 'single_choice'; title: string; context?: string; options: AgentQuestionOption[]; allow_custom_input: false }
export type AgentPart = { type: 'text'; text: string } | { type: 'questions'; questions: AgentQuestion[] } | { type: 'question_answers'; question_message_id: number; answers: Record<string, string> }
export interface AgentMessage { id: number; role: 'user' | 'assistant'; content: { parts: AgentPart[] }; created_at: string }
export interface AgentPage<T> { list: T[]; total: number; page: number; size: number }
export interface AgentChatResult { conversation_id: number; status: 'completed' | 'waiting'; answer?: string; questions?: AgentQuestion[]; question_message_id?: number }
