package org.xlyo.cocomonyab.plugin.tagforward;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.plugin.AbstractMessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.tagforward.component.ForwardScheduler;
import org.xlyo.cocomonyab.plugin.tagforward.component.QueueManager;
import org.xlyo.cocomonyab.plugin.tagforward.component.TagMatcher;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.List;

/**
 * 基于标签的消息转发插件
 * 
 * <p>此插件自动识别包含特定标签的Telegram频道消息，
 * 并将这些"感兴趣"的消息转发到指定的目标频道
 * 
 * <p>主要功能：
 * <ul>
 *   <li>从MongoDB标签系统加载标签配置</li>
 *   <li>对接收到的消息进行标签匹配识别</li>
 *   <li>将感兴趣的消息加入转发队列</li>
 *   <li>通过定时任务批量处理转发队列</li>
 * </ul>
 */
@Component
@Slf4j
public class TagBasedMessageForwardingPlugin extends AbstractMessagePlugin {
    
    private final TagMatcher tagMatcher;
    private final QueueManager queueManager;
    private final ForwardScheduler forwardScheduler;
    private final TagBasedForwardingProperties properties;
    private final TelegramClientManager clientManager;
    
    public TagBasedMessageForwardingPlugin(
            TagMatcher tagMatcher,
            QueueManager queueManager,
            ForwardScheduler forwardScheduler,
            TagBasedForwardingProperties properties,
            TelegramClientManager clientManager) {
        this.tagMatcher = tagMatcher;
        this.queueManager = queueManager;
        this.forwardScheduler = forwardScheduler;
        this.properties = properties;
        this.clientManager = clientManager;
    }
    
    @Override
    public String getName() {
        return "TagBasedMessageForwardingPlugin";
    }
    
    @Override
    public int getPriority() {
        return 100;  // 高优先级，在其他插件之前执行（尽早捕获感兴趣的消息）
    }
    
    @Override
    public boolean isEnabled() {
        return properties.getEnabled() != null && properties.getEnabled();
    }
    
    @Override
    public void initialize() {
        try {
            log.info("正在初始化 TagBasedMessageForwardingPlugin...");
            
            // 检查插件是否启用
            if (!isEnabled()) {
                log.info("TagBasedMessageForwardingPlugin 已禁用，跳过初始化");
                return;
            }
            
            // 验证目标频道配置
            validateTargetChannel();
            
            // 注意：标签配置的加载延迟到应用就绪后（onApplicationReady方法）
            // 这样可以确保 MongoDB 连接已建立
            
            log.info("TagBasedMessageForwardingPlugin 初始化成功（标签配置将在应用就绪后加载）");
            
        } catch (Exception e) {
            log.error("TagBasedMessageForwardingPlugin 初始化失败", e);
            // 禁用插件以防止运行时错误
            setEnabled(false);
        }
    }
    
    /**
     * 应用就绪事件监听器
     * 
     * <p>在应用完全启动后（包括 MongoDB 连接建立后）加载标签配置并启动调度器
     * 这样可以避免在 MongoDB 连接建立前尝试查询数据库
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // 检查插件是否启用
            if (!isEnabled()) {
                log.debug("TagBasedMessageForwardingPlugin 已禁用，跳过应用就绪处理");
                return;
            }
            
            log.info("应用已就绪，正在加载标签配置并启动转发调度器...");
            
            // 加载标签配置
            tagMatcher.loadTagConfiguration();
            
            // 验证目标频道（通过 TDLib API）
            validateTargetChannelWithTdLib();
            
            // 启动转发调度器
            forwardScheduler.start();
            
            log.info("TagBasedMessageForwardingPlugin 应用就绪处理完成");
            
        } catch (Exception e) {
            log.error("TagBasedMessageForwardingPlugin 应用就绪处理失败", e);
        }
    }
    
    /**
     * 通过 TDLib API 验证目标频道
     * 
     * <p>检查频道是否存在、是否可访问、是否有发送消息的权限
     */
    private void validateTargetChannelWithTdLib() {
        Long targetChannelId = properties.getTargetChannelId();
        
        try {
            log.info("正在通过 TDLib API 验证目标频道: {}", targetChannelId);
            
            // 检查客户端是否就绪
            if (!clientManager.isReady()) {
                log.warn("Telegram 客户端未就绪，跳过目标频道验证");
                return;
            }
            
            SimpleTelegramClient client = clientManager.getClient();
            
            // 构造 GetChat 请求
            TdApi.GetChat getChat = new TdApi.GetChat(targetChannelId);
            
            // 发送请求并处理响应
            client.send(getChat).whenComplete((resultObj, error) -> {
                if (error != null) {
                    log.error("验证目标频道时发生错误: chatId={}", targetChannelId, error);
                    return;
                }
                
                if (resultObj == null) {
                    log.warn("验证目标频道返回 null 结果: chatId={}", targetChannelId);
                    return;
                }
                
                // TdApi.GetChat 返回 TdApi.Chat 或 TdApi.Error
                // 由于泛型类型推断，编译器认为 resultObj 是 Chat 类型
                // 但运行时可能是 Error，需要通过类名检查
                String className = resultObj.getClass().getSimpleName();
                
                if ("Error".equals(className)) {
                    // 运行时是 Error 类型，需要通过反射访问
                    try {
                        // 获取 code 和 message 字段
                        int code = (int) resultObj.getClass().getField("code").get(resultObj);
                        String message = (String) resultObj.getClass().getField("message").get(resultObj);
                        
                        // 创建临时 Error 对象用于日志
                        TdApi.Error tdError = new TdApi.Error(code, message);
                        handleChatValidationError(targetChannelId, tdError);
                    } catch (Exception e) {
                        log.error("处理验证错误时发生异常: chatId={}", targetChannelId, e);
                    }
                } else if ("Chat".equals(className)) {
                    // 成功获取频道信息
                    handleChatValidationResult(resultObj);
                } else {
                    // 其他意外类型
                    log.error("验证目标频道返回意外类型: chatId={}, type={}", 
                            targetChannelId, className);
                }
            });
            
        } catch (Exception e) {
            log.error("验证目标频道时发生异常: chatId={}", targetChannelId, e);
        }
    }
    
