package com.qianxun.homestay.modules.agent.vo;
/** 等待用户回答的自由文本问题。 */
public record AgentQuestionVO(/** 问题标识。 */ String id, /** 问题类型。 */ String type, /** 问题内容。 */ String title) {}
