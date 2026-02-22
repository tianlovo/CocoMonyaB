package org.xlyo.cocomonyab.filter;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FilterChainManager单元测试
 */
class FilterChainManagerTest {
    
    private FilterChainManager filterChainManager;
    
    @BeforeEach
    void setUp() {
        filterChainManager = new FilterChainManager();
    }
    
    @Test
    void testEmptyChainAcceptsMessage() {
        // Given: 没有注册任何过滤器
        TdApi.Message message = createTestMessage();
        
        // When: 执行过滤器链
        boolean result = filterChainManager.executeChain(message);
        
        // Then: 消息应该被接受
        assertTrue(result, "Empty filter chain should accept message");
    }
    
    @Test
    void testFilterRegistration() {
        // Given: 创建一个测试过滤器
        MessageFilter filter = new TestAcceptFilter();
        
        // When: 注册过滤器
        filterChainManager.registerFilter(filter);
        
        // Then: 过滤器应该被注册
        assertEquals(1, filterChainManager.getFilters().size());
        assertEquals(filter, filterChainManager.getFilter("TestAcceptFilter"));
    }
    
    @Test
    void testFilterPriorityOrdering() {
        // Given: 创建多个不同优先级的过滤器
        MessageFilter lowPriority = new TestFilterWithPriority("Low", 10);
        MessageFilter highPriority = new TestFilterWithPriority("High", 100);
        MessageFilter mediumPriority = new TestFilterWithPriority("Medium", 50);
        
        // When: 注册过滤器
        filterChainManager.registerFilter(lowPriority);
        filterChainManager.registerFilter(highPriority);
        filterChainManager.registerFilter(mediumPriority);
        
        // Then: 过滤器应该按优先级排序（高优先级在前）
        var filters = filterChainManager.getFilters();
        assertEquals(3, filters.size());
        assertEquals("High", filters.get(0).getName());
        assertEquals("Medium", filters.get(1).getName());
        assertEquals("Low", filters.get(2).getName());
    }
    
    @Test
    void testAcceptFilter() {
        // Given: 注册一个接受所有消息的过滤器
        filterChainManager.registerFilter(new TestAcceptFilter());
        TdApi.Message message = createTestMessage();
        
        // When: 执行过滤器链
        boolean result = filterChainManager.executeChain(message);
        
        // Then: 消息应该被接受
        assertTrue(result);
    }
    
    @Test
    void testRejectFilter() {
        // Given: 注册一个拒绝所有消息的过滤器
        filterChainManager.registerFilter(new TestRejectFilter());
        TdApi.Message message = createTestMessage();
        
        // When: 执行过滤器链
        boolean result = filterChainManager.executeChain(message);
        
        // Then: 消息应该被拒绝
        assertFalse(result);
    }
    
    @Test
    void testMultipleFiltersStopOnReject() {
        // Given: 注册多个过滤器，第二个会拒绝
        filterChainManager.registerFilter(new TestFilterWithPriority("Accept1", 100));
        filterChainManager.registerFilter(new TestRejectFilter()); // priority 50
        filterChainManager.registerFilter(new TestFilterWithPriority("Accept2", 10));
        
        TdApi.Message message = createTestMessage();
        
        // When: 执行过滤器链
        boolean result = filterChainManager.executeChain(message);
        
        // Then: 消息应该被拒绝（在第二个过滤器处停止）
        assertFalse(result);
    }
    
    @Test
    void testFilterUnregistration() {
        // Given: 注册一个过滤器
        MessageFilter filter = new TestAcceptFilter();
        filterChainManager.registerFilter(filter);
        
        // When: 注销过滤器
        filterChainManager.unregisterFilter("TestAcceptFilter");
        
        // Then: 过滤器应该被移除
        assertEquals(0, filterChainManager.getFilters().size());
        assertNull(filterChainManager.getFilter("TestAcceptFilter"));
    }
    
    @Test
    void testDisableFilter() {
        // Given: 注册一个拒绝过滤器
        TestRejectFilter filter = new TestRejectFilter();
        filterChainManager.registerFilter(filter);
        
        // When: 禁用过滤器
        filterChainManager.disableFilter("TestRejectFilter");
        TdApi.Message message = createTestMessage();
        boolean result = filterChainManager.executeChain(message);
        
        // Then: 消息应该被接受（因为过滤器被禁用）
        assertTrue(result);
    }
    
    @Test
    void testRejectionStats() {
        // Given: 注册一个拒绝过滤器
        filterChainManager.registerFilter(new TestRejectFilter());
        
        // When: 执行多次过滤
        for (int i = 0; i < 5; i++) {
            filterChainManager.executeChain(createTestMessage());
        }
        
        // Then: 应该记录拒绝次数
        var stats = filterChainManager.getRejectionStats();
        assertEquals(5L, stats.get("TestRejectFilter"));
    }
    
    // 辅助方法和测试类
    
    private TdApi.Message createTestMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.content = new TdApi.MessageText(new TdApi.FormattedText("Test", new TdApi.TextEntity[0]), null, null);
        return message;
    }
    
    static class TestAcceptFilter extends AbstractMessageFilter {
        @Override
        public String getName() {
            return "TestAcceptFilter";
        }
        
        @Override
        public int getPriority() {
            return 50;
        }
        
        @Override
        protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
            return FilterResult.ACCEPT;
        }
    }
    
    static class TestRejectFilter extends AbstractMessageFilter {
        @Override
        public String getName() {
            return "TestRejectFilter";
        }
        
        @Override
        public int getPriority() {
            return 50;
        }
        
        @Override
        protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
            context.setRejectReason("Test rejection");
            return FilterResult.REJECT;
        }
    }
    
    static class TestFilterWithPriority extends AbstractMessageFilter {
        private final String name;
        private final int priority;
        
        TestFilterWithPriority(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
            return FilterResult.ACCEPT;
        }
    }
}
