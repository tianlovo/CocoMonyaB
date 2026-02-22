# Telegram 频道实时监控技术方案

## 一、方案概述

本方案基于 TDLight Java 客户端（TDLib 的 Java 封装）实现对特定 Telegram 频道的实时消息监控功能。通过注册 `UpdateNewMessage` 更新处理器，当监控的频道发布新消息时，系统能够实时接收并处理这些消息。

### 核心技术栈
- **TDLight Java**: 3.4.0+td.1.8.26
- **Spring Boot**: 4.0.3
- **MongoDB**: 用于存储频道配置和消息数据
- **WebSocket**: 用于实时推送消息到前端

## 二、TDLib 消息监控机制

### 2.1 Update 机制原理

TDLib 采用事件驱动的 Update 机制，所有来自 Telegram 服务器的变化都会以 Update 对象的形式推送到客户端。

#### 关键 Update 类型（消息相关）

| Update 类型 | 说明 | 使用场景 |
|------------|------|---------|
| `UpdateNewMessage` | 接收到新消息（包括频道消息） | 实时监控频道新消息 |
| `UpdateMessageContent` | 消息内容被修改 | 监控消息编辑 |
| `UpdateMessageEdited` | 消息被编辑 | 获取编辑时间戳 |
| `UpdateDeleteMessages` | 消息被删除 | 监控消息删除 |
| `UpdateChatLastMessage` | 聊天最后一条消息变化 | 更新频道最新消息 |


### 2.2 UpdateNewMessage 结构

```java
public class UpdateNewMessage extends Update {
    public Message message;  // 消息对象
}

public class Message {
    public long id;                    // 消息ID
    public MessageSender senderId;     // 发送者ID
    public long chatId;                // 聊天/频道ID
    public MessageSendingState sendingState;
    public MessageSchedulingState schedulingState;
    public boolean isOutgoing;         // 是否为发出的消息
    public boolean isPinned;           // 是否置顶
    public boolean isFromOffline;
    public boolean canBeEdited;
    public boolean canBeForwarded;
    public boolean canBeSaved;
    public boolean canBeDeletedOnlyForSelf;
    public boolean canBeDeletedForAllUsers;
    public boolean canGetAddedReactions;
    public boolean canGetStatistics;
    public boolean canGetMessageThread;
    public boolean canGetReadDate;
    public boolean canGetViewers;
    public boolean canGetMediaTimestampLinks;
    public boolean canReportReactions;
    public boolean hasTimestampedMedia;
    public boolean isChannelPost;      // 是否为频道帖子
    public boolean isTopicMessage;
    public boolean containsUnreadMention;
    public int date;                   // 发送时间戳
    public int editDate;               // 编辑时间戳
    public MessageForwardInfo forwardInfo;
    public MessageImportInfo importInfo;
    public MessageInteractionInfo interactionInfo;
    public MessageReplyTo replyTo;
    public long messageThreadId;
    public MessageSelfDestructType selfDestructType;
    public double selfDestructIn;
    public double autoDeleteIn;
    public int viaBotUserId;
    public long senderBusinessBotUserId;
    public int senderBoostCount;
    public String authorSignature;
    public long mediaAlbumId;
    public long effectId;
    public boolean hasSensitiveContent;
    public String restrictionReason;
    public MessageContent content;     // 消息内容（文本、图片、视频等）
    public ReplyMarkup replyMarkup;
}
```

### 2.3 频道识别方法

判断消息是否来自频道：
1. `message.isChannelPost == true` - 明确标识为频道帖子
2. `message.chatId` - 通过聊天ID判断是否为监控的频道
3. `message.senderId` - 发送者类型为 `MessageSenderChat`（频道作为发送者）


## 三、实现方案设计

### 3.1 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Telegram Server                          │
└────────────────────────┬────────────────────────────────────┘
                         │ TDLib Protocol
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              TelegramClientManager (已有)                    │
│  - 客户端初始化                                               │
│  - 认证管理                                                   │
│  - Update 分发                                                │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ NewMessage   │  │ MessageEdit  │  │ DeleteMessage│
│ Handler      │  │ Handler      │  │ Handler      │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ↓
              ┌─────────────────────┐
              │ ChannelMonitorService│
              │  - 频道过滤           │
              │  - 消息处理           │
              │  - 数据持久化         │
              └──────────┬───────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   MongoDB    │  │  WebSocket   │  │  业务逻辑    │
│  消息存储    │  │  实时推送    │  │  (审核/编辑) │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 3.2 核心组件设计

#### 3.2.1 频道监控服务 (ChannelMonitorService)

**职责**：
- 管理监控频道列表（从 MongoDB 加载）
- 过滤非监控频道的消息
- 协调消息处理流程
- 提供监控状态查询接口

**核心方法**：
```java
public class ChannelMonitorService {
    // 判断频道是否在监控列表中
    boolean isMonitoring(long chatId);
    
    // 处理新消息
    void handleNewMessage(Message message);
    
    // 启动/停止监控特定频道
    void startMonitoring(long chatId);
    void stopMonitoring(long chatId);
    
    // 获取监控统计信息
    MonitoringStats getStats(long chatId);
}
```


#### 3.2.2 消息处理器增强 (TgUpdateNewMessageHandler)

**当前状态**：空实现
**需要增强**：
```java
@Component
@RequiredArgsConstructor
public class TgUpdateNewMessageHandler {
    
    private final ChannelMonitorService channelMonitorService;
    
    public void onNewMessageUpdate(TdApi.UpdateNewMessage update) {
        Message message = update.message;
        
        // 1. 基础过滤
        if (!message.isChannelPost) {
            return; // 只处理频道消息
        }
        
        // 2. 检查是否为监控频道
        if (!channelMonitorService.isMonitoring(message.chatId)) {
            return;
        }
        
        // 3. 委托给监控服务处理
        channelMonitorService.handleNewMessage(message);
    }
}
```

#### 3.2.3 消息实体设计 (ChannelMessage)

