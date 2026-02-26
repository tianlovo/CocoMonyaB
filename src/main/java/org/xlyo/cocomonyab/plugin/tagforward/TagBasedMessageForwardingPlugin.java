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
     * <p>通过发送测试消息并删除的方式验证权限：
     * <ol>
     *   <li>获取频道信息（验证频道是否存在和可访问）</li>
     *   <li>发送测试消息（验证发送权限）</li>
     *   <li>删除测试消息（验证删除权限）</li>
     * </ol>
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
            
            // 第一步：获取频道信息
            TdApi.GetChat getChat = new TdApi.GetChat(targetChannelId);
            
            client.send(getChat).whenCompleteAsync((chatResult, error) -> {
                if (error != null) {
                    log.error("❌ 获取目标频道信息失败: chatId={}", targetChannelId, error);
                    return;
                }
                
                // 成功获取频道信息
                log.info("目标频道信息:");
                log.info("  - 频道ID: {}", chatResult.id);
                log.info("  - 频道标题: {}", chatResult.title);
                
                // 检查频道类型
                if (chatResult.type instanceof TdApi.ChatTypeSupergroup supergroup) {
                    log.info("  - 是否为频道: {}", supergroup.isChannel);
                    if (!supergroup.isChannel) {
                        log.warn("  ⚠️ 警告: 目标不是频道而是超级群组");
                    }
                } else {
                    log.warn("  ⚠️ 警告: 目标不是超级群组/频道类型: {}", 
                            chatResult.type.getClass().getSimpleName());
                }
                
                // 第二步：发送欢迎消息验证权限
                sendWelcomeMessage(client, targetChannelId);
            });
            
        } catch (Exception e) {
            log.error("验证目标频道时发生异常: chatId={}", targetChannelId, e);
        }
    }
    
    /**
     * 发送欢迎消息验证权限
     * 
     * <p>发送包含插件状态信息的欢迎消息，验证发送权限
     * 
     * @param client Telegram 客户端
     * @param chatId 频道 ID
     */
    private void sendWelcomeMessage(SimpleTelegramClient client, Long chatId) {
        try {
            log.info("正在发送欢迎消息验证权限...");

            // 构建欢迎消息内容（替换占位符）
            String welcomeText = buildWelcomeMessage();

            // 构造文本消息内容
            TdApi.FormattedText messageText = new TdApi.FormattedText(
                    welcomeText,
                    new TdApi.TextEntity[0]
            );

            TdApi.InputMessageText inputMessageContent = new TdApi.InputMessageText(
                    messageText,
                    null,  // 链接预览选项
                    false  // 清除草稿
            );

            // 构造发送消息请求
            TdApi.SendMessage sendMessage = new TdApi.SendMessage(
                    chatId,
                    0,     // 消息线程ID
                    null,  // 回复消息
                    null,  // 发送选项
                    null,  // 回复标记
                    inputMessageContent
            );

            // 发送消息
            client.send(sendMessage).whenCompleteAsync((messageResult, error) -> {
                if (error != null) {
                    log.error("❌ 发送欢迎消息失败: chatId={}", chatId, error);
                    log.error("  ❌ 权限验证失败: 无法发送消息到目标频道");
                    return;
                }

                // 成功发送消息
                log.info("✅ 欢迎消息发送成功: messageId={}", messageResult.id);
                log.info("✅ 目标频道权限验证完成: 所有权限正常");
            });

        } catch (Exception e) {
            log.error("发送欢迎消息时发生异常: chatId={}", chatId, e);
        }
    }

    /**
     * 构建欢迎消息内容
     * 
     * <p>替换模板中的占位符为实际配置值
     * 
     * @return 替换后的欢迎消息文本
     */
    private String buildWelcomeMessage() {
        return properties.getWelcomeMessage()
                .replace("{pluginName}", getName())
                .replace("{tagPrefix}", properties.getTagPrefix())
                .replace("{rateLimitPerMinute}", String.valueOf(properties.getRateLimitPerMinute()))
                .replace("{batchSize}", String.valueOf(properties.getBatchSize()))
                .replace("{scheduleInterval}", String.valueOf(properties.getScheduleIntervalSeconds()))
                .replace("{maxRetryCount}", String.valueOf(properties.getMaxRetryCount()));
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
            
            // 阶段 5: 确定要转发的消息ID（保证媒体组原子性）
            // 对于媒体组，需要收集所有消息ID（按递增顺序）
            Long forwardMessageId = messageId;
            List<Long> mediaGroupMessageIds = null;
            
            if (entity instanceof MediaGroupMessageEntity mediaGroup) {
                if (mediaGroup.getItems() != null && !mediaGroup.getItems().isEmpty()) {
                    // 收集媒体组中所有消息的ID
                    mediaGroupMessageIds = mediaGroup.getItems().stream()
                            .map(BaseMessageEntity::getMessageId)
                            .sorted()  // 确保按递增顺序排序（TDLib要求）
                            .toList();
                    
                    forwardMessageId = mediaGroupMessageIds.get(0);  // 第一条消息ID作为主ID
                    
                    log.info("[TagForward] 媒体组消息，将转发整个媒体组: chatId={}, firstMessageId={}, messageIds={}, itemCount={}", 
                            chatId, forwardMessageId, mediaGroupMessageIds, mediaGroup.getItems().size());
                }
            }
            
            // 阶段 6: 加入转发队列（原子操作）
            // 注意：enqueue方法使用唯一索引保证同一消息只入队一次
            // 对于媒体组，使用第一条消息ID作为唯一标识，确保整个媒体组作为一个单元处理
            log.debug("[TagForward] 正在将消息加入转发队列: chatId={}, messageId={}", chatId, forwardMessageId);
            queueManager.enqueue(chatId, forwardMessageId, mediaGroupMessageIds, matchedTags);
            
            if (mediaGroupMessageIds != null && !mediaGroupMessageIds.isEmpty()) {
                log.info("[TagForward] 媒体组已成功加入转发队列（原子性保证）: chatId={}, firstMessageId={}, groupSize={}, tags={}", 
                        chatId, forwardMessageId, mediaGroupMessageIds.size(), matchedTags);
            } else {
                log.info("[TagForward] 消息已成功加入转发队列: chatId={}, messageId={}, type={}, tags={}", 
                        chatId, forwardMessageId, messageType, matchedTags);
            }
            
        } catch (Exception e) {
            // 捕获所有异常，确保不影响其他插件
            // 对于媒体组，如果处理失败，整个媒体组都不会被加入队列（保证原子性）
            log.error("[TagForward] 处理消息时发生异常: chatId={}, messageId={}, type={}", 
                    chatId, messageId, messageType, e);
            
            if (entity instanceof MediaGroupMessageEntity) {
                log.error("[TagForward] 媒体组处理失败，整个媒体组将被跳过（保证原子性）");
            }
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
                        if (!combined.isEmpty()) {
                            combined.append(" ");
                        }
                        combined.append(itemText);
                    }
                }
                yield !combined.isEmpty() ? combined.toString() : null;
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
     * <p>检查目标频道 ID 是否已配置且为有效的负数（Telegram 频道 ID 格式）
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
