package org.xlyo.cocomonyab.service.tag.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.domain.entity.tag.Author;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preservation Property Tests for Bug 2: JSON Export Format
 * 
 * These tests verify that non-export functionality continues to work correctly
 * after fixing the JSON export bug. They run on UNFIXED code and should PASS,
 * confirming baseline behavior to preserve.
 * 
 * Key behaviors to preserve:
 * 1. List serialization includes type information (needed for TdApi)
 * 2. ApiResponse serialization works correctly
 * 3. Round-trip serialization/deserialization works
 * 4. All entity fields are preserved during serialization
 */
@SpringBootTest
@ActiveProfiles("test")
class JsonExportPreservationTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Property 1: List serialization with activateDefaultTyping includes type information
     * 
     * This verifies that the current ObjectMapper configuration adds type information
     * to collections, which is the root cause of Bug 2 but is needed for TdApi.
     * This behavior MUST be preserved for non-export scenarios.
     */
    @Test
    void listSerializationIncludesTypeInformation() throws Exception {
        // Create a list of authors
        List<Author> authors = new ArrayList<>();
        Author author = new Author();
        author.setName("Test Author");
        author.setAliases(new ArrayList<>());
        authors.add(author);
        
        // Serialize
        String json = objectMapper.writeValueAsString(authors);
        
        // Verify that JSON contains type information (this is the bug!)
        // On UNFIXED code, this should contain "java.util.ArrayList"
        assertThat(json)
                .as("List serialization should include type information with current config")
                .contains("java.util.ArrayList");
    }

    /**
     * Property 2: ApiResponse serialization works correctly
     * 
     * This test verifies that ApiResponse serialization (used by SystemReadyInterceptor)
     * continues to work correctly.
     */
    @Test
    void apiResponseSerializationWorks() throws Exception {
        // Create a sample ApiResponse
        ApiResponse<String> response = ApiResponse.success("test data");
        
        // Serialize
        String json = objectMapper.writeValueAsString(response);
        
        // Verify it's valid JSON with expected structure
        assertThat(json)
                .as("ApiResponse should serialize to valid JSON")
                .isNotNull()
                .contains("\"code\"")
                .contains("\"data\"");
        
        // Note: Due to activateDefaultTyping, the JSON will contain type information
        // This is expected behavior that we want to preserve for non-export scenarios
    }

    /**
     * Property 3: Round-trip serialization works with type information
     * 
     * This verifies that objects serialized with type information can be
     * correctly deserialized back using the same ObjectMapper.
     */
    @Test
    void roundTripSerializationWorks() throws Exception {
        // Create an Author
        Author originalAuthor = new Author();
        originalAuthor.setName("Test Author");
        originalAuthor.setAliases(new ArrayList<>(List.of("Alias1", "Alias2")));
        originalAuthor.setSignature("Test Signature");
        
        // Serialize the author directly (not as a list)
        String json = objectMapper.writeValueAsString(originalAuthor);
        
        // Deserialize back to Author
        Author deserializedAuthor = objectMapper.readValue(json, Author.class);
        
        // Verify deserialization succeeded and fields are preserved
        assertThat(deserializedAuthor)
                .as("Deserialization should work")
                .isNotNull();
        
        assertThat(deserializedAuthor.getName())
                .as("Name should be preserved")
                .isEqualTo("Test Author");
        
        assertThat(deserializedAuthor.getAliases())
                .as("Aliases should be preserved")
                .containsExactly("Alias1", "Alias2");
    }

    /**
     * Property 4: Entity serialization includes all required fields
     * 
     * This test verifies that when we serialize entities, all required fields are present.
     */
    @Test
    void entitySerializationIncludesAllFields() throws Exception {
        // Create an author with all fields
        Author author = new Author();
        author.setName("Test Author");
        author.setAliases(List.of("Alias1", "Alias2"));
        author.setSignature("Test Signature");
        author.setUrls(List.of("http://example.com"));
        author.setRemark("Test Remark");
        
        // Serialize
        String json = objectMapper.writeValueAsString(author);
        
        // Verify all fields are present
        assertThat(json)
                .as("Serialized author should contain all fields")
                .contains("\"name\"")
                .contains("\"aliases\"")
                .contains("\"signature\"")
                .contains("\"urls\"")
                .contains("\"remark\"");
    }

    /**
     * Property 5: Polymorphic type handling is enabled
     * 
     * This verifies that the ObjectMapper has polymorphic type handling enabled,
     * which is necessary for TdApi but causes the export bug.
     */
    @Test
    void polymorphicTypeHandlingIsEnabled() throws Exception {
        // Create a simple object with a List field
        TestEntity entity = new TestEntity();
        entity.items = new ArrayList<>();
        entity.items.add("item1");
        entity.items.add("item2");
        
        // Serialize
        String json = objectMapper.writeValueAsString(entity);
        
        // Verify type information is included
        assertThat(json)
                .as("Polymorphic type handling should add type information")
                .contains("java.util.ArrayList");
    }
    
    // Test helper class
    static class TestEntity {
        public List<String> items;
    }
}

