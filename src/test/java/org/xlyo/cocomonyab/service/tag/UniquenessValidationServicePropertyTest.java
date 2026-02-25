package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.exception.TagUniquenessException;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.enums.EntityType;
import org.xlyo.cocomonyab.domain.vo.ConflictInfo;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for UniquenessValidationService.
 * Tests Properties 1, 2, 7, 8, 13, 14, 20, 21, 22 from the design document.
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
class UniquenessValidationServicePropertyTest {
    
    @Autowired
    private UniquenessValidationService validationService;
    
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
     * Feature: tag-database-and-filter-refactor, Property 1: 作者名称唯一性
     * 
     * 对于任何作者创建或更新操作，如果使用的名称已经存在于作者库中（排除自身），
     * 则操作应该被拒绝并返回唯一性冲突错误。
     * 
     * **Validates: Requirements 1.2**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-1")
    void authorNameUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String name = "Author_" + random.nextInt(100000);
            
            // Create first author with this name
            Author author1 = createAuthor(name, List.of());
            Author savedAuthor1 = authorRepository.save(author1);
            
            // Attempt to create second author with the same name
            // Should throw TagUniquenessException
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(name, null, EntityType.AUTHOR);
            }, "Creating author with duplicate name should throw exception (iteration " + i + ")");
            
            // Verify that updating the same author with its own name is allowed
            assertDoesNotThrow(() -> {
                validationService.validateNameUniqueness(name, savedAuthor1.getId(), EntityType.AUTHOR);
            }, "Updating author with its own name should be allowed (iteration " + i + ")");
            
            // Cleanup
            authorRepository.deleteById(savedAuthor1.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 2: 作者别名全局唯一性
     * 
     * 对于任何作者创建或更新操作，如果使用的任何别名已经存在于作者库、原作库或角色库中（排除自身），
     * 则操作应该被拒绝并返回唯一性冲突错误。
     * 
     * **Validates: Requirements 1.3, 1.5**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-2")
    void authorAliasGlobalUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String alias = "Alias_" + random.nextInt(100000);
            
            // Test 1: Alias conflicts with another author's alias
            Author author1 = createAuthor("Author1_" + i, List.of(alias));
            Author savedAuthor1 = authorRepository.save(author1);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.AUTHOR);
            }, "Creating author with duplicate alias should throw exception (iteration " + i + ")");
            
            authorRepository.deleteById(savedAuthor1.getId());
            
            // Test 2: Alias conflicts with a work's alias
            Work work = createWork("Work_" + i, List.of(alias));
            Work savedWork = workRepository.save(work);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.AUTHOR);
            }, "Creating author with alias that exists in work should throw exception (iteration " + i + ")");
            
            workRepository.deleteById(savedWork.getId());
            
            // Test 3: Alias conflicts with a character's alias
            Work work2 = createWork("Work2_" + i, List.of());
            Work savedWork2 = workRepository.save(work2);
            
            Character character = createCharacter("Character_" + i, List.of(alias), savedWork2.getId());
            Character savedCharacter = characterRepository.save(character);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.AUTHOR);
            }, "Creating author with alias that exists in character should throw exception (iteration " + i + ")");
            
            // Cleanup
            characterRepository.deleteById(savedCharacter.getId());
            workRepository.deleteById(savedWork2.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 7: 原作名称唯一性
     * 
     * 对于任何原作创建或更新操作，如果使用的名称已经存在于原作库中（排除自身），
     * 则操作应该被拒绝并返回唯一性冲突错误。
     * 
     * **Validates: Requirements 2.2**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-7")
    void workNameUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String name = "Work_" + random.nextInt(100000);
            
            // Create first work with this name
            Work work1 = createWork(name, List.of());
            Work savedWork1 = workRepository.save(work1);
            
            // Attempt to create second work with the same name
            // Should throw TagUniquenessException
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(name, null, EntityType.WORK);
            }, "Creating work with duplicate name should throw exception (iteration " + i + ")");
            
            // Verify that updating the same work with its own name is allowed
            assertDoesNotThrow(() -> {
                validationService.validateNameUniqueness(name, savedWork1.getId(), EntityType.WORK);
            }, "Updating work with its own name should be allowed (iteration " + i + ")");
            
            // Cleanup
            workRepository.deleteById(savedWork1.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 8: 原作别名全局唯一性
     * 
     * 对于任何原作创建或更新操作，如果使用的任何别名已经存在于作者库、原作库或角色库中（排除自身），
     * 则操作应该被拒绝并返回唯一性冲突错误。
     * 
     * **Validates: Requirements 2.3, 2.5**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-8")
    void workAliasGlobalUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String alias = "WorkAlias_" + random.nextInt(100000);
            
            // Test 1: Alias conflicts with another work's alias
            Work work1 = createWork("Work1_" + i, List.of(alias));
            Work savedWork1 = workRepository.save(work1);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.WORK);
            }, "Creating work with duplicate alias should throw exception (iteration " + i + ")");
            
            workRepository.deleteById(savedWork1.getId());
            
            // Test 2: Alias conflicts with an author's alias
            Author author = createAuthor("Author_" + i, List.of(alias));
            Author savedAuthor = authorRepository.save(author);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.WORK);
            }, "Creating work with alias that exists in author should throw exception (iteration " + i + ")");
            
            authorRepository.deleteById(savedAuthor.getId());
            
            // Test 3: Alias conflicts with a character's alias
            Work work2 = createWork("Work2_" + i, List.of());
            Work savedWork2 = workRepository.save(work2);
            
            Character character = createCharacter("Character_" + i, List.of(alias), savedWork2.getId());
            Character savedCharacter = characterRepository.save(character);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.WORK);
            }, "Creating work with alias that exists in character should throw exception (iteration " + i + ")");
            
            // Cleanup
            characterRepository.deleteById(savedCharacter.getId());
            workRepository.deleteById(savedWork2.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 13: 角色名称唯一性
     * 
     * 对于任何角色创建或更新操作，如果使用的名称已经存在于角色库中（排除自身），
     * 则操作应该被拒绝并返回唯一性冲突错误。
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-13")
    void characterNameUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String name = "Character_" + random.nextInt(100000);
            
            // Create a work first (required for character)
            Work work = createWork("Work_" + i, List.of());
            Work savedWork = workRepository.save(work);
            
            // Create first character with this name
            Character character1 = createCharacter(name, List.of(), savedWork.getId());
            Character savedCharacter1 = characterRepository.save(character1);
            
            // Attempt to create second character with the same name
            // Should throw TagUniquenessException
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(name, null, EntityType.CHARACTER);
            }, "Creating character with duplicate name should throw exception (iteration " + i + ")");
            
            // Verify that updating the same character with its own name is allowed
            assertDoesNotThrow(() -> {
                validationService.validateNameUniqueness(name, savedCharacter1.getId(), EntityType.CHARACTER);
            }, "Updating character with its own name should be allowed (iteration " + i + ")");
            
            // Cleanup
            characterRepository.deleteById(savedCharacter1.getId());
            workRepository.deleteById(savedWork.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 14: 角色别名全局唯一性
     * 
     * 对于任何角色创建或更新操作，如果使用的任何别名已经存在于作者库、原作库或角色库中（排除自身），
     * 则操作应该被拒绝并返回唯一性冲突错误。
     * 
     * **Validates: Requirements 3.3, 3.6**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-14")
    void characterAliasGlobalUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String alias = "CharAlias_" + random.nextInt(100000);
            
            // Create a work first (required for character)
            Work work = createWork("Work_" + i, List.of());
            Work savedWork = workRepository.save(work);
            
            // Test 1: Alias conflicts with another character's alias
            Character character1 = createCharacter("Character1_" + i, List.of(alias), savedWork.getId());
            Character savedCharacter1 = characterRepository.save(character1);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.CHARACTER);
            }, "Creating character with duplicate alias should throw exception (iteration " + i + ")");
            
            characterRepository.deleteById(savedCharacter1.getId());
            
            // Test 2: Alias conflicts with an author's alias
            Author author = createAuthor("Author_" + i, List.of(alias));
            Author savedAuthor = authorRepository.save(author);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.CHARACTER);
            }, "Creating character with alias that exists in author should throw exception (iteration " + i + ")");
            
            authorRepository.deleteById(savedAuthor.getId());
            
            // Test 3: Alias conflicts with a work's alias
            Work work2 = createWork("Work2_" + i, List.of(alias));
            Work savedWork2 = workRepository.save(work2);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.CHARACTER);
            }, "Creating character with alias that exists in work should throw exception (iteration " + i + ")");
            
            // Cleanup
            workRepository.deleteById(savedWork2.getId());
            workRepository.deleteById(savedWork.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 20: 跨库名称全局唯一性
     * 
     * 对于任何标签实体（作者、原作、角色），如果其名称已经存在于任一标签库中，
     * 则创建操作应该被拒绝并返回冲突信息，指明冲突的实体类型和ID。
     * 
     * **Validates: Requirements 4.1, 4.3**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-20")
    void crossDatabaseNameGlobalUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String name = "Entity_" + random.nextInt(100000);
            
            // Test 1: Author name conflicts with work name
            Author author = createAuthor(name, List.of());
            Author savedAuthor = authorRepository.save(author);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(name, null, EntityType.WORK);
            }, "Creating work with name that exists in author should throw exception (iteration " + i + ")");
            
            // Verify conflict info
            ConflictInfo conflict = validationService.checkNameOrAliasConflict(name, null);
            assertTrue(conflict.isHasConflict(), "Should detect conflict (iteration " + i + ")");
            assertEquals(EntityType.AUTHOR, conflict.getConflictType(), 
                "Conflict type should be AUTHOR (iteration " + i + ")");
            assertEquals(savedAuthor.getId(), conflict.getConflictId(), 
                "Conflict ID should match author ID (iteration " + i + ")");
            
            authorRepository.deleteById(savedAuthor.getId());
            
            // Test 2: Work name conflicts with character name
            Work work = createWork(name, List.of());
            Work savedWork = workRepository.save(work);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(name, null, EntityType.CHARACTER);
            }, "Creating character with name that exists in work should throw exception (iteration " + i + ")");
            
            workRepository.deleteById(savedWork.getId());
            
            // Test 3: Character name conflicts with author name
            Work work2 = createWork("Work2_" + i, List.of());
            Work savedWork2 = workRepository.save(work2);
            
            Character character = createCharacter(name, List.of(), savedWork2.getId());
            Character savedCharacter = characterRepository.save(character);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(name, null, EntityType.AUTHOR);
            }, "Creating author with name that exists in character should throw exception (iteration " + i + ")");
            
            // Cleanup
            characterRepository.deleteById(savedCharacter.getId());
            workRepository.deleteById(savedWork2.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 21: 跨库别名全局唯一性
     * 
     * 对于任何标签实体（作者、原作、角色），如果其任何别名已经存在于任一标签库中，
     * 则创建操作应该被拒绝并返回冲突信息，指明冲突的实体类型和ID。
     * 
     * **Validates: Requirements 4.2, 4.4**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-21")
    void crossDatabaseAliasGlobalUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String alias = "GlobalAlias_" + random.nextInt(100000);
            
            // Test 1: Author alias conflicts with work alias
            Author author = createAuthor("Author_" + i, List.of(alias));
            Author savedAuthor = authorRepository.save(author);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.WORK);
            }, "Creating work with alias that exists in author should throw exception (iteration " + i + ")");
            
            // Verify conflict info
            ConflictInfo conflict = validationService.checkNameOrAliasConflict(alias, null);
            assertTrue(conflict.isHasConflict(), "Should detect conflict (iteration " + i + ")");
            assertEquals(EntityType.AUTHOR, conflict.getConflictType(), 
                "Conflict type should be AUTHOR (iteration " + i + ")");
            
            authorRepository.deleteById(savedAuthor.getId());
            
            // Test 2: Work alias conflicts with character alias
            Work work = createWork("Work_" + i, List.of(alias));
            Work savedWork = workRepository.save(work);
            Work work2 = createWork("Work2_" + i, List.of());
            Work savedWork2 = workRepository.save(work2);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.CHARACTER);
            }, "Creating character with alias that exists in work should throw exception (iteration " + i + ")");
            
            workRepository.deleteById(savedWork.getId());
            
            // Test 3: Character alias conflicts with author alias
            Character character = createCharacter("Character_" + i, List.of(alias), savedWork2.getId());
            Character savedCharacter = characterRepository.save(character);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(alias), null, EntityType.AUTHOR);
            }, "Creating author with alias that exists in character should throw exception (iteration " + i + ")");
            
            // Cleanup
            characterRepository.deleteById(savedCharacter.getId());
            workRepository.deleteById(savedWork2.getId());
        }
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 22: 名称与别名交叉唯一性
     * 
     * 对于任何标签实体（作者、原作、角色），如果其名称与其他实体的别名冲突，
     * 或其别名与其他实体的名称冲突，则创建或更新操作应该被拒绝。
     * 
     * **Validates: Requirements 4.3, 4.4**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-22")
    void nameCrossAliasUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            String value = "CrossValue_" + random.nextInt(100000);
            
            // Test 1: Author name conflicts with work alias
            Work work = createWork("Work_" + i, List.of(value));
            Work savedWork = workRepository.save(work);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateNameUniqueness(value, null, EntityType.AUTHOR);
            }, "Creating author with name that exists as work alias should throw exception (iteration " + i + ")");
            
            workRepository.deleteById(savedWork.getId());
            
            // Test 2: Work alias conflicts with character name
            Work work2 = createWork("Work2_" + i, List.of());
            Work savedWork2 = workRepository.save(work2);
            
            Character character = createCharacter(value, List.of(), savedWork2.getId());
            Character savedCharacter = characterRepository.save(character);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(value), null, EntityType.WORK);
            }, "Creating work with alias that exists as character name should throw exception (iteration " + i + ")");
            
            characterRepository.deleteById(savedCharacter.getId());
            workRepository.deleteById(savedWork2.getId());
            
            // Test 3: Character alias conflicts with author name
            Author author = createAuthor(value, List.of());
            Author savedAuthor = authorRepository.save(author);
            
            Work work3 = createWork("Work3_" + i, List.of());
            Work savedWork3 = workRepository.save(work3);
            
            assertThrows(TagUniquenessException.class, () -> {
                validationService.validateAliasUniqueness(List.of(value), null, EntityType.CHARACTER);
            }, "Creating character with alias that exists as author name should throw exception (iteration " + i + ")");
            
            // Cleanup
            authorRepository.deleteById(savedAuthor.getId());
            workRepository.deleteById(savedWork3.getId());
        }
    }
    
    // Helper methods to create test entities
    
    private Author createAuthor(String name, List<String> aliases) {
        Author author = new Author();
        author.setName(name);
        author.setAliases(new ArrayList<>(aliases));
        author.setSignature("Test signature");
        author.setUrls(List.of("https://example.com"));
        author.setAvatarBase64("base64data");
        author.setRemark("Test remark");
        author.setCreateTime(LocalDateTime.now());
        author.setUpdateTime(LocalDateTime.now());
        return author;
    }
    
    private Work createWork(String name, List<String> aliases) {
        Work work = new Work();
        work.setName(name);
        work.setAliases(new ArrayList<>(aliases));
        work.setUrls(List.of("https://example.com"));
        work.setAvatarBase64("base64data");
        work.setRemark("Test remark");
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        return work;
    }
    
    private Character createCharacter(String name, List<String> aliases, String workId) {
        Character character = new Character();
        character.setName(name);
        character.setAliases(new ArrayList<>(aliases));
        character.setWorkId(workId);
        character.setSpecies("Human");
        character.setAvatarBase64("base64data");
        character.setRemark("Test remark");
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        return character;
    }
}
