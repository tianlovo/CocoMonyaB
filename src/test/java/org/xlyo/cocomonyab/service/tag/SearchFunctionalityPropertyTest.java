package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkQueryDTO;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for search and filtering functionality.
 * Tests Properties 31 and 33 from the design document.
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
class SearchFunctionalityPropertyTest {
    
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
     * Feature: tag-database-and-filter-refactor, Property 31: 模糊搜索匹配性
     * 
     * 对于任何标签实体（作者、原作、角色），如果搜索关键词是其名称或任一别名的子串，
     * 则该实体应该出现在搜索结果中。
     * 
     * **Validates: Requirements 10.1, 10.2**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-31")
    void fuzzySearchMatching() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Test Author search
            testAuthorFuzzySearch();
            
            // Test Work search
            testWorkFuzzySearch();
            
            // Test Character search
            testCharacterFuzzySearch();
            
            // Clean up for next iteration
            authorRepository.deleteAll();
            workRepository.deleteAll();
            characterRepository.deleteAll();
        }
    }
    
    private void testAuthorFuzzySearch() {
        // Create an author with a unique name and aliases
        String uniqueId = "test" + random.nextInt(1000000);
        String authorName = "AuthorName_" + uniqueId;
        String alias1 = "AuthorAlias1_" + uniqueId;
        String alias2 = "AuthorAlias2_" + uniqueId;
        
        Author author = new Author();
        author.setName(authorName);
        author.setAliases(List.of(alias1, alias2));
        author.setCreateTime(LocalDateTime.now());
        author.setUpdateTime(LocalDateTime.now());
        authorRepository.save(author);
        
        // Test 1: Search by substring of name
        String nameSubstring = authorName.substring(5, Math.min(15, authorName.length()));
        AuthorQueryDTO query1 = new AuthorQueryDTO();
        query1.setKeyword(nameSubstring);
        PageResponse<AuthorVO> result1 = authorService.page(1L, 10L, query1);
        
        assertTrue(result1.getData().getRecords().stream()
            .anyMatch(a -> a.getName().equals(authorName)),
            "Author should be found when searching by name substring: " + nameSubstring);
        
        // Verify matchedField is set
        AuthorVO matchedAuthor = result1.getData().getRecords().stream()
            .filter(a -> a.getName().equals(authorName))
            .findFirst()
            .orElse(null);
        assertNotNull(matchedAuthor);
        assertEquals("name", matchedAuthor.getMatchedField(),
            "matchedField should be 'name' when name matches");
        
        // Test 2: Search by substring of alias
        String aliasSubstring = alias1.substring(5, Math.min(15, alias1.length()));
        AuthorQueryDTO query2 = new AuthorQueryDTO();
        query2.setKeyword(aliasSubstring);
        PageResponse<AuthorVO> result2 = authorService.page(1L, 10L, query2);
        
        assertTrue(result2.getData().getRecords().stream()
            .anyMatch(a -> a.getName().equals(authorName)),
            "Author should be found when searching by alias substring: " + aliasSubstring);
        
        // Verify matchedField is set to alias
        AuthorVO matchedByAlias = result2.getData().getRecords().stream()
            .filter(a -> a.getName().equals(authorName))
            .findFirst()
            .orElse(null);
        assertNotNull(matchedByAlias);
        assertEquals("alias", matchedByAlias.getMatchedField(),
            "matchedField should be 'alias' when alias matches");
        assertNotNull(matchedByAlias.getMatchedAlias(),
            "matchedAlias should be set when alias matches");
    }
    
    private void testWorkFuzzySearch() {
        // Create a work with a unique name and aliases
        String uniqueId = "test" + random.nextInt(1000000);
        String workName = "WorkName_" + uniqueId;
        String alias1 = "WorkAlias1_" + uniqueId;
        String alias2 = "WorkAlias2_" + uniqueId;
        
        Work work = new Work();
        work.setName(workName);
        work.setAliases(List.of(alias1, alias2));
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        workRepository.save(work);
        
        // Test 1: Search by substring of name
        String nameSubstring = workName.substring(4, Math.min(12, workName.length()));
        WorkQueryDTO query1 = new WorkQueryDTO();
        query1.setKeyword(nameSubstring);
        PageResponse<WorkVO> result1 = workService.page(1L, 10L, query1);
        
        assertTrue(result1.getData().getRecords().stream()
            .anyMatch(w -> w.getName().equals(workName)),
            "Work should be found when searching by name substring: " + nameSubstring);
        
        // Verify matchedField is set
        WorkVO matchedWork = result1.getData().getRecords().stream()
            .filter(w -> w.getName().equals(workName))
            .findFirst()
            .orElse(null);
        assertNotNull(matchedWork);
        assertEquals("name", matchedWork.getMatchedField(),
            "matchedField should be 'name' when name matches");
        
        // Test 2: Search by substring of alias
        String aliasSubstring = alias1.substring(4, Math.min(12, alias1.length()));
        WorkQueryDTO query2 = new WorkQueryDTO();
        query2.setKeyword(aliasSubstring);
        PageResponse<WorkVO> result2 = workService.page(1L, 10L, query2);
        
        assertTrue(result2.getData().getRecords().stream()
            .anyMatch(w -> w.getName().equals(workName)),
            "Work should be found when searching by alias substring: " + aliasSubstring);
        
        // Verify matchedField is set to alias
        WorkVO matchedByAlias = result2.getData().getRecords().stream()
            .filter(w -> w.getName().equals(workName))
            .findFirst()
            .orElse(null);
        assertNotNull(matchedByAlias);
        assertEquals("alias", matchedByAlias.getMatchedField(),
            "matchedField should be 'alias' when alias matches");
        assertNotNull(matchedByAlias.getMatchedAlias(),
            "matchedAlias should be set when alias matches");
    }
    
    private void testCharacterFuzzySearch() {
        // Create a work first (required for character)
        String workId = createTestWork();
        
        // Create a character with a unique name and aliases
        String uniqueId = "test" + random.nextInt(1000000);
        String characterName = "CharacterName_" + uniqueId;
        String alias1 = "CharacterAlias1_" + uniqueId;
        String alias2 = "CharacterAlias2_" + uniqueId;
        
        Character character = new Character();
        character.setName(characterName);
        character.setAliases(List.of(alias1, alias2));
        character.setWorkId(workId);
        character.setSpecies("Human");
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        characterRepository.save(character);
        
        // Test 1: Search by substring of name
        String nameSubstring = characterName.substring(9, Math.min(18, characterName.length()));
        CharacterQueryDTO query1 = new CharacterQueryDTO();
        query1.setKeyword(nameSubstring);
        PageResponse<CharacterVO> result1 = characterService.page(1L, 10L, query1);
        
        assertTrue(result1.getData().getRecords().stream()
            .anyMatch(c -> c.getName().equals(characterName)),
            "Character should be found when searching by name substring: " + nameSubstring);
        
        // Verify matchedField is set
        CharacterVO matchedCharacter = result1.getData().getRecords().stream()
            .filter(c -> c.getName().equals(characterName))
            .findFirst()
            .orElse(null);
        assertNotNull(matchedCharacter);
        assertEquals("name", matchedCharacter.getMatchedField(),
            "matchedField should be 'name' when name matches");
        
        // Test 2: Search by substring of alias
        String aliasSubstring = alias1.substring(9, Math.min(18, alias1.length()));
        CharacterQueryDTO query2 = new CharacterQueryDTO();
        query2.setKeyword(aliasSubstring);
        PageResponse<CharacterVO> result2 = characterService.page(1L, 10L, query2);
        
        assertTrue(result2.getData().getRecords().stream()
            .anyMatch(c -> c.getName().equals(characterName)),
            "Character should be found when searching by alias substring: " + aliasSubstring);
        
        // Verify matchedField is set to alias
        CharacterVO matchedByAlias = result2.getData().getRecords().stream()
            .filter(c -> c.getName().equals(characterName))
            .findFirst()
            .orElse(null);
        assertNotNull(matchedByAlias);
        assertEquals("alias", matchedByAlias.getMatchedField(),
            "matchedField should be 'alias' when alias matches");
        assertNotNull(matchedByAlias.getMatchedAlias(),
            "matchedAlias should be set when alias matches");
        
        // Test 3: Filter by workId
        CharacterQueryDTO query3 = new CharacterQueryDTO();
        query3.setWorkId(workId);
        PageResponse<CharacterVO> result3 = characterService.page(1L, 10L, query3);
        
        assertTrue(result3.getData().getRecords().stream()
            .allMatch(c -> c.getWorkId().equals(workId)),
            "All characters should belong to the specified work");
        
        // Test 4: Filter by species
        CharacterQueryDTO query4 = new CharacterQueryDTO();
        query4.setSpecies("Human");
        PageResponse<CharacterVO> result4 = characterService.page(1L, 10L, query4);
        
        assertTrue(result4.getData().getRecords().stream()
            .allMatch(c -> "Human".equals(c.getSpecies())),
            "All characters should have the specified species");
    }
    
    private String createTestWork() {
        String uniqueId = "test" + random.nextInt(1000000);
        Work work = new Work();
        work.setName("TestWork_" + uniqueId);
        work.setAliases(new ArrayList<>());
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        return workRepository.save(work).getId();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 33: 搜索结果分页正确性
     * 
     * 对于任何搜索条件，分页返回的所有页面合并后应该包含所有匹配的实体，且没有重复或遗漏。
     * 
     * **Validates: Requirements 10.6**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-33")
    void searchResultPaginationCorrectness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Test Author pagination
            testAuthorPagination();
            
            // Test Work pagination
            testWorkPagination();
            
            // Test Character pagination
            testCharacterPagination();
            
            // Clean up for next iteration
            authorRepository.deleteAll();
            workRepository.deleteAll();
            characterRepository.deleteAll();
        }
    }
    
    private void testAuthorPagination() {
        // Create multiple authors with a common keyword
        String commonKeyword = "TestAuthor" + random.nextInt(100000);
        int totalAuthors = 15 + random.nextInt(10); // 15-24 authors
        List<String> createdAuthorIds = new ArrayList<>();
        
        for (int i = 0; i < totalAuthors; i++) {
            Author author = new Author();
            author.setName(commonKeyword + "_" + i);
            author.setAliases(List.of("Alias_" + i));
            author.setCreateTime(LocalDateTime.now());
            author.setUpdateTime(LocalDateTime.now());
            createdAuthorIds.add(authorRepository.save(author).getId());
        }
        
        // Query with pagination (page size = 5)
        AuthorQueryDTO query = new AuthorQueryDTO();
        query.setKeyword(commonKeyword);
        
        List<String> allPaginatedIds = new ArrayList<>();
        long pageSize = 5L;
        long currentPage = 1L;
        long totalPages;
        
        do {
            PageResponse<AuthorVO> response = authorService.page(currentPage, pageSize, query);
            List<AuthorVO> pageRecords = response.getData().getRecords();
            
            // Collect IDs from this page
            pageRecords.forEach(author -> allPaginatedIds.add(author.getId()));
            
            totalPages = response.getData().getPages();
            currentPage++;
        } while (currentPage <= totalPages);
        
        // Verify: All created authors are in the paginated results
        assertEquals(totalAuthors, allPaginatedIds.size(),
            "Paginated results should contain all matching authors");
        
        // Verify: No duplicates
        assertEquals(allPaginatedIds.size(), new java.util.HashSet<>(allPaginatedIds).size(),
            "Paginated results should not contain duplicates");
        
        // Verify: All created IDs are present
        assertTrue(allPaginatedIds.containsAll(createdAuthorIds),
            "Paginated results should contain all created author IDs");
    }
    
    private void testWorkPagination() {
        // Create multiple works with a common keyword
        String commonKeyword = "TestWork" + random.nextInt(100000);
        int totalWorks = 12 + random.nextInt(8); // 12-19 works
        List<String> createdWorkIds = new ArrayList<>();
        
        for (int i = 0; i < totalWorks; i++) {
            Work work = new Work();
            work.setName(commonKeyword + "_" + i);
            work.setAliases(List.of("WorkAlias_" + i));
            work.setCreateTime(LocalDateTime.now());
            work.setUpdateTime(LocalDateTime.now());
            createdWorkIds.add(workRepository.save(work).getId());
        }
        
        // Query with pagination (page size = 4)
        WorkQueryDTO query = new WorkQueryDTO();
        query.setKeyword(commonKeyword);
        
        List<String> allPaginatedIds = new ArrayList<>();
        long pageSize = 4L;
        long currentPage = 1L;
        long totalPages;
        
        do {
            PageResponse<WorkVO> response = workService.page(currentPage, pageSize, query);
            List<WorkVO> pageRecords = response.getData().getRecords();
            
            // Collect IDs from this page
            pageRecords.forEach(work -> allPaginatedIds.add(work.getId()));
            
            totalPages = response.getData().getPages();
            currentPage++;
        } while (currentPage <= totalPages);
        
        // Verify: All created works are in the paginated results
        assertEquals(totalWorks, allPaginatedIds.size(),
            "Paginated results should contain all matching works");
        
        // Verify: No duplicates
        assertEquals(allPaginatedIds.size(), new java.util.HashSet<>(allPaginatedIds).size(),
            "Paginated results should not contain duplicates");
        
        // Verify: All created IDs are present
        assertTrue(allPaginatedIds.containsAll(createdWorkIds),
            "Paginated results should contain all created work IDs");
    }
    
    private void testCharacterPagination() {
        // Create a work first
        String workId = createTestWork();
        
        // Create multiple characters with a common keyword
        String commonKeyword = "TestCharacter" + random.nextInt(100000);
        int totalCharacters = 18 + random.nextInt(12); // 18-29 characters
        List<String> createdCharacterIds = new ArrayList<>();
        
        for (int i = 0; i < totalCharacters; i++) {
            Character character = new Character();
            character.setName(commonKeyword + "_" + i);
            character.setAliases(List.of("CharAlias_" + i));
            character.setWorkId(workId);
            character.setSpecies("Human");
            character.setCreateTime(LocalDateTime.now());
            character.setUpdateTime(LocalDateTime.now());
            createdCharacterIds.add(characterRepository.save(character).getId());
        }
        
        // Query with pagination (page size = 6)
        CharacterQueryDTO query = new CharacterQueryDTO();
        query.setKeyword(commonKeyword);
        
        List<String> allPaginatedIds = new ArrayList<>();
        long pageSize = 6L;
        long currentPage = 1L;
        long totalPages;
        
        do {
            PageResponse<CharacterVO> response = characterService.page(currentPage, pageSize, query);
            List<CharacterVO> pageRecords = response.getData().getRecords();
            
            // Collect IDs from this page
            pageRecords.forEach(character -> allPaginatedIds.add(character.getId()));
            
            totalPages = response.getData().getPages();
            currentPage++;
        } while (currentPage <= totalPages);
        
        // Verify: All created characters are in the paginated results
        assertEquals(totalCharacters, allPaginatedIds.size(),
            "Paginated results should contain all matching characters");
        
        // Verify: No duplicates
        assertEquals(allPaginatedIds.size(), new java.util.HashSet<>(allPaginatedIds).size(),
            "Paginated results should not contain duplicates");
        
        // Verify: All created IDs are present
        assertTrue(allPaginatedIds.containsAll(createdCharacterIds),
            "Paginated results should contain all created character IDs");
    }
}
