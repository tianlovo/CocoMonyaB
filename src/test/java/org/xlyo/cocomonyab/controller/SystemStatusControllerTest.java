package org.xlyo.cocomonyab.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.xlyo.cocomonyab.domain.vo.SystemStatusVO;
import org.xlyo.cocomonyab.event.startup.StartupStatus;
import org.xlyo.cocomonyab.service.SystemReadyService;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SystemStatusController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemStatusController Tests")
class SystemStatusControllerTest {
    
    @Mock
    private SystemReadyService systemReadyService;
    
    @InjectMocks
    private SystemStatusController controller;
    
    private AtomicBoolean systemReady;
    
    @BeforeEach
    void setUp() {
        systemReady = new AtomicBoolean(false);
    }
    
    @Test
    @DisplayName("Should return HTTP 503 and OUT_OF_SERVICE status when system is not ready")
    void testGetSystemStatus_NotReady() {
        // Given: System is not ready
        systemReady.set(false);
        when(systemReadyService.getSystemReady()).thenReturn(systemReady);
        when(systemReadyService.getCurrentStatus()).thenReturn(StartupStatus.DATABASE_INIT);
        when(systemReadyService.getNotReadyReason()).thenReturn("正在初始化数据库...");
        when(systemReadyService.getProgress()).thenReturn(25);
        when(systemReadyService.getCurrentPhase()).thenReturn("数据库初始化");
        
        // When: Get system status
        ResponseEntity<SystemStatusVO> response = controller.getSystemStatus();
        
        // Then: Should return HTTP 503
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // And: Response body should indicate not ready
        SystemStatusVO vo = response.getBody();
        assertThat(vo).isNotNull();
        assertThat(vo.getReady()).isFalse();
        assertThat(vo.getStatus()).isEqualTo(StartupStatus.DATABASE_INIT);
        assertThat(vo.getReason()).isEqualTo("正在初始化数据库...");
        assertThat(vo.getProgress()).isEqualTo(25);
        assertThat(vo.getCurrentPhase()).isEqualTo("数据库初始化");
        assertThat(vo.getTimestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("Should return HTTP 200 and UP status when system is ready")
    void testGetSystemStatus_Ready() {
        // Given: System is ready
        systemReady.set(true);
        when(systemReadyService.getSystemReady()).thenReturn(systemReady);
        when(systemReadyService.getCurrentStatus()).thenReturn(StartupStatus.READY);
        when(systemReadyService.getNotReadyReason()).thenReturn(null);
        when(systemReadyService.getProgress()).thenReturn(100);
        when(systemReadyService.getCurrentPhase()).thenReturn("应用就绪");
        
        // When: Get system status
        ResponseEntity<SystemStatusVO> response = controller.getSystemStatus();
        
        // Then: Should return HTTP 200
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // And: Response body should indicate ready
        SystemStatusVO vo = response.getBody();
        assertThat(vo).isNotNull();
        assertThat(vo.getReady()).isTrue();
        assertThat(vo.getStatus()).isEqualTo(StartupStatus.READY);
        assertThat(vo.getReason()).isNull();
        assertThat(vo.getProgress()).isEqualTo(100);
        assertThat(vo.getCurrentPhase()).isEqualTo("应用就绪");
        assertThat(vo.getTimestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("Should return different progress values for different startup phases")
    void testGetSystemStatus_DifferentPhases() {
        // Test Configuration Init phase
        systemReady.set(false);
        when(systemReadyService.getSystemReady()).thenReturn(systemReady);
        when(systemReadyService.getCurrentStatus()).thenReturn(StartupStatus.CONFIGURATION_INIT);
        when(systemReadyService.getProgress()).thenReturn(10);
        when(systemReadyService.getCurrentPhase()).thenReturn("配置初始化");
        
        ResponseEntity<SystemStatusVO> response = controller.getSystemStatus();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProgress()).isEqualTo(10);
        assertThat(response.getBody().getCurrentPhase()).isEqualTo("配置初始化");
        
        // Test Plugins Init phase
        when(systemReadyService.getCurrentStatus()).thenReturn(StartupStatus.PLUGINS_INIT);
        when(systemReadyService.getProgress()).thenReturn(55);
        when(systemReadyService.getCurrentPhase()).thenReturn("插件初始化");
        
        response = controller.getSystemStatus();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProgress()).isEqualTo(55);
        assertThat(response.getBody().getCurrentPhase()).isEqualTo("插件初始化");
    }
    
    @Test
    @DisplayName("Should return negative progress when startup failed")
    void testGetSystemStatus_Failed() {
        // Given: System startup failed
        systemReady.set(false);
        when(systemReadyService.getSystemReady()).thenReturn(systemReady);
        when(systemReadyService.getCurrentStatus()).thenReturn(StartupStatus.FAILED);
        when(systemReadyService.getNotReadyReason()).thenReturn("数据库连接失败");
        when(systemReadyService.getProgress()).thenReturn(-1);
        when(systemReadyService.getCurrentPhase()).thenReturn("启动失败");
        
        // When: Get system status
        ResponseEntity<SystemStatusVO> response = controller.getSystemStatus();
        
        // Then: Should return HTTP 503
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // And: Response should indicate failure
        SystemStatusVO vo = response.getBody();
        assertThat(vo).isNotNull();
        assertThat(vo.getReady()).isFalse();
        assertThat(vo.getStatus()).isEqualTo(StartupStatus.FAILED);
        assertThat(vo.getReason()).isEqualTo("数据库连接失败");
        assertThat(vo.getProgress()).isEqualTo(-1);
    }
}
