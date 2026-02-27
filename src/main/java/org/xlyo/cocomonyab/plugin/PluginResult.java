package org.xlyo.cocomonyab.plugin;

/**
 * 插件处理结果
 * 用于控制插件链的执行流程
 */
public enum PluginResult {
    /**
     * 继续执行下一个插件
     */
    CONTINUE,
    
    /**
     * 停止插件链执行
     */
    STOP
}
