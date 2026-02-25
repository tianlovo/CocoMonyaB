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
import org.xlyo.cocomonyab.domain.dto.tag.AuthorCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkCreateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.AuthorService;
import org.xlyo.cocomonyab.service.tag.CharacterService;
import org.xlyo.cocomonyab.service.tag.WorkService;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 跨库唯一性集成测试
 * 
 * 测试跨库名称唯一性约束
 * 测试跨库别名唯一性约束
 * 测试名称与别名交叉唯一性
 * 
 * 使用Testcontainers提供真实MongoDB环境
 * 
 * 需求: 4.1, 4.2, 4.3, 4.4
 */
@SpringBootTest
@Testcontainers
class CrossDatabaseUniquenessIntegrationTest {
    
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
    private WorkService workService;
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @Autowired
    private WorkRepository workRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        characterRepository.deleteAll();
        authorRepository.deleteAll();
        workRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        characterRepository.deleteAll();
        authorRepository.deleteAll();
        workRepository.deleteAll();
    }
    
    /**
     * 测试跨库名称唯一性约束
     * 需求: 4.1, 4.3
     */
    @Test
    void testCrossDatabaseNameUniqueness() {
        // 1. 在作者库中创建一个名称
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("重复名称");
        authorDTO.setAliases(Arrays.asList());
        authorService.create(authorDTO);
        
        // 2. 尝试在原作库中创建相同名称
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("重复名称");
        workDTO.setAliases(Arrays.asList());
        
        assertThrows(BusinessException.class, () -> workService.create(workDTO));
        
        // 3. 在原作库中创建一个不同的名称
        WorkCreateDTO work2DTO = new WorkCreateDTO();
        work2DTO.setName("原作名称");
        work2DTO.setAliases(Arrays.asList());
        WorkVO work = workService.create(work2DTO);
        
        // 4. 尝试在角色库中创建与原作相同的名称
        CharacterCreateDTO characterDTO = new CharacterCreateDTO();
        characterDTO.setName("原作名称");
        characterDTO.setAliases(Arrays.asList());
        characterDTO.setWorkId(work.getId());
        
        assertThrows(BusinessException.class, () -> characterService.create(characterDTO));
    }
    
    /**
     * 测试跨库别名唯一性约束
     * 需求: 4.2, 4.4
     */
    @Test
    void testCrossDatabaseAliasUniqueness() {
        // 1. 在作者库中创建一个别名
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("作者A");
        authorDTO.setAliases(Arrays.asList("重复别名"));
        authorService.create(authorDTO);
        
        // 2. 尝试在原作库中创建相同别名
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("原作A");
        workDTO.setAliases(Arrays.asList("重复别名"));
        
        assertThrows(BusinessException.class, () -> workService.create(workDTO));
        
        // 3. 在原作库中创建一个不同的别名
        WorkCreateDTO work2DTO = new WorkCreateDTO();
        work2DTO.setName("原作B");
        work2DTO.setAliases(Arrays.asList("原作别名"));
        WorkVO work = workService.create(work2DTO);
        
        // 4. 尝试在角色库中创建与原作相同的别名
        CharacterCreateDTO characterDTO = new CharacterCreateDTO();
        characterDTO.setName("角色A");
        characterDTO.setAliases(Arrays.asList("原作别名"));
        characterDTO.setWorkId(work.getId());
        
        assertThrows(BusinessException.class, () -> characterService.create(characterDTO));
    }
    
    /**
     * 测试名称与别名交叉唯一性（名称与其他库的别名冲突）
     * 需求: 4.3, 4.4
     */
    @Test
    void testNameAliasConflictAcrossDatabases() {
        // 1. 在作者库中创建一个别名
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("作者A");
        authorDTO.setAliases(Arrays.asList("特殊别名"));
        authorService.create(authorDTO);
        
        // 2. 尝试在原作库中创建名称与作者别名相同的原作
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("特殊别名"); // 与作者的别名冲突
        workDTO.setAliases(Arrays.asList());
        
        assertThrows(BusinessException.class, () -> workService.create(workDTO));
    }
    
    /**
     * 测试别名与名称交叉唯一性（别名与其他库的名称冲突）
     * 需求: 4.3, 4.4
     */
    @Test
    void testAliasNameConflictAcrossDatabases() {
        // 1. 在作者库中创建一个名称
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("特殊名称");
        authorDTO.setAliases(Arrays.asList());
        authorService.create(authorDTO);
        
        // 2. 尝试在原作库中创建别名与作者名称相同的原作
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("原作A");
        workDTO.setAliases(Arrays.asList("特殊名称")); // 与作者的名称冲突
        
        assertThrows(BusinessException.class, () -> workService.create(workDTO));
    }
    
    /**
     * 测试三库之间的完整唯一性约束
     * 需求: 4.1, 4.2, 4.3, 4.4
     */
    @Test
    void testCompleteUniquenessAcrossAllDatabases() {
        // 1. 在作者库中创建实体
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("作者名称");
        authorDTO.setAliases(Arrays.asList("作者别名1", "作者别名2"));
        authorService.create(authorDTO);
        
        // 2. 在原作库中创建实体
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("原作名称");
        workDTO.setAliases(Arrays.asList("原作别名1", "原作别名2"));
        WorkVO work = workService.create(workDTO);
        
        // 3. 在角色库中创建实体
        CharacterCreateDTO characterDTO = new CharacterCreateDTO();
        characterDTO.setName("角色名称");
        characterDTO.setAliases(Arrays.asList("角色别名1", "角色别名2"));
        characterDTO.setWorkId(work.getId());
        characterService.create(characterDTO);
        
        // 4. 尝试在任何库中创建与已存在名称冲突的实体
        AuthorCreateDTO conflictAuthor1 = new AuthorCreateDTO();
        conflictAuthor1.setName("原作名称"); // 与原作名称冲突
        conflictAuthor1.setAliases(Arrays.asList());
        assertThrows(BusinessException.class, () -> authorService.create(conflictAuthor1));
        
        WorkCreateDTO conflictWork1 = new WorkCreateDTO();
        conflictWork1.setName("角色名称"); // 与角色名称冲突
        conflictWork1.setAliases(Arrays.asList());
        assertThrows(BusinessException.class, () -> workService.create(conflictWork1));
        
        // 5. 尝试在任何库中创建与已存在别名冲突的实体
        AuthorCreateDTO conflictAuthor2 = new AuthorCreateDTO();
        conflictAuthor2.setName("新作者");
        conflictAuthor2.setAliases(Arrays.asList("原作别名1")); // 与原作别名冲突
        assertThrows(BusinessException.class, () -> authorService.create(conflictAuthor2));
        
        WorkCreateDTO conflictWork2 = new WorkCreateDTO();
        conflictWork2.setName("新原作");
        conflictWork2.setAliases(Arrays.asList("角色别名1")); // 与角色别名冲突
        assertThrows(BusinessException.class, () -> workService.create(conflictWork2));
        
        // 6. 尝试创建名称与其他库别名冲突的实体
        AuthorCreateDTO conflictAuthor3 = new AuthorCreateDTO();
        conflictAuthor3.setName("原作别名2"); // 名称与原作别名冲突
        conflictAuthor3.setAliases(Arrays.asList());
        assertThrows(BusinessException.class, () -> authorService.create(conflictAuthor3));
        
        // 7. 尝试创建别名与其他库名称冲突的实体
        WorkCreateDTO conflictWork3 = new WorkCreateDTO();
        conflictWork3.setName("新原作2");
        conflictWork3.setAliases(Arrays.asList("角色名称")); // 别名与角色名称冲突
        assertThrows(BusinessException.class, () -> workService.create(conflictWork3));
    }
    
    /**
     * 测试唯一性约束的错误信息
     * 需求: 4.5
     */
    @Test
    void testUniquenessConstraintErrorMessages() {
        // 创建作者
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("测试作者");
        authorDTO.setAliases(Arrays.asList("测试别名"));
        AuthorVO author = authorService.create(authorDTO);
        
        // 尝试创建冲突的原作
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("测试作者"); // 与作者名称冲突
        workDTO.setAliases(Arrays.asList());
        
        try {
            workService.create(workDTO);
            fail("应该抛出BusinessException");
        } catch (BusinessException e) {
            // 验证错误信息包含冲突详情
            String message = e.getMessage();
            assertNotNull(message);
            // 错误信息应该指明冲突的实体类型和相关信息
            assertTrue(message.contains("已存在") || message.contains("冲突") || message.contains("重复"));
        }
    }
    
    /**
     * 测试同一库内的唯一性不受跨库约束影响
     * 需求: 4.1, 4.2
     */
    @Test
    void testSameDatabaseUniquenessIndependence() {
        // 在作者库中创建两个不同的作者
        AuthorCreateDTO author1DTO = new AuthorCreateDTO();
        author1DTO.setName("作者1");
        author1DTO.setAliases(Arrays.asList("别名1"));
        authorService.create(author1DTO);
        
        AuthorCreateDTO author2DTO = new AuthorCreateDTO();
        author2DTO.setName("作者2");
        author2DTO.setAliases(Arrays.asList("别名2"));
        
        // 应该可以成功创建，因为名称和别名都不冲突
        assertDoesNotThrow(() -> authorService.create(author2DTO));
        
        // 但是尝试创建与作者1同名的作者应该失败
        AuthorCreateDTO author3DTO = new AuthorCreateDTO();
        author3DTO.setName("作者1"); // 与作者1名称冲突
        author3DTO.setAliases(Arrays.asList());
        
        assertThrows(BusinessException.class, () -> authorService.create(author3DTO));
    }
    
    /**
     * 测试更新时的跨库唯一性验证
     * 需求: 4.3, 4.4
     */
    @Test
    void testCrossDatabaseUniquenessOnUpdate() {
        // 创建作者和原作
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("作者A");
        authorDTO.setAliases(Arrays.asList("作者别名"));
        AuthorVO author = authorService.create(authorDTO);
        
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("原作A");
        workDTO.setAliases(Arrays.asList("原作别名"));
        WorkVO work = workService.create(workDTO);
        
        // 尝试将原作名称更新为与作者名称冲突
        org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO updateDTO1 = 
            new org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO();
        updateDTO1.setName("作者A"); // 与作者名称冲突
        
        assertThrows(BusinessException.class, () -> workService.update(work.getId(), updateDTO1));
        
        // 尝试将原作别名更新为与作者别名冲突
        org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO updateDTO2 = 
            new org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO();
        updateDTO2.setAliases(Arrays.asList("作者别名")); // 与作者别名冲突
        
        assertThrows(BusinessException.class, () -> workService.update(work.getId(), updateDTO2));
    }
}
