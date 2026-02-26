package org.xlyo.cocomonyab.plugin.tagforward;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.tagforward.component.ForwardScheduler;
import org.xlyo.cocomonyab.plugin.tagforward.component.QueueManager;
import org.xlyo.cocomonyab.plugin.tagforward.component.TagMatcher;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 单元测试：TagBasedMessageForwardingPlugin
 * 
 * <p>测试主插件类的核心功能，包括初始化、消息处理、异常处理和销毁
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagBasedMessageForwardingPlugin Unit Tests")
class TagBasedMessageForwardingPluginTest {
    
    @Mock
    private TagMatcher tagMatcher;
    
    @Mock
    private QueueManager queueManager;
    
    @Mock
    private ForwardScheduler forwardScheduler;
    
    @Mock
    private TelegramClientManager clientManager;
    
    @Mock
    private PluginContext pluginContext;
    
    private TagBasedForwardingProperties properties;
    private TagBasedMessageForwardingPlugin plugin;
    
    @BeforeEach
    void setUp() {
        properties = new TagBasedForwardingProperties();
        properties.setEnabled(true);
        properties.setTargetChannelId(-1001234567890L);
        properties.setTagPrefix("#");
        properties.setRateLimitPerMinute(20);
        properties.setBatchSize(10);
        properties.setScheduleIntervalSeconds(30);
        properties.setMaxRetryCount(3);
        
        plugin = new TagBasedMessageForwardingPlugin(
                tagMatcher,
                queueManager,
                forwardScheduler,
                properties,
                clientManager
        );
    }
    
    @Test
    @DisplayName("应该返回正确的插件名称")
    void shouldReturnCorrectPluginName() {
        assertEquals("TagBasedMessageForwardingPlugin", plugin.getName());
    }
    
    @Test
    @DisplayName("应该返回正确的优先级")
    void shouldReturnCorrectPriority() {
        assertEquals(100, plugin.getPriority());
    }
    
    @Test
    @DisplayName("当配置启用时应该返回true")
    void shouldReturnTrueWhenEnabled() {
        properties.setEnabled(true);
        assertTrue(plugin.isEnabled());
    }
    
    @Test
    @DisplayName("当配置禁用时应该返回false")
    void shouldReturnFalseWhenDisabled() {
        properties.setEnabled(false);
        assertFalse(plugin.isEnabled());
    }
    
    @Test
    @DisplayName("初始化应该加载标签配置、验证目标频道并启动调度器")
    void shouldInitializeSuccessfully() {
        // 执行初始化
        plugin.initialize();
        
        // 验证调用顺序
        verify(tagMatcher).loadTagConfiguration();
        verify(forwardScheduler).start();
    }
    
    @Test
    @DisplayName("当插件禁用时初始化应该跳过")
    void shouldSkipInitializationWhenDisabled() {
        properties.setEnabled(false);
        
        plugin.initialize();
        
        // 验证没有调用任何组件
        verify(tagMatcher, never()).loadTagConfiguration();
        verify(forwardScheduler, never()).start();
    }
    
    @Test
    @DisplayName("当目标频道ID为null时初始化应该失败")
    void shouldFailInitializationWhenTargetChannelIdIsNull() {
        properties.setTargetChannelId(null);
        
        plugin.initialize();
        
        // 验证插件被禁用
        assertFalse(plugin.isEnabled());
        verify(forwardScheduler, never()).start();
    }
    
    @Test
    @DisplayName("当目标频道ID为正数时初始化应该失败")
    void shouldFailInitializationWhenTargetChannelIdIsPositive() {
        properties.setTargetChannelId(1234567890L);
        
        plugin.initialize();
        
        // 验证插件被禁用
        assertFalse(plugin.isEnabled());
        verify(forwardScheduler, never()).start();
    }
    
    @Test
    @DisplayName("当目标频道ID为零时初始化应该失败")
    void shouldFailInitializationWhenTargetChannelIdIsZero() {
        properties.setTargetChannelId(0L);
        
        plugin.initialize();
        
        // 验证插件被禁用
        assertFalse(plugin.isEnabled());
        verify(forwardScheduler, never()).start();
    }
    
