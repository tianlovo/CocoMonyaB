package org.xlyo.cocomonyab.repository.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.entity.tag.Author;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AuthorRepository.
 * Tests Property 5 from the design document.
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
class AuthorRepositoryPropertyTest {
    
    @Autowired
    private AuthorRepository repository;
    
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
     * Feature: tag-database-and-filter-refactor, Property 5: 作者多路径查询一致性
     * 
     * 对于任何作者，通过ID、名称或任一别名查询应该返回同一个作者实体。
     * 
     * **Validates: Requirements 1.7**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-5")
    void authorMultiPathQueryConsistency() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Generate random author data
            String name = "Author_" + random.nextInt(100000);
            List<String> aliases = generateAliases(i);
            
            // Create and save author
            Author author = new Author();
            author.setName(name);
            author.setAliases(aliases);
            author.setSignature("Test signature " + i);
            author.setUrls(List.of("https://example.com/" + i));
            author.setAvatarBase64("base64data" + i);
            author.setRemark("Test remark " + i);
            author.setCreateTime(LocalDateTime.now());
            author.setUpdateTime(LocalDateTime.now());
            
            Author savedAuthor = repository.save(author);
            String authorId = savedAuthor.getId();
            
            // Query by ID
            Optional<Author> byId = repository.findById(authorId);
            assertTrue(byId.isPresent(), 
                "Author should be found by ID (iteration " + i + ")");
            
            // Query by name
            Optional<Author> byName = repository.findByName(name);
            assertTrue(byName.isPresent(), 
                "Author should be found by name (iteration " + i + ")");
            
            // Verify ID query and name query return the same entity
            assertEquals(byId.get().getId(), byName.get().getId(),
                "Query by ID and name should return the same author (iteration " + i + ")");
            assertEquals(byId.get().getName(), byName.get().getName(),
                "Author name should be consistent (iteration " + i + ")");
            
            // Query by each alias and verify consistency
            for (String alias : aliases) {
                Optional<Author> byAlias = repository.findByAliasesContaining(alias);
                assertTrue(byAlias.isPresent(), 
                    "Author should be found by alias '" + alias + "' (iteration " + i + ")");
                
                // Verify alias query returns the same entity
                assertEquals(authorId, byAlias.get().getId(),
                    "Query by alias '" + alias + "' should return the same author (iteration " + i + ")");
                assertEquals(name, byAlias.get().getName(),
                    "Author name should be consistent when queried by alias (iteration " + i + ")");
            }
            
            // Cleanup for this iteration
            repository.deleteById(authorId);
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
