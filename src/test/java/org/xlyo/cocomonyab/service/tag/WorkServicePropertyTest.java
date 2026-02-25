package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.WorkQueryDTO;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for WorkService.
 * Tests Properties 10, 12 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because jqwik doesn't support Spring's dependency injection well.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class WorkServicePropertyTest {
    
    @Autowired
    private WorkService workService;
    
    @Autowired
    private WorkRepository workRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        workRepository.deleteAll();
        characterRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        workRepository.deleteAll();
        characterRepository.deleteAll();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 10: 原作删除引用完整性检查
     * 
     * 对于任何原作，如果该原作被角色库引用或在过滤配置的原作标签中被引用，
     * 则删除操作应该被拒绝并返回引用信息；如果使用强制删除选项，则所有引用应该被自动清理。
     * 
     * **Validates: Requirements 2.6, 10.3, 10.4, 10.6, 10.7**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-10")
    void workDeleteReferenceIntegrityCheck() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a work
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of("Alias_" + random.nextInt(100000)));
            work = workRepository.save(work);
            
            // Create a character that references this work
            String characterName = "Character_" + random.nextInt(100000);
            Character character = createCharacter(characterName, List.of(), work.getId());
            character = characterRepository.save(character);
            
            // Test 1: Delete without force should fail when referenced
            String workId = work.getId();
            assertThrows(BusinessException.class, () -> {
                workService.delete(workId, false);
            }, "Deleting work with references should fail without force (iteration " + i + ")");
            
            // Test 2: Delete with force should succeed
            assertDoesNotThrow(() -> {
                workService.delete(workId, true);
            }, "Deleting work with force should succeed (iteration " + i + ")");
            
            // Verify work is deleted
            assertFalse(workRepository.existsById(workId),
                "Work should be deleted (iteration " + i + ")");
            
            // Clean up character
            characterRepository.deleteById(character.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 12: 原作分页查询正确性
     * 
     * 对于任何原作集合，分页查询的所有页面合并后应该包含所有原作，且没有重复或遗漏。
     * 
     * **Validates: Requirements 2.8**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-12")
    void workPaginationCorrectness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create random number of works (between 5 and 20)
            int workCount = 5 + random.nextInt(16);
            List<String> createdWorkIds = new ArrayList<>();
            
            for (int j = 0; j < workCount; j++) {
                String name = "Work_" + i + "_" + j + "_" + random.nextInt(100000);
                Work work = createWork(name, List.of());
                work = workRepository.save(work);
                createdWorkIds.add(work.getId());
            }
            
            // Query all pages with page size of 3
            long pageSize = 3L;
            Set<String> retrievedWorkIds = new HashSet<>();
            long currentPage = 1L;
            
            while (true) {
                PageResponse<WorkVO> response = workService.page(currentPage, pageSize, new WorkQueryDTO());
                
                // Collect work IDs from this page
                for (WorkVO vo : response.getData().getRecords()) {
                    retrievedWorkIds.add(vo.getId());
                }
                
                // Check if we've reached the last page
                if (currentPage >= response.getData().getPages()) {
                    break;
                }
                currentPage++;
            }
            
            // Verify all created works are retrieved
            assertEquals(createdWorkIds.size(), retrievedWorkIds.size(),
                "All works should be retrieved (iteration " + i + ")");
            
            for (String createdId : createdWorkIds) {
                assertTrue(retrievedWorkIds.contains(createdId),
                    "Work " + createdId + " should be in retrieved results (iteration " + i + ")");
            }
            
            // Clean up
            workRepository.deleteAll();
        }
    }
    
    // Helper methods
    
    private Work createWork(String name, List<String> aliases) {
        Work work = new Work();
        work.setName(name);
        work.setAliases(new ArrayList<>(aliases));
        work.setUrls(new ArrayList<>());
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        return work;
    }
    
    private Character createCharacter(String name, List<String> aliases, String workId) {
        Character character = new Character();
        character.setName(name);
        character.setAliases(new ArrayList<>(aliases));
        character.setWorkId(workId);
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        return character;
    }
}
