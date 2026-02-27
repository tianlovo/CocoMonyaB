package org.xlyo.cocomonyab.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UnreadMessageBufferRepository 单元测试
 * 使用 Testcontainers 提供真实的 MongoDB 环境
 */
@SpringBootTest
@Testcontainers
class UnreadMessageBufferRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UnreadMessageBufferRepository repository;

    private UnreadMessageBuffer testBuffer1;
    private UnreadMessageBuffer testBuffer2;
    private UnreadMessageBuffer testBuffer3;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testBuffer1 = UnreadMessageBuffer.builder()
                .chatId(-1001234567890L)
                .messageId(100L)
                .fetchTime(LocalDateTime.now().minusHours(2))
                .status(BufferStatus.PENDING)
                .rawMessage("{\"message_id\":100}")
                .createTime(LocalDateTime.now().minusHours(2))
                .updateTime(LocalDateTime.now().minusHours(2))
                .build();

        testBuffer2 = UnreadMessageBuffer.builder()
                .chatId(-1001234567890L)
                .messageId(101L)
                .fetchTime(LocalDateTime.now().minusHours(1))
                .status(BufferStatus.PROCESSED)
                .rawMessage("{\"message_id\":101}")
                .createTime(LocalDateTime.now().minusHours(1))
                .updateTime(LocalDateTime.now().minusHours(1))
                .build();

        testBuffer3 = UnreadMessageBuffer.builder()
                .chatId(-1009876543210L)
                .messageId(200L)
                .fetchTime(LocalDateTime.now())
                .status(BufferStatus.FAILED)
                .rawMessage("{\"message_id\":200}")
                .errorMessage("Processing failed")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(testBuffer1, testBuffer2, testBuffer3));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void findByChatIdAndMessageId_ShouldReturnBuffer_WhenExists() {
        // When
        Optional<UnreadMessageBuffer> result = repository.findByChatIdAndMessageId(-1001234567890L, 100L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.get().getMessageId()).isEqualTo(100L);
    }

    @Test
    void findByChatIdAndMessageId_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<UnreadMessageBuffer> result = repository.findByChatIdAndMessageId(-1001234567890L, 999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByChatIdOrderByFetchTimeAsc_ShouldReturnBuffersInAscendingOrder() {
        // When
        Page<UnreadMessageBuffer> result = repository.findByChatIdOrderByFetchTimeAsc(
                -1001234567890L, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getFetchTime())
                .isBefore(result.getContent().get(1).getFetchTime());
    }

    @Test
    void findByStatusOrderByFetchTimeAsc_ShouldReturnBuffersByStatus() {
        // When
        Page<UnreadMessageBuffer> result = repository.findByStatusOrderByFetchTimeAsc(
                BufferStatus.PENDING, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(BufferStatus.PENDING);
    }

    @Test
    void findByChatIdAndStatusOrderByFetchTimeAsc_ShouldReturnFilteredBuffers() {
        // When
        Page<UnreadMessageBuffer> result = repository.findByChatIdAndStatusOrderByFetchTimeAsc(
                -1001234567890L, 
                BufferStatus.PROCESSED, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(BufferStatus.PROCESSED);
    }

    @Test
    void countByStatus_ShouldReturnCorrectCount() {
        // When
        long pendingCount = repository.countByStatus(BufferStatus.PENDING);
        long processedCount = repository.countByStatus(BufferStatus.PROCESSED);
        long failedCount = repository.countByStatus(BufferStatus.FAILED);

        // Then
        assertThat(pendingCount).isEqualTo(1L);
        assertThat(processedCount).isEqualTo(1L);
        assertThat(failedCount).isEqualTo(1L);
    }

    @Test
    void countByChatIdAndStatus_ShouldReturnCorrectCount() {
        // When
        Long count = repository.countByChatIdAndStatus(-1001234567890L, BufferStatus.PENDING);

        // Then
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void countByChatIdAndStatus_ShouldReturnZero_WhenNoBuffersMatch() {
        // When
        Long count = repository.countByChatIdAndStatus(-1001234567890L, BufferStatus.FAILED);

        // Then
        assertThat(count).isZero();
    }
}
