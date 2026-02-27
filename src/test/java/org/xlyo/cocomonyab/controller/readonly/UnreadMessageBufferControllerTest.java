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
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferStatsVO;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferVO;
import org.xlyo.cocomonyab.service.UnreadMessageBufferService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UnreadMessageBufferController 单元测试
 */
@WebMvcTest(UnreadMessageBufferController.class)
@DisplayName("未读消息缓冲区控制器测试")
class UnreadMessageBufferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnreadMessageBufferService unreadMessageBufferService;

    @Test
    @DisplayName("GET /by-tg-id - 成功查询")
    void getByTgId_Success() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        UnreadMessageBufferVO vo = createTestUnreadMessageBufferVO("test-id");
        when(unreadMessageBufferService.getByTgId(chatId, messageId)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/by-tg-id")
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
        when(unreadMessageBufferService.getByTgId(chatId, messageId))
                .thenThrow(new BusinessException(ResponseCode.DATA_NOT_FOUND, "未读消息缓冲记录不存在"));

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/by-tg-id")
                        .param("chatId", chatId.toString())
                        .param("messageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("GET /by-tg-id - 缺少必填参数")
    void getByTgId_MissingParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/unread-buffer/by-tg-id")
                        .param("chatId", "-1001234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 成功分页查询")
    void page_Success() throws Exception {
        // Given
        List<UnreadMessageBufferVO> buffers = Arrays.asList(
                createTestUnreadMessageBufferVO("id1"),
                createTestUnreadMessageBufferVO("id2")
        );
        Page<UnreadMessageBufferVO> page = new PageImpl<>(buffers);
        when(unreadMessageBufferService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/page")
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
        List<UnreadMessageBufferVO> buffers = Arrays.asList(createTestUnreadMessageBufferVO("id1"));
        Page<UnreadMessageBufferVO> page = new PageImpl<>(buffers);
        when(unreadMessageBufferService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("chatId", "-1001234567890")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("GET /page - 页码验证失败")
    void page_InvalidCurrent() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/unread-buffer/page")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 每页大小验证失败")
    void page_InvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/unread-buffer/page")
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /pending-count - 成功查询待处理数量（无chatId）")
    void getPendingCount_WithoutChatId() throws Exception {
        // Given
        when(unreadMessageBufferService.getPendingCount(null)).thenReturn(25L);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/pending-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(25));
    }

    @Test
    @DisplayName("GET /pending-count - 成功查询待处理数量（带chatId）")
    void getPendingCount_WithChatId() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        when(unreadMessageBufferService.getPendingCount(chatId)).thenReturn(10L);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/pending-count")
                        .param("chatId", chatId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    @DisplayName("GET /stats - 成功查询统计信息")
    void getStats_Success() throws Exception {
        // Given
        UnreadMessageBufferStatsVO stats = UnreadMessageBufferStatsVO.builder()
                .pendingCount(15L)
                .processedCount(80L)
                .failedCount(5L)
                .totalCount(100L)
                .build();
        when(unreadMessageBufferService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(15))
                .andExpect(jsonPath("$.data.processedCount").value(80))
                .andExpect(jsonPath("$.data.failedCount").value(5))
                .andExpect(jsonPath("$.data.totalCount").value(100));
    }

    @Test
    @DisplayName("响应格式符合规范")
    void responseFormat_Compliant() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        UnreadMessageBufferVO vo = createTestUnreadMessageBufferVO("test-id");
        when(unreadMessageBufferService.getByTgId(chatId, messageId)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/unread-buffer/by-tg-id")
                        .param("chatId", chatId.toString())
                        .param("messageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    private UnreadMessageBufferVO createTestUnreadMessageBufferVO(String id) {
        UnreadMessageBufferVO vo = new UnreadMessageBufferVO();
        vo.setId(id);
        vo.setChatId(-1001234567890L);
        vo.setMessageId(123456L);
        vo.setFetchTime(LocalDateTime.now());
        vo.setStatus("PENDING");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