    /**
     * 处理频道验证成功的结果
     * 
     * @param chat 频道信息
     */
    private void handleChatValidationResult(TdApi.Chat chat) {
        log.info("目标频道验证成功:");
        log.info("  - 频道ID: {}", chat.id);
        log.info("  - 频道标题: {}", chat.title);
        
        // 检查频道类型
        if (chat.type instanceof TdApi.ChatTypeSupergroup supergroup) {
            log.info("  - 是否为频道: {}", supergroup.isChannel);
            
            if (!supergroup.isChannel) {
                log.warn("  ⚠️ 警告: 目标不是频道而是超级群组");
            }
        } else {
            log.warn("  ⚠️ 警告: 目标不是超级群组/频道类型: {}", 
                    chat.type.getClass().getSimpleName());
        }
        
        // 检查权限
        if (chat.permissions != null) {
            log.info("  - 可以发送消息: {}", chat.permissions.canSendBasicMessages);
            log.info("  - 可以发送媒体: {}", chat.permissions.canSendPhotos);
            
            if (!chat.permissions.canSendBasicMessages) {
                log.error("  ❌ 错误: 没有在目标频道发送消息的权限");
            }
        }
        
        // 检查未读消息数量（可选）
        log.info("  - 未读消息数: {}", chat.unreadCount);
        
        log.info("✅ 目标频道验证通过");
    }
    
    /**
     * 处理频道验证失败的错误
     * 
     * @param chatId 频道ID
     * @param error TDLib 错误对象
     */
    private void handleChatValidationError(Long chatId, TdApi.Error error) {
        log.error("❌ 目标频道验证失败: chatId={}", chatId);
        log.error("  - 错误代码: {}", error.code);
        log.error("  - 错误消息: {}", error.message);
        
        // 根据错误代码提供具体的解决建议
        switch (error.code) {
            case 400:
                if (error.message.contains("CHAT_ID_INVALID") || 
                    error.message.contains("CHANNEL_INVALID")) {
                    log.error("  💡 建议: 频道ID无效，请检查配置的 target-channel-id 是否正确");
                    log.error("     频道ID应该是负数，格式如: -1001234567890");
                }
                break;
                
            case 403:
                if (error.message.contains("CHAT_WRITE_FORBIDDEN")) {
                    log.error("  💡 建议: 没有在该频道发送消息的权限");
                    log.error("     请确保机器人是频道管理员，并且有发送消息的权限");
                } else {
                    log.error("  💡 建议: 访问被拒绝，可能是权限不足或频道设置问题");
                }
                break;
                
            case 404:
                log.error("  💡 建议: 频道不存在或无法访问");
                log.error("     1. 检查频道ID是否正确");
                log.error("     2. 确保账号已加入该频道");
                log.error("     3. 如果是私有频道，确保有访问权限");
                break;
                
            case 420:
                log.error("  💡 建议: 请求过于频繁，请稍后重试");
                break;
                
            default:
                log.error("  💡 建议: 请检查网络连接和 Telegram 客户端状态");
                break;
        }
    }
    
    @Override
    protected boolean supports(BaseMessageEntity entity) {
        // 支持所有消息类型
        return true;
    }

    
    @Override
    protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
        Long chatId = entity.getChatId();
        Long messageId = entity.getMessageId();
        String messageType = entity.getType().name();
        
