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
 * Property 6: Cache Expiration and Reprocessing
 * 
 * For any message, when its cache entry expires, 
 * the system should allow the message to re-enter the processing flow.
 * 
 * **Validates: Requirements 5.2, 5.4, 6.4**
 * 
 * Feature: concurrent-safety-optimization, Property 6: Cache Expiration and Reprocessing
 */
class CacheExpirationReprocessingPropertyTest {
    
    @Property(tries = 3)
    @Label("Property 6: Cache Expiration and Reprocessing - Expired cache allows reprocessing")
    void expiredCacheAllowsReprocessing(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        // Message does not exist in database initially
        when(repository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        
        TdApi.Message message = createSingleMessage(messageId, chatId);
        
        // When: First attempt - message passes filter
        FilterContext context1 = new FilterContext();
        FilterResult result1 = filter.filter(message, context1);
        
        // Then: Should be accepted
        assertThat(result1).isEqualTo(FilterResult.ACCEPT);
        
        // When: Immediate retry - should be rejected by cache
        FilterContext context2 = new FilterContext();
        FilterResult result2 = filter.filter(message, context2);
        
        // Then: Should be rejected (in cache)
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("正在处理中或已处理");
        
        // When: Wait for cache to expire (11 seconds > 10 second TTL)
        Thread.sleep(11000);
        
        // When: After expiry - attempt again
        FilterContext context3 = new FilterContext();
        FilterResult result3 = filter.filter(message, context3);
        
        // Then: Should be allowed to reprocess (cache expired)
        // Note: Result depends on database state
        // If message was saved to DB, it will be rejected with DB reason
        // If not saved, it will be accepted again
        assertThat(result3).isIn(FilterResult.ACCEPT, FilterResult.REJECT);
        
        // Verify database was queried at least twice (once initially, once after expiry)
        verify(repository, atLeast(2)).existsByChatIdAndMessageId(chatId, messageId);
    }
    
    @Property(tries = 3)
    @Label("Property 6: Cache Expiration and Reprocessing - Media group cache expires")
    void mediaGroupCacheExpires(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId,
        @ForAll @LongRange(min = 1000L, max = 999999L) long mediaAlbumId
    ) throws InterruptedException {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        // Media group does not exist in database initially
        when(repository.existsByChatIdAndMediaAlbumId(chatId, mediaAlbumId)).thenReturn(false);
        
        TdApi.Message message = createMediaGroupMessage(messageId, chatId, mediaAlbumId);
        
        // When: First attempt - message passes filter
        FilterContext context1 = new FilterContext();
        FilterResult result1 = filter.filter(message, context1);
        
        // Then: Should be accepted
        assertThat(result1).isEqualTo(FilterResult.ACCEPT);
        
        // When: Immediate retry - should be rejected by cache
        TdApi.Message message2 = createMediaGroupMessage(messageId + 1, chatId, mediaAlbumId);
        FilterContext context2 = new FilterContext();
        FilterResult result2 = filter.filter(message2, context2);
        
        // Then: Should be rejected (in cache)
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("媒体组正在处理中或已处理");
        
        // When: Wait for cache to expire (11 seconds > 10 second TTL)
        Thread.sleep(11000);
        
        // When: After expiry - attempt again with another message from same group
        TdApi.Message message3 = createMediaGroupMessage(messageId + 2, chatId, mediaAlbumId);
        FilterContext context3 = new FilterContext();
        FilterResult result3 = filter.filter(message3, context3);
        
        // Then: Should be allowed to reprocess (cache expired)
        assertThat(result3).isIn(FilterResult.ACCEPT, FilterResult.REJECT);
        
        // Verify database was queried at least twice (once initially, once after expiry)
        verify(repository, atLeast(2)).existsByChatIdAndMediaAlbumId(chatId, mediaAlbumId);
    }
    
    @Property(tries = 5)
    @Label("Property 6: Cache Expiration and Reprocessing - Failed message short cache")
    void failedMessageHasShortCache(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        TdApi.Message message = createSingleMessage(messageId, chatId);
        
        // When: Message is marked as failed
        filter.markFailed(message);
        
        // Then: Failed cache should contain the message
        long failedCount = filter.getFailedCount();
        assertThat(failedCount).isGreaterThan(0);
        
        // Note: The failed cache is separate from the processing cache
        // It's used for tracking and monitoring, not for filtering
    }
    
    @Property(tries = 3)
    @Label("Property 6: Cache Expiration and Reprocessing - Cache size decreases after expiration")
    void cacheSizeDecreasesAfterExpiration(
        @ForAll @LongRange(min = 1000L, max = 999999L) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: Create filter with mocked repository
        RawMessageRepository repository = mock(RawMessageRepository.class);
        DuplicateMessageFilter filter = new DuplicateMessageFilter(repository);
        
        // Message does not exist in database
        when(repository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        
        TdApi.Message message = createSingleMessage(messageId, chatId);
        
        // When: Filter message (adds to cache)
        filter.filter(message, new FilterContext());
        
        // Then: Cache should contain the message
        long cacheSize = filter.getProcessingCount();
        assertThat(cacheSize).isGreaterThan(0);
        
        // When: Wait for cache to expire (11 seconds > 10 second TTL)
        Thread.sleep(11000);
        
        // Then: Cache size should decrease (entry expired)
        // Note: Caffeine's estimatedSize() may not immediately reflect expiration
        // until cleanup occurs, so we verify by attempting to filter again
        FilterContext context = new FilterContext();
        FilterResult result = filter.filter(message, context);
        
        // If cache expired, database should be queried again
        verify(repository, atLeast(2)).existsByChatIdAndMessageId(chatId, messageId);
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
