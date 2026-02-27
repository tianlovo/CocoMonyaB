package org.xlyo.cocomonyab.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.xlyo.cocomonyab.service.SystemReadyService;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SystemStatusController单元测试
 */
@WebMvcTest(SystemStatusController.class)
class SystemStatusControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private SystemReadyService systemReadyService;
    
    @Test
    void testGetSystemStatus_Ready() throws Exception {
        // 模拟系统已就绪
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(true));
        when(systemReadyService.getNotReadyReason()).thenReturn(null);
        
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.reason").isEmpty())
                .andExpect(jsonPath("$.data.timestamp").isNumber());
    }
    
    @Test
    void testGetSystemStatus_NotReady() throws Exception {
        // 模拟系统未就绪
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(false));
        when(systemReadyService.getNotReadyReason()).thenReturn("系统正在启动中...");
        
        mockMvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.ready").value(false))
                .andExpect(jsonPath("$.data.reason").value("系统正在启动中..."))
                .andExpect(jsonPath("$.data.timestamp").isNumber());
    }
    
    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data").value("OK"));
    }
    
    @Test
    void testGetSystemInfo() throws Exception {
        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.projectName").value("CocoMonyaB"))
                .andExpect(jsonPath("$.data.version").exists())
                .andExpect(jsonPath("$.data.group").exists())
                .andExpect(jsonPath("$.data.description").exists())
                .andExpect(jsonPath("$.data.buildTime").exists())
                .andExpect(jsonPath("$.data.javaVersion").exists())
                .andExpect(jsonPath("$.data.gradleVersion").exists())
                .andExpect(jsonPath("$.data.fullVersionInfo").exists());
    }
}
