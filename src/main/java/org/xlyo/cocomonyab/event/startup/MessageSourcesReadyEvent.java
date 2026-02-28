package org.xlyo.cocomonyab.event.startup;

/**
 * 消息源就绪事件
 * <p>
 * 当消息源初始化阶段完成后发布此事件，表示Telegram客户端已初始化，所有消息源已注册并启动。
 * </p>
 */
public class MessageSourcesReadyEvent extends StartupEvent {
    
    /**
     * 构造消息源就绪事件
     *
     * @param source 事件源对象
     */
    public MessageSourcesReadyEvent(Object source) {
        super(source);
    }
}
