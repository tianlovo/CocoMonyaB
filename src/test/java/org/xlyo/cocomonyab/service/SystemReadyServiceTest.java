package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.xlyo.cocomonyab.event.SystemReadyEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SystemReadyService单元测试
 */
@ExtendWith(MockitoExtension.class)
class SystemReadyServiceTest {
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    private SystemReadyService systemReadyService;
    
    @BeforeEach
    void setUp() {
        systemReadyService = new SystemReadyService(eventPublisher);
    }
    
    @Test
    void testInitialState() {
        // 初始状态应该是未就绪
        assertFalse(systemReadyService.getSystemReady().get());
        assertEquals("系统正在启动中...", systemReadyService.getNotReadyReason());
    }
    
    @Test
    void testMarkAsReady() {
        // 手动标记为就绪
        systemReadyService.markAsReady();
        
        // 验证状态
        assertTrue(systemReadyService.getSystemReady().get());
        assertNull(systemReadyService.getNotReadyReason());
        
        // 验证事件发布
        verify(eventPublisher, times(1)).publishEvent(any(SystemReadyEvent.class));
    }
    
    @Test
    void testMarkAsNotReady() {
        // 先标记为就绪
        systemReadyService.markAsReady();
        assertTrue(systemReadyService.getSystemReady().get());
        
        // 再标记为未就绪
        String reason = "系统维护中";
        systemReadyService.markAsNotReady(reason);
        
        // 验证状态
        assertFalse(systemReadyService.getSystemReady().get());
        assertEquals(reason, systemReadyService.getNotReadyReason());
    }
    
    @Test
    void testMarkAsReadyTwice() {
        // 第一次标记
        systemReadyService.markAsReady();
        
        // 第二次标记（应该不会重复发布事件）
        systemReadyService.markAsReady();
        
        // 验证只发布了一次事件
        verify(eventPublisher, times(1)).publishEvent(any(SystemReadyEvent.class));
    }
    
    @Test
    void testOnApplicationReady() throws InterruptedException {
        // 创建模拟的ApplicationReadyEvent
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        when(event.getTimestamp()).thenReturn(System.currentTimeMillis());
        
        // 触发事件
        systemReadyService.onApplicationReady(event);
        
        // 等待异步检查完成（最多3秒）
        int maxWait = 30; // 3秒
        while (!systemReadyService.getSystemReady().get() && maxWait > 0) {
            Thread.sleep(100);
            maxWait--;
        }
        
        // 验证系统已就绪
        assertTrue(systemReadyService.getSystemReady().get());
        assertNull(systemReadyService.getNotReadyReason());
        
        // 验证事件发布
        ArgumentCaptor<SystemReadyEvent> eventCaptor = ArgumentCaptor.forClass(SystemReadyEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
        
        SystemReadyEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertTrue(publishedEvent.getStartupTimeMs() >= 0);
    }
}
