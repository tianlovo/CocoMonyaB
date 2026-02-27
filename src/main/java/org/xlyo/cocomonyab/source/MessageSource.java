package org.xlyo.cocomonyab.source;

/**
 * 消息来源接口
 * <p>
 * 定义了消息来源生成器的基本契约。任何消息来源（如 Telegram、Webhook、文件导入等）
 * 都需要实现此接口，将外部消息转换为系统可处理的 TdApi.Message 格式
 * <p>
 * 设计理念：
 * - 解耦消息来源和消息处理逻辑
 * - 支持多种消息来源并存
 * - 统一的消息格式便于后续处理
 * 
 * @author tianluoqaq
 * @since 1.0
 */
public interface MessageSource {
    
    /**
     * 获取消息来源的唯一标识符
     * <p>
     * 用于区分不同的消息来源，建议使用小写字母和连字符，如：
     * - "telegram-official" - 官方 Telegram 来源
     * - "webhook-custom" - 自定义 Webhook 来源
     * - "file-import" - 文件导入来源
     * 
     * @return 消息来源标识符，不能为 null 或空字符串
     */
    String getSourceId();
    
    /**
     * 获取消息来源的显示名称
     * <p>
     * 用于日志、监控和用户界面显示
     * 
     * @return 消息来源显示名称
     */
    String getSourceName();
    
    /**
     * 获取消息来源的描述信息
     * <p>
     * 详细说明此消息来源的用途、特点等
     * 
     * @return 消息来源描述
     */
    String getDescription();
    
    /**
     * 启动消息来源
     * <p>
     * 初始化消息来源，开始接收和处理消息。
     * 此方法应该是非阻塞的，如果需要长时间运行的任务，应该在后台线程中执行。
     * 
     * @throws MessageSourceException 如果启动失败
     */
    void start() throws MessageSourceException;
    
    /**
     * 停止消息来源
     * <p>
     * 停止接收新消息，清理资源。
     * 此方法应该优雅地关闭所有连接和线程。
     * 
     * @throws MessageSourceException 如果停止失败
     */
    void stop() throws MessageSourceException;
    
    /**
     * 检查消息来源是否正在运行
     * 
     * @return true 如果消息来源正在运行，false 否则
     */
    boolean isRunning();
    
    /**
     * 获取消息来源的健康状态
     * <p>
     * 用于监控和健康检查
     * 
     * @return 健康状态信息
     */
    MessageSourceHealth getHealth();
}
