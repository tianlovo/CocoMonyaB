package org.xlyo.cocomonyab.event.startup;

/**
 * 数据库就绪事件
 * <p>
 * 当数据库初始化阶段完成后发布此事件，表示数据库连接已建立并验证成功。
 * </p>
 */
public class DatabaseReadyEvent extends StartupEvent {
    
    /**
     * 构造数据库就绪事件
     *
     * @param source 事件源对象
     */
    public DatabaseReadyEvent(Object source) {
        super(source);
    }
}
