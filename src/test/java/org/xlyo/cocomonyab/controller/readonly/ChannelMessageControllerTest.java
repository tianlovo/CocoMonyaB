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
import org.xlyo.cocomonyab.domain.vo.ChannelMessageVO;
import org.xlyo.cocomonyab.service.ChannelMessageService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChannelMessageController 单元测试
 */
@WebMvcTest(ChannelMessageController.class)
@DisplayName("频道消息控制器测试")
class ChannelMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChannelMessageService channelMessageService;

    @Test
    @DisplayName("GET /{id} - 成功查询")
    void getById_Success() throws Exception {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        ChannelMessageVO vo = createTestChannelMessageVO(id);
        when(channelMessageService.getById(id)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/channel-message/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.messageId").value(123456));
    }

    @Test
    @DisplayName("GET /{id} - 消息不存在")
    void getById_NotFound() throws Exception {
        // Given
        String id = "nonexistent";
        when(channelMessageService.getById(id))
                .thenThrow(new BusinessException(ResponseCode.DATA_NOT_FOUND, "频道消息不存在: " + id));

        // When & Then
        mockMvc.perform(get("/api/channel-message/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("GET /by-tg-id - 成功查询")
    void getByTgId_Success() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        ChannelMessageVO vo = createTestChannelMessageVO("test-id");
        when(channelMessageService.getByTgId(chatId, messageId)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/channel-message/by-tg-id")
                        .param("chatId", chatId.toString())
                        .param("messageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.chatId").value(chatId))
                .andExpect(jsonPath("$.data.messageId").value(messageId));
    }

    @Test
    @DisplayName("GET /by-tg-id - 缺少必填参数chatId")
    void getByTgId_MissingChatId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/channel-message/by-tg-id")
                        .param("messageId", "123456"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /by-tg-id - 缺少必填参数messageId")
    void getByTgId_MissingMessageId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/channel-message/by-tg-id")
                        .param("chatId", "-1001234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 成功分页查询")
    void page_Success() throws Exception {
        // Given
        List<ChannelMessageVO> messages = Arrays.asList(
                createTestChannelMessageVO("id1"),
                createTestChannelMessageVO("id2")
        );
        Page<ChannelMessageVO> page = new PageImpl<>(messages);
        when(channelMessageService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/channel-message/page")
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
        List<ChannelMessageVO> messages = Arrays.asList(createTestChannelMessageVO("id1"));
        Page<ChannelMessageVO> page = new PageImpl<>(messages);
        when(channelMessageService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/channel-message/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("chatId", "-1001234567890")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("GET /page - 页码小于1")
    void page_InvalidCurrent() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/channel-message/page")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 每页大小小于1")
    void page_InvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/channel-message/page")
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 日期参数验证失败")
    void page_InvalidDateParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/channel-message/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("startDate", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /media-album - 成功查询")
    void getMediaAlbum_Success() throws Exception {
        // Given
        Long chatId = -1001234567890L;
        Long mediaAlbumId = 789L;
        List<ChannelMessageVO> messages = Arrays.asList(
                createTestChannelMessageVO("id1"),
                createTestChannelMessageVO("id2")
        );
        when(channelMessageService.getMediaAlbum(chatId, mediaAlbumId)).thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/channel-message/media-album")
                        .param("chatId", chatId.toString())
                        .param("mediaAlbumId", mediaAlbumId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /media-album - 缺少必填参数")
    void getMediaAlbum_MissingParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/channel-message/media-album")
                        .param("chatId", "-1001234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("响应格式符合规范")
    void responseFormat_Compliant() throws Exception {
        // Given
        String id = "test-id";
        ChannelMessageVO vo = createTestChannelMessageVO(id);
        when(channelMessageService.getById(id)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/channel-message/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    private ChannelMessageVO createTestChannelMessageVO(String id) {
        ChannelMessageVO vo = new ChannelMessageVO();
        vo.setId(id);
        vo.setMessageId(123456L);
        vo.setChatId(-1001234567890L);
        vo.setChannelUsername("test_channel");
        vo.setChannelTitle("Test Channel");
        vo.setDate(1700000000);
        vo.setContentType("TEXT");
        vo.setTextContent("Test message");
        vo.setStatus("PENDING");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
