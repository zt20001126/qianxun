package com.qianxun.homestay.modules.agent;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.modules.agent.core.model.AgentModel;
import com.qianxun.homestay.modules.agent.core.node.*;
import com.qianxun.homestay.modules.agent.core.schema.AgentModelResult;
import com.qianxun.homestay.modules.agent.core.tool.*;
import java.util.Map;
import com.qianxun.homestay.modules.agent.dto.AgentChatRequest;
import com.qianxun.homestay.modules.agent.entity.*;
import com.qianxun.homestay.modules.agent.mapper.AgentMapper;
import com.qianxun.homestay.modules.agent.service.impl.AgentServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
/** 检查节点流程、归属及并发冲突。 */
class AgentServiceImplTest {
    AgentMapper mapper; AgentModel model; ObjectMapper json; AgentServiceImpl service;
    @BeforeEach void setup() throws Exception {
        mapper=mock(AgentMapper.class); model=mock(AgentModel.class); json=new ObjectMapper();
        TransactionTemplate tx=mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        service=new AgentServiceImpl(mapper,json,tx,new InputNode(),new ModelNode(model),new ToolNode(new AgentToolRegistry(List.of(new AskUserQuestionsTool(), new AgentTool() {
            public String name() { return "test_query"; }
            public String description() { return "测试查询"; }
            public Map<String,Object> parameters() { return Map.of(); }
            public AgentToolResult invoke(com.fasterxml.jackson.databind.JsonNode args) { return AgentToolResult.completed("{\"value\":1}"); }
        }))),new OutputNode());
    }
    @Test void firstMessageSavesBothMessages() {
        when(model.generate(anyList())).thenReturn(new AgentModelResult("您好",null,null));
        doAnswer(call -> { AgentConversationEntity c=call.getArgument(0); c.id=12L; return 1; }).when(mapper).insertConversation(any());
        var result=service.chat(1L,new AgentChatRequest(null,"你好"));
        assertEquals(12L,result.conversationId()); assertEquals("completed",result.status()); assertEquals("您好",result.answer());
        verify(mapper,times(2)).insertMessage(any());
    }
    @Test void questionIsReturnedAndSaved() throws Exception {
        when(model.generate(anyList())).thenReturn(new AgentModelResult(null,"ask_user_questions",json.readTree("{\"questions\":[{\"id\":\"purpose\",\"title\":\"用途是什么？\"}]}")));
        doAnswer(call -> { AgentConversationEntity c=call.getArgument(0); c.id=12L; return 1; }).when(mapper).insertConversation(any());
        var result=service.chat(1L,new AgentChatRequest(null,"帮我写文案"));
        assertEquals("waiting",result.status()); assertEquals("用途是什么？",result.questions().get(0).title());
        verify(mapper,times(2)).insertMessage(any());
    }
    @Test void rejectsOtherAdministratorsConversationBeforeCallingModel() {
        when(mapper.findOwned(9L,1L)).thenReturn(null);
        assertThrows(BusinessException.class,()->service.chat(1L,new AgentChatRequest(9L,"你好")));
        verifyNoInteractions(model);
    }
    @Test void concurrentUpdateDoesNotSaveStaleAnswer() {
        AgentConversationEntity c=new AgentConversationEntity(); c.id=9L;
        AgentMessageEntity m=new AgentMessageEntity(); m.id=4L; m.role="user"; m.content="{\"parts\":[{\"type\":\"text\",\"text\":\"你好\"}]}";
        when(mapper.findOwned(9L,1L)).thenReturn(c); when(mapper.recentMessages(9L,40)).thenReturn(List.of(m));
        when(mapper.lockOwned(9L,1L)).thenReturn(c); when(mapper.latestMessageId(9L)).thenReturn(5L);
        when(model.generate(anyList())).thenReturn(new AgentModelResult("回答",null,null));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, assertThrows(BusinessException.class,()->service.chat(1L,new AgentChatRequest(9L,"继续"))).getStatus());
        verify(mapper,never()).insertMessage(any());
    }
    @Test void invalidToolAndInputAreRejected() throws Exception {
        var tool=new AskUserQuestionsTool();
        assertThrows(BusinessException.class,()->tool.execute(json.readTree("{\"questions\":[{\"id\":\"a\",\"title\":\"  \"}]}")));
        assertThrows(BusinessException.class,()->service.chat(1L,new AgentChatRequest(null," ")));
        verifyNoInteractions(model);
    }
    @Test void rejectsDuplicateChoiceLabels() throws Exception {
        var arguments = json.readTree("""
                {"questions":[{"id":"q","type":"single_choice","title":"选择？","allow_custom_input":false,
                "options":[{"label":"A","description":"一"},{"label":"a","description":"二"},
                {"label":"C","description":"三"},{"label":"D","description":"四"}]}]}
                """);
        assertThrows(BusinessException.class, () -> new AskUserQuestionsTool().execute(arguments));
    }
    @Test void modelFailureDoesNotPersist() {
        when(model.generate(anyList())).thenThrow(new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"模型服务暂时不可用"));
        assertThrows(BusinessException.class,()->service.chat(1L,new AgentChatRequest(null,"你好")));
        verify(mapper,never()).insertConversation(any()); verify(mapper,never()).insertMessage(any());
    }

    @Test void queryToolReturnsResultToModelBeforeSavingAnswer() throws Exception {
        when(model.generate(anyList())).thenReturn(
                new AgentModelResult(null,"test_query",json.readTree("{}"),"call-1"),
                new AgentModelResult("今日营业额为 1 元",null,null));
        doAnswer(call -> { AgentConversationEntity c=call.getArgument(0); c.id=12L; return 1; }).when(mapper).insertConversation(any());
        var result=service.chat(1L,new AgentChatRequest(null,"今日营业情况"));
        assertEquals("completed",result.status());
        assertEquals("今日营业额为 1 元",result.answer());
        var captor=org.mockito.ArgumentCaptor.forClass(List.class);
        verify(model,times(2)).generate(captor.capture());
        var second=captor.getAllValues().get(1);
        var response=(com.qianxun.homestay.modules.agent.core.schema.AgentModelMessage) second.get(second.size()-1);
        assertEquals("tool",response.role()); assertEquals("call-1",response.toolCallId());
        assertEquals("{\"value\":1}",response.content());
    }
    @Test void repeatedQueryToolCallsStopWithoutSaving() throws Exception {
        when(model.generate(anyList())).thenReturn(new AgentModelResult(null,"test_query",json.readTree("{}"),"call-1"));
        assertThrows(BusinessException.class,()->service.chat(1L,new AgentChatRequest(null,"查询")));
        verify(mapper,never()).insertConversation(any());
    }
}
