package org.xlyo.cocomonyab.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoDB 配置属性测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=remote",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class MongoDBPropertiesTest {
    
    @Autowired
    private MongoDBProperties properties;
    
    @Test
    void testDefaultConfiguration() {
        // 验证配置可以被正确读取
        assertNotNull(properties);
        assertNotNull(properties.getEmbedded());
        assertNotNull(properties.getEmbedded().getStorage());
    }
    
    @Test
    void testEmbeddedConfiguration() {
        // 验证嵌入式配置对象不为空
        assertNotNull(properties.getEmbedded());
        assertNotNull(properties.getEmbedded().getStorage());
        
        // 验证默认值
        assertEquals("7.0.12", properties.getEmbedded().getVersion());
        assertEquals(27017, properties.getEmbedded().getPort());
        assertEquals("127.0.0.1", properties.getEmbedded().getBindIp());
        assertEquals("data/db/mongo", properties.getEmbedded().getStorage().getDirectory());
    }
}
