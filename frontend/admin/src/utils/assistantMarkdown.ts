import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

// 只渲染助手文本；禁用原始 HTML 和图片，避免执行代码或加载外部图片。
const markdown = new MarkdownIt({ html: false, breaks: true, linkify: false }).disable('image')

/** 将助手 Markdown 转为经过白名单过滤的安全 HTML。 */
export function renderAssistantMarkdown(text: string): string {
  // 兼容模型在加粗结束标记前多输出空格的情况，保留空格本身。
  const normalized = text.replace(/\*\*([^*\n]*?\S)([ \t]+)\*\*/g, '**$1**$2')
  return DOMPurify.sanitize(markdown.render(normalized), {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 's', 'ul', 'ol', 'li', 'blockquote', 'code', 'pre', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'a', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ALLOWED_ATTR: ['href', 'title'],
    ALLOWED_URI_REGEXP: /^(?:(?:https?):|#)/i,
  })
}
