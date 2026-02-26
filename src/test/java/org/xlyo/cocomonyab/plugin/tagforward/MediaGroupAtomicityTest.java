package org.xlyo.cocomonyab.plugin.tagforward;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaGroupMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.tagforward.component.ForwardScheduler;
import org.xlyo.cocomonyab.plugin.tagforward.component.QueueManager;
import org.xlyo.cocomonyab.plugin.tagforward.component.TagMatcher;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 媒体组原子性测试
 * 
 * <p>验证媒体组消息在处理过程中的原子性保证：
 * <ul>
 *   <li>媒体组作为一个整体被处理</li>
 *   <li>如果处理失败，整个媒体组都不会被加入队列</li>
 *   <li>不会出现媒体组被分开处理的情况</li>
 * </ul>
 */
@DisplayName("媒体组原子性测试")
class MediaGroupAtomicityTest {
    
    @Mock
    private TagMatcher tagMatcher;
    
    @Mock
    private QueueManager queueManager;
    
    @Mock
    private ForwardScheduler forwardScheduler;
    
    @Mock
    private TagBasedForwardingProperties properties;
    
    @Mock
    private TelegramClientManager clientManager;
    
    private TagBasedMessageForwardingPlugin plugin;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置默认行为
        when(properties.getEnabled()).thenReturn(true);
        when(properties.getTargetChannelId()).thenReturn(-1001234567890L);
        when(tagMatcher.isConfigurationLoaded()).thenReturn(true);
        
