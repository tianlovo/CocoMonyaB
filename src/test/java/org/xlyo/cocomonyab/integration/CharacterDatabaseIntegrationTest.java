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
import org.xlyo.cocomonyab.domain.dto.tag.CharacterCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterUpdateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkCreateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.CharacterService;
import org.xlyo.cocomonyab.service.tag.WorkService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色库集成测试
 * 
 * 测试完整的创建-查询-更新-删除流程
 * 测试唯一性约束在并发场景下的正确性
 * 测试原作引用验证
 * 测试引用完整性检查
 * 测试导入导出功能
 * 
 * 使用Testcontainers提供真实MongoDB环境
 * 
 * 需求: 3.7, 9.5, 11.3, 11.6
 */
@SpringBootTest
@Testcontainers
class CharacterDatabaseIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private WorkService workService;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private WorkRepository workRepository;
    
    private WorkVO testWork;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        characterRepository.deleteAll();
        workRepository.deleteAll();
        
        // 创建测试用原作
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("测试原作");
        workDTO.setAliases(Arrays.asList());
        testWork = workService.create(workDTO);
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        characterRepository.deleteAll();
        workRepository.deleteAll();
    }
    
    /**
     * 测试完整的CRUD流程
     * 需求: 3.1, 3.2, 3.4
     */
    @Test
    void testCompleteCrudFlow() {
        // 1. 创建角色
        CharacterCreateDTO createDTO = new CharacterCreateDTO();
        createDTO.setName("测试角色");
        createDTO.setAliases(Arrays.asList("别名1", "别名2"));
        createDTO.setWorkId(testWork.getId());
        createDTO.setSpecies("人类");
        createDTO.setAvatarBase64("base64encodedimage");
        createDTO.setRemark("测试备注");
        
        CharacterVO created = characterService.create(createDTO);
        
        // 验证创建结果
        assertNotNull(created.getId());
        assertEquals("测试角色", created.getName());
        assertEquals(2, created.getAliases().size());
        assertEquals(testWork.getId(), created.getWorkId());
        assertEquals("人类", created.getSpecies());
        assertEquals("base64encodedimage", created.getAvatarBase64());
        assertEquals("测试备注", created.getRemark());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        
        // 2. 查询角色
        CharacterVO retrieved = characterService.getById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getName(), retrieved.getName());
        
        // 3. 更新角色
        CharacterUpdateDTO updateDTO = new CharacterUpdateDTO();
        updateDTO.setName("更新后的角色");
        updateDTO.setSpecies("精灵");
        
        CharacterVO updated = characterService.update(created.getId(), updateDTO);
        assertEquals("更新后的角色", updated.getName());
        assertEquals("精灵", updated.getSpecies());
        assertEquals(2, updated.getAliases().size()); // 别名未更新，应保持不变
        
        // 4. 删除角色
        characterService.delete(created.getId(), false);
        
        // 验证删除成功
        assertThrows(BusinessException.class, () -> characterService.getById(created.getId()));
    }
    
    /**
     * 测试唯一性约束
     * 需求: 3.2, 3.3
     */
    @Test
    void testUniquenessConstraints() {
        // 创建第一个角色
        CharacterCreateDTO dto1 = new CharacterCreateDTO();
        dto1.setName("角色A");
        dto1.setAliases(Arrays.asList("别名A1", "别名A2"));
        dto1.setWorkId(testWork.getId());
        
        characterService.create(dto1);
        
        // 尝试创建同名角色
        CharacterCreateDTO dto2 = new CharacterCreateDTO();
        dto2.setName("角色A");
        dto2.setAliases(Arrays.asList("别名B1"));
        dto2.setWorkId(testWork.getId());
        
        assertThrows(BusinessException.class, () -> characterService.create(dto2));
        
        // 尝试创建使用相同别名的角色
        CharacterCreateDTO dto3 = new CharacterCreateDTO();
        dto3.setName("角色B");
        dto3.setAliases(Arrays.asList("别名A1")); // 与角色A的别名冲突
        dto3.setWorkId(testWork.getId());
        
        assertThrows(BusinessException.class, () -> characterService.create(dto3));
    }
    
    /**
     * 测试并发场景下的唯一性约束
     * 需求: 3.2, 3.3
     */
    @Test
    void testConcurrentUniquenessConstraints() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // 多个线程同时尝试创建同名角色
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    CharacterCreateDTO dto = new CharacterCreateDTO();
                    dto.setName("并发测试角色");
                    dto.setAliases(Arrays.asList());
                    dto.setWorkId(testWork.getId());
                    
                    characterService.create(dto);
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
     * 测试原作引用验证
     * 需求: 3.5
     */
    @Test
    void testWorkReferenceValidation() {
        // 尝试创建引用不存在的原作的角色
        CharacterCreateDTO dto = new CharacterCreateDTO();
        dto.setName("测试角色");
        dto.setAliases(Arrays.asList());
        dto.setWorkId("nonexistent-work-id");
        
        assertThrows(BusinessException.class, () -> characterService.create(dto));
    }
    
    /**
     * 测试引用完整性检查
     * 需求: 3.7, 9.5
     */
    @Test
    void testReferenceIntegrityCheck() {
        // 创建角色
        CharacterCreateDTO createDTO = new CharacterCreateDTO();
        createDTO.setName("被引用的角色");
        createDTO.setAliases(Arrays.asList());
        createDTO.setWorkId(testWork.getId());
        
        CharacterVO character = characterService.create(createDTO);
        
        // 注意：此测试需要实际的引用关系（配置引用该角色）
        // 由于这是角色库的独立测试，我们只测试无引用时的删除
        
        // 无引用时应该可以删除
        assertDoesNotThrow(() -> characterService.delete(character.getId(), false));
    }
    
    /**
     * 测试强制删除
     * 需求: 3.7, 9.7
     */
    @Test
    void testForceDelete() {
        // 创建角色
        CharacterCreateDTO createDTO = new CharacterCreateDTO();
        createDTO.setName("强制删除测试");
        createDTO.setAliases(Arrays.asList());
        createDTO.setWorkId(testWork.getId());
        
        CharacterVO character = characterService.create(createDTO);
        
        // 强制删除应该总是成功
        assertDoesNotThrow(() -> characterService.delete(character.getId(), true));
        
        // 验证删除成功
        assertThrows(BusinessException.class, () -> characterService.getById(character.getId()));
    }
    
    /**
     * 测试导入导出功能
     * 需求: 11.3, 11.6
     */
    @Test
    void testImportExport() {
        // 创建多个角色
        for (int i = 1; i <= 3; i++) {
            CharacterCreateDTO dto = new CharacterCreateDTO();
            dto.setName("角色" + i);
            dto.setAliases(Arrays.asList("别名" + i + "A", "别名" + i + "B"));
            dto.setWorkId(testWork.getId());
            dto.setSpecies("种族" + i);
            characterService.create(dto);
        }
        
        // 导出
        String json = characterService.exportToJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("角色1"));
        assertTrue(json.contains("角色2"));
        assertTrue(json.contains("角色3"));
        
        // 清空数据库
        characterRepository.deleteAll();
        
        // 导入
        characterService.importFromJson(json);
        
        // 验证导入成功
        PageResponse<CharacterVO> page = characterService.page(1L, 10L, new CharacterQueryDTO());
        assertEquals(3, page.getData().getRecords().size());
        
        // 验证数据完整性
        CharacterVO character1 = page.getData().getRecords().stream()
                .filter(c -> c.getName().equals("角色1"))
                .findFirst()
                .orElse(null);
        assertNotNull(character1);
        assertEquals(2, character1.getAliases().size());
        assertEquals("种族1", character1.getSpecies());
    }
    
    /**
     * 测试更新别名时的唯一性验证
     * 需求: 3.6
     */
    @Test
    void testUpdateAliasUniqueness() {
        // 创建两个角色
        CharacterCreateDTO dto1 = new CharacterCreateDTO();
        dto1.setName("角色1");
        dto1.setAliases(Arrays.asList("别名1A"));
        dto1.setWorkId(testWork.getId());
        CharacterVO character1 = characterService.create(dto1);
        
        CharacterCreateDTO dto2 = new CharacterCreateDTO();
        dto2.setName("角色2");
        dto2.setAliases(Arrays.asList("别名2A"));
        dto2.setWorkId(testWork.getId());
        CharacterVO character2 = characterService.create(dto2);
        
        // 尝试将角色2的别名更新为与角色1冲突的别名
        CharacterUpdateDTO updateDTO = new CharacterUpdateDTO();
        updateDTO.setAliases(Arrays.asList("别名1A")); // 与角色1的别名冲突
        
        assertThrows(BusinessException.class, () -> characterService.update(character2.getId(), updateDTO));
    }
    
    /**
     * 测试通过名称和别名查询
     * 需求: 3.8
     */
    @Test
    void testQueryByNameAndAlias() {
        // 创建角色
        CharacterCreateDTO createDTO = new CharacterCreateDTO();
        createDTO.setName("查询测试角色");
        createDTO.setAliases(Arrays.asList("查询别名1", "查询别名2"));
        createDTO.setWorkId(testWork.getId());
        
        CharacterVO created = characterService.create(createDTO);
        
        // 通过名称查询
        CharacterVO byName = characterService.getByName("查询测试角色");
        assertNotNull(byName);
        assertEquals(created.getId(), byName.getId());
        
        // 通过别名查询
        CharacterVO byAlias1 = characterService.getByAlias("查询别名1");
        assertNotNull(byAlias1);
        assertEquals(created.getId(), byAlias1.getId());
        
        CharacterVO byAlias2 = characterService.getByAlias("查询别名2");
        assertNotNull(byAlias2);
        assertEquals(created.getId(), byAlias2.getId());
    }
    
    /**
     * 测试通过原作ID查询
     * 需求: 3.8
     */
    @Test
    void testQueryByWorkId() {
        // 创建另一个原作
        WorkCreateDTO work2DTO = new WorkCreateDTO();
        work2DTO.setName("测试原作2");
        work2DTO.setAliases(Arrays.asList());
        WorkVO work2 = workService.create(work2DTO);
        
        // 为第一个原作创建角色
        CharacterCreateDTO dto1 = new CharacterCreateDTO();
        dto1.setName("角色1");
        dto1.setAliases(Arrays.asList());
        dto1.setWorkId(testWork.getId());
        characterService.create(dto1);
        
        CharacterCreateDTO dto2 = new CharacterCreateDTO();
        dto2.setName("角色2");
        dto2.setAliases(Arrays.asList());
        dto2.setWorkId(testWork.getId());
        characterService.create(dto2);
        
        // 为第二个原作创建角色
        CharacterCreateDTO dto3 = new CharacterCreateDTO();
        dto3.setName("角色3");
        dto3.setAliases(Arrays.asList());
        dto3.setWorkId(work2.getId());
        characterService.create(dto3);
        
        // 查询第一个原作的角色
        List<CharacterVO> work1Characters = characterService.getByWorkId(testWork.getId());
        assertEquals(2, work1Characters.size());
        
        // 查询第二个原作的角色
        List<CharacterVO> work2Characters = characterService.getByWorkId(work2.getId());
        assertEquals(1, work2Characters.size());
    }
    
    /**
     * 测试分页查询
     * 需求: 3.9
     */
    @Test
    void testPagination() {
        // 创建多个角色
        for (int i = 1; i <= 15; i++) {
            CharacterCreateDTO dto = new CharacterCreateDTO();
            dto.setName("角色" + i);
            dto.setAliases(Arrays.asList());
            dto.setWorkId(testWork.getId());
            characterService.create(dto);
        }
        
        // 第一页
        PageResponse<CharacterVO> page1 = characterService.page(1L, 10L, new CharacterQueryDTO());
        assertEquals(10, page1.getData().getRecords().size());
        assertEquals(1L, page1.getData().getCurrent());
        assertEquals(15L, page1.getData().getTotal());
        assertEquals(2L, page1.getData().getPages());
        
        // 第二页
        PageResponse<CharacterVO> page2 = characterService.page(2L, 10L, new CharacterQueryDTO());
        assertEquals(5, page2.getData().getRecords().size());
        assertEquals(2L, page2.getData().getCurrent());
    }
}
