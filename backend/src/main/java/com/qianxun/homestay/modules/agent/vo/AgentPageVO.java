package com.qianxun.homestay.modules.agent.vo;
import java.util.List;
/** 智能体分页结果。 */
public record AgentPageVO<T>(/** 本页数据。 */ List<T> list, /** 总条数。 */ long total,
    /** 页码，从一开始。 */ int page, /** 每页数量。 */ int size) {}
