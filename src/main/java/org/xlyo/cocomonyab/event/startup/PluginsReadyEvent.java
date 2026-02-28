package org.xlyo.cocomonyab.event.startup;

/**
 * 插件就绪事件
 * <p>
 * 当消息处理插件初始化阶段完成后发布此事件，表示所有插件已扫描、排序并初始化完成。
 * </p>
 */
public class PluginsReadyEvent extends StartupEvent {
    
    /**
     * 构造插件就绪事件
     *
     * @param source 事件源对象
     */
    public PluginsReadyEvent(Object source) {
        super(source);
    }
}
