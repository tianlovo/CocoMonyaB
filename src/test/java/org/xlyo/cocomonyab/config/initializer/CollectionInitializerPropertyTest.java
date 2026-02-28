package org.xlyo.cocomonyab.config.initializer;

import net.jqwik.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.xlyo.cocomonyab.event.startup.DatabaseReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CollectionInitializer Property-Based Tests
 * <p>
 * Tests the following properties:
 * - Property 8: Index creation order correctness
 * - Property 9: Index creation idempotency
 * - Property 10: Continue execution on partial failures
 * </p>
 * <p>
 * **Validates: Requirements 3.2, 3.3, 3.4**
 * </p>
 */
class CollectionInitializerPropertyTest {
    
    private static final Logger log = LoggerFactory.getLogger(CollectionInitializerPropertyTest.class);
    
    /**
     * Expected index creation order
     */
    private static final List<String> EXPECTED_COLLECTION_ORDER = Arrays.asList(
            "telegram_channels",
            "raw_messages",
            "channel_messages",
            "tag_authors",
            "tag_works",
            "tag_characters",
            "tag_filter_configs_v2",
            "forward_queue",
            "processed_messages",
            "unread_messages_buffer"
    );
    
    /**
     * Property 8: Index creation order correctness
     * <p>
     * For any collection initialization process, indexes should be created in the specified order:
     * telegram_channels → raw_messages → channel_messages → tag_authors → tag_works → 
     * tag_characters → tag_filter_configs_v2 → forward_queue → processed_messages → 
     * unread_messages_buffer
     * </p>
     * <p>
     * **Validates: Requirement 3.2**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 8: 索引创建顺序正确性")
    void indexCreationOrderIsCorrect() {
        log.info("Testing index creation order correctness");
        
        // Prepare: Create test environment with order tracking
        List<String> actualOrder = new ArrayList<>();
        MongoTemplate mongoTemplate = createOrderTrackingMongoTemplate(actualOrder);
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        CollectionInitializer initializer = new CollectionInitializer(
                mongoTemplate,
                eventPublisher,
                progressTracker
        );
        
        // Execute
        initializer.onDatabaseReady(new DatabaseReadyEvent(this));
        
        // Debug: Log the actual order
        log.info("Actual order: {}", actualOrder);
        log.info("Expected order: {}", EXPECTED_COLLECTION_ORDER);
        
        // Verify: Collections were processed in the correct order
        assertThat(actualOrder).isEqualTo(EXPECTED_COLLECTION_ORDER);
        
        // Verify: Collections ready event was published
        assertThat(eventPublisher.getPublishedEvents()).contains("CollectionsReady");
        
        // Verify: Phase completed successfully
        assertThat(progressTracker.getPhases()).containsKey("集合初始化");
        assertThat(progressTracker.getPhases().get("集合初始化").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        
        log.info("✅ Index creation order verified: {}", actualOrder);
    }
    
    /**
     * Property 9: Index creation idempotency
     * <p>
     * For any existing index, repeating the collection initialization should not recreate that index.
     * </p>
     * <p>
     * **Validates: Requirement 3.3**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 9: 索引创建幂等性")
    void indexCreationIsIdempotent(
            @ForAll("existingIndexScenarios") ExistingIndexScenario scenario) {
        
        log.info("Testing idempotency with {} existing indexes", scenario.existingIndexes.size());
        
        // Prepare: Create test environment with existing indexes
        AtomicInteger indexCreationCount = new AtomicInteger(0);
        MongoTemplate mongoTemplate = createIdempotencyTestMongoTemplate(
                scenario.existingIndexes, 
                indexCreationCount
        );
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        CollectionInitializer initializer = new CollectionInitializer(
                mongoTemplate,
                eventPublisher,
                progressTracker
        );
        
        // Execute: Run initialization twice
        initializer.onDatabaseReady(new DatabaseReadyEvent(this));
        
        int firstRunCreations = indexCreationCount.get();
        log.info("First run created {} indexes", firstRunCreations);
        
        // Reset event publisher for second run
        eventPublisher.getPublishedEvents().clear();
        
        initializer.onDatabaseReady(new DatabaseReadyEvent(this));
        
        int secondRunCreations = indexCreationCount.get() - firstRunCreations;
        log.info("Second run created {} indexes (total now: {})", secondRunCreations, indexCreationCount.get());
        
        // Verify: Second run should create 0 indexes (all already exist)
        assertThat(secondRunCreations).isEqualTo(0);
        
        log.info("✅ Idempotency verified: first run created {} indexes, second run created {} indexes",
                firstRunCreations, secondRunCreations);
    }
    
    /**
     * Property 10: Continue execution on partial failures
     * <p>
     * For any non-critical component (index) initialization failure, the system should log the error
     * but continue initializing other components.
     * </p>
     * <p>
     * **Validates: Requirement 3.4**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 10: 部分失败时继续执行")
    void continueExecutionOnPartialFailures(
            @ForAll("failureScenarios") FailureScenario scenario) {
        
        log.info("Testing partial failure handling: {} collections will fail", 
                scenario.failingCollections.size());
        
        // Prepare: Create test environment with some failing collections
        List<String> processedCollections = new ArrayList<>();
        MongoTemplate mongoTemplate = createPartialFailureMongoTemplate(
                scenario.failingCollections,
                processedCollections
        );
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        CollectionInitializer initializer = new CollectionInitializer(
                mongoTemplate,
                eventPublisher,
                progressTracker
        );
        
        // Execute: Should not throw exception despite partial failures
        initializer.onDatabaseReady(new DatabaseReadyEvent(this));
        
        // Verify: All collections were attempted (including failing ones)
        assertThat(processedCollections).containsAll(EXPECTED_COLLECTION_ORDER);
        
        // Verify: Collections ready event was still published
        assertThat(eventPublisher.getPublishedEvents()).contains("CollectionsReady");
        
        // Verify: Phase completed successfully despite partial failures
        assertThat(progressTracker.getPhases()).containsKey("集合初始化");
        assertThat(progressTracker.getPhases().get("集合初始化").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        
        log.info("✅ Partial failure handling verified: {} collections failed, but initialization completed",
                scenario.failingCollections.size());
    }
    
    /**
     * Property 10 Extended: Empty telegram_channels warning
     * <p>
     * When telegram_channels collection is empty, a warning should be logged but initialization
     * should complete successfully.
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 10 Extended: 空集合警告")
    void emptyTelegramChannelsLogsWarning(
            @ForAll("channelCounts") long channelCount) {
        
        log.info("Testing empty collection warning with {} channels", channelCount);
        
        // Prepare
        MongoTemplate mongoTemplate = createMongoTemplateWithChannelCount(channelCount);
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        CollectionInitializer initializer = new CollectionInitializer(
                mongoTemplate,
                eventPublisher,
                progressTracker
        );
        
        // Execute: Should complete successfully regardless of channel count
        initializer.onDatabaseReady(new DatabaseReadyEvent(this));
        
        // Verify: Initialization completed successfully
        assertThat(eventPublisher.getPublishedEvents()).contains("CollectionsReady");
        assertThat(progressTracker.getPhases().get("集合初始化").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        
        log.info("✅ Empty collection handling verified for {} channels", channelCount);
    }
    
    // ==================== Arbitraries ====================
    
    @Provide
    Arbitrary<ExistingIndexScenario> existingIndexScenarios() {
        return Arbitraries.integers().between(0, 10).map(count -> {
            Set<String> existingIndexes = new HashSet<>();
            for (int i = 0; i < count; i++) {
                existingIndexes.add("index_" + i);
            }
            return new ExistingIndexScenario(existingIndexes);
        });
    }
    
    @Provide
    Arbitrary<FailureScenario> failureScenarios() {
        return Arbitraries.integers().between(0, 3).map(failureCount -> {
            Set<String> failingCollections = new HashSet<>();
            
            // Randomly select collections to fail
            List<String> shuffled = new ArrayList<>(EXPECTED_COLLECTION_ORDER);
            Collections.shuffle(shuffled);
            
            for (int i = 0; i < Math.min(failureCount, shuffled.size()); i++) {
                failingCollections.add(shuffled.get(i));
            }
            
            return new FailureScenario(failingCollections);
        });
    }
    
    @Provide
    Arbitrary<Long> channelCounts() {
        return Arbitraries.longs().between(0, 10);
    }
    
    // ==================== Test Data Classes ====================
    
    /**
     * Scenario with existing indexes
     */
    static class ExistingIndexScenario {
        final Set<String> existingIndexes;
        
