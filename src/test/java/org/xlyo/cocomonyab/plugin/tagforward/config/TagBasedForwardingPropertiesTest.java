package org.xlyo.cocomonyab.plugin.tagforward.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TagBasedForwardingProperties configuration class.
 * 
 * <p>Tests verify:
 * - Default values are correctly applied
 * - Invalid targetChannelId values are rejected
 * - Configuration binding works correctly
 */
@DisplayName("TagBasedForwardingProperties Unit Tests")
class TagBasedForwardingPropertiesTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    @DisplayName("Should apply default values correctly")
    void shouldApplyDefaultValues() {
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        
        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getTagPrefix()).isEqualTo("#");
        assertThat(properties.getRateLimitPerMinute()).isEqualTo(20);
        assertThat(properties.getBatchSize()).isEqualTo(10);
        assertThat(properties.getScheduleIntervalSeconds()).isEqualTo(30);
        assertThat(properties.getMaxRetryCount()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should reject null targetChannelId")
    void shouldRejectNullTargetChannelId() {
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(null);
        
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("targetChannelId") &&
            v.getMessage().contains("must be configured")
        );
    }
    
    @Test
    @DisplayName("Should reject positive targetChannelId")
    void shouldRejectPositiveTargetChannelId() {
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(12345L);
        
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("targetChannelId") &&
            v.getMessage().contains("must be negative")
        );
    }
    
    @Test
    @DisplayName("Should reject zero targetChannelId")
    void shouldRejectZeroTargetChannelId() {
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(0L);
        
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> 
            v.getPropertyPath().toString().equals("targetChannelId") &&
            v.getMessage().contains("must be negative")
        );
    }
    
    @Test
    @DisplayName("Should accept valid negative targetChannelId")
    void shouldAcceptValidNegativeTargetChannelId() {
        TagBasedForwardingProperties properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(-1001234567890L);
        
        Set<ConstraintViolation<TagBasedForwardingProperties>> violations = validator.validate(properties);
        
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("Should bind configuration from property source")
    void shouldBindConfigurationFromPropertySource() {
        Map<String, Object> source = new HashMap<>();
        source.put("plugin.tag-based-forwarding.enabled", "false");
        source.put("plugin.tag-based-forwarding.target-channel-id", "-1001234567890");
        source.put("plugin.tag-based-forwarding.tag-prefix", "##");
        source.put("plugin.tag-based-forwarding.rate-limit-per-minute", "30");
        source.put("plugin.tag-based-forwarding.batch-size", "20");
        source.put("plugin.tag-based-forwarding.schedule-interval-seconds", "60");
        source.put("plugin.tag-based-forwarding.max-retry-count", "5");
        
        ConfigurationPropertySource propertySource = new MapConfigurationPropertySource(source);
        Binder binder = new Binder(propertySource);
        
        BindResult<TagBasedForwardingProperties> result = binder.bind(
            "plugin.tag-based-forwarding",
            TagBasedForwardingProperties.class
        );
        
        assertThat(result.isBound()).isTrue();
        TagBasedForwardingProperties properties = result.get();
        
        assertThat(properties.getEnabled()).isFalse();
        assertThat(properties.getTargetChannelId()).isEqualTo(-1001234567890L);
        assertThat(properties.getTagPrefix()).isEqualTo("##");
        assertThat(properties.getRateLimitPerMinute()).isEqualTo(30);
        assertThat(properties.getBatchSize()).isEqualTo(20);
        assertThat(properties.getScheduleIntervalSeconds()).isEqualTo(60);
        assertThat(properties.getMaxRetryCount()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("Should use default values when properties are not configured")
    void shouldUseDefaultValuesWhenNotConfigured() {
        Map<String, Object> source = new HashMap<>();
        source.put("plugin.tag-based-forwarding.target-channel-id", "-1001234567890");
        
        ConfigurationPropertySource propertySource = new MapConfigurationPropertySource(source);
        Binder binder = new Binder(propertySource);
        
        BindResult<TagBasedForwardingProperties> result = binder.bind(
            "plugin.tag-based-forwarding",
            TagBasedForwardingProperties.class
        );
        
        assertThat(result.isBound()).isTrue();
        TagBasedForwardingProperties properties = result.get();
        
        // Should use default values for non-configured properties
        assertThat(properties.getEnabled()).isTrue();
        assertThat(properties.getTagPrefix()).isEqualTo("#");
        assertThat(properties.getRateLimitPerMinute()).isEqualTo(20);
        assertThat(properties.getBatchSize()).isEqualTo(10);
        assertThat(properties.getScheduleIntervalSeconds()).isEqualTo(30);
        assertThat(properties.getMaxRetryCount()).isEqualTo(3);
    }
}
