package org.xlyo.cocomonyab.controller.readonly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.vo.ProcessedMessageVO;
import org.xlyo.cocomonyab.service.ProcessedMessageService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProcessedMessageController 单元测试
 */
@WebMvcTest(ProcessedMessageController.class)
@DisplayName("已处理消息控制器测试")
class ProcessedMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessedMessageService processedMessageService;

    @Test
    @DisplayName("GET /by-tg-id - 成功查询")
    void getByTgId_Success() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        ProcessedMessageVO vo = createTestProcessedMessageVO("test-id");
        when(processedMessageService.getByTgId(chatId, messageId)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/processed-message/by-tg-id")
                        .param("chatId", chatId.toString())
                        .param("messageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.chatId").value(chatId))
                .andExpect(jsonPath("$.data.messageId").value(messageId));
    }

    @Test
    @DisplayName("GET /by-tg-id - 记录不存在")
    void getByTgId_NotFound() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        when(processedMessageService.getByTgId(chatId, messageId))
                .thenThrow(new BusinessException(ResponseCode.DATA_NOT_FOUND, "已处理消息记录不存在"));

        // When & Then
        mockMvc.perform(get("/api/processed-message/by-tg-id")
                        .param("chatId", chatId.toString())
                        .param("messageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("GET /by-tg-id - 缺少必填参数")
    void getByTgId_MissingParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/processed-message/by-tg-id")
                        .param("chatId", "-1001234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 成功分页查询")
    void page_Success() throws Exception {
        // Given
        List<ProcessedMessageVO> messages = Arrays.asList(
                createTestProcessedMessageVO("id1"),
                createTestProcessedMessageVO("id2")
        );
        Page<ProcessedMessageVO> page = new PageImpl<>(messages);
        when(processedMessageService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/processed-message/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /page - 带过滤条件查询")
    void page_WithFilters() throws Exception {
        // Given
        List<ProcessedMessageVO> messages = Arrays.asList(createTestProcessedMessageVO("id1"));
        Page<ProcessedMessageVO> page = new PageImpl<>(messages);
        when(processedMessageService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/processed-message/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("chatId", "-1001234567890")
                        .param("isRead", "false")
                        .param("isMatched", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("GET /page - 页码验证失败")
    void page_InvalidCurrent() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/processed-message/page")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 每页大小验证失败")
    void page_InvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/processed-message/page")
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /unread - 成功查询未读消息")
    void getUnreadMessages_Success() throws Exception {
        // Given
        List<ProcessedMessageVO> messages = Arrays.asList(
                createTestProcessedMessageVO("id1"),
                createTestProcessedMessageVO("id2")
        );
        Page<ProcessedMessageVO> page = new PageImpl<>(messages);
        when(processedMessageService.getUnreadMessages(eq(1L), eq(10L), eq(null))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/processed-message/unread")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /unread - 带chatId过滤")
    void getUnreadMessages_WithChatId() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        List<ProcessedMessageVO> messages = Arrays.asList(createTestProcessedMessageVO("id1"));
        Page<ProcessedMessageVO> page = new PageImpl<>(messages);
        when(processedMessageService.getUnreadMessages(eq(1L), eq(10L), eq(chatId))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/processed-message/unread")
                        .param("current", "1")
                        .param("size", "10")
                        .param("chatId", chatId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("GET /matched - 成功查询匹配标签的消息")
    void getMatchedMessages_Success() throws Exception {
        // Given
        List<ProcessedMessageVO> messages = Arrays.asList(
                createTestProcessedMessageVO("id1"),
                createTestProcessedMessageVO("id2")
        );
        Page<ProcessedMessageVO> page = new PageImpl<>(messages);
        when(processedMessageService.getMatchedMessages(eq(1L), eq(10L), eq(null))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/processed-message/matched")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /matched - 带chatId过滤")
    void getMatchedMessages_WithChatId() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        List<ProcessedMessageVO> messages = Arrays.asList(createTestProcessedMessageVO("id1"));
        Page<ProcessedMessageVO> page = new PageImpl<>(messages);
        when(processedMessageService.getMatchedMessages(eq(1L), eq(10L), eq(chatId))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/processed-message/matched")
                        .param("current", "1")
                        .param("size", "10")
                        .param("chatId", chatId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("响应格式符合规范")
    void responseFormat_Compliant() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        ProcessedMessageVO vo = createTestProcessedMessageVO("test-id");
        when(processedMessageService.getByTgId(chatId, messageId)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/processed-message/by-tg-id")
                        .param("chatId", chatId.toString())
                        .param("messageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    private ProcessedMessageVO createTestProcessedMessageVO(String id) {
        ProcessedMessageVO vo = new ProcessedMessageVO();
        vo.setId(id);
        vo.setChatId(-1001234567890L);
        vo.setMessageId(123456L);
        vo.setMessageType("TEXT");
        vo.setIsRead(false);
        vo.setIsMatched(true);
        vo.setMatchedTags(Arrays.asList("tag1", "tag2"));
        vo.setProcessTime(LocalDateTime.now());
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
