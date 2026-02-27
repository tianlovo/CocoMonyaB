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
import org.xlyo.cocomonyab.domain.entity.ProcessedMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProcessedMessageRepository 单元测试
 * 使用 Testcontainers 提供真实的 MongoDB 环境
 */
@SpringBootTest
@Testcontainers
class ProcessedMessageRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProcessedMessageRepository repository;

    private ProcessedMessage testMessage1;
    private ProcessedMessage testMessage2;
    private ProcessedMessage testMessage3;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testMessage1 = ProcessedMessage.builder()
                .chatId(-1001234567890L)
                .messageId(100L)
                .messageType("TEXT")
                .isRead(false)
                .isMatched(true)
                .matchedTags(new String[]{"tag1", "tag2"})
                .processTime(LocalDateTime.now().minusDays(2))
                .createTime(LocalDateTime.now().minusDays(2))
                .updateTime(LocalDateTime.now().minusDays(2))
                .build();

        testMessage2 = ProcessedMessage.builder()
                .chatId(-1001234567890L)
                .messageId(101L)
                .messageType("PHOTO")
                .isRead(true)
                .isMatched(false)
                .processTime(LocalDateTime.now().minusDays(1))
                .readTime(LocalDateTime.now().minusDays(1))
                .createTime(LocalDateTime.now().minusDays(1))
                .updateTime(LocalDateTime.now().minusDays(1))
                .build();

        testMessage3 = ProcessedMessage.builder()
                .chatId(-1009876543210L)
                .messageId(200L)
                .messageType("VIDEO")
                .isRead(false)
                .isMatched(true)
                .matchedTags(new String[]{"tag3"})
                .processTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(testMessage1, testMessage2, testMessage3));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void findByChatIdAndMessageId_ShouldReturnMessage_WhenExists() {
        // When
        Optional<ProcessedMessage> result = repository.findByChatIdAndMessageId(-1001234567890L, 100L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.get().getMessageId()).isEqualTo(100L);
    }

    @Test
    void findByChatIdAndMessageId_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<ProcessedMessage> result = repository.findByChatIdAndMessageId(-1001234567890L, 999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByChatIdOrderByProcessTimeDesc_ShouldReturnMessagesInDescendingOrder() {
        // When
        Page<ProcessedMessage> result = repository.findByChatIdOrderByProcessTimeDesc(
                -1001234567890L, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getProcessTime())
                .isAfter(result.getContent().get(1).getProcessTime());
    }

    @Test
    void findByIsReadOrderByProcessTimeDesc_ShouldReturnUnreadMessages() {
        // When
        Page<ProcessedMessage> result = repository.findByIsReadOrderByProcessTimeDesc(
                false, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(msg -> !msg.getIsRead());
    }

    @Test
    void findByIsMatchedOrderByProcessTimeDesc_ShouldReturnMatchedMessages() {
        // When
        Page<ProcessedMessage> result = repository.findByIsMatchedOrderByProcessTimeDesc(
                true, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(msg -> msg.getIsMatched());
    }

    @Test
    void findByChatIdAndIsReadOrderByProcessTimeDesc_ShouldReturnFilteredMessages() {
        // When
        Page<ProcessedMessage> result = repository.findByChatIdAndIsReadOrderByProcessTimeDesc(
                -1001234567890L, 
                false, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
    }

    @Test
    void findByChatIdAndIsMatchedOrderByProcessTimeDesc_ShouldReturnFilteredMessages() {
        // When
        Page<ProcessedMessage> result = repository.findByChatIdAndIsMatchedOrderByProcessTimeDesc(
                -1001234567890L, 
                true, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.getContent().get(0).getIsMatched()).isTrue();
    }
}
