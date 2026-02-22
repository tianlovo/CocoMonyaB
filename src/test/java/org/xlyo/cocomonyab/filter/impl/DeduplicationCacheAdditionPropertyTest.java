package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property 11: Deduplication Check Cache Addition
 * 
 * For any message that passes the deduplication check, 
 * its key should be added to the cache.
 * 
 * **Validates: Requirements 5.5**
 * 
 * Feature: concurrent-safety-optimization, Property 11: Deduplication Check Cache Addition
 */
class DeduplicationCacheAdditionPropertyTest {
    
    @Property(tries = 100)
    @Label("Property 11: Deduplication Check Cache Addition - New messages added to cache")
    void newMessagesAreAddedToCache(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        // Message does not exist in database
        when(repository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        
        TdApi.Message message = createSingleMessage(messageId, chatId);
        FilterContext context = new FilterContext();
        
        // Cache should be empty initially
        long initialCacheSize = filter.getProcessingCount();
        
        // When: Filter the message (passes deduplication check)
        FilterResult result = filter.filter(message, context);
        
        // Then: Message should be accepted
        assertThat(result).isEqualTo(FilterResult.ACCEPT);
        
        // And: Message key should be added to cache
        long finalCacheSize = filter.getProcessingCount();
        assertThat(finalCacheSize).isGreaterThan(initialCacheSize);
        
        // And: Subsequent identical message should be rejected by cache
        FilterContext context2 = new FilterContext();
        FilterResult result2 = filter.filter(message, context2);
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("正在处理中或已处理");
        
        // Verify database was only queried once (cache prevented second query)
        verify(repository, times(1)).existsByChatIdAndMessageId(chatId, messageId);
    }
    
    @Property(tries = 100)
    @Label("Property 11: Deduplication Check Cache Addition - Media groups added to cache")
    void mediaGroupsAreAddedToCache(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId,
        @ForAll @LongRange(min = 1000L, max = 999999L) long mediaAlbumId
    ) {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        // Media group does not exist in database
        when(repository.existsByChatIdAndMediaAlbumId(chatId, mediaAlbumId)).thenReturn(false);
        
        TdApi.Message message = createMediaGroupMessage(messageId, chatId, mediaAlbumId);
        FilterContext context = new FilterContext();
        
        // Cache should be empty initially
        long initialCacheSize = filter.getProcessingCount();
        
        // When: Filter the message (passes deduplication check)
        FilterResult result = filter.filter(message, context);
        
        // Then: Message should be accepted
        assertThat(result).isEqualTo(FilterResult.ACCEPT);
        
        // And: Media group key should be added to cache
        long finalCacheSize = filter.getProcessingCount();
        assertThat(finalCacheSize).isGreaterThan(initialCacheSize);
        
        // And: Subsequent message from same media group should be rejected by cache
        TdApi.Message message2 = createMediaGroupMessage(messageId + 1, chatId, mediaAlbumId);
        FilterContext context2 = new FilterContext();
        FilterResult result2 = filter.filter(message2, context2);
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("媒体组正在处理中或已处理");
        
        // Verify database was only queried once (cache prevented second query)
        verify(repository, times(1)).existsByChatIdAndMediaAlbumId(chatId, mediaAlbumId);
    }
    
    @Property(tries = 100)
    @Label("Property 11: Deduplication Check Cache Addition - Cache prevents redundant DB queries")
    void cachePreventsDatabaseQueries(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        // Message does not exist in database
        when(repository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        
        TdApi.Message message = createSingleMessage(messageId, chatId);
        
        // When: Filter the same message multiple times
        filter.filter(message, new FilterContext());
        filter.filter(message, new FilterContext());
        filter.filter(message, new FilterContext());
        
        // Then: Database should only be queried once
        // The cache prevents subsequent database queries
        verify(repository, times(1)).existsByChatIdAndMessageId(chatId, messageId);
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
}
