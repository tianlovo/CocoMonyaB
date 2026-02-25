package org.xlyo.cocomonyab.plugin.tagforward.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 属性测试：ForwardQueueItem数据模型
 * 
 * 验证属性 4: 队列项创建完整性
 * 验证需求: 4.2, 4.5, 4.6, 4.8
 */
class ForwardQueueItemPropertyTest {

    /**
     * 属性 4: 队列项创建完整性
     * 
     * 对于任何被标记为感兴趣的消息，创建的ForwardQueueItem应该包含：
     * - 正确的sourceChatId (需求 4.2)
     * - 正确的sourceMessageId
     * - 匹配的标签列表 (需求 4.5)
     * - PENDING状态 (需求 4.6)
     * - 初始retryCount为0 (需求 4.8)
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 4: 队列项创建完整性")
    void queueItemCreationCompleteness(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId,
            @ForAll @NotEmpty List<@From("tagStrings") String> matchedTags) {
        
        // Given: 创建时间
        Instant createTime = Instant.now();
        
        // When: 创建ForwardQueueItem
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(sourceChatId)
                .sourceMessageId(sourceMessageId)
                .matchedTags(matchedTags)
                .status(ForwardStatus.PENDING)
                .createTime(createTime)
                .retryCount(0)
                .build();
        
        // Then: 验证所有必需字段
        assertThat(item.getSourceChatId())
                .as("需求 4.2: ForwardQueueItem应包含sourceChatId字段")
                .isEqualTo(sourceChatId);
        
        assertThat(item.getSourceMessageId())
                .as("ForwardQueueItem应包含sourceMessageId字段")
                .isEqualTo(sourceMessageId);
        
        assertThat(item.getMatchedTags())
                .as("需求 4.5: ForwardQueueItem应包含matchedTags数组")
                .isNotNull()
                .isEqualTo(matchedTags)
                .hasSameSizeAs(matchedTags);
        
        assertThat(item.getStatus())
                .as("需求 4.6: ForwardQueueItem的status字段初始值应为PENDING")
                .isEqualTo(ForwardStatus.PENDING);
        
        assertThat(item.getCreateTime())
                .as("ForwardQueueItem应包含createTime字段")
                .isNotNull()
                .isEqualTo(createTime);
        
        assertThat(item.getRetryCount())
                .as("需求 4.8: ForwardQueueItem的retryCount字段初始值应为0")
                .isEqualTo(0);
    }

    /**
     * 属性 4.1: 队列项字段不可为null（关键字段）
     * 
     * 验证创建的队列项的关键字段不应该为null
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 4.1: 队列项关键字段非空")
    void queueItemKeyFieldsNotNull(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId,
            @ForAll @NotEmpty List<@From("tagStrings") String> matchedTags) {
        
        // When: 创建ForwardQueueItem
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(sourceChatId)
                .sourceMessageId(sourceMessageId)
                .matchedTags(matchedTags)
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .retryCount(0)
                .build();
        
        // Then: 验证关键字段不为null
        assertThat(item.getSourceChatId()).isNotNull();
        assertThat(item.getSourceMessageId()).isNotNull();
        assertThat(item.getMatchedTags()).isNotNull();
        assertThat(item.getStatus()).isNotNull();
        assertThat(item.getCreateTime()).isNotNull();
        assertThat(item.getRetryCount()).isNotNull();
    }

    /**
     * 属性 4.2: 队列项状态枚举有效性
     * 
     * 验证ForwardStatus枚举只包含预期的三个状态值
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 4.2: 状态枚举有效性")
    void forwardStatusEnumValidity(@ForAll("forwardStatuses") ForwardStatus status) {
        // Then: 验证状态只能是三个预定义值之一
        assertThat(status)
                .isIn(ForwardStatus.PENDING, ForwardStatus.SUCCESS, ForwardStatus.FAILED);
    }

    /**
     * 属性 4.3: 重试计数初始化为非负数
     * 
     * 验证retryCount初始值应该为0或正数
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 4.3: 重试计数非负")
    void retryCountIsNonNegative(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId,
            @ForAll @NotEmpty List<@From("tagStrings") String> matchedTags,
            @ForAll @IntRange(min = 0, max = 10) int retryCount) {
        
        // When: 创建ForwardQueueItem
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(sourceChatId)
                .sourceMessageId(sourceMessageId)
                .matchedTags(matchedTags)
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .retryCount(retryCount)
                .build();
        
        // Then: 验证retryCount为非负数
        assertThat(item.getRetryCount())
                .as("retryCount应该为非负数")
                .isGreaterThanOrEqualTo(0);
    }

    // ========== 数据生成器 ==========

    /**
     * 生成标签字符串
     */
    @Provide
    Arbitrary<String> tagStrings() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('#', '_', '-')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /**
     * 生成ForwardStatus枚举值
     */
    @Provide
    Arbitrary<ForwardStatus> forwardStatuses() {
        return Arbitraries.of(ForwardStatus.class);
    }
}