    @Test
    @DisplayName("应该处理有匹配标签的消息")
    void shouldHandleMessageWithMatchedTags() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, "#test message");
        List<String> matchedTags = Arrays.asList("#test");
        
        when(tagMatcher.matchTags(anyString())).thenReturn(matchedTags);
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(tagMatcher).matchTags("#test message");
        verify(queueManager).enqueue(123L, 456L, matchedTags);
    }
    
    @Test
    @DisplayName("应该处理无匹配标签的消息")
    void shouldHandleMessageWithoutMatchedTags() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, "no tags here");
        
        when(tagMatcher.matchTags(anyString())).thenReturn(Collections.emptyList());
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(tagMatcher).matchTags("no tags here");
        verify(queueManager, never()).enqueue(anyLong(), anyLong(), anyList());
    }
    
    @Test
    @DisplayName("应该处理空文本内容的消息")
    void shouldHandleMessageWithEmptyTextContent() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, "");
        
        when(tagMatcher.matchTags(anyString())).thenReturn(Collections.emptyList());
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(tagMatcher).matchTags("");
        verify(queueManager, never()).enqueue(anyLong(), anyLong(), anyList());
    }
    
    @Test
    @DisplayName("应该处理null文本内容的消息")
    void shouldHandleMessageWithNullTextContent() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, null);
        
        when(tagMatcher.matchTags(null)).thenReturn(Collections.emptyList());
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(tagMatcher).matchTags(null);
        verify(queueManager, never()).enqueue(anyLong(), anyLong(), anyList());
    }
    
    @Test
    @DisplayName("当TagMatcher抛出异常时应该继续处理")
    void shouldContinueWhenTagMatcherThrowsException() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, "#test");
        
        when(tagMatcher.matchTags(anyString())).thenThrow(new RuntimeException("Tag matcher error"));
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果 - 应该返回CONTINUE而不是抛出异常
        assertEquals(PluginResult.CONTINUE, result);
        verify(queueManager, never()).enqueue(anyLong(), anyLong(), anyList());
    }
    
    @Test
    @DisplayName("当QueueManager抛出异常时应该继续处理")
    void shouldContinueWhenQueueManagerThrowsException() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, "#test");
        List<String> matchedTags = Arrays.asList("#test");
        
        when(tagMatcher.matchTags(anyString())).thenReturn(matchedTags);
        doThrow(new RuntimeException("Queue manager error"))
                .when(queueManager).enqueue(anyLong(), anyLong(), anyList());
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果 - 应该返回CONTINUE而不是抛出异常
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    @Test
    @DisplayName("销毁应该停止转发调度器")
    void shouldStopSchedulerOnDestroy() {
        plugin.destroy();
        
        verify(forwardScheduler).stop();
    }
    
    @Test
    @DisplayName("当ForwardScheduler抛出异常时销毁应该处理异常")
    void shouldHandleExceptionWhenStoppingScheduler() {
        doThrow(new RuntimeException("Scheduler stop error"))
                .when(forwardScheduler).stop();
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> plugin.destroy());
    }
    
    @Test
    @DisplayName("应该处理多个匹配标签的消息")
    void shouldHandleMessageWithMultipleMatchedTags() {
        // 准备测试数据
        TextMessageEntity message = createTextMessage(123L, 456L, "#tag1 #tag2 #tag3");
        List<String> matchedTags = Arrays.asList("#tag1", "#tag2", "#tag3");
        
        when(tagMatcher.matchTags(anyString())).thenReturn(matchedTags);
        
        // 执行处理
        PluginResult result = plugin.handle(message, pluginContext);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(queueManager).enqueue(123L, 456L, matchedTags);
    }
    
    /**
     * 创建测试用的文本消息实体
     */
    private TextMessageEntity createTextMessage(Long chatId, Long messageId, String textContent) {
        TextMessageEntity message = new TextMessageEntity();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setTextContent(textContent);
        return message;
    }
}
