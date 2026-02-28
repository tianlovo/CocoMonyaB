package org.xlyo.cocomonyab.config.initializer;

import net.jqwik.api.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.xlyo.cocomonyab.config.properties.DatabaseStartupProperties;
import org.xlyo.cocomonyab.event.startup.ConfigurationReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DatabaseManager Property-Based Tests
 * <p>
 * Tests Property 7: Database connection retry mechanism
 * </p>
 * <p>
 * **Validates: Requirements 2.5, 2.6**
 * </p>
 */
class DatabaseManagerPropertyTest {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseManagerPropertyTest.class);
    
    /**
     * Property 7: Database connection retry mechanism
     * <p>
     * For any database connection failure, the system should retry up to 3 times,
     * and terminate startup if all 3 retries fail.
     * </p>
     * <p>
     * **Validates: Requirements 2.5, 2.6**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 7: 数据库连接重试机制")
    void databaseConnectionRetryMechanism(
            @ForAll("retryScenarios") RetryScenario scenario) {
        
        log.info("Testing retry scenario: failuresBeforeSuccess={}, maxRetries={}, retryDelay={}ms",
                scenario.failuresBeforeSuccess, scenario.maxRetries, scenario.retryDelayMs);
        
        // Prepare: Create test environment
        AtomicInteger connectionAttempts = new AtomicInteger(0);
        MongoTemplate mongoTemplate = createMockMongoTemplate(scenario.failuresBeforeSuccess, connectionAttempts);
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        DatabaseStartupProperties properties = new DatabaseStartupProperties();
        properties.setMaxRetries(scenario.maxRetries);
        properties.setRetryDelayMs(scenario.retryDelayMs);
        
        DatabaseManager manager = new DatabaseManager(
                mongoTemplate,
                eventPublisher,
                progressTracker,
                properties
        );
        
        ConfigurationReadyEvent event = new ConfigurationReadyEvent(this);
        
        // Execute and Verify
        if (scenario.shouldSucceed()) {
            // Should succeed after retries
            manager.onConfigurationReady(event);
            
            // Verify: Connection attempts = failures + 1 (final success)
            assertThat(connectionAttempts.get())
                    .isEqualTo(scenario.failuresBeforeSuccess + 1);
            
            // Verify: Database ready event was published
            assertThat(eventPublisher.getPublishedEvents()).contains("DatabaseReady");
            
            // Verify: Phase completed successfully
            assertThat(progressTracker.getPhases()).containsKey("数据库初始化");
            assertThat(progressTracker.getPhases().get("数据库初始化").getStatus())
                    .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
            
            log.info("✅ Succeeded after {} attempts (expected)", connectionAttempts.get());
            
        } else {
            // Should fail after exhausting retries
            assertThatThrownBy(() -> manager.onConfigurationReady(event))
                    .isInstanceOf(StartupException.class)
                    .hasMessageContaining("数据库初始化失败");
            
            // Verify: Connection attempts = maxRetries + 1 (initial attempt)
            assertThat(connectionAttempts.get())
                    .isEqualTo(scenario.maxRetries + 1);
            
            // Verify: Database ready event was NOT published
            assertThat(eventPublisher.getPublishedEvents()).doesNotContain("DatabaseReady");
            
            // Verify: Phase failed
            assertThat(progressTracker.getPhases()).containsKey("数据库初始化");
            assertThat(progressTracker.getPhases().get("数据库初始化").getStatus())
                    .isEqualTo(StartupProgressTracker.PhaseStatus.FAILED);
            
            log.info("❌ Failed after {} attempts (expected)", connectionAttempts.get());
        }
    }
    
    /**
     * Property 7 Extended: Retry delay timing
     * <p>
     * Verifies that the retry delay is actually applied between retry attempts.
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 7 Extended: 重试延迟时间验证")
    void retryDelayIsApplied(
            @ForAll("failuresForDelayTest") int failuresBeforeSuccess,
            @ForAll("retryDelays") long retryDelayMs) {
        
        // Prepare
        AtomicInteger connectionAttempts = new AtomicInteger(0);
        MongoTemplate mongoTemplate = createMockMongoTemplate(failuresBeforeSuccess, connectionAttempts);
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        DatabaseStartupProperties properties = new DatabaseStartupProperties();
        properties.setMaxRetries(3);
        properties.setRetryDelayMs(retryDelayMs);
        
        DatabaseManager manager = new DatabaseManager(
                mongoTemplate,
                eventPublisher,
                progressTracker,
                properties
        );
        
        // Execute
        long startTime = System.currentTimeMillis();
        manager.onConfigurationReady(new ConfigurationReadyEvent(this));
        long endTime = System.currentTimeMillis();
        long actualDuration = endTime - startTime;
        
        // Verify: Total duration should be at least (failuresBeforeSuccess * retryDelayMs)
        // We allow some tolerance for execution time
        long expectedMinDuration = failuresBeforeSuccess * retryDelayMs;
        assertThat(actualDuration).isGreaterThanOrEqualTo(expectedMinDuration);
        
        log.info("Retry delay verified: expected >= {}ms, actual = {}ms", 
                expectedMinDuration, actualDuration);
    }
    
    /**
     * Property 7 Extended: Zero retries configuration
     * <p>
     * When maxRetries is 0, the system should fail immediately without retrying.
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 7 Extended: 零重试配置")
    void zeroRetriesFailsImmediately() {
        // Prepare: Always failing connection
        AtomicInteger connectionAttempts = new AtomicInteger(0);
        MongoTemplate mongoTemplate = createMockMongoTemplate(Integer.MAX_VALUE, connectionAttempts);
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        DatabaseStartupProperties properties = new DatabaseStartupProperties();
        properties.setMaxRetries(0);
        properties.setRetryDelayMs(1000);
        
        DatabaseManager manager = new DatabaseManager(
                mongoTemplate,
                eventPublisher,
                progressTracker,
                properties
        );
        
        // Execute and Verify: Should fail after 1 attempt (no retries)
        assertThatThrownBy(() -> manager.onConfigurationReady(new ConfigurationReadyEvent(this)))
                .isInstanceOf(StartupException.class);
        
        assertThat(connectionAttempts.get()).isEqualTo(1);
        
        log.info("Zero retries: failed after 1 attempt as expected");
    }
    
    // ==================== Arbitraries ====================
    
    @Provide
    Arbitrary<RetryScenario> retryScenarios() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 5),  // failuresBeforeSuccess
                Arbitraries.integers().between(0, 5),  // maxRetries
                Arbitraries.longs().between(10, 100)   // retryDelayMs (short for testing)
        ).as(RetryScenario::new);
    }
    
    @Provide
    Arbitrary<Integer> failuresForDelayTest() {
        return Arbitraries.integers().between(1, 2);
    }
    
    @Provide
    Arbitrary<Long> retryDelays() {
        return Arbitraries.longs().between(100, 500);
    }
    
    // ==================== Test Data Classes ====================
    
    /**
     * Retry scenario for testing
     */
    static class RetryScenario {
        final int failuresBeforeSuccess;
        final int maxRetries;
        final long retryDelayMs;
        
        RetryScenario(int failuresBeforeSuccess, int maxRetries, long retryDelayMs) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.maxRetries = maxRetries;
            this.retryDelayMs = retryDelayMs;
        }
        
        boolean shouldSucceed() {
            return failuresBeforeSuccess <= maxRetries;
        }
    }
    
    // ==================== Test Doubles ====================
    
    /**
     * Creates a mock MongoTemplate that fails a specified number of times before succeeding
     */
    static MongoTemplate createMockMongoTemplate(int failuresBeforeSuccess, AtomicInteger connectionAttempts) {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        
        when(mongoTemplate.executeCommand(anyString())).thenAnswer(invocation -> {
            int attempt = connectionAttempts.incrementAndGet();
            
            if (attempt <= failuresBeforeSuccess) {
                throw new RuntimeException("Database connection failed (simulated failure " + attempt + ")");
            }
            
            // Success: return valid ping response
            Document result = new Document();
            result.put("ok", 1.0);
            return result;
        });
        
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
        public void publishDatabaseReady() {
            publishedEvents.add("DatabaseReady");
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
