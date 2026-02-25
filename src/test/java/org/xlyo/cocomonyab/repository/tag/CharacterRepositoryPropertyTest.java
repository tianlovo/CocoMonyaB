package org.xlyo.cocomonyab.repository.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CharacterRepository.
 * Tests Property 18 from the design document.
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
class CharacterRepositoryPropertyTest {
    
    @Autowired
    private CharacterRepository repository;
    
    @Autowired
    private WorkRepository workRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        repository.deleteAll();
        workRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        repository.deleteAll();
        workRepository.deleteAll();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 18: 角色多路径查询一致性
     * 
     * 对于任何角色，通过ID、名称、任一别名或所属原作ID查询应该能够找到该角色。
     * 
     * **Validates: Requirements 3.8, 10.3**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-18")
    void characterMultiPathQueryConsistency() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a work first (required for character)
            Work work = new Work();
            work.setName("Work_" + i + "_" + random.nextInt(100000));
            work.setAliases(new ArrayList<>());
            work.setUrls(new ArrayList<>());
            work.setCreateTime(LocalDateTime.now());
            work.setUpdateTime(LocalDateTime.now());
            Work savedWork = workRepository.save(work);
            String workId = savedWork.getId();
            
            // Generate random character data
            String name = "Character_" + random.nextInt(100000);
            List<String> aliases = generateAliases(i);
            String species = generateSpecies();
            
            // Create and save character
            Character character = new Character();
            character.setName(name);
            character.setAliases(aliases);
            character.setWorkId(workId);
            character.setSpecies(species);
            character.setAvatarBase64("base64data" + i);
            character.setRemark("Test remark " + i);
            character.setCreateTime(LocalDateTime.now());
            character.setUpdateTime(LocalDateTime.now());
            
            Character savedCharacter = repository.save(character);
            String characterId = savedCharacter.getId();
            
            // Query by ID
            Optional<Character> byId = repository.findById(characterId);
            assertTrue(byId.isPresent(), 
                "Character should be found by ID (iteration " + i + ")");
            
            // Query by name
            Optional<Character> byName = repository.findByName(name);
            assertTrue(byName.isPresent(), 
                "Character should be found by name (iteration " + i + ")");
            
            // Verify ID query and name query return the same entity
            assertEquals(byId.get().getId(), byName.get().getId(),
                "Query by ID and name should return the same character (iteration " + i + ")");
            assertEquals(byId.get().getName(), byName.get().getName(),
                "Character name should be consistent (iteration " + i + ")");
            
            // Query by each alias and verify consistency
            for (String alias : aliases) {
                Optional<Character> byAlias = repository.findByAliasesContaining(alias);
                assertTrue(byAlias.isPresent(), 
                    "Character should be found by alias '" + alias + "' (iteration " + i + ")");
                
                // Verify alias query returns the same entity
                assertEquals(characterId, byAlias.get().getId(),
                    "Query by alias '" + alias + "' should return the same character (iteration " + i + ")");
                assertEquals(name, byAlias.get().getName(),
                    "Character name should be consistent when queried by alias (iteration " + i + ")");
            }
            
            // Query by work ID
            List<Character> byWorkId = repository.findByWorkId(workId);
            assertFalse(byWorkId.isEmpty(), 
                "Character should be found by work ID (iteration " + i + ")");
            assertTrue(byWorkId.stream().anyMatch(c -> c.getId().equals(characterId)),
                "Character list by work ID should contain the created character (iteration " + i + ")");
            
            // Verify work ID query returns the same entity
            Character foundByWorkId = byWorkId.stream()
                .filter(c -> c.getId().equals(characterId))
                .findFirst()
                .orElseThrow();
            assertEquals(characterId, foundByWorkId.getId(),
                "Query by work ID should return the same character (iteration " + i + ")");
            assertEquals(name, foundByWorkId.getName(),
                "Character name should be consistent when queried by work ID (iteration " + i + ")");
            
            // Cleanup for this iteration
            repository.deleteById(characterId);
            workRepository.deleteById(workId);
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
    
    /**
     * Generates a random species
     */
    private String generateSpecies() {
        String[] species = {"Human", "Elf", "Dwarf", "Dragon", "Demon", "Angel", "Beast", "Robot", null};
        return species[random.nextInt(species.length)];
    }
}
