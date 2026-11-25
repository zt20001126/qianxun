package com.qianxun.homestay.modules.agent.vo;
import java.util.List;
/** 提问工具生成的自由文本题或单选题。 */
public record AgentQuestionVO(
        /** 批次内唯一的问题标识。 */ String id,
        /** text 或 single_choice。 */ String type,
        /** 问题标题。 */ String title,
        /** 可选的问题背景。 */ String context,
        /** 单选题选项。 */ List<AgentQuestionOptionVO> options,
        /** 单选题是否允许自定义答案。 */ Boolean allowCustomInput) {
    /** 兼容原有自由文本问题。 */
    public AgentQuestionVO(String id, String type, String title) { this(id, type, title, null, null, null); }
}
