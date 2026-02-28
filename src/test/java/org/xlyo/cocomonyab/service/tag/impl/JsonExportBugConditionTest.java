package org.xlyo.cocomonyab.service.tag.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.AuthorService;
import org.xlyo.cocomonyab.service.tag.CharacterService;
import org.xlyo.cocomonyab.service.tag.WorkService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Bug Condition Exploration Test for Bug 2: JSON Export Format Contains Java Class Names
 * <p>
 * **Validates: Requirements 1.3, 1.4, 2.3, 2.4**
 * <p>
 * This test is designed to FAIL on unfixed code to confirm the bug exists.
 * When exporting Author, Work, or Character data to JSON, the generated JSON includes
 * Java type information (e.g., ["java.util.ArrayList", [...]] or 
 * ["org.xlyo.cocomonyab.domain.entity.tag.Character", {...}]), which is not standard JSON format
 * and causes import failures.
 * <p>
 * Expected behavior (after fix):
 * - Exported JSON should NOT contain "java.util.ArrayList"
 * - Exported JSON should NOT contain "org.xlyo.cocomonyab.domain.entity.tag" class names
 * - Exported JSON should be valid standard JSON format
 * - Exported JSON should be successfully importable via importFromJson()
 * <p>
 * This test will PASS after the fix is implemented.
 */
@SpringBootTest
class JsonExportBugConditionTest {
    
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
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Clean up test data
        authorRepository.deleteAll();
        workRepository.deleteAll();
        characterRepository.deleteAll();
    }
    
    @Test
    void authorExportShouldNotContainJavaClassNames() {
        // Create test author data
        Author author = new Author();
        author.setName("Test Author");
        author.setAliases(Arrays.asList("Alias1", "Alias2"));
        author.setSignature("Test Signature");
        author.setUrls(Arrays.asList("http://example.com"));
        author.setRemark("Test Remark");
        author.setCreateTime(LocalDateTime.now());
        author.setUpdateTime(LocalDateTime.now());
        authorRepository.save(author);
        
        // Export to JSON
        String json = authorService.exportToJson();
        
        // Verify: JSON should NOT contain Java class names
        assertThat(json)
            .as("Exported JSON should not contain 'java.util.ArrayList' type information")
            .doesNotContain("java.util.ArrayList");
        
        assertThat(json)
            .as("Exported JSON should not contain 'org.xlyo.cocomonyab.domain.entity.tag' package names")
            .doesNotContain("org.xlyo.cocomonyab.domain.entity.tag");
        
        // Verify: JSON should be valid standard JSON (parseable without type information)
        assertThatCode(() -> objectMapper.readTree(json))
            .as("Exported JSON should be valid standard JSON format")
            .doesNotThrowAnyException();
        
        // Verify: Exported JSON should be importable
        assertThatCode(() -> authorService.importFromJson(json))
            .as("Exported JSON should be successfully importable")
            .doesNotThrowAnyException();
    }
    
    @Test
    void workExportShouldNotContainJavaClassNames() {
        // Create test work data
        Work work = new Work();
        work.setName("Test Work");
        work.setAliases(Arrays.asList("Work Alias1", "Work Alias2"));
        work.setUrls(Arrays.asList("http://work.example.com"));
        work.setRemark("Test Work Remark");
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        workRepository.save(work);
        
        // Export to JSON
        String json = workService.exportToJson();
        
        // Verify: JSON should NOT contain Java class names
        assertThat(json)
            .as("Exported JSON should not contain 'java.util.ArrayList' type information")
            .doesNotContain("java.util.ArrayList");
        
        assertThat(json)
            .as("Exported JSON should not contain 'org.xlyo.cocomonyab.domain.entity.tag' package names")
            .doesNotContain("org.xlyo.cocomonyab.domain.entity.tag");
        
        // Verify: JSON should be valid standard JSON (parseable without type information)
        assertThatCode(() -> objectMapper.readTree(json))
            .as("Exported JSON should be valid standard JSON format")
            .doesNotThrowAnyException();
        
        // Verify: Exported JSON should be importable
        assertThatCode(() -> workService.importFromJson(json))
            .as("Exported JSON should be successfully importable")
            .doesNotThrowAnyException();
    }
    
    @Test
    void characterExportShouldNotContainJavaClassNames() {
        // Create test work first (required for character)
        Work work = new Work();
        work.setName("Character Test Work");
        work.setAliases(Arrays.asList());
        work.setUrls(Arrays.asList());
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        work = workRepository.save(work);
        
        // Create test character data
        Character character = new Character();
        character.setName("Test Character");
        character.setAliases(Arrays.asList("Char Alias1", "Char Alias2"));
        character.setWorkId(work.getId());
        character.setSpecies("Human");
        character.setRemark("Test Character Remark");
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        characterRepository.save(character);
        
        // Export to JSON
        String json = characterService.exportToJson();
        
        // Verify: JSON should NOT contain Java class names
        assertThat(json)
            .as("Exported JSON should not contain 'java.util.ArrayList' type information")
            .doesNotContain("java.util.ArrayList");
        
        assertThat(json)
            .as("Exported JSON should not contain 'org.xlyo.cocomonyab.domain.entity.tag.Character' class name")
            .doesNotContain("org.xlyo.cocomonyab.domain.entity.tag.Character");
        
        assertThat(json)
            .as("Exported JSON should not contain any 'org.xlyo.cocomonyab' package names")
            .doesNotContain("org.xlyo.cocomonyab");
        
        // Verify: JSON should be valid standard JSON (parseable without type information)
        assertThatCode(() -> objectMapper.readTree(json))
            .as("Exported JSON should be valid standard JSON format")
            .doesNotThrowAnyException();
        
        // Verify: Exported JSON should be importable
        assertThatCode(() -> characterService.importFromJson(json))
            .as("Exported JSON should be successfully importable")
            .doesNotThrowAnyException();
    }
    
    @Test
    void exportedJsonShouldStartWithStandardArrayBracket() {
        // Create test data for all three types
        Author author = new Author();
        author.setName("Array Test Author");
        author.setAliases(Arrays.asList("Alias"));
        author.setCreateTime(LocalDateTime.now());
        author.setUpdateTime(LocalDateTime.now());
        authorRepository.save(author);
        
        Work work = new Work();
        work.setName("Array Test Work");
        work.setAliases(Arrays.asList("Alias"));
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        workRepository.save(work);
        
        Character character = new Character();
        character.setName("Array Test Character");
        character.setAliases(Arrays.asList("Alias"));
        character.setWorkId(work.getId());
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        characterRepository.save(character);
        
        // Export all three types
        String authorJson = authorService.exportToJson();
        String workJson = workService.exportToJson();
        String characterJson = characterService.exportToJson();
        
        // Verify: JSON should start with standard array bracket, not type information
        assertThat(authorJson.trim())
            .as("Author JSON should start with '[' not '[\"java.util.ArrayList\"'")
            .startsWith("[")
            .doesNotStartWith("[\"java.util.ArrayList\"");
        
        assertThat(workJson.trim())
            .as("Work JSON should start with '[' not '[\"java.util.ArrayList\"'")
            .startsWith("[")
            .doesNotStartWith("[\"java.util.ArrayList\"");
        
        assertThat(characterJson.trim())
            .as("Character JSON should start with '[' not '[\"java.util.ArrayList\"'")
            .startsWith("[")
            .doesNotStartWith("[\"java.util.ArrayList\"");
    }
}
