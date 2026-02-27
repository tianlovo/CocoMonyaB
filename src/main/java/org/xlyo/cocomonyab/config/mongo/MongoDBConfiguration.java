package org.xlyo.cocomonyab.config.mongo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.xlyo.cocomonyab.config.data.DataDirectoryManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableMongoRepositories(basePackages = "org.xlyo.cocomonyab.repository")
public class MongoDBConfiguration {
    
    private final MongoDBProperties properties;
    private final DataDirectoryManager dataDirectoryManager;
    
    @PostConstruct
    public void validateConfiguration() {
        MongoMode mode = properties.getMode();
        
        // 如果是 remote 模式，验证 URI 必须配置
        if (mode.isRemote()) {
            if (properties.getUri() == null || properties.getUri().trim().isEmpty()) {
                throw new IllegalStateException(
                    "MongoDB配置无效: remote模式下必须配置spring.data.mongodb.uri"
                );
            }
            log.info("MongoDB配置: 使用远程模式, URI: {}", maskUri(properties.getUri()));
        } else {
            // embedded 模式，使用 DataDirectoryManager 获取存储目录
            String storageDir = dataDirectoryManager.getMongoDbPath().toString();
            log.info("MongoDB配置: 使用嵌入式模式, 存储目录: {}", storageDir);
        }
    }
    
    /**
     * 隐藏 URI 中的敏感信息（用户名和密码）
     */
    private String maskUri(String uri) {
        if (uri == null) {
            return null;
        }
        // 简单的掩码处理，隐藏用户名密码部分
        return uri.replaceAll("://[^@]+@", "://***:***@");
    }
}
