package org.xlyo.cocomonyab.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 系统完全就绪事件
 * <p>
 * 当系统所有关键组件（Telegram客户端、MongoDB、消息处理器等）
 * 都初始化完成并准备好接受请求时发布此事件
 * </p>
 */
@Getter
public class SystemReadyEvent extends ApplicationEvent {
    
    /**
     * 系统启动耗时（毫秒）
     */
    private final long startupTimeMs;
    
    /**
     * 系统就绪时间戳
     */
    private final long readyTimestamp;
    
    public SystemReadyEvent(Object source, long startupTimeMs) {
        super(source);
        this.startupTimeMs = startupTimeMs;
        this.readyTimestamp = System.currentTimeMillis();
    }
}
