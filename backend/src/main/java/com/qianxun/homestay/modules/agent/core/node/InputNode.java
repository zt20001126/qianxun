package com.qianxun.homestay.modules.agent.core.node;
import com.qianxun.homestay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** 校验用户输入；输出规范化文本供模型节点使用。 */
@Component public class InputNode {
    /** 校验并去除首尾空白。 */ public String execute(String input) {
        if (input == null || input.isBlank() || input.length() > 10000) throw new BusinessException(HttpStatus.BAD_REQUEST, "消息不能为空或超过10000字");
        return input.strip();
    }
}
