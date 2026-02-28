package org.xlyo.cocomonyab.event.startup;

import net.jqwik.api.*;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动事件发布属性测试
 * <p>
 * 验证属性 3: 阶段完成后发布事件
 * </p>
 * <p>
 * **Validates: Requirements 1.8, 2.7, 3.7, 4.7, 5.11, 6.7, 7.1**
 * </p>
 */
class StartupEventPublishingPropertyTest {
    
    /**
     * 属性 3: 对于任何成功完成的启动阶段，系统应发布相应的就绪事件
     * <p>
     * 此测试验证每个启动阶段完成后都会发布对应的事件类型。
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 3: 阶段完成后发布事件")
    void phaseCompletionPublishesCorrespondingEvent(
            @ForAll("startupPhases") StartupPhase phase) {
        
        // 准备：创建事件捕获器
        TestEventPublisher eventPublisher = new TestEventPublisher();
        
        // 执行：模拟阶段完成并发布事件
        StartupEvent event = phase.createEvent(this);
        eventPublisher.publishEvent(event);
        
        // 验证：确认发布了正确类型的事件
        assertThat(eventPublisher.getPublishedEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(phase.getExpectedEventClass());
        
        // 验证：事件包含时间戳
        assertThat(event.getEventTime()).isNotNull();
        
        // 验证：事件源正确
        assertThat(event.getSource()).isEqualTo(this);
    }
    
    /**
     * 属性 3 扩展: 多个阶段按顺序完成时，应按顺序发布对应的事件
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 3 Extended: 多阶段顺序发布事件")
    void multiplePhaseCompletionsPublishEventsInOrder(
            @ForAll("phaseSequences") List<StartupPhase> phases) {
        
        // 准备：创建事件捕获器
        TestEventPublisher eventPublisher = new TestEventPublisher();
        
        // 执行：按顺序完成各阶段并发布事件
        for (StartupPhase phase : phases) {
            StartupEvent event = phase.createEvent(this);
            eventPublisher.publishEvent(event);
        }
        
        // 验证：发布的事件数量正确
        assertThat(eventPublisher.getPublishedEvents()).hasSize(phases.size());
        
        // 验证：事件类型顺序正确
        for (int i = 0; i < phases.size(); i++) {
            assertThat(eventPublisher.getPublishedEvents().get(i))
                    .isInstanceOf(phases.get(i).getExpectedEventClass());
        }
    }
    
    /**
     * 属性 3 扩展: 事件时间戳应该单调递增（后发布的事件时间不早于先发布的）
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 3 Extended: 事件时间戳单调性")
    void eventTimestampsAreMonotonic(
            @ForAll("phaseSequences") List<StartupPhase> phases) {
        
        // 准备：创建事件捕获器
        TestEventPublisher eventPublisher = new TestEventPublisher();
        
        // 执行：按顺序发布事件
        for (StartupPhase phase : phases) {
            StartupEvent event = phase.createEvent(this);
            eventPublisher.publishEvent(event);
            // 添加小延迟确保时间戳不同
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 验证：时间戳单调递增
        List<StartupEvent> events = eventPublisher.getPublishedEvents();
        for (int i = 1; i < events.size(); i++) {
            assertThat(events.get(i).getEventTime())
                    .isAfterOrEqualTo(events.get(i - 1).getEventTime());
        }
    }
    
    // ==================== 测试数据生成器 ====================
    
    /**
     * 生成单个启动阶段
     */
    @Provide
    Arbitrary<StartupPhase> startupPhases() {
        return Arbitraries.of(StartupPhase.values());
    }
    
    /**
     * 生成启动阶段序列（1-7个阶段）
     */
    @Provide
    Arbitrary<List<StartupPhase>> phaseSequences() {
        return Arbitraries.of(StartupPhase.values())
                .list()
                .ofMinSize(1)
                .ofMaxSize(7);
    }
    
    // ==================== 测试辅助类 ====================
    
    /**
     * 启动阶段枚举
     * <p>
     * 定义所有启动阶段及其对应的事件类型
     * </p>
     */
    enum StartupPhase {
        CONFIGURATION(ConfigurationReadyEvent.class),
        DATABASE(DatabaseReadyEvent.class),
        COLLECTIONS(CollectionsReadyEvent.class),
        PLUGINS(PluginsReadyEvent.class),
        MESSAGE_SOURCES(MessageSourcesReadyEvent.class),
        API(ApiReadyEvent.class),
        APPLICATION(ApplicationReadyEvent.class);
        
        private final Class<? extends StartupEvent> eventClass;
        
        StartupPhase(Class<? extends StartupEvent> eventClass) {
            this.eventClass = eventClass;
        }
        
        public Class<? extends StartupEvent> getExpectedEventClass() {
            return eventClass;
        }
        
        /**
         * 创建对应的事件实例
         */
        public StartupEvent createEvent(Object source) {
            return switch (this) {
                case CONFIGURATION -> new ConfigurationReadyEvent(source);
                case DATABASE -> new DatabaseReadyEvent(source);
                case COLLECTIONS -> new CollectionsReadyEvent(source);
                case PLUGINS -> new PluginsReadyEvent(source);
                case MESSAGE_SOURCES -> new MessageSourcesReadyEvent(source);
                case API -> new ApiReadyEvent(source);
                case APPLICATION -> new ApplicationReadyEvent(source);
            };
        }
    }
    
    /**
     * 测试用事件发布器
     * <p>
     * 捕获所有发布的事件以便验证
     * </p>
     */
    static class TestEventPublisher implements ApplicationEventPublisher {
        private final List<StartupEvent> publishedEvents = new ArrayList<>();
        
        @Override
        public void publishEvent(Object event) {
            if (event instanceof StartupEvent startupEvent) {
                publishedEvents.add(startupEvent);
            }
        }
        
        public List<StartupEvent> getPublishedEvents() {
            return publishedEvents;
        }
    }
}