```java
@Document(collection = "channel_messages")
@Data
public class ChannelMessage {
    @Id
    private String id;                    // MongoDB ID
    
    @Indexed
    private Long messageId;               // Telegram 消息ID
    
    @Indexed
    private Long chatId;                  // 频道ID
    
    private String channelUsername;       // 频道用户名
    private String channelTitle;          // 频道标题
    
    private Integer date;                 // 发送时间戳
    private Integer editDate;             // 编辑时间戳
    
    private String contentType;           // 内容类型：text/photo/video/document
    private String textContent;           // 文本内容
    private List<MediaFile> mediaFiles;   // 媒体文件列表
    
    private Integer views;                // 浏览次数
    private Integer forwards;             // 转发次数
    
    @Indexed
    private MessageStatus status;         // 消息状态：PENDING/APPROVED/REJECTED
    
    private LocalDateTime createTime;     // 入库时间
    private LocalDateTime updateTime;     // 更新时间
}

@Data
public class MediaFile {
    private String fileId;                // TDLib 文件ID
    private String fileType;              // photo/video/document/audio
    private Long fileSize;                // 文件大小
    private String mimeType;              // MIME类型
    private String localPath;             // 本地存储路径
    private Boolean downloaded;           // 是否已下载
}

public enum MessageStatus {
    PENDING,    // 待审核
    APPROVED,   // 已通过
    REJECTED    // 已拒绝
}
```


### 3.3 频道信息获取

#### 3.3.1 通过用户名搜索频道

```java
// 使用 searchPublicChat 方法
TdApi.SearchPublicChat request = new TdApi.SearchPublicChat("channelUsername");
client.send(request, result -> {
    if (result.isError()) {
        log.error("搜索频道失败: {}", result.error);
        return;
    }
    TdApi.Chat chat = (TdApi.Chat) result.object;
    long chatId = chat.id;
    String title = chat.title;
    // 保存到数据库
});
```

#### 3.3.2 获取频道详细信息

```java
// 使用 getChat 方法
TdApi.GetChat request = new TdApi.GetChat(chatId);
client.send(request, result -> {
    if (result.isError()) {
        log.error("获取频道信息失败: {}", result.error);
        return;
    }
    TdApi.Chat chat = (TdApi.Chat) result.object;
    
    // 频道基本信息
    String title = chat.title;
    TdApi.ChatPhotoInfo photo = chat.photo;
    
    // 频道类型判断
    if (chat.type instanceof TdApi.ChatTypeSupergroup) {
        TdApi.ChatTypeSupergroup supergroup = 
            (TdApi.ChatTypeSupergroup) chat.type;
        boolean isChannel = supergroup.isChannel;
        long supergroupId = supergroup.supergroupId;
    }
});
```

#### 3.3.3 获取频道完整信息

```java
// 使用 getSupergroupFullInfo 获取更多信息
TdApi.GetSupergroupFullInfo request = 
    new TdApi.GetSupergroupFullInfo(supergroupId);
client.send(request, result -> {
    if (result.isError()) {
        return;
    }
    TdApi.SupergroupFullInfo info = 
        (TdApi.SupergroupFullInfo) result.object;
    
    String description = info.description;
    int memberCount = info.memberCount;
    boolean canGetMembers = info.canGetMembers;
    String inviteLink = info.inviteLink != null ? 
        info.inviteLink.inviteLink : null;
});
```


## 四、实现步骤

### 4.1 第一阶段：基础监控功能

#### 步骤 1：创建消息实体和仓储

1. 创建 `ChannelMessage` 实体类
2. 创建 `ChannelMessageRepository` 接口
3. 添加必要的索引（chatId, messageId, status, createTime）

#### 步骤 2：实现频道监控服务

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelMonitorService {
    
    private final ChannelRepository channelRepository;
    private final ChannelMessageRepository messageRepository;
    private final TelegramClientManager clientManager;
    
    // 缓存监控中的频道ID（提高性能）
    private final Set<Long> monitoringChannels = 
        Collections.synchronizedSet(new HashSet<>());
    
    @PostConstruct
    public void initialize() {
        // 从数据库加载启用监控的频道
        List<Channel> channels = channelRepository
            .findByMonitoringStatus(true);
        
        channels.forEach(channel -> {
            monitoringChannels.add(channel.getChannelId());
            log.info("已加载监控频道: {} ({})", 
                channel.getChannelTitle(), 
                channel.getChannelId());
        });
    }
    
    public boolean isMonitoring(long chatId) {
        return monitoringChannels.contains(chatId);
    }
    
    public void handleNewMessage(TdApi.Message message) {
        try {
            // 1. 解析消息内容
            ChannelMessage channelMessage = parseMessage(message);
            
            // 2. 保存到数据库
            messageRepository.save(channelMessage);
            
            // 3. 记录日志
            log.info("收到频道消息: chatId={}, messageId={}, type={}", 
                message.chatId, 
                message.id, 
                channelMessage.getContentType());
            
            // 4. 触发后续处理（可选）
            // - WebSocket 推送
            // - 媒体文件下载
            // - 业务逻辑处理
            
        } catch (Exception e) {
            log.error("处理频道消息失败: chatId={}, messageId={}", 
                message.chatId, message.id, e);
        }
    }
    
    private ChannelMessage parseMessage(TdApi.Message message) {
        ChannelMessage channelMessage = new ChannelMessage();
        channelMessage.setMessageId(message.id);
        channelMessage.setChatId(message.chatId);
        channelMessage.setDate(message.date);
        channelMessage.setEditDate(message.editDate);
        channelMessage.setStatus(MessageStatus.PENDING);
        channelMessage.setCreateTime(LocalDateTime.now());
        channelMessage.setUpdateTime(LocalDateTime.now());
        
        // 解析消息内容
        parseMessageContent(message.content, channelMessage);
        
        // 获取频道信息（异步）
        fetchChannelInfo(message.chatId, channelMessage);
        
        return channelMessage;
    }
    
    private void parseMessageContent(
        TdApi.MessageContent content, 
        ChannelMessage message) {
        
        switch (content) {
            case TdApi.MessageText text -> {
                message.setContentType("text");
                message.setTextContent(text.text.text);
            }
            case TdApi.MessagePhoto photo -> {
                message.setContentType("photo");
                message.setTextContent(photo.caption.text);
                message.setMediaFiles(parsePhotoSizes(photo.photo));
            }
            case TdApi.MessageVideo video -> {
                message.setContentType("video");
                message.setTextContent(video.caption.text);
                message.setMediaFiles(List.of(parseFile(video.video.video)));
            }
            case TdApi.MessageDocument document -> {
                message.setContentType("document");
                message.setTextContent(document.caption.text);
                message.setMediaFiles(List.of(parseFile(document.document.document)));
            }
            default -> {
                message.setContentType("other");
                message.setTextContent(content.getClass().getSimpleName());
            }
        }
    }
    
    private List<MediaFile> parsePhotoSizes(TdApi.Photo photo) {
        return Arrays.stream(photo.sizes)
            .map(size -> {
                MediaFile file = new MediaFile();
                file.setFileId(String.valueOf(size.photo.id));
                file.setFileType("photo");
                file.setFileSize((long) size.photo.size);
                file.setDownloaded(size.photo.local.isDownloadingCompleted);
                file.setLocalPath(size.photo.local.path);
                return file;
            })
            .collect(Collectors.toList());
    }
    
    private MediaFile parseFile(TdApi.File file) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(String.valueOf(file.id));
        mediaFile.setFileSize((long) file.size);
        mediaFile.setDownloaded(file.local.isDownloadingCompleted);
        mediaFile.setLocalPath(file.local.path);
        return mediaFile;
    }
    
    private void fetchChannelInfo(long chatId, ChannelMessage message) {
        SimpleTelegramClient client = clientManager.getClient();
        
        client.send(new TdApi.GetChat(chatId), result -> {
            if (!result.isError()) {
                TdApi.Chat chat = (TdApi.Chat) result.object;
                message.setChannelTitle(chat.title);
                
                // 获取用户名
                if (chat.type instanceof TdApi.ChatTypeSupergroup) {
                    // 需要额外调用 getSupergroup 获取 username
                }
            }
        });
    }
}
```


#### 步骤 3：增强 TelegramClientManager

在 `TelegramClientManager` 中注入 `ChannelMonitorService`：

```java
@Component
@RequiredArgsConstructor
public class TelegramClientManager {
    
