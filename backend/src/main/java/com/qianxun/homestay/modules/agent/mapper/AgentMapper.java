package com.qianxun.homestay.modules.agent.mapper;
import com.qianxun.homestay.modules.agent.entity.AgentConversationEntity;
import com.qianxun.homestay.modules.agent.entity.AgentMessageEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 智能体会话及消息的数据访问。 */
@Mapper
public interface AgentMapper {
    AgentConversationEntity findOwned(@Param("id") Long id, @Param("adminId") Long adminId);
    AgentConversationEntity lockOwned(@Param("id") Long id, @Param("adminId") Long adminId);
    List<AgentConversationEntity> listConversations(@Param("adminId") Long adminId, @Param("offset") long offset, @Param("size") int size);
    long countConversations(@Param("adminId") Long adminId);
    List<AgentMessageEntity> recentMessages(@Param("conversationId") Long conversationId, @Param("limit") int limit);
    List<AgentMessageEntity> listMessages(@Param("conversationId") Long conversationId, @Param("offset") long offset, @Param("size") int size);
    long countMessages(@Param("conversationId") Long conversationId);
    Long latestMessageId(@Param("conversationId") Long conversationId);
    int insertConversation(AgentConversationEntity conversation);
    int insertMessage(AgentMessageEntity message);
    int touchConversation(@Param("id") Long id);
}
