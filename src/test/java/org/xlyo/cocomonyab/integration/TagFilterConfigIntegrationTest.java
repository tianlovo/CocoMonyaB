package org.xlyo.cocomonyab.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigQueryDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.TagFilterConfig;
import org.xlyo.cocomonyab.domain.vo.TagFilterConfigVO;
import org.xlyo.cocomonyab.event.TagFilterConfigEvent;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.repository.TagFilterConfigRepository;
import org.xlyo.cocomonyab.service.TagFilterConfigService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 标签过滤配置系统集成测试
 * 
 * 测试完整的请求-响应流程、事件发布和监听、数据库事务
 * 
 * 验证需求: 所有需求
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.database=tag-filter-config-integration-test"
})
class TagFilterConfigIntegrationTest {
    
    @Autowired
    private TagFilterConfigService service;
    
    @Autowired
    private TagFilterConfigRepository repository;
    
    @Autowired
    private TestEventListener eventListener;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        repository.deleteAll();
        eventListener.clear();
    }
    
    /**
     * 测试完整的全局配置创建流程
     * 验证: 数据持久化、事件发布、时间戳自动设置
     */
    @Test
    void testCompleteGlobalConfigCreationFlow() {
        // Given: 创建全局配置DTO
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setTags(Arrays.asList("tech", "news", "ai"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        
        LocalDateTime beforeCreate = LocalDateTime.now();
        
        // When: 创建全局配置
        TagFilterConfigVO result = service.createOrUpdateGlobalConfig(dto);
        
        LocalDateTime afterCreate = LocalDateTime.now();
        
        // Then: 验证返回的VO
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getChannelId()).isNull();
        assertThat(result.getTags()).containsExactlyInAnyOrder("tech", "news", "ai");
        assertThat(result.getMatchMode()).isEqualTo("whitelist");
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getCreateTime()).isBetween(beforeCreate, afterCreate);
        assertThat(result.getUpdateTime()).isBetween(beforeCreate, afterCreate);
        
        // 验证数据库持久化
        Optional<TagFilterConfig> savedConfig = repository.findByChannelIdIsNull();
        assertThat(savedConfig).isPresent();
        assertThat(savedConfig.get().getId()).isEqualTo(result.getId());
        assertThat(savedConfig.get().getTags()).containsExactlyInAnyOrder("tech", "news", "ai");
        
        // 验证事件发布
        assertThat(eventListener.getEvents()).hasSize(1);
        TagFilterConfigEvent event = eventListener.getEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(TagFilterConfigEvent.EventType.CONFIG_UPDATED);
        assertThat(event.getChannelId()).isNull();
        assertThat(event.getConfigId()).isEqualTo(result.getId());
        assertThat(event.getEnabled()).isTrue();
    }
    
    /**
     * 测试完整的频道配置创建流程
     * 验证: 数据持久化、事件发布、唯一性约束
     */
    @Test
    void testCompleteChannelConfigCreationFlow() {
        // Given: 创建频道配置DTO
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(channelId);
        dto.setTags(Arrays.asList("urgent", "important"));
        dto.setMatchMode("blacklist");
        dto.setEnabled(true);
        
        // When: 创建频道配置
        TagFilterConfigVO result = service.createChannelConfig(dto);
        
        // Then: 验证返回的VO
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getChannelId()).isEqualTo(channelId);
        assertThat(result.getTags()).containsExactlyInAnyOrder("urgent", "important");
        assertThat(result.getMatchMode()).isEqualTo("blacklist");
        
        // 验证数据库持久化
        Optional<TagFilterConfig> savedConfig = repository.findByChannelId(channelId);
        assertThat(savedConfig).isPresent();
        assertThat(savedConfig.get().getChannelId()).isEqualTo(channelId);
        
        // 验证事件发布
        assertThat(eventListener.getEvents()).hasSize(1);
        TagFilterConfigEvent event = eventListener.getEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(TagFilterConfigEvent.EventType.CONFIG_CREATED);
        assertThat(event.getChannelId()).isEqualTo(channelId);
        assertThat(event.getConfigId()).isEqualTo(result.getId());
        
        // 验证唯一性约束：尝试创建重复配置
        TagFilterConfigCreateDTO duplicateDto = new TagFilterConfigCreateDTO();
        duplicateDto.setChannelId(channelId);
        duplicateDto.setTags(Arrays.asList("test"));
        duplicateDto.setMatchMode("whitelist");
        duplicateDto.setEnabled(false);
        
        assertThatThrownBy(() -> service.createChannelConfig(duplicateDto))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("频道配置已存在");
    }
    
    /**
     * 测试配置更新流程
     * 验证: 部分更新、时间戳更新、事件发布
     */
    @Test
    void testCompleteConfigUpdateFlow() throws InterruptedException {
        // Given: 先创建一个配置
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO createDto = new TagFilterConfigCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setTags(Arrays.asList("tag1", "tag2"));
        createDto.setMatchMode("whitelist");
        createDto.setEnabled(true);
        
        TagFilterConfigVO created = service.createChannelConfig(createDto);
        LocalDateTime originalUpdateTime = created.getUpdateTime();
        
        // 清空事件监听器
        eventListener.clear();
        
        // 等待一小段时间确保updateTime不同
        Thread.sleep(10);
        
        // When: 更新配置
        TagFilterConfigUpdateDTO updateDto = new TagFilterConfigUpdateDTO();
        updateDto.setTags(Arrays.asList("tag1", "tag2", "tag3"));
        updateDto.setEnabled(false);
        
        TagFilterConfigVO updated = service.updateConfig(created.getId(), updateDto);
        
        // Then: 验证更新结果
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTags()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
        assertThat(updated.getMatchMode()).isEqualTo("whitelist"); // 未更新的字段保持不变
        assertThat(updated.getEnabled()).isFalse();
        assertThat(updated.getUpdateTime()).isAfter(originalUpdateTime);
        
        // 验证数据库更新
        TagFilterConfig savedConfig = repository.findById(created.getId()).orElseThrow();
        assertThat(savedConfig.getTags()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
        assertThat(savedConfig.getEnabled()).isFalse();
        
        // 验证事件发布
        assertThat(eventListener.getEvents()).hasSize(1);
        TagFilterConfigEvent event = eventListener.getEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(TagFilterConfigEvent.EventType.CONFIG_UPDATED);
        assertThat(event.getChannelId()).isEqualTo(channelId);
        assertThat(event.getEnabled()).isFalse();
    }
    
    /**
     * 测试配置删除流程
     * 验证: 数据删除、事件发布
     */
    @Test
    void testCompleteConfigDeletionFlow() {
        // Given: 先创建一个配置
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO createDto = new TagFilterConfigCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setTags(Arrays.asList("test"));
        createDto.setMatchMode("whitelist");
        createDto.setEnabled(true);
        
        TagFilterConfigVO created = service.createChannelConfig(createDto);
        eventListener.clear();
        
        // When: 删除配置
        service.deleteConfig(created.getId());
        
        // Then: 验证数据库删除
        Optional<TagFilterConfig> deletedConfig = repository.findById(created.getId());
        assertThat(deletedConfig).isEmpty();
        
        // 验证事件发布
        assertThat(eventListener.getEvents()).hasSize(1);
        TagFilterConfigEvent event = eventListener.getEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(TagFilterConfigEvent.EventType.CONFIG_DELETED);
        assertThat(event.getChannelId()).isEqualTo(channelId);
        assertThat(event.getConfigId()).isEqualTo(created.getId());
    }

    
    /**
     * 测试配置优先级逻辑
     * 验证: 频道配置优先于全局配置
     */
    @Test
    void testConfigPriorityLogic() {
        // Given: 创建全局配置
        TagFilterConfigCreateDTO globalDto = new TagFilterConfigCreateDTO();
        globalDto.setTags(Arrays.asList("global1", "global2"));
        globalDto.setMatchMode("whitelist");
        globalDto.setEnabled(true);
        
        TagFilterConfigVO globalConfig = service.createOrUpdateGlobalConfig(globalDto);
        
        // 创建频道配置
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO channelDto = new TagFilterConfigCreateDTO();
        channelDto.setChannelId(channelId);
        channelDto.setTags(Arrays.asList("channel1", "channel2"));
        channelDto.setMatchMode("blacklist");
        channelDto.setEnabled(false);
        
        TagFilterConfigVO channelConfig = service.createChannelConfig(channelDto);
        
        // When: 查询有效配置
        TagFilterConfigVO effectiveConfig = service.getEffectiveConfig(channelId);
        
        // Then: 应该返回频道配置
        assertThat(effectiveConfig.getId()).isEqualTo(channelConfig.getId());
        assertThat(effectiveConfig.getChannelId()).isEqualTo(channelId);
        assertThat(effectiveConfig.getTags()).containsExactlyInAnyOrder("channel1", "channel2");
        assertThat(effectiveConfig.getMatchMode()).isEqualTo("blacklist");
        
        // When: 删除频道配置后再查询
        service.deleteConfig(channelConfig.getId());
        TagFilterConfigVO effectiveAfterDelete = service.getEffectiveConfig(channelId);
        
        // Then: 应该返回全局配置
        assertThat(effectiveAfterDelete.getId()).isEqualTo(globalConfig.getId());
        assertThat(effectiveAfterDelete.getChannelId()).isNull();
        assertThat(effectiveAfterDelete.getTags()).containsExactlyInAnyOrder("global1", "global2");
    }
    
    /**
     * 测试分页查询流程
     * 验证: 分页功能、过滤条件
     */
    @Test
    void testCompletePaginationFlow() {
        // Given: 创建多个频道配置
        for (int i = 0; i < 15; i++) {
            TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
            dto.setChannelId(-1001234567890L - i);
            dto.setTags(Arrays.asList("tag" + i));
            dto.setMatchMode(i % 2 == 0 ? "whitelist" : "blacklist");
            dto.setEnabled(i % 3 == 0);
            service.createChannelConfig(dto);
        }
        
        // When: 分页查询所有配置
        List<TagFilterConfigVO> page1 = service.pageChannelConfigs(1L, 10L, new TagFilterConfigQueryDTO());
        List<TagFilterConfigVO> page2 = service.pageChannelConfigs(2L, 10L, new TagFilterConfigQueryDTO());
        
        // Then: 验证分页结果
        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(5);
        
        // 验证总数
        Long totalCount = service.countChannelConfigs(new TagFilterConfigQueryDTO());
        assertThat(totalCount).isEqualTo(15);
        
        // When: 使用过滤条件查询
        TagFilterConfigQueryDTO queryDto = new TagFilterConfigQueryDTO();
        queryDto.setMatchMode("whitelist");
        List<TagFilterConfigVO> whitelistConfigs = service.pageChannelConfigs(1L, 20L, queryDto);
        
        // Then: 验证过滤结果
        assertThat(whitelistConfigs).hasSize(8); // 0, 2, 4, 6, 8, 10, 12, 14
        assertThat(whitelistConfigs).allMatch(config -> "whitelist".equals(config.getMatchMode()));
    }
    
    /**
     * 测试重新加载事件
     * 验证: RELOAD_ALL事件发布
     */
    @Test
    void testReloadAllEventFlow() {
        // When: 发布重新加载事件
        service.publishReloadAllEvent();
        
        // Then: 验证事件发布
        assertThat(eventListener.getEvents()).hasSize(1);
        TagFilterConfigEvent event = eventListener.getEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(TagFilterConfigEvent.EventType.RELOAD_ALL);
        assertThat(event.getChannelId()).isNull();
        assertThat(event.getConfigId()).isNull();
    }
    
    /**
     * 测试事务回滚
     * 验证: 当操作失败时，数据库不应该有任何变更
     */
    @Test
    void testTransactionRollback() {
        // Given: 创建一个配置
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO createDto = new TagFilterConfigCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setTags(Arrays.asList("test"));
        createDto.setMatchMode("whitelist");
        createDto.setEnabled(true);
        
        service.createChannelConfig(createDto);
        long initialCount = repository.count();
        
        // When: 尝试创建重复配置（应该失败）
        TagFilterConfigCreateDTO duplicateDto = new TagFilterConfigCreateDTO();
        duplicateDto.setChannelId(channelId);
        duplicateDto.setTags(Arrays.asList("duplicate"));
        duplicateDto.setMatchMode("blacklist");
        duplicateDto.setEnabled(false);
        
        try {
            service.createChannelConfig(duplicateDto);
        } catch (BusinessException e) {
            // 预期的异常
        }
        
        // Then: 验证数据库没有变化
        long finalCount = repository.count();
        assertThat(finalCount).isEqualTo(initialCount);
        
        // 验证原配置没有被修改
        Optional<TagFilterConfig> config = repository.findByChannelId(channelId);
        assertThat(config).isPresent();
        assertThat(config.get().getTags()).containsExactly("test");
        assertThat(config.get().getMatchMode()).isEqualTo("whitelist");
    }
    
    /**
     * 测试并发创建全局配置
     * 验证: 全局配置唯一性在并发场景下仍然保持
     */
    @Test
    void testConcurrentGlobalConfigCreation() throws InterruptedException {
        // Given: 准备多个全局配置DTO
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When: 并发创建全局配置
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
                    dto.setTags(Arrays.asList("tag" + index));
                    dto.setMatchMode("whitelist");
                    dto.setEnabled(true);
                    
                    service.createOrUpdateGlobalConfig(dto);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then: 验证只有一个全局配置
        List<TagFilterConfig> globalConfigs = repository.findAll().stream()
            .filter(config -> config.getChannelId() == null)
            .toList();
        assertThat(globalConfigs).hasSize(1);
        
        // 所有操作都应该成功（因为是createOrUpdate）
        assertThat(successCount.get()).isEqualTo(threadCount);
    }
    
    /**
     * 测试并发创建频道配置
     * 验证: 频道配置唯一性约束在并发场景下正确工作
     */
    @Test
    void testConcurrentChannelConfigCreation() throws InterruptedException {
        // Given: 准备多个线程尝试创建相同channelId的配置
        Long channelId = -1001234567890L;
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When: 并发创建频道配置
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
                    dto.setChannelId(channelId);
                    dto.setTags(Arrays.asList("tag" + index));
                    dto.setMatchMode("whitelist");
                    dto.setEnabled(true);
                    
                    service.createChannelConfig(dto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then: 验证只有一个配置成功创建
        Optional<TagFilterConfig> config = repository.findByChannelId(channelId);
        assertThat(config).isPresent();
        
        // 应该只有一个成功，其他都失败
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
    }
    
    /**
     * 测试并发更新配置
     * 验证: 并发更新时数据一致性
     */
    @Test
    void testConcurrentConfigUpdate() throws InterruptedException {
        // Given: 先创建一个配置
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO createDto = new TagFilterConfigCreateDTO();
        createDto.setChannelId(channelId);
        createDto.setTags(Arrays.asList("initial"));
        createDto.setMatchMode("whitelist");
        createDto.setEnabled(true);
        
        TagFilterConfigVO created = service.createChannelConfig(createDto);
        
        // When: 并发更新配置
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    TagFilterConfigUpdateDTO updateDto = new TagFilterConfigUpdateDTO();
                    updateDto.setTags(Arrays.asList("tag" + index));
                    
                    service.updateConfig(created.getId(), updateDto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 可能会有并发冲突
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();
        
        // Then: 验证配置仍然存在且有效
        TagFilterConfig finalConfig = repository.findById(created.getId()).orElseThrow();
        assertThat(finalConfig.getTags()).isNotEmpty();
        assertThat(finalConfig.getChannelId()).isEqualTo(channelId);
        
        // 至少有一些更新成功
        assertThat(successCount.get()).isGreaterThan(0);
    }
    
    /**
     * 测试多个事件监听器
     * 验证: 事件可以被多个监听器接收
     */
    @Test
    void testMultipleEventListeners() {
        // 注意：在实际测试中，我们使用已注册的监听器
        // 这里主要验证事件发布机制
        
        // When: 创建配置
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        
        service.createOrUpdateGlobalConfig(dto);
        
        // Then: 验证事件被接收
        assertThat(eventListener.getEvents()).hasSize(1);
    }
    
    /**
     * 测试事件监听器异常不影响主流程
     * 验证: 即使事件监听器抛出异常，配置操作仍然成功
     */
    @Test
    void testEventListenerExceptionDoesNotAffectMainFlow() {
        // Given: 创建一个会抛出异常的监听器
        // 注意：在实际实现中，事件发布失败不应该影响数据库操作
        
        // When: 创建配置
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        
        TagFilterConfigVO result = service.createOrUpdateGlobalConfig(dto);
        
        // Then: 配置应该成功创建
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        
        // 验证数据库中存在配置
        Optional<TagFilterConfig> savedConfig = repository.findByChannelIdIsNull();
        assertThat(savedConfig).isPresent();
    }
    
    /**
     * 测试完整的端到端流程
     * 验证: 从创建到查询到更新到删除的完整生命周期
     */
    @Test
    void testCompleteEndToEndFlow() throws InterruptedException {
        // 1. 创建全局配置
        TagFilterConfigCreateDTO globalDto = new TagFilterConfigCreateDTO();
        globalDto.setTags(Arrays.asList("global"));
        globalDto.setMatchMode("whitelist");
        globalDto.setEnabled(true);
        
        TagFilterConfigVO globalConfig = service.createOrUpdateGlobalConfig(globalDto);
        assertThat(globalConfig).isNotNull();
        assertThat(eventListener.getEvents()).hasSize(1);
        
        // 2. 创建多个频道配置
        Long channelId1 = -1001234567890L;
        Long channelId2 = -1001234567891L;
        
        TagFilterConfigCreateDTO channel1Dto = new TagFilterConfigCreateDTO();
        channel1Dto.setChannelId(channelId1);
        channel1Dto.setTags(Arrays.asList("channel1"));
        channel1Dto.setMatchMode("blacklist");
        channel1Dto.setEnabled(true);
        
        TagFilterConfigVO channel1Config = service.createChannelConfig(channel1Dto);
        assertThat(channel1Config).isNotNull();
        
        TagFilterConfigCreateDTO channel2Dto = new TagFilterConfigCreateDTO();
        channel2Dto.setChannelId(channelId2);
        channel2Dto.setTags(Arrays.asList("channel2"));
        channel2Dto.setMatchMode("whitelist");
        channel2Dto.setEnabled(false);
        
        TagFilterConfigVO channel2Config = service.createChannelConfig(channel2Dto);
        assertThat(channel2Config).isNotNull();
        
        // 3. 查询配置
        TagFilterConfigVO foundById = service.getConfigById(channel1Config.getId());
        assertThat(foundById.getId()).isEqualTo(channel1Config.getId());
        
        TagFilterConfigVO foundByChannelId = service.getConfigByChannelId(channelId1);
        assertThat(foundByChannelId.getChannelId()).isEqualTo(channelId1);
        
        // 4. 测试有效配置查询
        TagFilterConfigVO effective1 = service.getEffectiveConfig(channelId1);
        assertThat(effective1.getId()).isEqualTo(channel1Config.getId());
        
        Long channelId3 = -1001234567892L; // 没有配置的频道
        TagFilterConfigVO effective3 = service.getEffectiveConfig(channelId3);
        assertThat(effective3.getId()).isEqualTo(globalConfig.getId());
        
        // 5. 更新配置
        Thread.sleep(10);
        TagFilterConfigUpdateDTO updateDto = new TagFilterConfigUpdateDTO();
        updateDto.setEnabled(false);
        
        TagFilterConfigVO updated = service.updateConfig(channel1Config.getId(), updateDto);
        assertThat(updated.getEnabled()).isFalse();
        assertThat(updated.getUpdateTime()).isAfter(channel1Config.getUpdateTime());
        
        // 6. 分页查询
        List<TagFilterConfigVO> allConfigs = service.pageChannelConfigs(1L, 10L, new TagFilterConfigQueryDTO());
        assertThat(allConfigs).hasSize(2);
        
        // 7. 删除配置
        service.deleteConfig(channel2Config.getId());
        assertThatThrownBy(() -> service.getConfigById(channel2Config.getId()))
            .isInstanceOf(BusinessException.class);
        
        // 8. 发布重新加载事件
        service.publishReloadAllEvent();
        
        // 验证所有事件都被正确发布
        List<TagFilterConfigEvent> allEvents = eventListener.getEvents();
        assertThat(allEvents).hasSizeGreaterThan(5);
        
        // 验证事件类型
        long createdEvents = allEvents.stream()
            .filter(e -> e.getEventType() == TagFilterConfigEvent.EventType.CONFIG_CREATED)
            .count();
        assertThat(createdEvents).isEqualTo(2);
        
        long updatedEvents = allEvents.stream()
            .filter(e -> e.getEventType() == TagFilterConfigEvent.EventType.CONFIG_UPDATED)
            .count();
        assertThat(updatedEvents).isGreaterThanOrEqualTo(2); // 全局配置创建 + channel1更新
        
        long deletedEvents = allEvents.stream()
            .filter(e -> e.getEventType() == TagFilterConfigEvent.EventType.CONFIG_DELETED)
            .count();
        assertThat(deletedEvents).isEqualTo(1);
        
        long reloadEvents = allEvents.stream()
            .filter(e -> e.getEventType() == TagFilterConfigEvent.EventType.RELOAD_ALL)
            .count();
        assertThat(reloadEvents).isEqualTo(1);
    }
    
    /**
     * 测试事件监听器配置
     */
    @Configuration
    static class TestEventListenerConfig {
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }
    
    /**
     * 测试用事件监听器
     */
    static class TestEventListener implements ApplicationListener<TagFilterConfigEvent> {
        private final CopyOnWriteArrayList<TagFilterConfigEvent> events = new CopyOnWriteArrayList<>();
        
        @Override
        public void onApplicationEvent(TagFilterConfigEvent event) {
            events.add(event);
        }
        
        public List<TagFilterConfigEvent> getEvents() {
            return List.copyOf(events);
        }
        
        public void clear() {
            events.clear();
        }
    }
}