    // ... 现有字段 ...
    
    private final TgUpdateNewMessageHandler updateNewMessageHandler;
    
    @PostConstruct
    public void initialize() {
        // ... 现有代码 ...
        
        // 注册更新处理器
        clientBuilder.addUpdateHandler(
            TdApi.UpdateNewMessage.class, 
            updateNewMessageHandler::onNewMessageUpdate
        );
        
        // ... 现有代码 ...
    }
}
```

#### 步骤 4：完善消息处理器

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TgUpdateNewMessageHandler {
    
    private final ChannelMonitorService channelMonitorService;
    
    public void onNewMessageUpdate(TdApi.UpdateNewMessage update) {
        TdApi.Message message = update.message;
        
        // 只处理频道消息
        if (!message.isChannelPost) {
            return;
        }
        
        // 检查是否为监控频道
        if (!channelMonitorService.isMonitoring(message.chatId)) {
            return;
        }
        
        // 处理消息
        channelMonitorService.handleNewMessage(message);
    }
}
```

### 4.2 第二阶段：频道管理功能

#### 添加频道到监控列表

```java
@Service
@RequiredArgsConstructor
public class ChannelService {
    
    private final ChannelRepository channelRepository;
    private final TelegramClientManager clientManager;
    private final ChannelMonitorService monitorService;
    
    /**
     * 通过用户名添加频道
     */
    public ChannelVO addChannelByUsername(String username) {
        SimpleTelegramClient client = clientManager.getClient();
        
        // 1. 搜索频道
        TdApi.SearchPublicChat request = 
            new TdApi.SearchPublicChat(username);
        
        CompletableFuture<TdApi.Chat> future = new CompletableFuture<>();
        client.send(request, result -> {
            if (result.isError()) {
                future.completeExceptionally(
                    new BusinessException(ResponseCode.EXTERNAL_SERVICE_ERROR, 
                        "频道不存在或无法访问: " + username));
            } else {
                future.complete((TdApi.Chat) result.object);
            }
        });
        
        try {
            TdApi.Chat chat = future.get(10, TimeUnit.SECONDS);
            
            // 2. 验证是否为频道
            if (!(chat.type instanceof TdApi.ChatTypeSupergroup)) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, 
                    "不是有效的频道");
            }
            
            TdApi.ChatTypeSupergroup supergroup = 
                (TdApi.ChatTypeSupergroup) chat.type;
            if (!supergroup.isChannel) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, 
                    "这是一个群组，不是频道");
            }
            
            // 3. 检查是否已存在
            if (channelRepository.existsByChannelId(chat.id)) {
                throw new BusinessException(ResponseCode.DATA_ALREADY_EXISTS, 
                    "频道已在监控列表中");
            }
            
            // 4. 保存到数据库
            Channel channel = new Channel();
            channel.setChannelId(chat.id);
            channel.setChannelUsername(username);
            channel.setChannelTitle(chat.title);
            channel.setMonitoringStatus(true);
            channel.setCreateTime(LocalDateTime.now());
            channel.setUpdateTime(LocalDateTime.now());
            
            Channel saved = channelRepository.save(channel);
            
            // 5. 启动监控
            monitorService.startMonitoring(chat.id);
            
            return convertToVO(saved);
            
        } catch (TimeoutException e) {
            throw new BusinessException(ResponseCode.EXTERNAL_SERVICE_ERROR, 
                "请求超时");
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, 
                "添加频道失败: " + e.getMessage());
        }
    }
}
```


### 4.3 第三阶段：WebSocket 实时推送

