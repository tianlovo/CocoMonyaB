package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TagExpansionService.
 * Tests Properties 24-30 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because jqwik doesn't support Spring's dependency injection well.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya_test"
})
class TagExpansionServicePropertyTest {
    
    @Autowired
    private TagExpansionService tagExpansionService;
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @Autowired
    private WorkRepository workRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        authorRepository.deleteAll();
        workRepository.deleteAll();
        characterRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        authorRepository.deleteAll();
        workRepository.deleteAll();
        characterRepository.deleteAll();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 24: 作者标签展开完整性
     * 
     * 对于任何作者ID，展开操作应该返回包含该作者名称和所有别名的字符串列表。
     * 
     * **Validates: Requirements 7.1**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-24")
    void authorTagExpansionCompleteness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create an author with random aliases
            String authorName = "Author_" + random.nextInt(100000);
            int aliasCount = random.nextInt(5) + 1; // 1-5 aliases
            List<String> aliases = new ArrayList<>();
            for (int j = 0; j < aliasCount; j++) {
                aliases.add("Alias_" + i + "_" + j + "_" + random.nextInt(100000));
            }
            
            Author author = createAuthor(authorName, aliases);
            author = authorRepository.save(author);
            
            // Expand the author ID
            List<String> expandedTags = tagExpansionService.expandAuthor(author.getId());
            
            // Verify the expanded tags contain the author name
            assertTrue(expandedTags.contains(authorName),
                "Expanded tags should contain author name (iteration " + i + ")");
            
            // Verify the expanded tags contain all aliases
            for (String alias : aliases) {
                assertTrue(expandedTags.contains(alias),
                    "Expanded tags should contain alias '" + alias + "' (iteration " + i + ")");
            }
            
            // Verify the size is correct (name + aliases)
            assertEquals(1 + aliasCount, expandedTags.size(),
                "Expanded tags should have correct size (iteration " + i + ")");
            
            // Clean up
            authorRepository.deleteById(author.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 25: 角色标签展开完整性
     * 
     * 对于任何角色ID，展开操作应该返回包含该角色名称和所有别名的字符串列表。
     * 
     * **Validates: Requirements 7.2**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-25")
    void characterTagExpansionCompleteness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a work first
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of());
            work = workRepository.save(work);
            
            // Create a character with random aliases
            String characterName = "Character_" + random.nextInt(100000);
            int aliasCount = random.nextInt(5) + 1; // 1-5 aliases
            List<String> aliases = new ArrayList<>();
            for (int j = 0; j < aliasCount; j++) {
                aliases.add("CharAlias_" + i + "_" + j + "_" + random.nextInt(100000));
            }
            
            Character character = createCharacter(characterName, aliases, work.getId());
            character = characterRepository.save(character);
            
            // Expand the character ID
            List<String> expandedTags = tagExpansionService.expandCharacter(character.getId());
            
            // Verify the expanded tags contain the character name
            assertTrue(expandedTags.contains(characterName),
                "Expanded tags should contain character name (iteration " + i + ")");
            
            // Verify the expanded tags contain all aliases
            for (String alias : aliases) {
                assertTrue(expandedTags.contains(alias),
                    "Expanded tags should contain alias '" + alias + "' (iteration " + i + ")");
            }
            
            // Verify the size is correct (name + aliases)
            assertEquals(1 + aliasCount, expandedTags.size(),
                "Expanded tags should have correct size (iteration " + i + ")");
            
