package org.xlyo.cocomonyab.plugin;

import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;

/**
 * 消息处理插件接口
 * 所有插件必须实现此接口
 */
public interface MessagePlugin {
    
    /**
     * 获取插件名称
     * 
     * @return 插件名称（唯一标识）
     */
    String getName();
    
    /**
     * 获取插件优先级
     * 数值越大优先级越高，优先级高的插件先执行
     * 
     * @return 优先级值
     */
    int getPriority();
    
    /**
     * 插件是否启用
     * 
     * @return 如果启用返回true，否则返回false
     */
    boolean isEnabled();
    
    /**
     * 插件初始化
     * 在插件注册时调用，用于执行初始化操作
     */
    void initialize();
    
    /**
     * 处理消息
     * 
     * @param entity 消息实体
     * @param context 处理上下文
     * @return 处理结果（CONTINUE继续执行下一个插件，STOP停止插件链）
     */
    PluginResult handle(BaseMessageEntity entity, PluginContext context);
    
    /**
     * 插件销毁
     * 在应用关闭或插件注销时调用，用于执行清理操作
     */
    void destroy();
}