        try {
            log.debug("[TagForward] 开始处理消息: chatId={}, messageId={}, type={}", 
                    chatId, messageId, messageType);
            
            // 阶段 1: 检查标签配置是否已加载
            if (!tagMatcher.isConfigurationLoaded()) {
                log.warn("[TagForward] 标签配置尚未加载，跳过消息处理: chatId={}, messageId={}", 
                        chatId, messageId);
                return PluginResult.CONTINUE;
            }
            
            // 阶段 2: 提取文本内容（根据消息类型）
            String textContent = extractTextContent(entity);
            
            if (textContent == null || textContent.isEmpty()) {
                log.debug("[TagForward] 消息无文本内容，跳过: chatId={}, messageId={}, type={}", 
                        chatId, messageId, messageType);
                return PluginResult.CONTINUE;
            }
            
            log.debug("[TagForward] 提取文本内容成功: chatId={}, messageId={}, type={}, textLength={}",
                    chatId, messageId, messageType, textContent.length());
            
            // 阶段 3: 匹配标签
            log.debug("[TagForward] 开始标签匹配: chatId={}, messageId={}", chatId, messageId);
            List<String> matchedTags = tagMatcher.matchTags(textContent);
            
            // 阶段 4: 处理匹配结果
            if (matchedTags.isEmpty()) {
                log.debug("[TagForward] 未匹配到标签，跳过: chatId={}, messageId={}", chatId, messageId);
                return PluginResult.CONTINUE;
            }
            
            log.info("[TagForward] 匹配到 {} 个标签: chatId={}, messageId={}, type={}, tags={}", 
                    matchedTags.size(), chatId, messageId, messageType, matchedTags);
            
            // 阶段 5: 确定要转发的消息ID
            // 对于媒体组，转发第一条消息即可（TDLib会自动转发整个媒体组）
            Long forwardMessageId = messageId;
            if (entity instanceof MediaGroupMessageEntity mediaGroup) {
                if (mediaGroup.getItems() != null && !mediaGroup.getItems().isEmpty()) {
                    forwardMessageId = mediaGroup.getItems().get(0).getMessageId();
                    log.info("[TagForward] 媒体组消息，将转发第一条消息: chatId={}, originalId={}, forwardId={}, itemCount={}", 
                            chatId, messageId, forwardMessageId, mediaGroup.getItems().size());
                }
            }
            
            // 阶段 6: 加入转发队列
            log.debug("[TagForward] 正在将消息加入转发队列: chatId={}, messageId={}", chatId, forwardMessageId);
            queueManager.enqueue(chatId, forwardMessageId, matchedTags);
            
            log.info("[TagForward] 消息已成功加入转发队列: chatId={}, messageId={}, type={}, tags={}", 
                    chatId, forwardMessageId, messageType, matchedTags);
            
        } catch (Exception e) {
            // 捕获所有异常，确保不影响其他插件
            log.error("[TagForward] 处理消息时发生异常: chatId={}, messageId={}, type={}", 
                    chatId, messageId, messageType, e);
        }
        
        // 始终返回CONTINUE，确保不影响其他插件
        return PluginResult.CONTINUE;
    }
    
    /**
     * 从不同类型的消息中提取文本内容
     * 
     * @param entity 消息实体
     * @return 提取的文本内容，如果没有文本则返回 null
     */
    private String extractTextContent(BaseMessageEntity entity) {
        return switch (entity) {
            case TextMessageEntity text -> text.getTextContent();
            case PhotoMessageEntity photo -> photo.getCaption();
            case VideoMessageEntity video -> video.getCaption();
            case DocumentMessageEntity doc -> doc.getCaption();
            case AudioMessageEntity audio -> audio.getCaption();
            case VoiceMessageEntity voice -> voice.getCaption();
            case AnimationMessageEntity animation -> animation.getCaption();
            case TelegraphMessageEntity telegraph -> telegraph.getTextContent();
            case PollMessageEntity poll -> poll.getQuestion();
            case MediaGroupMessageEntity mediaGroup -> {
                // 对于媒体组，合并所有项的文本内容
                if (mediaGroup.getItems() == null || mediaGroup.getItems().isEmpty()) {
                    yield null;
                }
                StringBuilder combined = new StringBuilder();
                for (BaseMessageEntity item : mediaGroup.getItems()) {
                    String itemText = extractTextContent(item);
                    if (itemText != null && !itemText.isEmpty()) {
                        if (combined.length() > 0) {
                            combined.append(" ");
                        }
                        combined.append(itemText);
                    }
                }
                yield combined.length() > 0 ? combined.toString() : null;
            }
            default -> null;
        };
    }
    
    @Override
    public void destroy() {
        try {
            log.info("正在销毁 TagBasedMessageForwardingPlugin...");
            
            // 停止转发调度器
            forwardScheduler.stop();
            
            log.info("TagBasedMessageForwardingPlugin 销毁成功");
            
        } catch (Exception e) {
            log.error("销毁 TagBasedMessageForwardingPlugin 时出错", e);
        }
    }
    
    /**
     * 验证目标频道配置
     * 
     * <p>检查目标频道ID是否已配置且为有效的负数（Telegram频道ID格式）
     * 
     * @throws IllegalStateException 如果目标频道配置无效
     */
    private void validateTargetChannel() {
        Long targetChannelId = properties.getTargetChannelId();
        
        if (targetChannelId == null) {
            throw new IllegalStateException(
                    "目标频道 ID 未配置。" +
                    "请在 application.yaml 中设置 plugin.tag-based-forwarding.target-channel-id");
        }
        
        if (targetChannelId >= 0) {
            throw new IllegalStateException(
                    "目标频道 ID 必须为负数（Telegram 频道 ID 格式）。" +
                    "当前值: " + targetChannelId);
        }
        
        log.info("目标频道验证通过: {}", targetChannelId);
    }
}
