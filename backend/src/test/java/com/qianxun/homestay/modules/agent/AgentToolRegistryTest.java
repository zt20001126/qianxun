package com.qianxun.homestay.modules.agent;
import static org.junit.jupiter.api.Assertions.*;
import com.qianxun.homestay.modules.agent.core.tool.*;
import com.qianxun.homestay.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
/** 注册表确保工具定义和分发使用相同工具集合。 */
class AgentToolRegistryTest {
    @Test void discoversToolAndRejectsDuplicatesAndUnknownNames() {
        var tool = new AskUserQuestionsTool();
        var registry = new AgentToolRegistry(List.of(tool));
        assertSame(tool, registry.find("ask_user_questions"));
        assertEquals(1, registry.definitions().size());
        assertThrows(IllegalStateException.class, () -> new AgentToolRegistry(List.of(tool, tool)));
        assertThrows(BusinessException.class, () -> registry.find("missing"));
    }
}
