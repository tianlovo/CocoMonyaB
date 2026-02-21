package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.ChannelCreateDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.ChannelVO;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ChannelService business logic.
 * Tests Property 3 from the design document.
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
class ChannelServicePropertyTest {
    
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private ChannelRepository channelRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        channelRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        channelRepository.deleteAll();
    }
    
    /**
     * Property 3: Duplicate channel ID rejection
     * 
     * For any valid ChannelCreateDTO, if a channel with the same channelId already exists
     * in the database, attempting to create another channel with that channelId should throw
     * a BusinessException with ResponseCode.DATA_ALREADY_EXISTS.
     * 
     * Validates: Requirements 5.4
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 3: Duplicate channel ID rejection")
    void duplicateChannelIdRejection() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            String firstUsername = generateValidChannelUsername();
            String firstTitle = generateValidChannelTitle();
            Boolean firstStatus = random.nextBoolean();
            
            // Create first channel with this channelId
            ChannelCreateDTO firstDTO = new ChannelCreateDTO();
            firstDTO.setChannelId(channelId);
            firstDTO.setChannelUsername(firstUsername);
            firstDTO.setChannelTitle(firstTitle);
            firstDTO.setMonitoringStatus(firstStatus);
            
            ChannelVO firstChannel = channelService.create(firstDTO);
            assertNotNull(firstChannel, "First channel should be created successfully (iteration " + i + ")");
            
            // Attempt to create second channel with same channelId but different other fields
            String secondUsername = generateValidChannelUsername();
            String secondTitle = generateValidChannelTitle();
            Boolean secondStatus = random.nextBoolean();
            
            ChannelCreateDTO secondDTO = new ChannelCreateDTO();
            secondDTO.setChannelId(channelId); // Same channelId
            secondDTO.setChannelUsername(secondUsername);
            secondDTO.setChannelTitle(secondTitle);
            secondDTO.setMonitoringStatus(secondStatus);
            
            // Verify that BusinessException is thrown with DATA_ALREADY_EXISTS
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> channelService.create(secondDTO),
                "Creating channel with duplicate channelId should throw BusinessException (iteration " + i + ")");
            
            assertEquals(ResponseCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode(),
                "Exception should have ResponseCode.DATA_ALREADY_EXISTS (iteration " + i + ")");
            
            assertTrue(exception.getMessage().contains(channelId.toString()),
                "Exception message should contain the duplicate channelId (iteration " + i + ")");
            
            // Cleanup for this iteration
            channelRepository.deleteById(firstChannel.getId());
        }
    }
    
    /**
     * Property 4: Channel creation round-trip
     * 
     * For any valid ChannelCreateDTO, creating a channel and then retrieving it by ID
     * should return a ChannelVO with matching channelId, channelUsername, and channelTitle values.
     * 
     * Validates: Requirements 5.5, 5.6, 8.2
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 4: Channel creation round-trip")
    void channelCreationRoundTrip() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            String channelUsername = generateValidChannelUsername();
            String channelTitle = generateValidChannelTitle();
            Boolean monitoringStatus = random.nextBoolean();
            
            // Create channel
            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId(channelId);
            createDTO.setChannelUsername(channelUsername);
            createDTO.setChannelTitle(channelTitle);
            createDTO.setMonitoringStatus(monitoringStatus);
            
            ChannelVO createdChannel = channelService.create(createDTO);
            assertNotNull(createdChannel, "Created channel should not be null (iteration " + i + ")");
            assertNotNull(createdChannel.getId(), "Created channel should have an ID (iteration " + i + ")");
            
            // Retrieve channel by ID
            ChannelVO retrievedChannel = channelService.getById(createdChannel.getId());
            assertNotNull(retrievedChannel, "Retrieved channel should not be null (iteration " + i + ")");
            
            // Verify round-trip: all fields should match
            assertEquals(channelId, retrievedChannel.getChannelId(),
                "Retrieved channelId should match original (iteration " + i + ")");
            assertEquals(channelUsername, retrievedChannel.getChannelUsername(),
                "Retrieved channelUsername should match original (iteration " + i + ")");
            assertEquals(channelTitle, retrievedChannel.getChannelTitle(),
                "Retrieved channelTitle should match original (iteration " + i + ")");
            assertEquals(monitoringStatus, retrievedChannel.getMonitoringStatus(),
                "Retrieved monitoringStatus should match original (iteration " + i + ")");
            
            // Verify VO completeness (all required fields present)
            assertNotNull(retrievedChannel.getId(), 
                "Retrieved channel should have id field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getChannelId(), 
                "Retrieved channel should have channelId field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getChannelUsername(), 
                "Retrieved channel should have channelUsername field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getChannelTitle(), 
                "Retrieved channel should have channelTitle field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getMonitoringStatus(), 
                "Retrieved channel should have monitoringStatus field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getCreateTime(), 
                "Retrieved channel should have createTime field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getUpdateTime(), 
                "Retrieved channel should have updateTime field (iteration " + i + ")");
            
            // Cleanup for this iteration
            channelRepository.deleteById(createdChannel.getId());
        }
    }

    /**
     * Property 5: Partial update field preservation
     *
     * For any existing channel and any ChannelUpdateDTO with a subset of fields populated,
     * updating the channel should preserve all fields not included in the DTO while updating
     * only the specified fields.
     *
     * Validates: Requirements 6.6
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 5: Partial update field preservation")
    void partialUpdateFieldPreservation() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create initial channel with all fields
            Long originalChannelId = generateValidChannelId();
            String originalUsername = generateValidChannelUsername();
            String originalTitle = generateValidChannelTitle();
            Boolean originalStatus = random.nextBoolean();

            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId(originalChannelId);
            createDTO.setChannelUsername(originalUsername);
            createDTO.setChannelTitle(originalTitle);
            createDTO.setMonitoringStatus(originalStatus);

            ChannelVO createdChannel = channelService.create(createDTO);
            assertNotNull(createdChannel, "Created channel should not be null (iteration " + i + ")");

            // Test Case 1: Update only channelUsername
            String newUsername = generateValidChannelUsername();
            ChannelUpdateDTO updateUsernameOnly = new ChannelUpdateDTO();
            updateUsernameOnly.setChannelUsername(newUsername);

            ChannelVO afterUsernameUpdate = channelService.update(createdChannel.getId(), updateUsernameOnly);
            assertEquals(newUsername, afterUsernameUpdate.getChannelUsername(),
                "Username should be updated (iteration " + i + ")");
            assertEquals(originalTitle, afterUsernameUpdate.getChannelTitle(),
                "Title should be preserved when updating username only (iteration " + i + ")");
            assertEquals(originalStatus, afterUsernameUpdate.getMonitoringStatus(),
                "Status should be preserved when updating username only (iteration " + i + ")");
            assertEquals(originalChannelId, afterUsernameUpdate.getChannelId(),
                "ChannelId should be preserved (iteration " + i + ")");

            // Test Case 2: Update only channelTitle
            String newTitle = generateValidChannelTitle();
            ChannelUpdateDTO updateTitleOnly = new ChannelUpdateDTO();
            updateTitleOnly.setChannelTitle(newTitle);

            ChannelVO afterTitleUpdate = channelService.update(createdChannel.getId(), updateTitleOnly);
            assertEquals(newTitle, afterTitleUpdate.getChannelTitle(),
                "Title should be updated (iteration " + i + ")");
            assertEquals(newUsername, afterTitleUpdate.getChannelUsername(),
                "Username should be preserved when updating title only (iteration " + i + ")");
            assertEquals(originalStatus, afterTitleUpdate.getMonitoringStatus(),
                "Status should be preserved when updating title only (iteration " + i + ")");
            assertEquals(originalChannelId, afterTitleUpdate.getChannelId(),
                "ChannelId should be preserved (iteration " + i + ")");

            // Test Case 3: Update only monitoringStatus
            Boolean newStatus = !originalStatus; // Toggle status
            ChannelUpdateDTO updateStatusOnly = new ChannelUpdateDTO();
            updateStatusOnly.setMonitoringStatus(newStatus);

            ChannelVO afterStatusUpdate = channelService.update(createdChannel.getId(), updateStatusOnly);
            assertEquals(newStatus, afterStatusUpdate.getMonitoringStatus(),
                "Status should be updated (iteration " + i + ")");
            assertEquals(newUsername, afterStatusUpdate.getChannelUsername(),
                "Username should be preserved when updating status only (iteration " + i + ")");
            assertEquals(newTitle, afterStatusUpdate.getChannelTitle(),
                "Title should be preserved when updating status only (iteration " + i + ")");
            assertEquals(originalChannelId, afterStatusUpdate.getChannelId(),
                "ChannelId should be preserved (iteration " + i + ")");

            // Test Case 4: Update two fields (username and title), preserve status
            String finalUsername = generateValidChannelUsername();
            String finalTitle = generateValidChannelTitle();
            ChannelUpdateDTO updateTwoFields = new ChannelUpdateDTO();
            updateTwoFields.setChannelUsername(finalUsername);
            updateTwoFields.setChannelTitle(finalTitle);

            ChannelVO afterTwoFieldUpdate = channelService.update(createdChannel.getId(), updateTwoFields);
            assertEquals(finalUsername, afterTwoFieldUpdate.getChannelUsername(),
                "Username should be updated (iteration " + i + ")");
            assertEquals(finalTitle, afterTwoFieldUpdate.getChannelTitle(),
                "Title should be updated (iteration " + i + ")");
            assertEquals(newStatus, afterTwoFieldUpdate.getMonitoringStatus(),
                "Status should be preserved when updating username and title (iteration " + i + ")");
            assertEquals(originalChannelId, afterTwoFieldUpdate.getChannelId(),
                "ChannelId should be preserved (iteration " + i + ")");

            // Test Case 5: Empty update (no fields set) - all fields should be preserved
            ChannelUpdateDTO emptyUpdate = new ChannelUpdateDTO();

            ChannelVO afterEmptyUpdate = channelService.update(createdChannel.getId(), emptyUpdate);
            assertEquals(finalUsername, afterEmptyUpdate.getChannelUsername(),
                "Username should be preserved with empty update (iteration " + i + ")");
            assertEquals(finalTitle, afterEmptyUpdate.getChannelTitle(),
                "Title should be preserved with empty update (iteration " + i + ")");
            assertEquals(newStatus, afterEmptyUpdate.getMonitoringStatus(),
                "Status should be preserved with empty update (iteration " + i + ")");
            assertEquals(originalChannelId, afterEmptyUpdate.getChannelId(),
                "ChannelId should be preserved (iteration " + i + ")");

            // Cleanup for this iteration
            channelRepository.deleteById(createdChannel.getId());
        }
    }

    
    /**
     * Property 6: Channel deletion removes from database
     *
     * For any existing channel, after successfully deleting it by ID, attempting to retrieve
     * that channel should throw a BusinessException with ResponseCode.DATA_NOT_FOUND.
     *
     * Validates: Requirements 7.2, 7.4
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 6: Channel deletion removes from database")
    void channelDeletionRemovesFromDatabase() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a channel
            Long channelId = generateValidChannelId();
            String channelUsername = generateValidChannelUsername();
            String channelTitle = generateValidChannelTitle();
            Boolean monitoringStatus = random.nextBoolean();

            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId(channelId);
            createDTO.setChannelUsername(channelUsername);
            createDTO.setChannelTitle(channelTitle);
            createDTO.setMonitoringStatus(monitoringStatus);

            ChannelVO createdChannel = channelService.create(createDTO);
            assertNotNull(createdChannel, "Created channel should not be null (iteration " + i + ")");
            String channelIdToDelete = createdChannel.getId();

            // Verify channel exists before deletion
            ChannelVO beforeDeletion = channelService.getById(channelIdToDelete);
            assertNotNull(beforeDeletion, "Channel should exist before deletion (iteration " + i + ")");

            // Delete the channel
            assertDoesNotThrow(() -> channelService.deleteById(channelIdToDelete),
                "Deletion should not throw exception (iteration " + i + ")");

            // Verify that attempting to retrieve the deleted channel throws DATA_NOT_FOUND
            BusinessException exception = assertThrows(BusinessException.class,
                () -> channelService.getById(channelIdToDelete),
                "Retrieving deleted channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), exception.getCode(),
                "Exception should have ResponseCode.DATA_NOT_FOUND (iteration " + i + ")");

            assertTrue(exception.getMessage().contains(channelIdToDelete),
                "Exception message should contain the deleted channel ID (iteration " + i + ")");

            // Verify channel is not in repository
            assertFalse(channelRepository.existsById(channelIdToDelete),
                "Channel should not exist in repository after deletion (iteration " + i + ")");
        }
    }

    
    /**
     * Property 7: Channel retrieval returns complete data
     *
     * For any existing channel, retrieving it by ID should return a ChannelVO containing
     * all required fields: id, channelId, channelUsername, channelTitle, monitoringStatus,
     * createTime, and updateTime.
     *
     * Validates: Requirements 8.2, 8.4
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 7: Channel retrieval returns complete data")
    void channelRetrievalReturnsCompleteData() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a channel with random valid data
            Long channelId = generateValidChannelId();
            String channelUsername = generateValidChannelUsername();
            String channelTitle = generateValidChannelTitle();
            Boolean monitoringStatus = random.nextBoolean();

            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId(channelId);
            createDTO.setChannelUsername(channelUsername);
            createDTO.setChannelTitle(channelTitle);
            createDTO.setMonitoringStatus(monitoringStatus);

            ChannelVO createdChannel = channelService.create(createDTO);
            assertNotNull(createdChannel, "Created channel should not be null (iteration " + i + ")");

            // Retrieve the channel by ID
            ChannelVO retrievedChannel = channelService.getById(createdChannel.getId());
            assertNotNull(retrievedChannel, "Retrieved channel should not be null (iteration " + i + ")");

            // Verify all required fields are present and non-null
            assertNotNull(retrievedChannel.getId(),
                "Retrieved channel must have non-null id field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getChannelId(),
                "Retrieved channel must have non-null channelId field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getChannelUsername(),
                "Retrieved channel must have non-null channelUsername field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getChannelTitle(),
                "Retrieved channel must have non-null channelTitle field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getMonitoringStatus(),
                "Retrieved channel must have non-null monitoringStatus field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getCreateTime(),
                "Retrieved channel must have non-null createTime field (iteration " + i + ")");
            assertNotNull(retrievedChannel.getUpdateTime(),
                "Retrieved channel must have non-null updateTime field (iteration " + i + ")");

            // Verify field values match the created channel
            assertEquals(createdChannel.getId(), retrievedChannel.getId(),
                "Retrieved id should match created channel (iteration " + i + ")");
            assertEquals(channelId, retrievedChannel.getChannelId(),
                "Retrieved channelId should match original (iteration " + i + ")");
            assertEquals(channelUsername, retrievedChannel.getChannelUsername(),
                "Retrieved channelUsername should match original (iteration " + i + ")");
            assertEquals(channelTitle, retrievedChannel.getChannelTitle(),
                "Retrieved channelTitle should match original (iteration " + i + ")");
            assertEquals(monitoringStatus, retrievedChannel.getMonitoringStatus(),
                "Retrieved monitoringStatus should match original (iteration " + i + ")");

            // Verify timestamps are reasonable (not in the future, not too old)
            assertNotNull(retrievedChannel.getCreateTime(),
                "CreateTime should not be null (iteration " + i + ")");
            assertNotNull(retrievedChannel.getUpdateTime(),
                "UpdateTime should not be null (iteration " + i + ")");
            assertTrue(retrievedChannel.getCreateTime().isBefore(java.time.LocalDateTime.now().plusSeconds(1)),
                "CreateTime should not be in the future (iteration " + i + ")");
            assertTrue(retrievedChannel.getUpdateTime().isBefore(java.time.LocalDateTime.now().plusSeconds(1)),
                "UpdateTime should not be in the future (iteration " + i + ")");

            // Verify field types are correct (implicit through getters, but verify values are sensible)
            assertTrue(retrievedChannel.getId().length() > 0,
                "ID should be a non-empty string (iteration " + i + ")");
            assertTrue(retrievedChannel.getChannelId() > 0,
                "ChannelId should be positive (iteration " + i + ")");
            assertTrue(retrievedChannel.getChannelUsername().length() >= 1 && 
                       retrievedChannel.getChannelUsername().length() <= 100,
                "ChannelUsername should be within valid length range (iteration " + i + ")");
            assertTrue(retrievedChannel.getChannelTitle().length() >= 1 && 
                       retrievedChannel.getChannelTitle().length() <= 200,
                "ChannelTitle should be within valid length range (iteration " + i + ")");

            // Cleanup for this iteration
            channelRepository.deleteById(createdChannel.getId());
        }
    }

    
    /**
     * Property 8: Non-existent channel operations throw DATA_NOT_FOUND
     *
     * For any non-existent channel ID, attempting to retrieve, update, or delete that channel
     * should throw a BusinessException with ResponseCode.DATA_NOT_FOUND.
     *
     * Validates: Requirements 6.4, 7.3, 8.3
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 8: Non-existent channel operations throw DATA_NOT_FOUND")
    void nonExistentChannelOperationsThrowDataNotFound() {
        // Run 100 iterations with random non-existent IDs
        for (int i = 0; i < 100; i++) {
            // Generate a random non-existent channel ID (MongoDB ObjectId format)
            String nonExistentId = generateNonExistentChannelId();

            // Test Case 1: Retrieve non-existent channel
            BusinessException getException = assertThrows(BusinessException.class,
                () -> channelService.getById(nonExistentId),
                "Retrieving non-existent channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), getException.getCode(),
                "Get operation should throw DATA_NOT_FOUND (iteration " + i + ")");
            assertTrue(getException.getMessage().contains(nonExistentId),
                "Get exception message should contain the channel ID (iteration " + i + ")");

            // Test Case 2: Update non-existent channel
            ChannelUpdateDTO updateDTO = new ChannelUpdateDTO();
            updateDTO.setChannelUsername(generateValidChannelUsername());
            updateDTO.setChannelTitle(generateValidChannelTitle());
            updateDTO.setMonitoringStatus(random.nextBoolean());

            BusinessException updateException = assertThrows(BusinessException.class,
                () -> channelService.update(nonExistentId, updateDTO),
                "Updating non-existent channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), updateException.getCode(),
                "Update operation should throw DATA_NOT_FOUND (iteration " + i + ")");
            assertTrue(updateException.getMessage().contains(nonExistentId),
                "Update exception message should contain the channel ID (iteration " + i + ")");

            // Test Case 3: Delete non-existent channel
            BusinessException deleteException = assertThrows(BusinessException.class,
                () -> channelService.deleteById(nonExistentId),
                "Deleting non-existent channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), deleteException.getCode(),
                "Delete operation should throw DATA_NOT_FOUND (iteration " + i + ")");
            assertTrue(deleteException.getMessage().contains(nonExistentId),
                "Delete exception message should contain the channel ID (iteration " + i + ")");

            // Verify that the non-existent ID truly doesn't exist in the repository
            assertFalse(channelRepository.existsById(nonExistentId),
                "Non-existent ID should not exist in repository (iteration " + i + ")");
        }
    }

    /**
     * Generates a non-existent channel ID (MongoDB ObjectId format: 24 hex characters)
     */
    private String generateNonExistentChannelId() {
        StringBuilder sb = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < 24; i++) {
            sb.append(hexChars.charAt(random.nextInt(hexChars.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a valid channel ID (positive Long value)
     */
    private Long generateValidChannelId() {
        return Math.abs(random.nextLong()) + 1;
    }
    
    /**
     * Generates a valid channel username (1-100 characters)
     */
    private String generateValidChannelUsername() {
        int length = random.nextInt(100) + 1; // 1 to 100
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generates a valid channel title (1-200 characters)
     */
    private String generateValidChannelTitle() {
        int length = random.nextInt(200) + 1; // 1 to 200
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -_";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
