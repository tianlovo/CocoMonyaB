package org.xlyo.cocomonyab.controller.readonly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.vo.MessageVO;
import org.xlyo.cocomonyab.service.MessageService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * MessageController 向后兼容性测试
 * 验证移动到 readonly 子包后，所有现有 API 端点仍然可访问且功能正常
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController 向后兼容性测试")
class MessageControllerBackwardCompatibilityTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    @Test
    @DisplayName("GET /{id} - API路径保持不变")
    void getById_ApiPathUnchanged() {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        MessageVO messageVO = createTestMessageVO(id);
        when(messageService.getById(id)).thenReturn(messageVO);

        // When
        ApiResponse<MessageVO> response = messageController.getById(id);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(id);
        assertThat(response.getData().getMessageId()).isEqualTo(123456L);
    }

    @Test
    @DisplayName("GET /by-tg-id - API路径保持不变")
    void getByTgId_ApiPathUnchanged() {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        MessageVO messageVO = createTestMessageVO("test-id");
        when(messageService.getByTgId(chatId, messageId)).thenReturn(messageVO);

        // When
        ApiResponse<MessageVO> response = messageController.getByTgId(chatId, messageId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getChatId()).isEqualTo(chatId);
        assertThat(response.getData().getMessageId()).isEqualTo(messageId);
    }

    @Test
    @DisplayName("GET /page - API路径保持不变")
    void page_ApiPathUnchanged() {
        // Given
        List<MessageVO> messages = Arrays.asList(
                createTestMessageVO("id1"),
                createTestMessageVO("id2")
        );
        Page<MessageVO> page = new PageImpl<>(messages);
        when(messageService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When
        PageResponse<MessageVO> response = messageController.page(1L, 10L, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getCurrent()).isEqualTo(1L);
        assertThat(response.getSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("GET /media-album - API路径保持不变")
    void getMediaAlbum_ApiPathUnchanged() {
        // Given
        Long chatId = -1001234567890L;
        Long mediaAlbumId = 789L;
        List<MessageVO> messages = Arrays.asList(
                createTestMessageVO("id1"),
                createTestMessageVO("id2")
        );
        when(messageService.getMediaAlbum(chatId, mediaAlbumId)).thenReturn(messages);

        // When
        ApiResponse<List<MessageVO>> response = messageController.getMediaAlbum(chatId, mediaAlbumId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    @DisplayName("响应格式保持不变 - 包含code、msg、data字段")
    void responseFormat_Unchanged() {
        // Given
        String id = "test-id";
        MessageVO messageVO = createTestMessageVO(id);
        when(messageService.getById(id)).thenReturn(messageVO);

        // When
        ApiResponse<MessageVO> response = messageController.getById(id);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isNotNull();
        assertThat(response.getMsg()).isNotNull();
        assertThat(response.getData()).isNotNull();
    }

    @Test
    @DisplayName("分页响应格式保持不变 - 包含data、current、size、total、pages字段")
    void pageResponseFormat_Unchanged() {
        // Given
        List<MessageVO> messages = Arrays.asList(createTestMessageVO("id1"));
        Page<MessageVO> page = new PageImpl<>(messages);
        when(messageService.page(eq(1L), eq(10L), any())).thenReturn(page);

        // When
        PageResponse<MessageVO> response = messageController.page(1L, 10L, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isNotNull();
        assertThat(response.getMsg()).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getCurrent()).isNotNull();
        assertThat(response.getSize()).isNotNull();
        assertThat(response.getTotal()).isNotNull();
        assertThat(response.getPages()).isNotNull();
    }

    private MessageVO createTestMessageVO(String id) {
        MessageVO vo = new MessageVO();
        vo.setId(id);
        vo.setMessageId(123456L);
        vo.setChatId(-1001234567890L);
        vo.setDate(1700000000);
        vo.setMediaAlbumId(null);
        vo.setRawJson("{\"test\": \"data\"}");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }
}