        plugin = new TagBasedMessageForwardingPlugin(
                tagMatcher,
                queueManager,
                forwardScheduler,
                properties,
                clientManager
        );
    }
    
    @Test
    @DisplayName("媒体组匹配标签时，整个媒体组作为一个单元入队")
    void testMediaGroupEnqueuedAsUnit() {
        // 准备测试数据：包含3条消息的媒体组
        MediaGroupMessageEntity mediaGroup = createMediaGroup(
                12345L,  // chatId
                100L,    // 第一条消息ID
                List.of(100L, 101L, 102L),  // 所有消息ID
                List.of("Caption 1 #test", "Caption 2", "Caption 3")
        );
        
        // 配置标签匹配器返回匹配结果
        when(tagMatcher.matchTags(anyString())).thenReturn(List.of("#test"));
        
        // 执行处理
        PluginResult result = plugin.doHandle(mediaGroup, new PluginContext(null));
        
        // 验证结果
        assertThat(result).isEqualTo(PluginResult.CONTINUE);
        
        // 验证入队调用
        ArgumentCaptor<Long> chatIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> messageIdCaptor = ArgumentCaptor.forClass(Long.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> mediaGroupIdsCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        
        verify(queueManager, times(1)).enqueue(
                chatIdCaptor.capture(),
                messageIdCaptor.capture(),
                mediaGroupIdsCaptor.capture(),
                tagsCaptor.capture()
        );
        
        // 验证入队参数
        assertThat(chatIdCaptor.getValue()).isEqualTo(12345L);
        assertThat(messageIdCaptor.getValue()).isEqualTo(100L);  // 第一条消息ID
        assertThat(mediaGroupIdsCaptor.getValue())
                .containsExactly(100L, 101L, 102L)  // 所有消息ID，按递增顺序
                .as("媒体组的所有消息ID应该一起入队");
        assertThat(tagsCaptor.getValue()).containsExactly("#test");
    }
    
    @Test
    @DisplayName("媒体组处理失败时，整个媒体组都不会入队")
    void testMediaGroupNotEnqueuedOnFailure() {
        // 准备测试数据
        MediaGroupMessageEntity mediaGroup = createMediaGroup(
                12345L,
                100L,
                List.of(100L, 101L, 102L),
                List.of("Caption 1 #test", "Caption 2", "Caption 3")
        );
        
        // 配置标签匹配器抛出异常
        when(tagMatcher.matchTags(anyString())).thenThrow(new RuntimeException("标签匹配失败"));
        
        // 执行处理
        PluginResult result = plugin.doHandle(mediaGroup, new PluginContext(null));
        
        // 验证结果
        assertThat(result).isEqualTo(PluginResult.CONTINUE);
        
        // 验证没有入队调用（保证原子性）
        verify(queueManager, never()).enqueue(anyLong(), anyLong(), any(), anyList());
    }
    
    @Test
    @DisplayName("媒体组消息ID按递增顺序排序")
    void testMediaGroupMessageIdsAreSorted() {
        // 准备测试数据：消息ID乱序
        MediaGroupMessageEntity mediaGroup = createMediaGroup(
                12345L,
                102L,  // 第一条消息ID（不是最小的）
                List.of(102L, 100L, 101L),  // 乱序的消息ID
                List.of("Caption 1 #test", "Caption 2", "Caption 3")
        );
        
        // 配置标签匹配器返回匹配结果
        when(tagMatcher.matchTags(anyString())).thenReturn(List.of("#test"));
        
        // 执行处理
        plugin.doHandle(mediaGroup, new PluginContext(null));
        
        // 验证入队调用
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> mediaGroupIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(queueManager).enqueue(anyLong(), anyLong(), mediaGroupIdsCaptor.capture(), anyList());
        
        // 验证消息ID已排序
        assertThat(mediaGroupIdsCaptor.getValue())
                .containsExactly(100L, 101L, 102L)
                .as("媒体组消息ID应该按递增顺序排序（TDLib要求）");
    }
    
    @Test
    @DisplayName("媒体组合并所有子消息的文本内容进行标签匹配")
    void testMediaGroupCombinesAllCaptions() {
        // 准备测试数据：标签分散在不同的caption中
        MediaGroupMessageEntity mediaGroup = createMediaGroup(
                12345L,
                100L,
                List.of(100L, 101L, 102L),
                List.of("Caption 1 #tag1", "Caption 2 #tag2", "Caption 3")
        );
        
        // 配置标签匹配器返回匹配结果
        when(tagMatcher.matchTags(anyString())).thenReturn(List.of("#tag1", "#tag2"));
        
        // 执行处理
        plugin.doHandle(mediaGroup, new PluginContext(null));
        
        // 验证标签匹配器被调用，且传入的是合并后的文本
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(tagMatcher).matchTags(textCaptor.capture());
        
        String combinedText = textCaptor.getValue();
        assertThat(combinedText)
                .contains("Caption 1 #tag1")
                .contains("Caption 2 #tag2")
                .contains("Caption 3")
                .as("应该合并所有子消息的文本内容");
    }
    
    @Test
    @DisplayName("空媒体组不会入队")
    void testEmptyMediaGroupNotEnqueued() {
        // 准备测试数据：空媒体组
        MediaGroupMessageEntity mediaGroup = new MediaGroupMessageEntity();
        mediaGroup.setMessageId(100L);
        mediaGroup.setChatId(12345L);
        mediaGroup.setItems(new ArrayList<>());
        
        // 执行处理
        plugin.doHandle(mediaGroup, new PluginContext(null));
        
        // 验证没有入队调用
        verify(queueManager, never()).enqueue(anyLong(), anyLong(), any(), anyList());
    }
    
    /**
     * 创建测试用的媒体组消息
     */
    private MediaGroupMessageEntity createMediaGroup(
            Long chatId,
            Long firstMessageId,
            List<Long> messageIds,
            List<String> captions) {
        
        MediaGroupMessageEntity mediaGroup = new MediaGroupMessageEntity();
        mediaGroup.setMessageId(firstMessageId);
        mediaGroup.setChatId(chatId);
        mediaGroup.setMediaAlbumId(999L);
        
        List<BaseMessageEntity> items = new ArrayList<>();
        for (int i = 0; i < messageIds.size(); i++) {
            PhotoMessageEntity photo = new PhotoMessageEntity();
            photo.setMessageId(messageIds.get(i));
            photo.setChatId(chatId);
            photo.setCaption(i < captions.size() ? captions.get(i) : "");
            items.add(photo);
        }
        
        mediaGroup.setItems(items);
        return mediaGroup;
    }
}
