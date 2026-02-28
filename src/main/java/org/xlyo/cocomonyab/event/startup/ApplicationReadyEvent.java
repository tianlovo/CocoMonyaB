package org.xlyo.cocomonyab.event.startup;

/**
 * 应用准备就绪事件
 * <p>
 * 当所有启动阶段完成后发布此事件，表示应用已完全启动，所有组件都已就绪。
 * </p>
 */
public class ApplicationReadyEvent extends StartupEvent {
    
    /**
     * 构造应用准备就绪事件
     *
     * @param source 事件源对象
     */
    public ApplicationReadyEvent(Object source) {
        super(source);
    }
}
