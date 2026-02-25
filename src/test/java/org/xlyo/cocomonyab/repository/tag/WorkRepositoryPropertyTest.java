package org.xlyo.cocomonyab.repository.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.entity.tag.Work;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for WorkRepository.
 * Tests Property 11 from the design document.
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
class WorkRepositoryPropertyTest {
    
    @Autowired
    private WorkRepository repository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        repository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        repository.deleteAll();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 11: 原作多路径查询一致性
     * 
     * 对于任何原作，通过ID、名称或任一别名查询应该返回同一个原作实体。
     * 
     * **Validates: Requirements 2.7**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-11")
    void workMultiPathQueryConsistency() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Generate random work data
            String name = "Work_" + random.nextInt(100000);
            List<String> aliases = generateAliases(i);
            
            // Create and save work
            Work work = new Work();
            work.setName(name);
            work.setAliases(aliases);
            work.setUrls(List.of("https://example.com/" + i));
            work.setAvatarBase64("base64data" + i);
            work.setRemark("Test remark " + i);
            work.setCreateTime(LocalDateTime.now());
            work.setUpdateTime(LocalDateTime.now());
            
            Work savedWork = repository.save(work);
            String workId = savedWork.getId();
            
            // Query by ID
            Optional<Work> byId = repository.findById(workId);
            assertTrue(byId.isPresent(), 
                "Work should be found by ID (iteration " + i + ")");
            
            // Query by name
            Optional<Work> byName = repository.findByName(name);
            assertTrue(byName.isPresent(), 
                "Work should be found by name (iteration " + i + ")");
            
            // Verify ID query and name query return the same entity
            assertEquals(byId.get().getId(), byName.get().getId(),
                "Query by ID and name should return the same work (iteration " + i + ")");
            assertEquals(byId.get().getName(), byName.get().getName(),
                "Work name should be consistent (iteration " + i + ")");
            
            // Query by each alias and verify consistency
            for (String alias : aliases) {
                Optional<Work> byAlias = repository.findByAliasesContaining(alias);
                assertTrue(byAlias.isPresent(), 
                    "Work should be found by alias '" + alias + "' (iteration " + i + ")");
                
                // Verify alias query returns the same entity
                assertEquals(workId, byAlias.get().getId(),
                    "Query by alias '" + alias + "' should return the same work (iteration " + i + ")");
                assertEquals(name, byAlias.get().getName(),
                    "Work name should be consistent when queried by alias (iteration " + i + ")");
            }
            
            // Cleanup for this iteration
            repository.deleteById(workId);
        }
    }
    
    /**
     * Generates a list of aliases (0 to 5 aliases)
     */
    private List<String> generateAliases(int seed) {
        int aliasCount = random.nextInt(6); // 0 to 5 aliases
        List<String> aliases = new ArrayList<>();
        
        for (int i = 0; i < aliasCount; i++) {
            aliases.add("Alias_" + seed + "_" + i + "_" + random.nextInt(10000));
        }
        
        return aliases;
    }
}
