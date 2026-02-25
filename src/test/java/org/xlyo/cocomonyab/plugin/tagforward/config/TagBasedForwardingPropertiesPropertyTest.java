package org.xlyo.cocomonyab.plugin.tagforward.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TagBasedForwardingProperties配置类的属性测试
 * 
 * <p>这些测试验证对所有可能输入都应该成立的通用属性，
 * 作为基于示例的单元测试的补充
 */
class TagBasedForwardingPropertiesPropertyTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    /**
     * 属性 6: Target Channel ID验证
     * 
     * <p>对于任何配置的target-channel-id，如果它不是负数，
     * 应该被拒绝并且转发功能应该被禁用
     * 
     * <p>验证需求: 5.3
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 6: 目标频道ID验证")
    void targetChannelIdMustBeNegative(@ForAll("nonNegativeChannelIds") Long channelId) {
        // Given: 一个包含非负频道ID的properties对象
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(channelId);
        
        // When: 验证properties
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        // Then: 验证应该失败并提示需要负数
        assertThat(violations)
            .as("Non-negative channel ID %d should be rejected", channelId)
            .isNotEmpty();
        
        assertThat(violations)
            .as("Violation should be on targetChannelId field")
            .anyMatch(v -> v.getPropertyPath().toString().equals("targetChannelId"));
        
        assertThat(violations)
            .as("Violation message should mention 'negative' requirement")
            .anyMatch(v -> v.getMessage().toLowerCase().contains("negative"));
    }
    
    /**
     * 属性 6 (补充): 有效的负数频道ID
     * 
     * <p>对于任何负数频道ID，验证应该通过
     * 这是补充属性，用于确保负数ID被接受
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 6 (complement): 负数频道ID应被接受")
    void negativeChannelIdsShouldBeAccepted(@ForAll("negativeChannelIds") Long channelId) {
        // Given: 一个包含负数频道ID的properties对象
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(channelId);
        
        // When: 验证properties
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        // Then: 验证应该通过（targetChannelId上没有违规）
        assertThat(violations)
            .as("Negative channel ID %d should be accepted", channelId)
            .filteredOn(v -> v.getPropertyPath().toString().equals("targetChannelId"))
            .isEmpty();
    }
    
    /**
     * 属性 6 (null情况): Null频道ID验证
     * 
     * <p>Null频道ID应该总是被拒绝，因为该字段是必需的
     */
    @Property
    @Label("Feature: tag-based-message-forwarding, Property 6 (null): null频道ID应被拒绝")
    void nullChannelIdShouldBeRejected() {
        // Given: 一个包含null频道ID的properties对象
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(null);
        
        // When: 验证properties
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        // Then: 验证应该失败并提示NotNull违规
        assertThat(violations)
            .as("Null channel ID should be rejected")
            .isNotEmpty();
        
        assertThat(violations)
            .as("Violation should be on targetChannelId field")
            .anyMatch(v -> v.getPropertyPath().toString().equals("targetChannelId"));
        
        assertThat(violations)
            .as("Violation message should mention 'configured' or 'not null'")
            .anyMatch(v -> {
                String msg = v.getMessage().toLowerCase();
                return msg.contains("configured") || msg.contains("not null") || msg.contains("must not be null");
            });
    }
    
    // ========== 数据生成器 ==========
    
    /**
     * 提供非负Long值（0和正数）
     * 这些值都应该作为无效的Telegram频道ID被拒绝
     */
    @Provide
    Arbitrary<Long> nonNegativeChannelIds() {
        return Arbitraries.longs()
            .greaterOrEqual(0L)
            .lessOrEqual(Long.MAX_VALUE);
    }
    
    /**
     * 提供负数Long值
     * 这些值代表有效的Telegram频道ID，应该被接受
     */
    @Provide
    Arbitrary<Long> negativeChannelIds() {
        return Arbitraries.longs()
            .lessOrEqual(-1L)
            .greaterOrEqual(Long.MIN_VALUE);
    }
}
