package org.xlyo.cocomonyab.event.startup;

/**
 * API就绪事件
 * <p>
 * 当RESTful API初始化阶段完成后发布此事件，表示Web服务器已启动，API已准备好接收请求。
 * </p>
 */
public class ApiReadyEvent extends StartupEvent {
    
    /**
     * 构造API就绪事件
     *
     * @param source 事件源对象
     */
    public ApiReadyEvent(Object source) {
        super(source);
    }
}
