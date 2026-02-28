package org.xlyo.cocomonyab.plugin.tagforward;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.version.VersionInfo;
import org.xlyo.cocomonyab.domain.entity.ProcessedMessage;
import org.xlyo.cocomonyab.domain.entity.message.AnimationMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.AudioMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.DocumentMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaGroupMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.PollMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TelegraphMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.VideoMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.VoiceMessageEntity;
import org.xlyo.cocomonyab.plugin.AbstractMessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.tagforward.component.ForwardScheduler;
import org.xlyo.cocomonyab.plugin.tagforward.component.QueueManager;
import org.xlyo.cocomonyab.plugin.tagforward.component.TagMatcher;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;
import org.xlyo.cocomonyab.service.MessageReadMarkingService;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
@RequiredArgsConstructor
public class TagBasedMessageForwardingPlugin extends AbstractMessagePlugin {
    
    private final TagMatcher tagMatcher;
    private final QueueManager queueManager;
    private final ForwardScheduler forwardScheduler;
    private final TagBasedForwardingProperties properties;
    private final TelegramClientManager clientManager;
    private final MessageReadMarkingService readMarkingService;
    private final ProcessedMessageRepository processedMessageRepository;
    
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
                .replace("{version}", VersionInfo.VERSION)
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
            log.info("🏷️ [标签转发] 开始处理消息: chatId={}, messageId={}, type={}", 
                    chatId, messageId, messageType);
            
            // 阶段 0: 检查消息是否已处理
            Optional<ProcessedMessage> existingRecord = processedMessageRepository
                .findByChatIdAndMessageId(chatId, messageId);
            
            if (existingRecord.isPresent()) {
                ProcessedMessage record = existingRecord.get();
                log.info("ℹ️ [标签转发] 消息已处理过: chatId={}, messageId={}, isRead={}, isMatched={}", 
                    chatId, messageId, record.getIsRead(), record.getIsMatched());
                
                // 如果消息存在但未读，标记为已读
                if (Boolean.FALSE.equals(record.getIsRead())) {
                    log.info("📖 [标签转发] 消息未读，标记为已读: chatId={}, messageId={}", 
                        chatId, messageId);
                    
                    try {
                        readMarkingService.markAsRead(chatId, messageId);
                        
                        // 更新数据库记录
                        record.setIsRead(true);
                        record.setReadTime(LocalDateTime.now());
                        record.setUpdateTime(LocalDateTime.now());
                        processedMessageRepository.save(record);
                        
                        log.info("✅ [标签转发] 已更新消息已读状态: chatId={}, messageId={}", 
                            chatId, messageId);
                    } catch (Exception e) {
                        log.warn("⚠️ [标签转发] 标记消息为已读失败: chatId={}, messageId={}", 
                            chatId, messageId, e);
                    }
                }
                
                // 消息已处理，跳过后续处理
                return PluginResult.CONTINUE;
            }
            
            // 阶段 1: 标记消息为已读（新消息）
            try {
                log.info("📖 [标签转发] 标记新消息为已读: chatId={}, messageId={}", 
                    chatId, messageId);
                readMarkingService.markAsRead(chatId, messageId);
                log.debug("✅ [标签转发] 消息已提交标记为已读: chatId={}, messageId={}", 
                    chatId, messageId);
            } catch (Exception e) {
                // 标记失败不影响后续处理
                log.warn("⚠️ [标签转发] 标记消息为已读失败: chatId={}, messageId={}", 
                    chatId, messageId, e);
            }
            
            // 阶段 2: 检查标签配置是否已加载
            if (!tagMatcher.isConfigurationLoaded()) {
                log.warn("⚠️ [标签转发] 标签配置尚未加载，跳过消息处理: chatId={}, messageId={}", 
                        chatId, messageId);
                
                // 记录为已处理但未匹配
                saveProcessedMessage(chatId, messageId, messageType, true, false, null);
                return PluginResult.CONTINUE;
            }
            
            // 阶段 3: 提取文本内容（根据消息类型）
            String textContent = extractTextContent(entity);
            
            if (textContent == null || textContent.isEmpty()) {
                log.info("ℹ️ [标签转发] 消息无文本内容，跳过: chatId={}, messageId={}, type={}", 
                        chatId, messageId, messageType);
                
                // 记录为已处理但未匹配
                saveProcessedMessage(chatId, messageId, messageType, true, false, null);
                return PluginResult.CONTINUE;
            }
            
            log.info("📝 [标签转发] 提取文本内容成功: chatId={}, messageId={}, type={}, textLength={}",
                    chatId, messageId, messageType, textContent.length());
            
