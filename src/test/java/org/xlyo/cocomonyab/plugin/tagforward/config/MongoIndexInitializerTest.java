package org.xlyo.cocomonyab.plugin.tagforward.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MongoDB索引初始化器测试
 * 
 * 验证MongoIndexInitializer正确创建forward_queue集合的索引
 */
@SpringBootTest
@Testcontainers
class MongoIndexInitializerTest {
    
    @Container
    @SuppressWarnings("resource")
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Test
    void shouldCreateTTLIndex() {
        // Given: MongoIndexInitializer已通过@PostConstruct初始化
        
        // When: 查询forward_queue集合的索引信息
        List<IndexInfo> indexes = mongoTemplate.indexOps("forward_queue").getIndexInfo();
        
        // Then: 应该存在TTL索引
        IndexInfo ttlIndex = indexes.stream()
                .filter(index -> "idx_ttl_30days".equals(index.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(ttlIndex).isNotNull();
        assertThat(ttlIndex.getExpireAfter()).isEqualTo(TimeUnit.DAYS.toSeconds(30));
        assertThat(ttlIndex.getIndexFields()).hasSize(1);
        assertThat(ttlIndex.getIndexFields().get(0).getKey()).isEqualTo("createTime");
    }
    
    @Test
    void shouldCreateUniqueCompoundIndex() {
        // Given: ForwardQueueItem类上的@CompoundIndex注解
        
        // When: 查询forward_queue集合的索引信息
        List<IndexInfo> indexes = mongoTemplate.indexOps("forward_queue").getIndexInfo();
        
        // Then: 应该存在唯一复合索引
        IndexInfo uniqueIndex = indexes.stream()
                .filter(index -> "idx_source_unique".equals(index.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(uniqueIndex).isNotNull();
        assertThat(uniqueIndex.isUnique()).isTrue();
        assertThat(uniqueIndex.getIndexFields()).hasSize(2);
        assertThat(uniqueIndex.getIndexFields().get(0).getKey()).isEqualTo("sourceChatId");
        assertThat(uniqueIndex.getIndexFields().get(1).getKey()).isEqualTo("sourceMessageId");
    }
    
    @Test
    void shouldCreateQueryCompoundIndex() {
        // Given: ForwardQueueItem类上的@CompoundIndex注解
        
        // When: 查询forward_queue集合的索引信息
        List<IndexInfo> indexes = mongoTemplate.indexOps("forward_queue").getIndexInfo();
        
        // Then: 应该存在查询复合索引
        IndexInfo queryIndex = indexes.stream()
                .filter(index -> "idx_status_createTime".equals(index.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(queryIndex).isNotNull();
        assertThat(queryIndex.getIndexFields()).hasSize(2);
        assertThat(queryIndex.getIndexFields().get(0).getKey()).isEqualTo("status");
        assertThat(queryIndex.getIndexFields().get(1).getKey()).isEqualTo("createTime");
    }
}
