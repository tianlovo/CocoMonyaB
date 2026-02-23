package org.xlyo.cocomonyab.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.xlyo.cocomonyab.config.data.DataDirectoryManager;
import org.xlyo.cocomonyab.config.properties.DataDirectoryProperties;

/**
 * 测试环境的DataDirectoryManager配置
 * 用于在测试中提供DataDirectoryManager实例
 */
@TestConfiguration
public class TestDataDirectoryConfiguration {
    
    @Bean
    @Primary
    public DataDirectoryProperties testDataDirectoryProperties() {
        return new DataDirectoryProperties();
    }
    
    @Bean
    @Primary
    public DataDirectoryManager testDataDirectoryManager(DataDirectoryProperties properties) {
        DataDirectoryManager manager = new DataDirectoryManager(properties);
        manager.initialize();
        return manager;
    }
}
