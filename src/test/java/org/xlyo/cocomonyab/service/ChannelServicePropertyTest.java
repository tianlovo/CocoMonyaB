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
 * ChannelService 业务逻辑的基于属性的测试
 * 测试设计文档中的 Property 3
 * 
 * 注意：使用 JUnit @Test 手动生成属性而不是 jqwik
 * 因为 jqwik 对 Spring 的依赖注入支持不好
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya_test"
})
class ChannelServicePropertyTest {
    
    @Autowired
    private ChannelService channelService;
    
    @Autowired
    private ChannelRepository channelRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // 清理任何现有的测试数据
        channelRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 测试后清理
        channelRepository.deleteAll();
    }
    
    /**
     * Property 3: 重复的 channel ID 拒绝
     * 
     * 对于任何有效的 ChannelCreateDTO，如果数据库中已存在具有相同 channelId 的 channel
     * 尝试创建另一个具有该 channelId 的 channel 应该抛出
     * 带有 ResponseCode.DATA_ALREADY_EXISTS 的 BusinessException
     * 
     * 验证：Requirements 5.4
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 3: Duplicate channel ID rejection")
    void duplicateChannelIdRejection() {
        // 使用随机有效输入运行 100 次迭代
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            String firstUsername = generateValidChannelUsername();
            String firstTitle = generateValidChannelTitle();
            Boolean firstStatus = random.nextBoolean();
            
            // 使用此 channelId 创建第一个 channel
            ChannelCreateDTO firstDTO = new ChannelCreateDTO();
            firstDTO.setChannelId(channelId);
            firstDTO.setChannelUsername(firstUsername);
            firstDTO.setChannelTitle(firstTitle);
            firstDTO.setMonitoringStatus(firstStatus);
            
            ChannelVO firstChannel = channelService.create(firstDTO);
            assertNotNull(firstChannel, "First channel should be created successfully (iteration " + i + ")");
            
            // 尝试创建具有相同 channelId 但其他字段不同的第二个 channel
            String secondUsername = generateValidChannelUsername();
            String secondTitle = generateValidChannelTitle();
            Boolean secondStatus = random.nextBoolean();
            
            ChannelCreateDTO secondDTO = new ChannelCreateDTO();
            secondDTO.setChannelId(channelId); // 相同的 channelId
            secondDTO.setChannelUsername(secondUsername);
            secondDTO.setChannelTitle(secondTitle);
            secondDTO.setMonitoringStatus(secondStatus);
            
            // 验证抛出带有 DATA_ALREADY_EXISTS 的 BusinessException
            BusinessException exception = assertThrows(BusinessException.class, 
                () -> channelService.create(secondDTO),
                "Creating channel with duplicate channelId should throw BusinessException (iteration " + i + ")");
            
            assertEquals(ResponseCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode(),
                "Exception should have ResponseCode.DATA_ALREADY_EXISTS (iteration " + i + ")");
            
            assertTrue(exception.getMessage().contains(channelId.toString()),
                "Exception message should contain the duplicate channelId (iteration " + i + ")");
            
            // 清理此次迭代
            channelRepository.deleteById(firstChannel.getId());
        }
    }
    
    /**
     * Property 4: Channel 创建往返
     * 
     * 对于任何有效的 ChannelCreateDTO，创建一个 channel 然后通过 ID 检索它
     * 应该返回一个具有匹配的 channelId、channelUsername 和 channelTitle 值的 ChannelVO
     * 
     * 验证：Requirements 5.5, 5.6, 8.2
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 4: Channel creation round-trip")
    void channelCreationRoundTrip() {
        // 使用随机有效输入运行 100 次迭代
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            String channelUsername = generateValidChannelUsername();
            String channelTitle = generateValidChannelTitle();
            Boolean monitoringStatus = random.nextBoolean();
            
            // 创建 channel
            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId(channelId);
            createDTO.setChannelUsername(channelUsername);
            createDTO.setChannelTitle(channelTitle);
            createDTO.setMonitoringStatus(monitoringStatus);
            
            ChannelVO createdChannel = channelService.create(createDTO);
            assertNotNull(createdChannel, "Created channel should not be null (iteration " + i + ")");
            assertNotNull(createdChannel.getId(), "Created channel should have an ID (iteration " + i + ")");
            
            // 通过 ID 检索 channel
            ChannelVO retrievedChannel = channelService.getById(createdChannel.getId());
            assertNotNull(retrievedChannel, "Retrieved channel should not be null (iteration " + i + ")");
            
            // 验证往返：所有字段应该匹配
            assertEquals(channelId, retrievedChannel.getChannelId(),
                "Retrieved channelId should match original (iteration " + i + ")");
            assertEquals(channelUsername, retrievedChannel.getChannelUsername(),
                "Retrieved channelUsername should match original (iteration " + i + ")");
            assertEquals(channelTitle, retrievedChannel.getChannelTitle(),
                "Retrieved channelTitle should match original (iteration " + i + ")");
            assertEquals(monitoringStatus, retrievedChannel.getMonitoringStatus(),
                "Retrieved monitoringStatus should match original (iteration " + i + ")");
            
            // 验证 VO 完整性（所有必需字段都存在）
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
            
            // 清理此次迭代
            channelRepository.deleteById(createdChannel.getId());
        }
    }

    /**
     * Property 5: 部分更新字段保留
     *
     * 对于任何现有的 channel 和任何填充了字段子集的 ChannelUpdateDTO
     * 更新 channel 应该保留 DTO 中未包含的所有字段，同时仅更新
     * 指定的字段
     *
     * 验证：Requirements 6.6
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 5: Partial update field preservation")
    void partialUpdateFieldPreservation() {
        // 使用随机有效输入运行 100 次迭代
        for (int i = 0; i < 100; i++) {
            // 创建包含所有字段的初始 channel
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

            // 测试用例 1：仅更新 channelUsername
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

            // 测试用例 2：仅更新 channelTitle
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

            // 测试用例 3：仅更新 monitoringStatus
            Boolean newStatus = !originalStatus; // 切换状态
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

            // 测试用例 4：更新两个字段（username 和 title），保留 status
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

            // 测试用例 5：空更新（未设置字段）- 所有字段应该被保留
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

            // 清理此次迭代
            channelRepository.deleteById(createdChannel.getId());
        }
    }

    
    /**
     * Property 6: Channel 删除从数据库中移除
     *
     * 对于任何现有的 channel，在通过 ID 成功删除它之后，尝试检索
     * 该 channel 应该抛出带有 ResponseCode.DATA_NOT_FOUND 的 BusinessException
     *
     * 验证：Requirements 7.2, 7.4
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 6: Channel deletion removes from database")
    void channelDeletionRemovesFromDatabase() {
        // 使用随机有效输入运行 100 次迭代
        for (int i = 0; i < 100; i++) {
            // 创建一个 channel
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

            // 验证删除前 channel 存在
            ChannelVO beforeDeletion = channelService.getById(channelIdToDelete);
            assertNotNull(beforeDeletion, "Channel should exist before deletion (iteration " + i + ")");

            // 删除 channel
            assertDoesNotThrow(() -> channelService.deleteById(channelIdToDelete),
                "Deletion should not throw exception (iteration " + i + ")");

            // 验证尝试检索已删除的 channel 抛出 DATA_NOT_FOUND
            BusinessException exception = assertThrows(BusinessException.class,
                () -> channelService.getById(channelIdToDelete),
                "Retrieving deleted channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), exception.getCode(),
                "Exception should have ResponseCode.DATA_NOT_FOUND (iteration " + i + ")");

            assertTrue(exception.getMessage().contains(channelIdToDelete),
                "Exception message should contain the deleted channel ID (iteration " + i + ")");

            // 验证 channel 不在 repository 中
            assertFalse(channelRepository.existsById(channelIdToDelete),
                "Channel should not exist in repository after deletion (iteration " + i + ")");
        }
    }

    
    /**
     * Property 7: Channel 检索返回完整数据
     *
     * 对于任何现有的 channel，通过 ID 检索它应该返回一个包含
     * 所有必需字段的 ChannelVO：id、channelId、channelUsername、channelTitle、monitoringStatus
     * createTime 和 updateTime
     *
     * 验证：Requirements 8.2, 8.4
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 7: Channel retrieval returns complete data")
    void channelRetrievalReturnsCompleteData() {
        // 使用随机有效输入运行 100 次迭代
        for (int i = 0; i < 100; i++) {
            // 使用随机有效数据创建一个 channel
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

            // 通过 ID 检索 channel
            ChannelVO retrievedChannel = channelService.getById(createdChannel.getId());
            assertNotNull(retrievedChannel, "Retrieved channel should not be null (iteration " + i + ")");

            // 验证所有必需字段都存在且非空
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

            // 验证字段值与创建的 channel 匹配
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

            // 验证时间戳是合理的（不在未来，不太旧）
            assertNotNull(retrievedChannel.getCreateTime(),
                "CreateTime should not be null (iteration " + i + ")");
            assertNotNull(retrievedChannel.getUpdateTime(),
                "UpdateTime should not be null (iteration " + i + ")");
            assertTrue(retrievedChannel.getCreateTime().isBefore(java.time.LocalDateTime.now().plusSeconds(1)),
                "CreateTime should not be in the future (iteration " + i + ")");
            assertTrue(retrievedChannel.getUpdateTime().isBefore(java.time.LocalDateTime.now().plusSeconds(1)),
                "UpdateTime should not be in the future (iteration " + i + ")");

            // 验证字段类型正确（通过 getter 隐式验证，但验证值是合理的）
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

            // 清理此次迭代
            channelRepository.deleteById(createdChannel.getId());
        }
    }

    
    /**
     * Property 8: 不存在的 channel 操作抛出 DATA_NOT_FOUND
     *
     * 对于任何不存在的 channel ID，尝试检索、更新或删除该 channel
     * 应该抛出带有 ResponseCode.DATA_NOT_FOUND 的 BusinessException
     *
     * 验证：Requirements 6.4, 7.3, 8.3
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 8: Non-existent channel operations throw DATA_NOT_FOUND")
    void nonExistentChannelOperationsThrowDataNotFound() {
        // 使用随机不存在的 ID 运行 100 次迭代
        for (int i = 0; i < 100; i++) {
            // 生成一个随机的不存在的 channel ID（MongoDB ObjectId 格式）
            String nonExistentId = generateNonExistentChannelId();

            // 测试用例 1：检索不存在的 channel
            BusinessException getException = assertThrows(BusinessException.class,
                () -> channelService.getById(nonExistentId),
                "Retrieving non-existent channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), getException.getCode(),
                "Get operation should throw DATA_NOT_FOUND (iteration " + i + ")");
            assertTrue(getException.getMessage().contains(nonExistentId),
                "Get exception message should contain the channel ID (iteration " + i + ")");

            // 测试用例 2：更新不存在的 channel
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

            // 测试用例 3：删除不存在的 channel
            BusinessException deleteException = assertThrows(BusinessException.class,
                () -> channelService.deleteById(nonExistentId),
                "Deleting non-existent channel should throw BusinessException (iteration " + i + ")");

            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), deleteException.getCode(),
                "Delete operation should throw DATA_NOT_FOUND (iteration " + i + ")");
            assertTrue(deleteException.getMessage().contains(nonExistentId),
                "Delete exception message should contain the channel ID (iteration " + i + ")");

            // 验证不存在的 ID 确实不存在于 repository 中
            assertFalse(channelRepository.existsById(nonExistentId),
                "Non-existent ID should not exist in repository (iteration " + i + ")");
        }
    }

    /**
     * 生成一个不存在的 channel ID（MongoDB ObjectId 格式：24 个十六进制字符）
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
     * 生成一个有效的 channel ID（正 Long 值）
     */
    private Long generateValidChannelId() {
        return Math.abs(random.nextLong()) + 1;
    }
    
    /**
     * 生成一个有效的 channel username（1-100 个字符）
     */
    private String generateValidChannelUsername() {
        int length = random.nextInt(100) + 1; // 1 到 100
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成一个有效的 channel title（1-200 个字符）
     */
    private String generateValidChannelTitle() {
        int length = random.nextInt(200) + 1; // 1 到 200
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -_";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 测试分页参数验证
     * 验证无效的分页参数抛出适当的异常
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Pagination Parameter Validation")
    void testPaginationParameterValidation() {
        // 测试用例 1：current < 1 应该抛出异常
        BusinessException currentZeroException = assertThrows(BusinessException.class,
            () -> channelService.page(0L, 10L, null),
            "Page index 0 should throw BusinessException");
        
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), currentZeroException.getCode(),
            "Should throw BAD_REQUEST for current=0");
        assertTrue(currentZeroException.getMessage().contains("页码必须大于等于1"),
            "Exception message should indicate page index must be >= 1");
        
        // 测试用例 2：current < 0 应该抛出异常
        BusinessException currentNegativeException = assertThrows(BusinessException.class,
            () -> channelService.page(-1L, 10L, null),
            "Negative page index should throw BusinessException");
        
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), currentNegativeException.getCode(),
            "Should throw BAD_REQUEST for negative current");
        
        // 测试用例 3：size < 1 应该抛出异常
        BusinessException sizeZeroException = assertThrows(BusinessException.class,
            () -> channelService.page(1L, 0L, null),
            "Page size 0 should throw BusinessException");
        
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), sizeZeroException.getCode(),
            "Should throw BAD_REQUEST for size=0");
        assertTrue(sizeZeroException.getMessage().contains("每页大小必须大于等于1"),
            "Exception message should indicate page size must be >= 1");
        
        // 测试用例 4：size > 100 应该抛出异常
        BusinessException sizeTooLargeException = assertThrows(BusinessException.class,
            () -> channelService.page(1L, 101L, null),
            "Page size > 100 should throw BusinessException");
        
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), sizeTooLargeException.getCode(),
            "Should throw BAD_REQUEST for size > 100");
        assertTrue(sizeTooLargeException.getMessage().contains("每页大小不能超过100"),
            "Exception message should indicate page size cannot exceed 100");
        
        // 测试用例 5：null current 应该抛出异常
        BusinessException currentNullException = assertThrows(BusinessException.class,
            () -> channelService.page(null, 10L, null),
            "Null current should throw BusinessException");
        
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), currentNullException.getCode(),
            "Should throw BAD_REQUEST for null current");
        
        // 测试用例 6：null size 应该抛出异常
        BusinessException sizeNullException = assertThrows(BusinessException.class,
            () -> channelService.page(1L, null, null),
            "Null size should throw BusinessException");
        
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), sizeNullException.getCode(),
            "Should throw BAD_REQUEST for null size");
        
        // 测试用例 7：有效参数不应该抛出异常
        assertDoesNotThrow(() -> channelService.page(1L, 10L, null),
            "Valid pagination parameters should not throw exception");
        
        assertDoesNotThrow(() -> channelService.page(1L, 1L, null),
            "Minimum valid parameters (current=1, size=1) should not throw exception");
        
        assertDoesNotThrow(() -> channelService.page(1L, 100L, null),
            "Maximum valid size (100) should not throw exception");
    }
}