            // 阶段 4: 匹配标签
            log.info("🔍 [标签转发] 开始标签匹配: chatId={}, messageId={}", chatId, messageId);
            List<String> matchedTags = tagMatcher.matchTags(textContent);
            
            // 阶段 5: 处理匹配结果
            if (matchedTags.isEmpty()) {
                log.info("ℹ️ [标签转发] 未匹配到标签，跳过: chatId={}, messageId={}", chatId, messageId);
                
                // 记录为已处理但未匹配
                saveProcessedMessage(chatId, messageId, messageType, true, false, null);
                return PluginResult.CONTINUE;
            }
            
            log.info("✅ [标签转发] 匹配到标签: chatId={}, messageId={}, type={}, 标签数量={}, tags={}", 
                    matchedTags.size(), chatId, messageId, messageType, matchedTags);
            
            // 阶段 6: 确定要转发的消息ID（保证媒体组原子性）
            Long forwardMessageId = messageId;
            List<Long> mediaGroupMessageIds = null;
            
            if (entity instanceof MediaGroupMessageEntity mediaGroup) {
                if (mediaGroup.getItems() != null && !mediaGroup.getItems().isEmpty()) {
                    // 收集媒体组中所有消息的ID（去重并排序）
                    mediaGroupMessageIds = mediaGroup.getItems().stream()
                            .map(BaseMessageEntity::getMessageId)
                            .distinct()  // 去除重复的messageId
                            .sorted()    // 确保递增顺序
                            .toList();
                    
                    forwardMessageId = mediaGroupMessageIds.getFirst();
                    
                    log.info("📦 [标签转发] 媒体组消息，将转发整个媒体组: chatId={}, firstMessageId={}, messageIds={}, itemCount={}", 
                            chatId, forwardMessageId, mediaGroupMessageIds, mediaGroup.getItems().size());
                }
            }
            
            // 阶段 7: 加入转发队列
            log.info("📤 [标签转发] 将消息加入转发队列: chatId={}, messageId={}", chatId, forwardMessageId);
            queueManager.enqueue(chatId, forwardMessageId, mediaGroupMessageIds, matchedTags);
            
            if (mediaGroupMessageIds != null && !mediaGroupMessageIds.isEmpty()) {
                log.info("✅ [标签转发] 媒体组已加入转发队列: chatId={}, firstMessageId={}, groupSize={}, tags={}", 
                        chatId, forwardMessageId, mediaGroupMessageIds.size(), matchedTags);
            } else {
                log.info("✅ [标签转发] 消息已加入转发队列: chatId={}, messageId={}, type={}, tags={}", 
                        chatId, forwardMessageId, messageType, matchedTags);
            }
            
            // 阶段 8: 记录为已处理且已匹配
            saveProcessedMessage(chatId, messageId, messageType, true, true, 
                matchedTags.toArray(new String[0]));
            
            log.info("✅ [标签转发] 消息处理完成: chatId={}, messageId={}", chatId, messageId);
            
        } catch (Exception e) {
            log.error("❌ [标签转发] 处理消息时发生异常: chatId={}, messageId={}, type={}", 
                    chatId, messageId, messageType, e);
            
            // 即使发生异常，也记录为已处理（避免重复处理）
            try {
                saveProcessedMessage(chatId, messageId, messageType, false, false, null);
            } catch (Exception ex) {
                log.error("❌ [标签转发] 保存处理记录失败: chatId={}, messageId={}", 
                    chatId, messageId, ex);
            }
        }
        
        // 始终返回CONTINUE，确保不影响其他插件
        return PluginResult.CONTINUE;
    }
    
    /**
     * 保存已处理消息记录
     * 
     * @param chatId 频道 ID
     * @param messageId 消息 ID
     * @param messageType 消息类型
     * @param isRead 是否已读
     * @param isMatched 是否匹配标签
     * @param matchedTags 匹配到的标签
     */
    private void saveProcessedMessage(Long chatId, Long messageId, String messageType,
                                     boolean isRead, boolean isMatched, String[] matchedTags) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            ProcessedMessage record = ProcessedMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .messageType(messageType)
                .isRead(isRead)
                .isMatched(isMatched)
                .matchedTags(matchedTags)
                .processTime(now)
                .readTime(isRead ? now : null)
                .createTime(now)
                .updateTime(now)
                .build();
            
            processedMessageRepository.save(record);
            
            log.debug("[TagForward] 已保存处理记录: chatId={}, messageId={}, isRead={}, isMatched={}", 
                chatId, messageId, isRead, isMatched);
            
        } catch (Exception e) {
            log.error("[TagForward] 保存处理记录失败: chatId={}, messageId={}", 
                chatId, messageId, e);
        }
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
