package com.qianxun.homestay.modules.agent.core.node;
import com.qianxun.homestay.modules.agent.core.model.AgentModel;
import com.qianxun.homestay.modules.agent.core.schema.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
/** 组装服务端提示词与历史，调用模型后交给工具或输出节点。 */
@Component public class ModelNode {
    private final AgentModel model;
    private final String prompt;
    public ModelNode(AgentModel model) throws IOException {
        this.model = model;
        this.prompt = read("prompts/agent/system.md") + "\n\n" + read("prompts/agent/agent.md");
    }
    private static String read(String path) throws IOException { return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8); }
    /** 将提示词置于服务端控制的 system 消息中。 */
    public AgentModelResult execute(List<AgentModelMessage> history, String message) {
        List<AgentModelMessage> messages = new ArrayList<>();
        messages.add(new AgentModelMessage("system", prompt)); messages.addAll(history); messages.add(new AgentModelMessage("user", message));
        return model.generate(messages);
    }
}