#### WebSocket 消息推送服务

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePushService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 推送新消息到所有订阅的客户端
     */
    public void pushNewMessage(ChannelMessage message) {
        try {
            MessagePushVO vo = convertToPushVO(message);
            
            // 推送到主题：/topic/channel/messages
            messagingTemplate.convertAndSend(
                "/topic/channel/messages", 
                vo
            );
            
            log.debug("已推送消息: chatId={}, messageId={}", 
                message.getChatId(), 
                message.getMessageId());
                
        } catch (Exception e) {
            log.error("推送消息失败", e);
        }
    }
    
    /**
     * 推送到特定频道的订阅者
     */
    public void pushToChannel(Long chatId, ChannelMessage message) {
        try {
            MessagePushVO vo = convertToPushVO(message);
            
            // 推送到特定频道主题：/topic/channel/{chatId}
            messagingTemplate.convertAndSend(
                "/topic/channel/" + chatId, 
                vo
            );
            
        } catch (Exception e) {
            log.error("推送频道消息失败: chatId={}", chatId, e);
        }
    }
    
    private MessagePushVO convertToPushVO(ChannelMessage message) {
        MessagePushVO vo = new MessagePushVO();
        vo.setMessageId(message.getMessageId());
        vo.setChatId(message.getChatId());
        vo.setChannelTitle(message.getChannelTitle());
        vo.setContentType(message.getContentType());
        vo.setTextContent(message.getTextContent());
        vo.setDate(message.getDate());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
```

#### 在监控服务中集成推送

```java
@Service
@RequiredArgsConstructor
public class ChannelMonitorService {
    
    // ... 现有字段 ...
    private final MessagePushService pushService;
    
    public void handleNewMessage(TdApi.Message message) {
        try {
            // 1. 解析并保存消息
            ChannelMessage channelMessage = parseMessage(message);
            ChannelMessage saved = messageRepository.save(channelMessage);
            
            // 2. WebSocket 推送
            pushService.pushNewMessage(saved);
            pushService.pushToChannel(message.chatId, saved);
            
            // 3. 记录日志
            log.info("已处理并推送频道消息: chatId={}, messageId={}", 
                message.chatId, message.id);
                
        } catch (Exception e) {
            log.error("处理频道消息失败", e);
        }
    }
}
```


### 4.4 第四阶段：媒体文件下载

#### 文件下载服务

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaDownloadService {
    
    private final TelegramClientManager clientManager;
    private final ChannelMessageRepository messageRepository;
    
    @Value("${telegram.download-directory}")
    private String downloadDirectory;
    
    /**
     * 下载消息中的所有媒体文件
     */
    public void downloadMessageMedia(ChannelMessage message) {
        if (message.getMediaFiles() == null || 
            message.getMediaFiles().isEmpty()) {
            return;
        }
        
        SimpleTelegramClient client = clientManager.getClient();
        
        for (MediaFile mediaFile : message.getMediaFiles()) {
            if (Boolean.TRUE.equals(mediaFile.getDownloaded())) {
                continue; // 已下载，跳过
            }
            
            int fileId = Integer.parseInt(mediaFile.getFileId());
            
            // 创建下载请求
            TdApi.DownloadFile request = new TdApi.DownloadFile();
            request.fileId = fileId;
            request.priority = 1;
            request.offset = 0;
            request.limit = 0; // 0 表示下载整个文件
            request.synchronous = false; // 异步下载
            
            client.send(request, result -> {
                if (result.isError()) {
                    log.error("下载文件失败: fileId={}, error={}", 
                        fileId, result.error);
                    return;
                }
                
                TdApi.File file = (TdApi.File) result.object;
                
                if (file.local.isDownloadingCompleted) {
                    // 更新数据库
                    mediaFile.setDownloaded(true);
                    mediaFile.setLocalPath(file.local.path);
                    messageRepository.save(message);
                    
                    log.info("文件下载完成: fileId={}, path={}", 
                        fileId, file.local.path);
                }
            });
        }
    }
    
    /**
     * 监听文件下载进度
     */
    public void onFileUpdate(TdApi.UpdateFile update) {
        TdApi.File file = update.file;
        
        if (file.local.isDownloadingCompleted) {
            log.info("文件下载完成: fileId={}, size={}, path={}", 
                file.id, 
                file.size, 
                file.local.path);
            
            // 更新数据库中的文件状态
            updateFileStatus(file);
        } else if (file.local.isDownloadingActive) {
            int progress = file.local.downloadedSize * 100 / file.size;
            log.debug("文件下载中: fileId={}, progress={}%", 
                file.id, progress);
        }
    }
    
    private void updateFileStatus(TdApi.File file) {
        // 查找包含此文件的消息并更新
        String fileId = String.valueOf(file.id);
        
        // 这里需要根据实际情况实现查询逻辑
        // 可以考虑维护一个 fileId -> messageId 的映射
    }
}
```

#### 注册文件更新处理器

在 `TelegramClientManager` 中添加：

```java
@PostConstruct
public void initialize() {
    // ... 现有代码 ...
    
    // 注册文件更新处理器
    clientBuilder.addUpdateHandler(
        TdApi.UpdateFile.class, 
        mediaDownloadService::onFileUpdate
    );
    
    // ... 现有代码 ...
}
```


## 五、关键技术点

### 5.1 消息去重

由于网络波动或客户端重连，可能收到重复的消息更新。需要实现去重机制：

```java
@Service
public class ChannelMonitorService {
    
    public void handleNewMessage(TdApi.Message message) {
        // 使用 chatId + messageId 作为唯一标识
        boolean exists = messageRepository.existsByChatIdAndMessageId(
            message.chatId, 
            message.id
        );
        
        if (exists) {
            log.debug("消息已存在，跳过: chatId={}, messageId={}", 
                message.chatId, message.id);
            return;
        }
        
        // 继续处理...
    }
}
```

在 `ChannelMessageRepository` 中添加：

```java
public interface ChannelMessageRepository 
    extends MongoRepository<ChannelMessage, String> {
    
    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);
    
    Optional<ChannelMessage> findByChatIdAndMessageId(
        Long chatId, Long messageId);
}
```

### 5.2 异步处理

消息处理应该是异步的，避免阻塞 TDLib 的更新线程：

```java
@Service
public class ChannelMonitorService {
    
    @Async("messageProcessorExecutor")
    public void handleNewMessage(TdApi.Message message) {
        // 异步处理消息
    }
}

@Configuration
public class AsyncConfiguration {
    
    @Bean(name = "messageProcessorExecutor")
    public Executor messageProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("msg-processor-");
        executor.initialize();
        return executor;
    }
}
```

### 5.3 错误处理和重试

```java
@Service
public class ChannelMonitorService {
    
    @Retryable(
        value = {DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public void handleNewMessage(TdApi.Message message) {
        try {
            // 处理逻辑
        } catch (Exception e) {
            log.error("处理消息失败，将重试: chatId={}, messageId={}", 
                message.chatId, message.id, e);
            throw e;
        }
    }
    
    @Recover
    public void recover(DataAccessException e, TdApi.Message message) {
        log.error("消息处理最终失败: chatId={}, messageId={}", 
            message.chatId, message.id, e);
        // 可以将失败的消息记录到死信队列
    }
}
```


### 5.4 监控频道缓存管理

```java
@Service
@RequiredArgsConstructor
public class ChannelMonitorService {
    
    private final Set<Long> monitoringChannels = 
        Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 启动监控
     */
    public void startMonitoring(long chatId) {
        monitoringChannels.add(chatId);
        log.info("已启动频道监控: chatId={}", chatId);
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring(long chatId) {
        monitoringChannels.remove(chatId);
        log.info("已停止频道监控: chatId={}", chatId);
    }
    
    /**
     * 重新加载监控列表（用于配置变更后）
     */
    public void reloadMonitoringChannels() {
        monitoringChannels.clear();
        
        List<Channel> channels = channelRepository
            .findByMonitoringStatus(true);
        
        channels.forEach(channel -> 
            monitoringChannels.add(channel.getChannelId())
        );
        
        log.info("已重新加载监控频道列表，共 {} 个频道", 
            monitoringChannels.size());
    }
}
```

### 5.5 消息内容类型完整处理

```java
private void parseMessageContent(
    TdApi.MessageContent content, 
    ChannelMessage message) {
    
    switch (content) {
        case TdApi.MessageText text -> {
            message.setContentType("text");
            message.setTextContent(text.text.text);
        }
        
        case TdApi.MessagePhoto photo -> {
            message.setContentType("photo");
            message.setTextContent(photo.caption.text);
            message.setMediaFiles(parsePhotoSizes(photo.photo));
        }
        
        case TdApi.MessageVideo video -> {
            message.setContentType("video");
            message.setTextContent(video.caption.text);
            MediaFile file = parseFile(video.video.video);
            file.setFileType("video");
            file.setMimeType(video.video.mimeType);
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessageDocument document -> {
            message.setContentType("document");
            message.setTextContent(document.caption.text);
            MediaFile file = parseFile(document.document.document);
            file.setFileType("document");
            file.setMimeType(document.document.mimeType);
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessageAudio audio -> {
            message.setContentType("audio");
            message.setTextContent(audio.caption.text);
            MediaFile file = parseFile(audio.audio.audio);
            file.setFileType("audio");
            file.setMimeType(audio.audio.mimeType);
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessageVoiceNote voice -> {
            message.setContentType("voice");
            MediaFile file = parseFile(voice.voiceNote.voice);
            file.setFileType("voice");
            file.setMimeType(voice.voiceNote.mimeType);
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessageVideoNote videoNote -> {
            message.setContentType("video_note");
            MediaFile file = parseFile(videoNote.videoNote.video);
            file.setFileType("video_note");
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessageAnimation animation -> {
            message.setContentType("animation");
            message.setTextContent(animation.caption.text);
            MediaFile file = parseFile(animation.animation.animation);
            file.setFileType("animation");
            file.setMimeType(animation.animation.mimeType);
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessageSticker sticker -> {
            message.setContentType("sticker");
            MediaFile file = parseFile(sticker.sticker.sticker);
            file.setFileType("sticker");
            message.setMediaFiles(List.of(file));
        }
        
        case TdApi.MessagePoll poll -> {
            message.setContentType("poll");
            message.setTextContent(poll.poll.question.text);
        }
        
        default -> {
            message.setContentType("other");
            message.setTextContent(content.getClass().getSimpleName());
        }
    }
}
```


## 六、API 接口设计

### 6.1 频道管理接口

```java
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    
    private final ChannelService channelService;
    
    /**
     * 通过用户名添加频道
     */
    @PostMapping("/add-by-username")
    public ApiResponse<ChannelVO> addByUsername(
        @RequestParam String username) {
        ChannelVO channel = channelService.addChannelByUsername(username);
        return ApiResponse.success(channel);
    }
    
    /**
     * 启用/禁用频道监控
     */
    @PutMapping("/{id}/monitoring")
    public ApiResponse<ChannelVO> toggleMonitoring(
        @PathVariable String id,
        @RequestParam Boolean enabled) {
        ChannelVO channel = channelService.toggleMonitoring(id, enabled);
        return ApiResponse.success(channel);
    }
    
    /**
     * 获取监控统计
     */
    @GetMapping("/{id}/stats")
    public ApiResponse<ChannelStatsVO> getStats(@PathVariable String id) {
        ChannelStatsVO stats = channelService.getChannelStats(id);
        return ApiResponse.success(stats);
    }
}
```

### 6.2 消息查询接口

```java
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * 分页查询频道消息
     */
    @GetMapping
    public ApiResponse<PageResponse<MessageVO>> listMessages(
        @RequestParam(required = false) Long chatId,
        @RequestParam(required = false) String contentType,
        @RequestParam(required = false) MessageStatus status,
        @RequestParam(defaultValue = "1") Long current,
        @RequestParam(defaultValue = "20") Long size) {
        
        MessageQueryDTO query = new MessageQueryDTO();
        query.setChatId(chatId);
        query.setContentType(contentType);
        query.setStatus(status);
        
        List<MessageVO> messages = messageService.page(current, size, query);
        Long total = messageService.count(query);
        
        return ApiResponse.success(new PageResponse<>(messages, total));
    }
    
    /**
     * 获取消息详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MessageVO> getMessageDetail(@PathVariable String id) {
        MessageVO message = messageService.getById(id);
        return ApiResponse.success(message);
    }
    
    /**
     * 更新消息状态（审核）
     */
    @PutMapping("/{id}/status")
    public ApiResponse<MessageVO> updateStatus(
        @PathVariable String id,
        @RequestParam MessageStatus status) {
        MessageVO message = messageService.updateStatus(id, status);
        return ApiResponse.success(message);
    }
    
    /**
     * 下载消息媒体文件
     */
    @PostMapping("/{id}/download")
    public ApiResponse<Void> downloadMedia(@PathVariable String id) {
        messageService.downloadMedia(id);
        return ApiResponse.success();
    }
}
```


## 七、WebSocket 订阅主题

### 7.1 前端订阅示例

```javascript
// 连接 WebSocket
const socket = new SockJS('http://localhost:10721/ws');
const stompClient = Stomp.over(socket);

// 连接配置
const headers = {
    'X-Auth-Token': 'your-trusted-token'
};

stompClient.connect(headers, function(frame) {
    console.log('Connected: ' + frame);
    
    // 订阅所有频道消息
    stompClient.subscribe('/topic/channel/messages', function(message) {
        const data = JSON.parse(message.body);
        console.log('收到新消息:', data);
        // 更新 UI
        displayNewMessage(data);
    });
    
    // 订阅特定频道消息
    const chatId = 1234567890;
    stompClient.subscribe(`/topic/channel/${chatId}`, function(message) {
        const data = JSON.parse(message.body);
        console.log(`频道 ${chatId} 新消息:`, data);
        // 更新特定频道的 UI
        displayChannelMessage(chatId, data);
    });
});

// 发送心跳
setInterval(() => {
    stompClient.send('/app/heartbeat', {}, JSON.stringify({
        timestamp: Date.now()
    }));
}, 10000);
```

### 7.2 消息推送数据格式

```json
{
    "messageId": 123456,
    "chatId": 1234567890,
    "channelTitle": "示例频道",
    "channelUsername": "example_channel",
    "contentType": "photo",
    "textContent": "这是一张图片的描述",
    "date": 1708617600,
    "createTime": "2024-02-22T20:00:00",
    "mediaFiles": [
        {
            "fileId": "987654",
            "fileType": "photo",
            "fileSize": 1024000,
            "mimeType": "image/jpeg",
            "downloaded": false
        }
    ],
    "status": "PENDING"
}
```


## 八、配置说明

### 8.1 application.yaml 新增配置

```yaml
# Telegram 配置
telegram:
  # ... 现有配置 ...
  
  # 消息处理配置
  message:
    # 是否自动下载媒体文件
    auto-download-media: true
    # 自动下载的文件类型（photo/video/document/audio）
    auto-download-types: photo,video
    # 单个文件最大下载大小（MB）
    max-download-size: 100
    # 是否启用消息去重
    enable-deduplication: true
  
  # 监控配置
  monitor:
    # 是否在启动时自动加载监控频道
    auto-load-channels: true
    # 消息处理线程池大小
    thread-pool-size: 10
    # 消息队列容量
    queue-capacity: 100

# MongoDB 索引配置
spring:
  data:
    mongodb:
      auto-index-creation: true
```

### 8.2 MongoDB 索引创建

```java
@Configuration
public class MongoIndexConfiguration {
    
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Collections.emptyList());
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initIndicesAfterStartup() {
        // 索引会通过 @Indexed 注解自动创建
        // 这里可以创建复合索引
    }
}

// 在 ChannelMessage 实体中添加复合索引
@Document(collection = "channel_messages")
@CompoundIndexes({
    @CompoundIndex(
        name = "chat_message_idx", 
        def = "{'chatId': 1, 'messageId': 1}", 
        unique = true
    ),
    @CompoundIndex(
        name = "chat_date_idx", 
        def = "{'chatId': 1, 'date': -1}"
    ),
    @CompoundIndex(
        name = "status_date_idx", 
        def = "{'status': 1, 'createTime': -1}"
    )
})
public class ChannelMessage {
    // ... 字段定义 ...
}
```


## 九、测试方案

### 9.1 单元测试

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yaml")
class ChannelMonitorServiceTest {
    
    @Autowired
    private ChannelMonitorService monitorService;
    
    @Autowired
    private ChannelMessageRepository messageRepository;
    
    @Test
    void testHandleNewMessage() {
        // 创建测试消息
        TdApi.Message message = new TdApi.Message();
        message.id = 123456L;
        message.chatId = 1234567890L;
        message.isChannelPost = true;
        message.date = (int) (System.currentTimeMillis() / 1000);
        
        TdApi.MessageText textContent = new TdApi.MessageText();
        textContent.text = new TdApi.FormattedText();
        textContent.text.text = "测试消息";
        message.content = textContent;
        
        // 处理消息
        monitorService.handleNewMessage(message);
        
        // 验证消息已保存
        Optional<ChannelMessage> saved = messageRepository
            .findByChatIdAndMessageId(message.chatId, message.id);
        
        assertTrue(saved.isPresent());
        assertEquals("text", saved.get().getContentType());
        assertEquals("测试消息", saved.get().getTextContent());
    }
    
    @Test
    void testMessageDeduplication() {
        // 创建测试消息
        TdApi.Message message = createTestMessage();
        
        // 第一次处理
        monitorService.handleNewMessage(message);
        
        // 第二次处理（重复）
        monitorService.handleNewMessage(message);
        
        // 验证只保存了一条
        long count = messageRepository.countByChatIdAndMessageId(
            message.chatId, message.id);
        assertEquals(1, count);
    }
}
```

### 9.2 集成测试

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChannelMonitorIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = 
        new MongoDBContainer("mongo:7.0.12");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", 
            mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testAddChannelByUsername() {
        // 测试添加频道
        String url = "/api/channels/add-by-username?username=test_channel";
        
        ResponseEntity<ApiResponse> response = restTemplate
            .postForEntity(url, null, ApiResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
    }
}
```

### 9.3 手动测试步骤

1. **启动应用**
   ```bash
   ./gradlew bootRun
   ```

2. **添加测试频道**
   ```bash
   curl -X POST "http://localhost:10721/api/channels/add-by-username?username=test_channel"
   ```

3. **查看监控频道列表**
   ```bash
   curl "http://localhost:10721/api/channels"
   ```

4. **在 Telegram 中向测试频道发送消息**

5. **查询收到的消息**
   ```bash
   curl "http://localhost:10721/api/messages?chatId=1234567890"
   ```

6. **测试 WebSocket 连接**
   - 使用浏览器开发者工具或 WebSocket 客户端
   - 连接到 `ws://localhost:10721/ws`
   - 订阅 `/topic/channel/messages`
   - 发送消息到测试频道，观察是否收到推送


## 十、性能优化建议

### 10.1 数据库优化

1. **索引优化**
   - 为高频查询字段创建索引：`chatId`, `messageId`, `status`, `createTime`
   - 使用复合索引优化多条件查询
   - 定期分析慢查询并优化

2. **数据归档**
   ```java
   @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
   public void archiveOldMessages() {
       LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
       
       // 将3个月前的消息移动到归档集合
       List<ChannelMessage> oldMessages = messageRepository
           .findByCreateTimeBefore(cutoffDate);
       
       if (!oldMessages.isEmpty()) {
           archiveRepository.saveAll(oldMessages);
           messageRepository.deleteAll(oldMessages);
           
           log.info("已归档 {} 条历史消息", oldMessages.size());
       }
   }
   ```

3. **分片策略**
   - 对于大量消息的场景，考虑按频道或时间分片
   - 使用 MongoDB 的 Sharding 功能

### 10.2 内存优化

1. **监控频道缓存**
   ```java
   @Service
   public class ChannelMonitorService {
       
       // 使用 Caffeine 缓存
       private final LoadingCache<Long, Boolean> monitoringCache = 
           Caffeine.newBuilder()
               .maximumSize(1000)
               .expireAfterWrite(10, TimeUnit.MINUTES)
               .build(this::loadMonitoringStatus);
       
       private Boolean loadMonitoringStatus(Long chatId) {
           return channelRepository.existsByChannelIdAndMonitoringStatus(
               chatId, true);
       }
       
       public boolean isMonitoring(long chatId) {
           return monitoringCache.get(chatId);
       }
   }
   ```

2. **消息批量处理**
   ```java
   @Service
   public class BatchMessageProcessor {
       
       private final List<ChannelMessage> messageBuffer = 
           new CopyOnWriteArrayList<>();
       
       private static final int BATCH_SIZE = 50;
       
       public void addMessage(ChannelMessage message) {
           messageBuffer.add(message);
           
           if (messageBuffer.size() >= BATCH_SIZE) {
               flushMessages();
           }
       }
       
       @Scheduled(fixedDelay = 5000) // 每5秒刷新一次
       public void flushMessages() {
           if (messageBuffer.isEmpty()) {
               return;
           }
           
           List<ChannelMessage> toSave = new ArrayList<>(messageBuffer);
           messageBuffer.clear();
           
           messageRepository.saveAll(toSave);
           log.info("批量保存 {} 条消息", toSave.size());
       }
   }
   ```

### 10.3 网络优化

1. **文件下载限流**
   ```java
   @Service
   public class MediaDownloadService {
       
       private final RateLimiter downloadLimiter = 
           RateLimiter.create(5.0); // 每秒最多5个下载请求
       
       public void downloadFile(int fileId) {
           downloadLimiter.acquire(); // 获取许可
           
           // 执行下载
           client.send(new TdApi.DownloadFile(fileId, ...));
       }
   }
   ```

2. **WebSocket 消息压缩**
   ```yaml
   spring:
     websocket:
       compression:
         enabled: true
   ```


## 十一、监控和日志

### 11.1 监控指标

```java
@Service
@RequiredArgsConstructor
public class MonitoringMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // 消息接收计数器
    private final Counter messageReceivedCounter;
    
    // 消息处理耗时
    private final Timer messageProcessingTimer;
    
    @PostConstruct
    public void init() {
        messageReceivedCounter = Counter.builder("telegram.messages.received")
            .description("接收到的频道消息总数")
            .tag("type", "channel")
            .register(meterRegistry);
        
        messageProcessingTimer = Timer.builder("telegram.messages.processing")
            .description("消息处理耗时")
            .register(meterRegistry);
    }
    
    public void recordMessageReceived(String contentType) {
        messageReceivedCounter.increment();
        
        Counter.builder("telegram.messages.by.type")
            .tag("content_type", contentType)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordProcessingTime(Runnable task) {
        messageProcessingTimer.record(task);
    }
}
```

### 11.2 日志配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 频道监控专用日志 -->
    <appender name="CHANNEL_MONITOR" 
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/channel-monitor.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/channel-monitor.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="org.xlyo.cocomonyab.telegram" level="INFO" additivity="false">
        <appender-ref ref="CHANNEL_MONITOR"/>
        <appender-ref ref="CONSOLE"/>
    </logger>
    
    <logger name="org.xlyo.cocomonyab.service.ChannelMonitorService" 
            level="DEBUG" additivity="false">
        <appender-ref ref="CHANNEL_MONITOR"/>
    </logger>
</configuration>
```

### 11.3 健康检查

```java
@Component
public class TelegramHealthIndicator implements HealthIndicator {
    
    private final TelegramClientManager clientManager;
    private final ChannelMonitorService monitorService;
    
    @Override
    public Health health() {
        try {
            // 检查客户端状态
            if (!clientManager.isReady()) {
                return Health.down()
                    .withDetail("reason", "Telegram 客户端未就绪")
                    .build();
            }
            
            // 检查监控频道数量
            int monitoringCount = monitorService.getMonitoringChannelCount();
            
            return Health.up()
                .withDetail("client", "ready")
                .withDetail("monitoring_channels", monitoringCount)
                .withDetail("user", clientManager.getCurrentUser().firstName)
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```


## 十二、常见问题和解决方案

### 12.1 消息接收延迟

**问题**：消息接收存在延迟，不够实时

**原因**：
- 网络延迟
- TDLib 更新队列积压
- 消息处理逻辑耗时过长

**解决方案**：
1. 使用异步处理，避免阻塞更新线程
2. 优化消息处理逻辑，减少数据库操作
3. 使用批量保存减少 I/O 次数
4. 监控处理耗时，识别性能瓶颈

### 12.2 重复消息

**问题**：同一条消息被处理多次

**原因**：
- 网络波动导致重复推送
- 客户端重连后重新接收历史消息

**解决方案**：
```java
// 使用唯一索引防止重复插入
@CompoundIndex(
    name = "chat_message_unique", 
    def = "{'chatId': 1, 'messageId': 1}", 
    unique = true
)

// 在处理前检查是否已存在
if (messageRepository.existsByChatIdAndMessageId(chatId, messageId)) {
    return; // 跳过已处理的消息
}
```

### 12.3 频道信息获取失败

**问题**：无法获取频道标题、用户名等信息

**原因**：
- 频道为私有频道，需要先加入
- API 调用超时
- 权限不足

**解决方案**：
```java
// 1. 先尝试从缓存获取
Chat cachedChat = chatCache.get(chatId);
if (cachedChat != null) {
    return cachedChat;
}

// 2. 异步获取，避免阻塞
CompletableFuture<Chat> future = new CompletableFuture<>();
client.send(new TdApi.GetChat(chatId), result -> {
    if (result.isError()) {
        log.warn("获取频道信息失败: chatId={}, error={}", 
            chatId, result.error);
        future.complete(null);
    } else {
        Chat chat = (Chat) result.object;
        chatCache.put(chatId, chat);
        future.complete(chat);
    }
});

// 3. 设置超时
try {
    return future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    log.error("获取频道信息超时: chatId={}", chatId);
    return null;
}
```

### 12.4 媒体文件下载失败

**问题**：图片、视频等媒体文件下载失败

**原因**：
- 文件过大
- 网络不稳定
- 存储空间不足

**解决方案**：
```java
// 1. 检查文件大小
if (file.size > maxDownloadSize) {
    log.warn("文件过大，跳过下载: fileId={}, size={}", 
        file.id, file.size);
    return;
}

// 2. 检查存储空间
File downloadDir = new File(downloadDirectory);
long freeSpace = downloadDir.getFreeSpace();
if (freeSpace < file.size * 2) { // 预留2倍空间
    log.error("存储空间不足: free={}, required={}", 
        freeSpace, file.size);
    return;
}

// 3. 实现重试机制
@Retryable(
    value = {IOException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000, multiplier = 2)
)
public void downloadFile(int fileId) {
    // 下载逻辑
}
```

### 12.5 内存占用过高

**问题**：长时间运行后内存占用持续增长

**原因**：
- 消息缓存未清理
- 文件句柄未释放
- 监听器未正确注销

**解决方案**：
```java
// 1. 限制缓存大小
private final LoadingCache<Long, Chat> chatCache = 
    Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(this::loadChat);

// 2. 定期清理过期数据
@Scheduled(cron = "0 0 * * * ?") // 每小时执行
public void cleanupExpiredData() {
    chatCache.cleanUp();
    log.info("已清理过期缓存");
}

// 3. 监控内存使用
@Scheduled(fixedDelay = 60000)
public void logMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    long maxMemory = runtime.maxMemory();
    
    double usagePercent = (double) usedMemory / maxMemory * 100;
    
    if (usagePercent > 80) {
        log.warn("内存使用率过高: {}%", String.format("%.2f", usagePercent));
    }
}
```


## 十三、安全考虑

### 13.1 访问控制

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/channels/**").authenticated()
                .requestMatchers("/api/messages/**").authenticated()
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**")
            );
        
        return http.build();
    }
}
```

### 13.2 敏感信息保护

```java
@Service
public class MessageService {
    
    /**
     * 脱敏处理敏感信息
     */
    private String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        
        // 移除手机号
        content = content.replaceAll(
            "\\+?\\d{1,3}[\\s-]?\\d{3,4}[\\s-]?\\d{4,}", 
            "[PHONE]"
        );
        
        // 移除邮箱
        content = content.replaceAll(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", 
            "[EMAIL]"
        );
        
        // 移除身份证号
        content = content.replaceAll(
            "\\d{17}[\\dXx]", 
            "[ID_CARD]"
        );
        
        return content;
    }
}
```

### 13.3 频道验证

```java
@Service
public class ChannelService {
    
    /**
     * 验证频道是否可以添加
     */
    private void validateChannel(TdApi.Chat chat) {
        // 1. 验证是否为频道
        if (!(chat.type instanceof TdApi.ChatTypeSupergroup)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, 
                "不是有效的频道");
        }
        
        TdApi.ChatTypeSupergroup supergroup = 
            (TdApi.ChatTypeSupergroup) chat.type;
        
        if (!supergroup.isChannel) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, 
                "这是一个群组，不是频道");
        }
        
        // 2. 检查是否有访问权限
        // 如果无法获取频道信息，说明没有权限
        
        // 3. 检查频道是否在黑名单中
        if (isInBlacklist(chat.id)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, 
                "该频道已被禁止监控");
        }
    }
    
    private boolean isInBlacklist(long chatId) {
        // 实现黑名单检查逻辑
        return false;
    }
}
```


## 十四、部署建议

### 14.1 生产环境配置

```yaml
# application-prod.yaml
spring:
  data:
    mongodb:
      mode: remote
      uri: mongodb://username:password@mongodb-host:27017/cocomonya?authSource=admin

telegram:
  # 生产环境使用更大的线程池
  message:
    auto-download-media: true
    auto-download-types: photo
    max-download-size: 50
  
  monitor:
    thread-pool-size: 20
    queue-capacity: 500

# 日志级别
logging:
  level:
    root: INFO
    org.xlyo.cocomonyab: INFO
    it.tdlight: WARN

# 监控端点
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 14.2 Docker 部署

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 复制应用 JAR
COPY build/libs/*.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data/session/td/data \
    && mkdir -p /app/data/session/td/downloads \
    && mkdir -p /app/data/db/mongo \
    && mkdir -p /app/data/config

# 暴露端口
EXPOSE 10721

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  mongodb:
    image: mongo:7.0.12
    container_name: cocomonya-mongo
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
    volumes:
      - mongodb_data:/data/db
    ports:
      - "27017:27017"
    networks:
      - cocomonya-network

  app:
    build: .
    container_name: cocomonya-app
    restart: unless-stopped
    depends_on:
      - mongodb
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATA_MONGODB_URI: mongodb://admin:${MONGO_PASSWORD}@mongodb:27017/cocomonya?authSource=admin
    volumes:
      - ./data/config:/app/data/config
      - ./data/session:/app/data/session
      - ./data/downloads:/app/data/session/td/downloads
    ports:
      - "10721:10721"
    networks:
      - cocomonya-network

volumes:
  mongodb_data:

networks:
  cocomonya-network:
    driver: bridge
```

### 14.3 系统资源要求

**最低配置**：
- CPU: 2 核
- 内存: 2GB
- 磁盘: 20GB（根据消息量和媒体文件调整）

**推荐配置**：
- CPU: 4 核
- 内存: 4GB
- 磁盘: 100GB SSD
- 网络: 10Mbps 上下行


## 十五、总结

### 15.1 方案优势

1. **实时性强**：基于 TDLib 的 Update 机制，消息推送延迟低于 1 秒
2. **可扩展性好**：模块化设计，易于添加新功能（如消息编辑监控、删除监控等）
3. **性能优异**：异步处理、批量操作、缓存优化，支持高并发场景
4. **易于维护**：清晰的分层架构，完善的日志和监控
5. **安全可靠**：访问控制、数据脱敏、错误重试机制

### 15.2 技术要点总结

| 技术点 | 实现方式 | 说明 |
|--------|---------|------|
| 消息接收 | `UpdateNewMessage` | TDLib 更新处理器 |
| 频道识别 | `message.isChannelPost` | 判断是否为频道消息 |
| 频道搜索 | `SearchPublicChat` | 通过用户名搜索频道 |
| 频道信息 | `GetChat` / `GetSupergroupFullInfo` | 获取频道详细信息 |
| 消息存储 | MongoDB | 结构化存储，支持复杂查询 |
| 实时推送 | WebSocket + STOMP | 双向通信，支持订阅 |
| 文件下载 | `DownloadFile` | 异步下载，进度监控 |
| 消息去重 | 唯一索引 | chatId + messageId |
| 异步处理 | `@Async` + 线程池 | 避免阻塞更新线程 |
| 缓存优化 | Caffeine | 减少数据库查询 |

### 15.3 后续扩展方向

1. **消息编辑监控**：监听 `UpdateMessageEdited` 和 `UpdateMessageContent`
2. **消息删除监控**：监听 `UpdateDeleteMessages`
3. **频道统计分析**：消息数量、活跃时段、内容类型分布
4. **智能过滤**：基于关键词、正则表达式过滤消息
5. **多账号支持**：支持多个 Telegram 账号同时监控
6. **消息转发**：将监控到的消息转发到其他频道或群组
7. **AI 内容审核**：集成 AI 模型自动审核消息内容
8. **消息搜索**：全文搜索、高级过滤

### 15.4 参考资源

- **TDLib 官方文档**：https://core.telegram.org/tdlib
- **TDLight Java 文档**：https://tdlight-team.github.io/tdlight-docs/
- **Telegram Bot API**：https://core.telegram.org/bots/api
- **项目 tdlib 文档**：`docs/t3d/tdlib/`

---

**文档版本**：1.0  
**创建日期**：2024-02-22  
**作者**：Kiro AI Assistant  
**适用项目**：CocoMonyaB (基于 TG Userbot 的媒体资源存档系统)
