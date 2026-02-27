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
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ForwardQueueRepository 单元测试
 * 使用 Testcontainers 提供真实的 MongoDB 环境
 */
@SpringBootTest
@Testcontainers
class ForwardQueueRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ForwardQueueRepository repository;

    private ForwardQueueItem testItem1;
    private ForwardQueueItem testItem2;
    private ForwardQueueItem testItem3;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testItem1 = ForwardQueueItem.builder()
                .sourceChatId(-1001234567890L)
                .sourceMessageId(100L)
                .matchedTags(List.of("tag1", "tag2"))
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now().minusSeconds(3600))
                .updateTime(Instant.now().minusSeconds(3600))
                .retryCount(0)
                .build();

        testItem2 = ForwardQueueItem.builder()
                .sourceChatId(-1001234567890L)
                .sourceMessageId(101L)
                .matchedTags(List.of("tag3"))
                .status(ForwardStatus.SUCCESS)
                .createTime(Instant.now().minusSeconds(1800))
                .updateTime(Instant.now().minusSeconds(1800))
                .forwardTime(Instant.now().minusSeconds(1800))
                .retryCount(0)
                .build();

        testItem3 = ForwardQueueItem.builder()
                .sourceChatId(-1009876543210L)
                .sourceMessageId(200L)
                .matchedTags(List.of("tag1"))
                .status(ForwardStatus.FAILED)
                .createTime(Instant.now().minusSeconds(900))
                .updateTime(Instant.now().minusSeconds(900))
                .retryCount(3)
                .errorMessage("Failed to forward")
                .build();

        repository.saveAll(List.of(testItem1, testItem2, testItem3));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void findBySourceChatIdAndSourceMessageId_ShouldReturnItem_WhenExists() {
        // When
        Optional<ForwardQueueItem> result = repository.findBySourceChatIdAndSourceMessageId(
                -1001234567890L, 100L
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSourceChatId()).isEqualTo(-1001234567890L);
        assertThat(result.get().getSourceMessageId()).isEqualTo(100L);
    }

    @Test
    void findBySourceChatIdAndSourceMessageId_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<ForwardQueueItem> result = repository.findBySourceChatIdAndSourceMessageId(
                -1001234567890L, 999L
        );

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findBySourceChatIdOrderByCreateTimeAsc_ShouldReturnItemsInAscendingOrder() {
        // When
        Page<ForwardQueueItem> result = repository.findBySourceChatIdOrderByCreateTimeAsc(
                -1001234567890L, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getCreateTime())
                .isBefore(result.getContent().get(1).getCreateTime());
    }

    @Test
    void findByStatusOrderByCreateTimeAsc_ShouldReturnItemsByStatus() {
        // When
        Page<ForwardQueueItem> result = repository.findByStatusOrderByCreateTimeAsc(
                ForwardStatus.PENDING, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ForwardStatus.PENDING);
    }

    @Test
    void findBySourceChatIdAndStatusOrderByCreateTimeAsc_ShouldReturnFilteredItems() {
        // When
        Page<ForwardQueueItem> result = repository.findBySourceChatIdAndStatusOrderByCreateTimeAsc(
                -1001234567890L, 
                ForwardStatus.SUCCESS, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSourceChatId()).isEqualTo(-1001234567890L);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ForwardStatus.SUCCESS);
    }

    @Test
    void countByStatus_ShouldReturnCorrectCount() {
        // When
        Long pendingCount = repository.countByStatus(ForwardStatus.PENDING);
        Long successCount = repository.countByStatus(ForwardStatus.SUCCESS);
        Long failedCount = repository.countByStatus(ForwardStatus.FAILED);

        // Then
        assertThat(pendingCount).isEqualTo(1L);
        assertThat(successCount).isEqualTo(1L);
        assertThat(failedCount).isEqualTo(1L);
    }

    @Test
    void countByStatus_ShouldReturnZero_WhenNoItemsWithStatus() {
        // Given
        repository.deleteAll();

        // When
        Long count = repository.countByStatus(ForwardStatus.PENDING);

        // Then
        assertThat(count).isZero();
    }
}
