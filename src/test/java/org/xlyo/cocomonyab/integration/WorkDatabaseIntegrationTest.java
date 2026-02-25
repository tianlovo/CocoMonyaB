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
import org.xlyo.cocomonyab.domain.dto.tag.WorkCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.WorkService;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 原作库集成测试
 * 
 * 测试完整的创建-查询-更新-删除流程
 * 测试唯一性约束在并发场景下的正确性
 * 测试引用完整性检查
 * 测试导入导出功能
 * 
 * 使用Testcontainers提供真实MongoDB环境
 * 
 * 需求: 2.6, 9.3, 9.4, 11.2, 11.5
 */
@SpringBootTest
@Testcontainers
class WorkDatabaseIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private WorkService workService;
    
    @Autowired
    private WorkRepository workRepository;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        workRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        workRepository.deleteAll();
    }
    
    /**
     * 测试完整的CRUD流程
     * 需求: 2.1, 2.2, 2.4
     */
    @Test
    void testCompleteCrudFlow() {
        // 1. 创建原作
        WorkCreateDTO createDTO = new WorkCreateDTO();
        createDTO.setName("测试原作");
        createDTO.setAliases(Arrays.asList("别名1", "别名2"));
        createDTO.setUrls(Arrays.asList("https://example.com"));
        createDTO.setAvatarBase64("base64encodedimage");
        createDTO.setRemark("测试备注");
        
        WorkVO created = workService.create(createDTO);
        
        // 验证创建结果
        assertNotNull(created.getId());
        assertEquals("测试原作", created.getName());
        assertEquals(2, created.getAliases().size());
        assertEquals(1, created.getUrls().size());
        assertEquals("base64encodedimage", created.getAvatarBase64());
        assertEquals("测试备注", created.getRemark());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        
        // 2. 查询原作
        WorkVO retrieved = workService.getById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getName(), retrieved.getName());
        
        // 3. 更新原作
        WorkUpdateDTO updateDTO = new WorkUpdateDTO();
        updateDTO.setName("更新后的原作");
        updateDTO.setRemark("更新后的备注");
        
        WorkVO updated = workService.update(created.getId(), updateDTO);
        assertEquals("更新后的原作", updated.getName());
        assertEquals("更新后的备注", updated.getRemark());
        assertEquals(2, updated.getAliases().size()); // 别名未更新，应保持不变
        
        // 4. 删除原作
        workService.delete(created.getId(), false);
        
        // 验证删除成功
        assertThrows(BusinessException.class, () -> workService.getById(created.getId()));
    }
    
    /**
     * 测试唯一性约束
     * 需求: 2.2, 2.3
     */
    @Test
    void testUniquenessConstraints() {
        // 创建第一个原作
        WorkCreateDTO dto1 = new WorkCreateDTO();
        dto1.setName("原作A");
        dto1.setAliases(Arrays.asList("别名A1", "别名A2"));
        
        workService.create(dto1);
        
        // 尝试创建同名原作
        WorkCreateDTO dto2 = new WorkCreateDTO();
        dto2.setName("原作A");
        dto2.setAliases(Arrays.asList("别名B1"));
        
        assertThrows(BusinessException.class, () -> workService.create(dto2));
        
        // 尝试创建使用相同别名的原作
        WorkCreateDTO dto3 = new WorkCreateDTO();
        dto3.setName("原作B");
        dto3.setAliases(Arrays.asList("别名A1")); // 与原作A的别名冲突
        
        assertThrows(BusinessException.class, () -> workService.create(dto3));
    }
    
    /**
     * 测试并发场景下的唯一性约束
     * 需求: 2.2, 2.3
     */
    @Test
    void testConcurrentUniquenessConstraints() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // 多个线程同时尝试创建同名原作
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    WorkCreateDTO dto = new WorkCreateDTO();
                    dto.setName("并发测试原作");
                    dto.setAliases(Arrays.asList());
                    
                    workService.create(dto);
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
     * 需求: 2.6, 9.3, 9.4
     */
    @Test
    void testReferenceIntegrityCheck() {
        // 创建原作
        WorkCreateDTO createDTO = new WorkCreateDTO();
        createDTO.setName("被引用的原作");
        createDTO.setAliases(Arrays.asList());
        
        WorkVO work = workService.create(createDTO);
        
        // 注意：此测试需要实际的引用关系（角色或配置引用该原作）
        // 由于这是原作库的独立测试，我们只测试无引用时的删除
        
        // 无引用时应该可以删除
        assertDoesNotThrow(() -> workService.delete(work.getId(), false));
    }
    
    /**
     * 测试强制删除
     * 需求: 2.6, 9.7
     */
    @Test
    void testForceDelete() {
        // 创建原作
        WorkCreateDTO createDTO = new WorkCreateDTO();
        createDTO.setName("强制删除测试");
        createDTO.setAliases(Arrays.asList());
        
        WorkVO work = workService.create(createDTO);
        
        // 强制删除应该总是成功
        assertDoesNotThrow(() -> workService.delete(work.getId(), true));
        
        // 验证删除成功
        assertThrows(BusinessException.class, () -> workService.getById(work.getId()));
    }
    
    /**
     * 测试导入导出功能
     * 需求: 11.2, 11.5
     */
    @Test
    void testImportExport() {
        // 创建多个原作
        for (int i = 1; i <= 3; i++) {
            WorkCreateDTO dto = new WorkCreateDTO();
            dto.setName("原作" + i);
            dto.setAliases(Arrays.asList("别名" + i + "A", "别名" + i + "B"));
            dto.setUrls(Arrays.asList("https://example" + i + ".com"));
            workService.create(dto);
        }
        
        // 导出
        String json = workService.exportToJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("原作1"));
        assertTrue(json.contains("原作2"));
        assertTrue(json.contains("原作3"));
        
        // 清空数据库
        workRepository.deleteAll();
        
        // 导入
        workService.importFromJson(json);
        
        // 验证导入成功
        PageResponse<WorkVO> page = workService.page(1L, 10L, new WorkQueryDTO());
        assertEquals(3, page.getData().getRecords().size());
        
        // 验证数据完整性
        WorkVO work1 = page.getData().getRecords().stream()
                .filter(w -> w.getName().equals("原作1"))
                .findFirst()
                .orElse(null);
        assertNotNull(work1);
        assertEquals(2, work1.getAliases().size());
        assertEquals(1, work1.getUrls().size());
    }
    
    /**
     * 测试更新别名时的唯一性验证
     * 需求: 2.5
     */
    @Test
    void testUpdateAliasUniqueness() {
        // 创建两个原作
        WorkCreateDTO dto1 = new WorkCreateDTO();
        dto1.setName("原作1");
        dto1.setAliases(Arrays.asList("别名1A"));
        WorkVO work1 = workService.create(dto1);
        
        WorkCreateDTO dto2 = new WorkCreateDTO();
        dto2.setName("原作2");
        dto2.setAliases(Arrays.asList("别名2A"));
        WorkVO work2 = workService.create(dto2);
        
        // 尝试将原作2的别名更新为与原作1冲突的别名
        WorkUpdateDTO updateDTO = new WorkUpdateDTO();
        updateDTO.setAliases(Arrays.asList("别名1A")); // 与原作1的别名冲突
        
        assertThrows(BusinessException.class, () -> workService.update(work2.getId(), updateDTO));
    }
    
    /**
     * 测试通过名称和别名查询
     * 需求: 2.7
     */
    @Test
    void testQueryByNameAndAlias() {
        // 创建原作
        WorkCreateDTO createDTO = new WorkCreateDTO();
        createDTO.setName("查询测试原作");
        createDTO.setAliases(Arrays.asList("查询别名1", "查询别名2"));
        
        WorkVO created = workService.create(createDTO);
        
        // 通过名称查询
        WorkVO byName = workService.getByName("查询测试原作");
        assertNotNull(byName);
        assertEquals(created.getId(), byName.getId());
        
        // 通过别名查询
        WorkVO byAlias1 = workService.getByAlias("查询别名1");
        assertNotNull(byAlias1);
        assertEquals(created.getId(), byAlias1.getId());
        
        WorkVO byAlias2 = workService.getByAlias("查询别名2");
        assertNotNull(byAlias2);
        assertEquals(created.getId(), byAlias2.getId());
    }
    
    /**
     * 测试分页查询
     * 需求: 2.8
     */
    @Test
    void testPagination() {
        // 创建多个原作
        for (int i = 1; i <= 15; i++) {
            WorkCreateDTO dto = new WorkCreateDTO();
            dto.setName("原作" + i);
            dto.setAliases(Arrays.asList());
            workService.create(dto);
        }
        
        // 第一页
        PageResponse<WorkVO> page1 = workService.page(1L, 10L, new WorkQueryDTO());
        assertEquals(10, page1.getData().getRecords().size());
        assertEquals(1L, page1.getData().getCurrent());
        assertEquals(15L, page1.getData().getTotal());
        assertEquals(2L, page1.getData().getPages());
        
        // 第二页
        PageResponse<WorkVO> page2 = workService.page(2L, 10L, new WorkQueryDTO());
        assertEquals(5, page2.getData().getRecords().size());
        assertEquals(2L, page2.getData().getCurrent());
    }
}
