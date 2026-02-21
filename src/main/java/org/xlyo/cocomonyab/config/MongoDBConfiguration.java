package org.xlyo.cocomonyab.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Slf4j
@Configuration
@EnableMongoRepositories(basePackages = "org.xlyo.cocomonyab.repository")
public class MongoDBConfiguration {
    
    @Autowired
    private MongoDBProperties properties;
    
    @PostConstruct
    public void validateConfiguration() {
        String mode = properties.getMode();
        
        // 验证 mode 必须是 "embedded" 或 "remote"
        if (!"embedded".equalsIgnoreCase(mode) && !"remote".equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                String.format("MongoDB配置无效: mode必须是embedded或remote，当前值为: %s", mode)
            );
        }
        
        // 如果是 remote 模式，验证 URI 必须配置
        if ("remote".equalsIgnoreCase(mode)) {
            if (properties.getUri() == null || properties.getUri().trim().isEmpty()) {
                throw new IllegalStateException(
                    "MongoDB配置无效: remote模式下必须配置spring.data.mongodb.uri"
                );
            }
            log.info("MongoDB配置: 使用远程模式, URI: {}", maskUri(properties.getUri()));
        } else {
            // embedded 模式
            String storageDir = properties.getEmbedded().getStorage().getDirectory();
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
