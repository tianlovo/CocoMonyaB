package org.xlyo.cocomonyab.plugin;

import lombok.extern.slf4j.Slf4j;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;

/**
 * 插件抽象基类
 * 提供默认实现和通用功能
 */
@Slf4j
public abstract class AbstractMessagePlugin implements MessagePlugin {
    
    /**
     * 插件启用状态
     */
    private boolean enabled = true;
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置插件启用状态
     * 
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void initialize() {
        log.info("Initializing plugin: {}", getName());
    }
    
    @Override
    public void destroy() {
        log.info("Destroying plugin: {}", getName());
    }
    
    /**
     * 检查是否支持该消息类型
     * 子类可以重写此方法来限制支持的消息类型
     * 
     * @param entity 消息实体
     * @return 如果支持返回true，否则返回false
     */
    protected boolean supports(BaseMessageEntity entity) {
        return true;  // 默认支持所有类型
    }
    
    @Override
    public PluginResult handle(BaseMessageEntity entity, PluginContext context) {
        // 检查插件是否启用
        if (!isEnabled()) {
            log.debug("Plugin {} is disabled, skipping", getName());
            return PluginResult.CONTINUE;
        }
        
        // 检查是否支持该消息类型
        if (!supports(entity)) {
            log.debug("Plugin {} does not support message type {}", 
                getName(), entity.getType());
            return PluginResult.CONTINUE;
        }
        
        // 执行实际处理逻辑，捕获异常以保证插件链的健壮性
        try {
            return doHandle(entity, context);
        } catch (Exception e) {
            log.error("Error in plugin {}: {}", getName(), e.getMessage(), e);
            return PluginResult.CONTINUE;  // 出错继续执行下一个插件
        }
    }
    
    /**
     * 实际的处理逻辑，由子类实现
     * 
     * @param entity 消息实体
     * @param context 处理上下文
     * @return 处理结果
     */
    protected abstract PluginResult doHandle(BaseMessageEntity entity, PluginContext context);
}
