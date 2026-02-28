package org.xlyo.cocomonyab.event.startup;

/**
 * 集合就绪事件
 * <p>
 * 当数据库集合初始化阶段完成后发布此事件，表示所有集合索引已创建，初始数据已检查。
 * </p>
 */
public class CollectionsReadyEvent extends StartupEvent {
    
    /**
     * 构造集合就绪事件
     *
     * @param source 事件源对象
     */
    public CollectionsReadyEvent(Object source) {
        super(source);
    }
}
