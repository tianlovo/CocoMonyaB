package org.xlyo.cocomonyab.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.xlyo.cocomonyab.config.data.DataDirectoryManager;

/**
 * Logback 配置
 * 动态设置日志文件的存储路径
 */
@Slf4j
@Configuration
public class LogbackConfiguration {
    
    private final DataDirectoryManager dataDirectoryManager;
    
    public LogbackConfiguration(DataDirectoryManager dataDirectoryManager) {
        this.dataDirectoryManager = dataDirectoryManager;
    }
    
    @PostConstruct
    public void configureLogPath() {
        String logsPath = dataDirectoryManager.getLogsPath().toString();
        System.setProperty("LOG_PATH", logsPath);
        log.debug("日志文件路径设置为: {}", logsPath);
    }
}
