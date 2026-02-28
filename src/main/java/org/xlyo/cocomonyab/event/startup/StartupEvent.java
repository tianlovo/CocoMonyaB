package org.xlyo.cocomonyab.event.startup;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 启动事件基类
 * <p>
 * 所有启动阶段事件的基类，包含事件发生的时间戳
 * </p>
 */
public abstract class StartupEvent extends ApplicationEvent {
    
    private final LocalDateTime eventTime;
    
    /**
     * 构造启动事件
     *
     * @param source 事件源对象
     */
    public StartupEvent(Object source) {
        super(source);
        this.eventTime = LocalDateTime.now();
    }
    
    /**
     * 获取事件发生的时间戳
     *
     * @return 事件时间戳
     */
    public LocalDateTime getEventTime() {
        return eventTime;
    }
}
