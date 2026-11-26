package com.qianxun.homestay.modules.agent.core.node;
import com.qianxun.homestay.modules.agent.core.model.AgentModel;
import com.qianxun.homestay.modules.agent.core.schema.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
/** 组装服务端提示词与历史，调用模型后交给工具或输出节点。 */
@Component public class ModelNode {
    private final AgentModel model;
    private final String prompt;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    public ModelNode(AgentModel model) throws IOException {
        this.model = model;
        this.prompt = read("prompts/agent/system.md") + "\n\n" + read("prompts/agent/agent.md");
    }
    private static String read(String path) throws IOException { return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8); }
    /** 将提示词置于服务端控制的 system 消息中。 */
    public AgentModelResult execute(List<AgentModelMessage> history, String message) {
        List<AgentModelMessage> messages = new ArrayList<>();
        messages.add(new AgentModelMessage("system", currentPrompt())); messages.addAll(history); messages.add(new AgentModelMessage("user", message));
        return model.generate(messages);
    }
    /** 保持原上下文并将工具结果交还模型。 */
    public AgentModelResult resume(List<AgentModelMessage> history, String message, List<AgentModelMessage> toolMessages) {
        List<AgentModelMessage> messages = new ArrayList<>();
        messages.add(new AgentModelMessage("system", currentPrompt()));
        messages.addAll(history);
        messages.add(new AgentModelMessage("user", message));
        messages.addAll(toolMessages);
        return model.generate(messages);
    }
    /** 提供北京时间日期，避免模型猜测相对日期。 */
    private String currentPrompt() {
        return prompt + "\n\n当前北京时间日期：" + LocalDate.now(BUSINESS_ZONE) + "。";
    }
}
