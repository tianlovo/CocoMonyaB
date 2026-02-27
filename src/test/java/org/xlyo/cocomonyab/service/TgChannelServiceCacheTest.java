package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.xlyo.cocomonyab.config.CacheConfig;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TG频道服务缓存功能测试
 */
@SpringBootTest
class TgChannelServiceCacheTest {

    @Autowired
    private TgChannelService tgChannelService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private TelegramClientManager telegramClientManager;

    @Test
    void testCacheManagerExists() {
        assertNotNull(cacheManager, "CacheManager应该被正确注入");
    }

    @Test
    void testCacheNamesConfigured() {
        var cache1 = cacheManager.getCache(CacheConfig.TG_CHANNELS_CACHE);
        var cache2 = cacheManager.getCache(CacheConfig.TG_CHANNELS_COUNT_CACHE);
        
        assertNotNull(cache1, "tgChannels缓存应该存在");
        assertNotNull(cache2, "tgChannelsCount缓存应该存在");
    }

    @Test
    void testEvictChannelsCache() {
        // 清除缓存不应该抛出异常
        assertDoesNotThrow(() -> tgChannelService.evictChannelsCache());
    }
}
