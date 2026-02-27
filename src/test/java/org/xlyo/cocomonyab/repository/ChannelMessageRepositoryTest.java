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
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage.MessageStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChannelMessageRepository 单元测试
 * 使用 Testcontainers 提供真实的 MongoDB 环境
 */
@SpringBootTest
@Testcontainers
class ChannelMessageRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ChannelMessageRepository repository;

    private ChannelMessage testMessage1;
    private ChannelMessage testMessage2;
    private ChannelMessage testMessage3;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testMessage1 = new ChannelMessage();
        testMessage1.setChatId(-1001234567890L);
        testMessage1.setMessageId(100L);
        testMessage1.setDate(1700000000);
        testMessage1.setStatus(MessageStatus.PENDING);
        testMessage1.setMediaAlbumId(0L);
        testMessage1.setCreateTime(LocalDateTime.now().minusDays(2));
        testMessage1.setUpdateTime(LocalDateTime.now().minusDays(2));

        testMessage2 = new ChannelMessage();
        testMessage2.setChatId(-1001234567890L);
        testMessage2.setMessageId(101L);
        testMessage2.setDate(1700000100);
        testMessage2.setStatus(MessageStatus.APPROVED);
        testMessage2.setMediaAlbumId(12345L);
        testMessage2.setCreateTime(LocalDateTime.now().minusDays(1));
        testMessage2.setUpdateTime(LocalDateTime.now().minusDays(1));

        testMessage3 = new ChannelMessage();
        testMessage3.setChatId(-1009876543210L);
        testMessage3.setMessageId(200L);
        testMessage3.setDate(1700000200);
        testMessage3.setStatus(MessageStatus.PENDING);
        testMessage3.setMediaAlbumId(12345L);
        testMessage3.setCreateTime(LocalDateTime.now());
        testMessage3.setUpdateTime(LocalDateTime.now());

        repository.saveAll(List.of(testMessage1, testMessage2, testMessage3));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void findByChatIdAndMessageId_ShouldReturnMessage_WhenExists() {
        // When
        Optional<ChannelMessage> result = repository.findByChatIdAndMessageId(-1001234567890L, 100L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.get().getMessageId()).isEqualTo(100L);
    }

    @Test
    void findByChatIdAndMessageId_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<ChannelMessage> result = repository.findByChatIdAndMessageId(-1001234567890L, 999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByChatIdOrderByDateDesc_ShouldReturnMessagesInDescendingOrder() {
        // When
        Page<ChannelMessage> result = repository.findByChatIdOrderByDateDesc(
                -1001234567890L, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDate()).isGreaterThan(result.getContent().get(1).getDate());
    }

    @Test
    void findByStatusOrderByCreateTimeDesc_ShouldReturnMessagesByStatus() {
        // When
        Page<ChannelMessage> result = repository.findByStatusOrderByCreateTimeDesc(
                MessageStatus.PENDING, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(msg -> msg.getStatus() == MessageStatus.PENDING);
    }

    @Test
    void findByChatIdAndStatusOrderByDateDesc_ShouldReturnFilteredMessages() {
        // When
        Page<ChannelMessage> result = repository.findByChatIdAndStatusOrderByDateDesc(
                -1001234567890L, 
                MessageStatus.PENDING, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(MessageStatus.PENDING);
    }

    @Test
    void findByChatIdAndDateBetweenOrderByDateDesc_ShouldReturnMessagesInDateRange() {
        // When
        Page<ChannelMessage> result = repository.findByChatIdAndDateBetweenOrderByDateDesc(
                -1001234567890L, 
                1699999999, 
                1700000150, 
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(msg -> 
                msg.getDate() >= 1699999999 && msg.getDate() <= 1700000150
        );
    }

    @Test
    void findAllByChatIdAndMediaAlbumId_ShouldReturnMediaGroupMessages() {
        // When
        List<ChannelMessage> result = repository.findAllByChatIdAndMediaAlbumId(-1001234567890L, 12345L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMediaAlbumId()).isEqualTo(12345L);
    }

    @Test
    void findAllByChatIdAndMediaAlbumId_ShouldReturnEmpty_WhenNoMediaGroup() {
        // When
        List<ChannelMessage> result = repository.findAllByChatIdAndMediaAlbumId(-1001234567890L, 99999L);

        // Then
        assertThat(result).isEmpty();
    }
}