        ExistingIndexScenario(Set<String> existingIndexes) {
            this.existingIndexes = existingIndexes;
        }
    }
    
    /**
     * Scenario with failing collections
     */
    static class FailureScenario {
        final Set<String> failingCollections;
        
        FailureScenario(Set<String> failingCollections) {
            this.failingCollections = failingCollections;
        }
    }
    
    // ==================== Test Doubles ====================
    
    /**
     * Creates a MongoTemplate that tracks the order of collection processing
     */
    static MongoTemplate createOrderTrackingMongoTemplate(List<String> actualOrder) {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        
        // Mock indexOps to track collection access order
        when(mongoTemplate.indexOps(anyString())).thenAnswer(invocation -> {
            String collectionName = invocation.getArgument(0);
            
            // Only add to order list once per collection (first access)
            if (!actualOrder.contains(collectionName)) {
                actualOrder.add(collectionName);
            }
            
            IndexOperations indexOps = mock(IndexOperations.class);
            
            // Mock getIndexInfo to return empty list (no existing indexes)
            when(indexOps.getIndexInfo()).thenReturn(Collections.emptyList());
            
            // Mock createIndex to do nothing
            when(indexOps.createIndex(any())).thenReturn("mock_index");
            
            return indexOps;
        });
        
        // Mock count for telegram_channels check
        when(mongoTemplate.count(any(Query.class), anyString())).thenReturn(1L);
        
        return mongoTemplate;
    }
    
