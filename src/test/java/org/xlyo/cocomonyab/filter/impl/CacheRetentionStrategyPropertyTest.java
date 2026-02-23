package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;

import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property 10: Cache Retention Strategy
 * 
 * For any message that already exists in the database, 
 * the system should retain its key in the cache rather than immediately removing it.
 * 
 * **Validates: Requirements 5.1**
 * 
 * Feature: concurrent-safety-optimization, Property 10: Cache Retention Strategy
 */
class CacheRetentionStrategyPropertyTest {
    
    @Property(tries = 100)
    @Label("Property 10: Cache Retention Strategy - Database duplicates retained in cache")
    void databaseDuplicatesAreRetainedInCache(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository, properties);
        
        // Message exists in database
        when(repository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(true);
        
        TdApi.Message message = createSingleMessage(messageId, chatId);
        FilterContext context = new FilterContext();
        
        // When: Filter the message (first time)
        FilterResult result = filter.filter(message, context);
        
        // Then: Message should be rejected (exists in DB)
        assertThat(result).isEqualTo(FilterResult.REJECT);
        assertThat(context.getRejectReason()).contains("数据库中存在重复消息");
        
        // And: Cache should contain the message key
        long cacheSize = filter.getProcessingCount();
        assertThat(cacheSize).isGreaterThan(0);
        
        // When: Same message arrives again immediately
        FilterContext context2 = new FilterContext();
        FilterResult result2 = filter.filter(message, context2);
        
        // Then: Should be rejected by cache (not hitting database again)
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("正在处理中或已处理");
        
        // Verify database was only queried once (cache prevented second query)
        verify(repository, times(1)).existsByChatIdAndMessageId(chatId, messageId);
    }
    
    @Property(tries = 100)
    @Label("Property 10: Cache Retention Strategy - Media group duplicates retained in cache")
    void mediaGroupDuplicatesAreRetainedInCache(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId,
        @ForAll @LongRange(min = 1000L, max = 999999L) long mediaAlbumId
    ) {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository, properties);
        
        // Media group exists in database
        when(repository.existsByChatIdAndMediaAlbumId(chatId, mediaAlbumId)).thenReturn(true);
        
        TdApi.Message message = createMediaGroupMessage(messageId, chatId, mediaAlbumId);
        FilterContext context = new FilterContext();
        
        // When: Filter the message (first time)
        FilterResult result = filter.filter(message, context);
        
        // Then: Message should be rejected (exists in DB)
        assertThat(result).isEqualTo(FilterResult.REJECT);
        assertThat(context.getRejectReason()).contains("数据库中存在重复媒体组");
        
        // And: Cache should contain the media group key
        long cacheSize = filter.getProcessingCount();
        assertThat(cacheSize).isGreaterThan(0);
        
        // When: Same media group message arrives again immediately
        FilterContext context2 = new FilterContext();
        FilterResult result2 = filter.filter(message, context2);
        
        // Then: Should be rejected by cache (not hitting database again)
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("媒体组正在处理中或已处理");
        
        // Verify database was only queried once (cache prevented second query)
        verify(repository, times(1)).existsByChatIdAndMediaAlbumId(chatId, mediaAlbumId);
    }
    
    // Helper methods
    
    private TdApi.Message createSingleMessage(long messageId, long chatId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        return message;
    }
    
    private TdApi.Message createMediaGroupMessage(long messageId, long chatId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.mediaAlbumId = mediaAlbumId;
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
            new TdApi.FormattedText("Caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }

    private ConcurrentSafetyProperties createDefaultProperties() {
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        properties.getMediaGroup().setTimeout(2000);
        properties.getMediaGroup().setMaxBufferSize(1000);
        properties.getLock().setStripes(128);
        properties.getLock().setTimeout(5000);
        properties.getCache().setTtl(10);
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(5);
        return properties;
    }

}
