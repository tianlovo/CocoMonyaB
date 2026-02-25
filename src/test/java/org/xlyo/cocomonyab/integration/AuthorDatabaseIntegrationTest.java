package org.xlyo.cocomonyab.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.service.tag.AuthorService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 作者库集成测试
 * 
 * 测试完整的创建-查询-更新-删除流程
 * 测试唯一性约束在并发场景下的正确性
 * 测试引用完整性检查
 * 测试导入导出功能
 * 
 * 使用Testcontainers提供真实MongoDB环境
 * 
 * 需求: 1.1, 1.2, 1.3, 1.6, 12.1, 12.4
 */
@SpringBootTest
@Testcontainers
class AuthorDatabaseIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private AuthorService authorService;
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        authorRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        authorRepository.deleteAll();
    }
    
    /**
     * 测试完整的CRUD流程
     * 需求: 1.1, 1.2, 1.4
     */
    @Test
    void testCompleteCrudFlow() {
        // 1. 创建作者
        AuthorCreateDTO createDTO = new AuthorCreateDTO();
        createDTO.setName("测试作者");
        createDTO.setAliases(Arrays.asList("别名1", "别名2"));
        createDTO.setSignature("这是个性签名");
        createDTO.setUrls(Arrays.asList("https://example.com"));
        createDTO.setAvatarBase64("base64encodedimage");
        createDTO.setRemark("测试备注");
        
        AuthorVO created = authorService.create(createDTO);
        
        // 验证创建结果
        assertNotNull(created.getId());
        assertEquals("测试作者", created.getName());
        assertEquals(2, created.getAliases().size());
        assertEquals("这是个性签名", created.getSignature());
        assertEquals(1, created.getUrls().size());
        assertEquals("base64encodedimage", created.getAvatarBase64());
        assertEquals("测试备注", created.getRemark());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        
        // 2. 查询作者
        AuthorVO retrieved = authorService.getById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getName(), retrieved.getName());
        
        // 3. 更新作者
        AuthorUpdateDTO updateDTO = new AuthorUpdateDTO();
        updateDTO.setName("更新后的作者");
        updateDTO.setSignature("更新后的签名");
        
        AuthorVO updated = authorService.update(created.getId(), updateDTO);
        assertEquals("更新后的作者", updated.getName());
        assertEquals("更新后的签名", updated.getSignature());
        assertEquals(2, updated.getAliases().size()); // 别名未更新，应保持不变
        
        // 4. 删除作者
        authorService.delete(created.getId(), false);
        
        // 验证删除成功
        assertThrows(BusinessException.class, () -> authorService.getById(created.getId()));
    }
    
    /**
     * 测试唯一性约束
     * 需求: 1.2, 1.3
     */
    @Test
    void testUniquenessConstraints() {
        // 创建第一个作者
        AuthorCreateDTO dto1 = new AuthorCreateDTO();
        dto1.setName("作者A");
        dto1.setAliases(Arrays.asList("别名A1", "别名A2"));
        
        authorService.create(dto1);
        
        // 尝试创建同名作者
        AuthorCreateDTO dto2 = new AuthorCreateDTO();
        dto2.setName("作者A");
        dto2.setAliases(Arrays.asList("别名B1"));
        
        assertThrows(BusinessException.class, () -> authorService.create(dto2));
        
        // 尝试创建使用相同别名的作者
        AuthorCreateDTO dto3 = new AuthorCreateDTO();
        dto3.setName("作者B");
        dto3.setAliases(Arrays.asList("别名A1")); // 与作者A的别名冲突
        
        assertThrows(BusinessException.class, () -> authorService.create(dto3));
    }
    
    /**
     * 测试并发场景下的唯一性约束
     * 需求: 1.2, 1.3
     */
    @Test
    void testConcurrentUniquenessConstraints() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // 多个线程同时尝试创建同名作者
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    AuthorCreateDTO dto = new AuthorCreateDTO();
                    dto.setName("并发测试作者");
                    dto.setAliases(Arrays.asList());
                    
                    authorService.create(dto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 应该只有一个成功，其他都失败
        assertEquals(1, successCount.get());
        assertEquals(threadCount - 1, failureCount.get());
    }
    
    /**
     * 测试引用完整性检查
     * 需求: 1.6
     */
    @Test
    void testReferenceIntegrityCheck() {
        // 创建作者
        AuthorCreateDTO createDTO = new AuthorCreateDTO();
        createDTO.setName("被引用的作者");
        createDTO.setAliases(Arrays.asList());
        
        AuthorVO author = authorService.create(createDTO);
        
        // 注意：此测试需要实际的引用关系（角色或配置引用该作者）
        // 由于这是作者库的独立测试，我们只测试无引用时的删除
        
        // 无引用时应该可以删除
        assertDoesNotThrow(() -> authorService.delete(author.getId(), false));
    }
    
    /**
     * 测试强制删除
     * 需求: 1.6
     */
    @Test
    void testForceDelete() {
        // 创建作者
        AuthorCreateDTO createDTO = new AuthorCreateDTO();
        createDTO.setName("强制删除测试");
        createDTO.setAliases(Arrays.asList());
        
        AuthorVO author = authorService.create(createDTO);
        
        // 强制删除应该总是成功
        assertDoesNotThrow(() -> authorService.delete(author.getId(), true));
        
        // 验证删除成功
        assertThrows(BusinessException.class, () -> authorService.getById(author.getId()));
    }
    
    /**
     * 测试导入导出功能
     * 需求: 12.1, 12.4
     */
    @Test
    void testImportExport() {
        // 创建多个作者
        for (int i = 1; i <= 3; i++) {
            AuthorCreateDTO dto = new AuthorCreateDTO();
            dto.setName("作者" + i);
            dto.setAliases(Arrays.asList("别名" + i + "A", "别名" + i + "B"));
            dto.setSignature("签名" + i);
            authorService.create(dto);
        }
        
        // 导出
        String json = authorService.exportToJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("作者1"));
        assertTrue(json.contains("作者2"));
        assertTrue(json.contains("作者3"));
        
        // 清空数据库
        authorRepository.deleteAll();
        
        // 导入
        authorService.importFromJson(json);
        
        // 验证导入成功
        PageResponse<AuthorVO> page = authorService.page(1L, 10L, new AuthorQueryDTO());
        assertEquals(3, page.getData().getRecords().size());
        
        // 验证数据完整性
        AuthorVO author1 = page.getData().getRecords().stream()
                .filter(a -> a.getName().equals("作者1"))
                .findFirst()
                .orElse(null);
        assertNotNull(author1);
        assertEquals(2, author1.getAliases().size());
        assertEquals("签名1", author1.getSignature());
    }
    
    /**
     * 测试更新别名时的唯一性验证
     * 需求: 1.5
     */
    @Test
    void testUpdateAliasUniqueness() {
        // 创建两个作者
        AuthorCreateDTO dto1 = new AuthorCreateDTO();
        dto1.setName("作者1");
        dto1.setAliases(Arrays.asList("别名1A"));
        AuthorVO author1 = authorService.create(dto1);
        
        AuthorCreateDTO dto2 = new AuthorCreateDTO();
        dto2.setName("作者2");
        dto2.setAliases(Arrays.asList("别名2A"));
        AuthorVO author2 = authorService.create(dto2);
        
        // 尝试将作者2的别名更新为与作者1冲突的别名
        AuthorUpdateDTO updateDTO = new AuthorUpdateDTO();
        updateDTO.setAliases(Arrays.asList("别名1A")); // 与作者1的别名冲突
        
        assertThrows(BusinessException.class, () -> authorService.update(author2.getId(), updateDTO));
    }
    
    /**
     * 测试通过名称和别名查询
     * 需求: 1.7
     */
    @Test
    void testQueryByNameAndAlias() {
        // 创建作者
        AuthorCreateDTO createDTO = new AuthorCreateDTO();
        createDTO.setName("查询测试作者");
        createDTO.setAliases(Arrays.asList("查询别名1", "查询别名2"));
        
        AuthorVO created = authorService.create(createDTO);
        
        // 通过名称查询
        AuthorVO byName = authorService.getByName("查询测试作者");
        assertNotNull(byName);
        assertEquals(created.getId(), byName.getId());
        
        // 通过别名查询
        AuthorVO byAlias1 = authorService.getByAlias("查询别名1");
        assertNotNull(byAlias1);
        assertEquals(created.getId(), byAlias1.getId());
        
        AuthorVO byAlias2 = authorService.getByAlias("查询别名2");
        assertNotNull(byAlias2);
        assertEquals(created.getId(), byAlias2.getId());
    }
}
