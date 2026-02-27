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
import org.xlyo.cocomonyab.domain.vo.ForwardQueueStatsVO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueVO;
import org.xlyo.cocomonyab.service.ForwardQueueService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ForwardQueueController 单元测试
 */
@WebMvcTest(ForwardQueueController.class)
@DisplayName("转发队列控制器测试")
class ForwardQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForwardQueueService forwardQueueService;

    @Test
    @DisplayName("GET /{id} - 成功查询")
    void getById_Success() throws Exception {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        ForwardQueueVO vo = createTestForwardQueueVO(id);
        when(forwardQueueService.getById(id)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/forward-queue/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.sourceChatId").value(-1001234567890L));
    }

    @Test
    @DisplayName("GET /{id} - 记录不存在")
    void getById_NotFound() throws Exception {
        // Given
        String id = "nonexistent";
        when(forwardQueueService.getById(id))
                .thenThrow(new BusinessException(ResponseCode.DATA_NOT_FOUND, "转发队列记录不存在: " + id));

        // When & Then
        mockMvc.perform(get("/api/forward-queue/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("GET /by-source - 成功查询")
    void getBySource_Success() throws Exception {
        // Given
        Long sourceChatId = -1001234567890L;
        Long sourceMessageId = 123456L;
        ForwardQueueVO vo = createTestForwardQueueVO("test-id");
        when(forwardQueueService.getBySource(sourceChatId, sourceMessageId)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/forward-queue/by-source")
                        .param("sourceChatId", sourceChatId.toString())
                        .param("sourceMessageId", sourceMessageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.sourceChatId").value(sourceChatId))
                .andExpect(jsonPath("$.data.sourceMessageId").value(sourceMessageId));
    }

    @Test
    @DisplayName("GET /by-source - 缺少必填参数")
    void getBySource_MissingParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/forward-queue/by-source")
                        .param("sourceChatId", "-1001234567890"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 成功分页查询")
    void page_Success() throws Exception {
        // Given
        List<ForwardQueueVO> queues = Arrays.asList(
                createTestForwardQueueVO("id1"),
                createTestForwardQueueVO("id2")
        );
        Page<ForwardQueueVO> page = new PageImpl<>(queues);
        when(forwardQueueService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/forward-queue/page")
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
        List<ForwardQueueVO> queues = Arrays.asList(createTestForwardQueueVO("id1"));
        Page<ForwardQueueVO> page = new PageImpl<>(queues);
        when(forwardQueueService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/forward-queue/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("sourceChatId", "-1001234567890")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("GET /page - 页码验证失败")
    void page_InvalidCurrent() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/forward-queue/page")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /page - 每页大小验证失败")
    void page_InvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/forward-queue/page")
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /stats - 成功查询统计信息")
    void getStats_Success() throws Exception {
        // Given
        ForwardQueueStatsVO stats = ForwardQueueStatsVO.builder()
                .pendingCount(10L)
                .successCount(50L)
                .failedCount(5L)
                .totalCount(65L)
                .build();
        when(forwardQueueService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/forward-queue/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(10))
                .andExpect(jsonPath("$.data.successCount").value(50))
                .andExpect(jsonPath("$.data.failedCount").value(5))
                .andExpect(jsonPath("$.data.totalCount").value(65));
    }

    @Test
    @DisplayName("响应格式符合规范")
    void responseFormat_Compliant() throws Exception {
        // Given
        String id = "test-id";
        ForwardQueueVO vo = createTestForwardQueueVO(id);
        when(forwardQueueService.getById(id)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/forward-queue/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    private ForwardQueueVO createTestForwardQueueVO(String id) {
        ForwardQueueVO vo = new ForwardQueueVO();
        vo.setId(id);
        vo.setSourceChatId(-1001234567890L);
        vo.setSourceMessageId(123456L);
        vo.setStatus("PENDING");
        vo.setRetryCount(0);
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