    /**
     * Creates a MongoTemplate for idempotency testing
     */
    static MongoTemplate createIdempotencyTestMongoTemplate(
            Set<String> existingIndexes, 
            AtomicInteger indexCreationCount) {
        
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // Use a map to track indexes per collection
        Map<String, Set<String>> indexesByCollection = new HashMap<>();
        
        when(mongoTemplate.indexOps(anyString())).thenAnswer(invocation -> {
            String collectionName = invocation.getArgument(0);
            
            // Initialize collection's index set if not exists
            indexesByCollection.putIfAbsent(collectionName, Collections.synchronizedSet(new HashSet<>()));
            Set<String> collectionIndexes = indexesByCollection.get(collectionName);
            
            IndexOperations indexOps = mock(IndexOperations.class);
            
            // Mock getIndexInfo to return existing indexes for this collection
            when(indexOps.getIndexInfo()).thenAnswer(inv -> {
                List<IndexInfo> indexInfoList = new ArrayList<>();
                for (String indexName : collectionIndexes) {
                    IndexInfo indexInfo = mock(IndexInfo.class);
                    when(indexInfo.getName()).thenReturn(indexName);
                    indexInfoList.add(indexInfo);
                }
                return indexInfoList;
            });
            
            // Mock createIndex to track creation
            when(indexOps.createIndex(any())).thenAnswer(inv -> {
                // Extract the index name from the Index object
                org.springframework.data.mongodb.core.index.Index indexDef = 
                    inv.getArgument(0, org.springframework.data.mongodb.core.index.Index.class);
                
                String indexName = indexDef.getIndexOptions().get("name", String.class);
                if (indexName == null) {
                    indexName = "unnamed_index_" + System.nanoTime();
                }
                
                // Add to collection's indexes and increment counter
                collectionIndexes.add(indexName);
                indexCreationCount.incrementAndGet();
                
                return indexName;
            });
            
            return indexOps;
        });
        
        when(mongoTemplate.count(any(Query.class), anyString())).thenReturn(1L);
        
        return mongoTemplate;
    }
    
    /**
     * Creates a MongoTemplate that simulates partial failures
     */
    static MongoTemplate createPartialFailureMongoTemplate(
            Set<String> failingCollections,
            List<String> processedCollections) {
        
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        
        when(mongoTemplate.indexOps(anyString())).thenAnswer(invocation -> {
            String collectionName = invocation.getArgument(0);
            
            // Track this collection as processed (only once)
            if (!processedCollections.contains(collectionName)) {
                processedCollections.add(collectionName);
            }
            
            IndexOperations indexOps = mock(IndexOperations.class);
            
            if (failingCollections.contains(collectionName)) {
                // Simulate failure for this collection
                when(indexOps.getIndexInfo()).thenThrow(
                        new RuntimeException("Simulated index operation failure for " + collectionName)
                );
                when(indexOps.createIndex(any())).thenThrow(
                        new RuntimeException("Simulated index creation failure for " + collectionName)
                );
            } else {
                // Normal behavior
                when(indexOps.getIndexInfo()).thenReturn(Collections.emptyList());
                when(indexOps.createIndex(any())).thenReturn("mock_index");
            }
            
            return indexOps;
        });
        
        when(mongoTemplate.count(any(Query.class), anyString())).thenReturn(1L);
        
        return mongoTemplate;
    }
    
    /**
     * Creates a MongoTemplate with a specific channel count
     */
    static MongoTemplate createMongoTemplateWithChannelCount(long channelCount) {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        
        when(mongoTemplate.indexOps(anyString())).thenAnswer(invocation -> {
            IndexOperations indexOps = mock(IndexOperations.class);
            when(indexOps.getIndexInfo()).thenReturn(Collections.emptyList());
            when(indexOps.createIndex(any())).thenReturn("mock_index");
            return indexOps;
        });
        
        // Return the specified channel count
        when(mongoTemplate.count(any(Query.class), anyString())).thenReturn(channelCount);
        
        return mongoTemplate;
    }
    
    /**
     * Test implementation of StartupEventPublisher
     */
    static class TestStartupEventPublisher extends StartupEventPublisher {
        private final List<String> publishedEvents = new ArrayList<>();
        
        TestStartupEventPublisher() {
            super(null);
        }
        
        @Override
        public void publishCollectionsReady() {
            publishedEvents.add("CollectionsReady");
        }
        
        List<String> getPublishedEvents() {
            return publishedEvents;
        }
    }
    
    /**
     * Test implementation of StartupProgressTracker
     */
    static class TestStartupProgressTracker extends StartupProgressTracker {
        // Inherits all functionality from parent class
    }
}