            // Clean up
            characterRepository.deleteById(character.getId());
            workRepository.deleteById(work.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 26: 原作标签展开完整性
     * 
     * 对于任何原作ID，展开操作应该返回包含该原作名称和所有别名的字符串列表。
     * 
     * **Validates: Requirements 7.3**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-26")
    void workTagExpansionCompleteness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a work with random aliases
            String workName = "Work_" + random.nextInt(100000);
            int aliasCount = random.nextInt(5) + 1; // 1-5 aliases
            List<String> aliases = new ArrayList<>();
            for (int j = 0; j < aliasCount; j++) {
                aliases.add("WorkAlias_" + i + "_" + j + "_" + random.nextInt(100000));
            }
            
            Work work = createWork(workName, aliases);
            work = workRepository.save(work);
            
            // Expand the work ID
            List<String> expandedTags = tagExpansionService.expandWork(work.getId());
            
            // Verify the expanded tags contain the work name
            assertTrue(expandedTags.contains(workName),
                "Expanded tags should contain work name (iteration " + i + ")");
            
            // Verify the expanded tags contain all aliases
            for (String alias : aliases) {
                assertTrue(expandedTags.contains(alias),
                    "Expanded tags should contain alias '" + alias + "' (iteration " + i + ")");
            }
            
            // Verify the size is correct (name + aliases)
            assertEquals(1 + aliasCount, expandedTags.size(),
                "Expanded tags should have correct size (iteration " + i + ")");
            
            // Clean up
            workRepository.deleteById(work.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 27: 自定义标签展开正确性
     * 
     * 对于任何自定义标签ID，展开操作应该返回配置中对应的标签字符串。
     * 
     * **Validates: Requirements 7.4**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-27")
    void customTagExpansionCorrectness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a config with random custom tags
            TagFilterConfigVO config = new TagFilterConfigVO();
            Map<String, String> customTags = new HashMap<>();
            
            int tagCount = random.nextInt(5) + 1; // 1-5 custom tags
            List<String> tagIds = new ArrayList<>();
            List<String> tagValues = new ArrayList<>();
            
            for (int j = 0; j < tagCount; j++) {
                String tagId = "customTag_" + i + "_" + j;
                String tagValue = "CustomValue_" + random.nextInt(100000);
                customTags.put(tagId, tagValue);
                tagIds.add(tagId);
                tagValues.add(tagValue);
            }
            
            config.setCustomTags(customTags);
            
            // Expand each custom tag ID
            for (int j = 0; j < tagCount; j++) {
                String expandedTag = tagExpansionService.expandCustomTag(tagIds.get(j), config);
                
                // Verify the expanded tag matches the expected value
                assertEquals(tagValues.get(j), expandedTag,
                    "Expanded custom tag should match expected value (iteration " + i + ", tag " + j + ")");
            }
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 28: 全量标签展开完整性
     * 
     * 对于任何标签过滤配置，展开所有标签应该返回包含所有作者、角色、原作和自定义标签的完整字符串列表。
     * 
     * **Validates: Requirements 7.5**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-28")
    void allTagExpansionCompleteness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create authors
            int authorCount = random.nextInt(3) + 1; // 1-3 authors
            List<String> authorIds = new ArrayList<>();
            Set<String> expectedAuthorTags = new HashSet<>();
            
            for (int j = 0; j < authorCount; j++) {
                String name = "Author_" + i + "_" + j + "_" + random.nextInt(100000);
                List<String> aliases = List.of("AuthorAlias_" + i + "_" + j + "_" + random.nextInt(100000));
                Author author = createAuthor(name, aliases);
                author = authorRepository.save(author);
                authorIds.add(author.getId());
                expectedAuthorTags.add(name);
                expectedAuthorTags.addAll(aliases);
            }
            
            // Create works
            int workCount = random.nextInt(3) + 1; // 1-3 works
            List<String> workIds = new ArrayList<>();
            Set<String> expectedWorkTags = new HashSet<>();
            
            for (int j = 0; j < workCount; j++) {
                String name = "Work_" + i + "_" + j + "_" + random.nextInt(100000);
                List<String> aliases = List.of("WorkAlias_" + i + "_" + j + "_" + random.nextInt(100000));
                Work work = createWork(name, aliases);
                work = workRepository.save(work);
                workIds.add(work.getId());
                expectedWorkTags.add(name);
                expectedWorkTags.addAll(aliases);
            }
            
            // Create characters
            int characterCount = random.nextInt(3) + 1; // 1-3 characters
            List<String> characterIds = new ArrayList<>();
            Set<String> expectedCharacterTags = new HashSet<>();
            
            for (int j = 0; j < characterCount; j++) {
                String name = "Character_" + i + "_" + j + "_" + random.nextInt(100000);
                List<String> aliases = List.of("CharAlias_" + i + "_" + j + "_" + random.nextInt(100000));
                Character character = createCharacter(name, aliases, workIds.get(0));
                character = characterRepository.save(character);
                characterIds.add(character.getId());
                expectedCharacterTags.add(name);
                expectedCharacterTags.addAll(aliases);
            }
            
            // Create custom tags
            Map<String, String> customTags = new HashMap<>();
            Set<String> expectedCustomTags = new HashSet<>();
            int customTagCount = random.nextInt(3) + 1; // 1-3 custom tags
            
            for (int j = 0; j < customTagCount; j++) {
                String tagId = "customTag_" + i + "_" + j;
                String tagValue = "CustomValue_" + i + "_" + j + "_" + random.nextInt(100000);
                customTags.put(tagId, tagValue);
                expectedCustomTags.add(tagValue);
            }
            
            // Create config
            TagFilterConfigVO config = new TagFilterConfigVO();
            config.setAuthorIds(authorIds);
            config.setWorkIds(workIds);
            config.setCharacterIds(characterIds);
            config.setCustomTags(customTags);
            
            // Expand all tags
            List<String> expandedTags = tagExpansionService.expandAll(config);
            Set<String> expandedTagSet = new HashSet<>(expandedTags);
            
            // Verify all expected tags are present
            for (String expectedTag : expectedAuthorTags) {
                assertTrue(expandedTagSet.contains(expectedTag),
                    "Expanded tags should contain author tag '" + expectedTag + "' (iteration " + i + ")");
            }
            
            for (String expectedTag : expectedWorkTags) {
                assertTrue(expandedTagSet.contains(expectedTag),
                    "Expanded tags should contain work tag '" + expectedTag + "' (iteration " + i + ")");
            }
            
            for (String expectedTag : expectedCharacterTags) {
                assertTrue(expandedTagSet.contains(expectedTag),
                    "Expanded tags should contain character tag '" + expectedTag + "' (iteration " + i + ")");
            }
            
            for (String expectedTag : expectedCustomTags) {
                assertTrue(expandedTagSet.contains(expectedTag),
                    "Expanded tags should contain custom tag '" + expectedTag + "' (iteration " + i + ")");
            }
            
            // Clean up
            authorRepository.deleteAll();
            workRepository.deleteAll();
            characterRepository.deleteAll();
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 29: 标签展开容错性
     * 
     * 对于任何不存在的标签ID，展开操作应该跳过该ID而不抛出异常，并记录警告日志。
     * 
     * **Validates: Requirements 7.6**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-29")
    void tagExpansionFaultTolerance() {
        // Run 100 iterations with random invalid inputs
        for (int i = 0; i < 100; i++) {
            final int iteration = i;
            // Generate random non-existent IDs
            String nonExistentAuthorId = "nonexistent_author_" + random.nextInt(100000);
            String nonExistentWorkId = "nonexistent_work_" + random.nextInt(100000);
            String nonExistentCharacterId = "nonexistent_character_" + random.nextInt(100000);
            
            // Test expanding non-existent author ID
            assertDoesNotThrow(() -> {
                List<String> tags = tagExpansionService.expandAuthor(nonExistentAuthorId);
                assertTrue(tags.isEmpty(),
                    "Expanding non-existent author should return empty list (iteration " + iteration + ")");
            }, "Expanding non-existent author should not throw exception (iteration " + iteration + ")");
            
            // Test expanding non-existent work ID
            assertDoesNotThrow(() -> {
                List<String> tags = tagExpansionService.expandWork(nonExistentWorkId);
                assertTrue(tags.isEmpty(),
                    "Expanding non-existent work should return empty list (iteration " + iteration + ")");
            }, "Expanding non-existent work should not throw exception (iteration " + iteration + ")");
            
            // Test expanding non-existent character ID
            assertDoesNotThrow(() -> {
                List<String> tags = tagExpansionService.expandCharacter(nonExistentCharacterId);
                assertTrue(tags.isEmpty(),
                    "Expanding non-existent character should return empty list (iteration " + iteration + ")");
            }, "Expanding non-existent character should not throw exception (iteration " + iteration + ")");
            
            // Test expanding non-existent custom tag ID
            TagFilterConfigVO config = new TagFilterConfigVO();
            config.setCustomTags(new HashMap<>());
            
            assertDoesNotThrow(() -> {
                String tag = tagExpansionService.expandCustomTag("nonexistent_custom_" + random.nextInt(100000), config);
                assertNull(tag,
                    "Expanding non-existent custom tag should return null (iteration " + iteration + ")");
            }, "Expanding non-existent custom tag should not throw exception (iteration " + iteration + ")");
            
            // Test expandAll with non-existent IDs
            config.setAuthorIds(List.of(nonExistentAuthorId));
            config.setWorkIds(List.of(nonExistentWorkId));
            config.setCharacterIds(List.of(nonExistentCharacterId));
            
            assertDoesNotThrow(() -> {
                List<String> tags = tagExpansionService.expandAll(config);
                assertTrue(tags.isEmpty(),
                    "Expanding all with non-existent IDs should return empty list (iteration " + iteration + ")");
            }, "Expanding all with non-existent IDs should not throw exception (iteration " + iteration + ")");
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 30: 标签展开去重性
     * 
     * 对于任何标签过滤配置，展开所有标签返回的字符串列表应该不包含重复项。
     * 
     * **Validates: Requirements 7.7**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-30")
    void tagExpansionDeduplication() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create an author and a work with the same alias to test deduplication
            String sharedAlias = "SharedAlias_" + random.nextInt(100000);
            
            String authorName = "Author_" + random.nextInt(100000);
            Author author = createAuthor(authorName, List.of(sharedAlias));
            author = authorRepository.save(author);
            
            String workName = "Work_" + random.nextInt(100000);
            Work work = createWork(workName, List.of(sharedAlias));
            work = workRepository.save(work);
            
            // Create config with both author and work
            TagFilterConfigVO config = new TagFilterConfigVO();
            config.setAuthorIds(List.of(author.getId()));
            config.setWorkIds(List.of(work.getId()));
            config.setCharacterIds(new ArrayList<>());
            config.setCustomTags(new HashMap<>());
            
            // Expand all tags
            List<String> expandedTags = tagExpansionService.expandAll(config);
            
            // Verify no duplicates
            Set<String> uniqueTags = new HashSet<>(expandedTags);
            assertEquals(uniqueTags.size(), expandedTags.size(),
                "Expanded tags should not contain duplicates (iteration " + i + ")");
            
            // Verify the shared alias appears only once
            long sharedAliasCount = expandedTags.stream()
                .filter(tag -> tag.equals(sharedAlias))
                .count();
            assertEquals(1, sharedAliasCount,
                "Shared alias should appear only once (iteration " + i + ")");
            
            // Clean up
            authorRepository.deleteById(author.getId());
            workRepository.deleteById(work.getId());
        }
    }
    
    // Helper methods
    
    private Author createAuthor(String name, List<String> aliases) {
        Author author = new Author();
        author.setName(name);
        author.setAliases(new ArrayList<>(aliases));
        author.setUrls(new ArrayList<>());
        author.setCreateTime(LocalDateTime.now());
        author.setUpdateTime(LocalDateTime.now());
        return author;
    }
    
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
