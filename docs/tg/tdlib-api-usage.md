# TDLib API 使用指南

本文档说明如何使用 TDLib API 进行常见的消息操作。

## 目录

- [获取特定频道的未读消息](#获取特定频道的未读消息)
- [标记未读消息为已读](#标记未读消息为已读)
- [完整使用示例](#完整使用示例)

---

## 获取特定频道的未读消息

使用 `GetChatHistory` API 来获取聊天历史消息。

### API 类

```java
TdApi.GetChatHistory
```

### 构造方法

```java
public GetChatHistory(
    long chatId,           // 频道的聊天ID
    long fromMessageId,    // 从哪条消息开始获取
    int offset,            // 偏移量
    int limit,             // 要返回的最大消息数量
    boolean onlyLocal      // 是否只获取本地缓存
)
```

### 参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `chatId` | `long` | 频道的聊天标识符 |
| `fromMessageId` | `long` | 从哪条消息ID开始获取历史记录。使用 `0` 表示从最新消息开始 |
| `offset` | `int` | 指定 `0` 从 fromMessageId 精确获取，或使用负数 (-99 到 -1) 来额外获取更新的消息 |
| `limit` | `int` | 返回的最大消息数量，必须为正数且不能大于 100。如果 offset 为负数，limit 必须大于或等于 -offset |
| `onlyLocal` | `boolean` | 设置为 `true` 只获取本地缓存的消息（不发送网络请求），设置为 `false` 从服务器获取 |

### 返回值

返回 `TdApi.Messages` 对象，包含消息列表。

### 重要说明

- 消息按**时间倒序**返回（即按 messageId 降序）
- 为了获得最佳性能，实际返回的消息数量由 TDLib 选择，可能小于指定的 limit
- 如果 `onlyLocal` 为 `true`，这是一个离线方法

### 使用示例

```java
// 获取频道最新的 50 条消息
long channelChatId = 123456789L;
TdApi.GetChatHistory request = new TdApi.GetChatHistory(
    channelChatId,  // 频道ID
    0,              // 从最新消息开始
    0,              // 不使用偏移
    50,             // 获取最多50条消息
    false           // 从服务器获取
);

client.send(request, result -> {
    if (result.getConstructor() == TdApi.Messages.CONSTRUCTOR) {
        TdApi.Messages messages = (TdApi.Messages) result;
        System.out.println("获取到 " + messages.messages.length + " 条消息");
        
        for (TdApi.Message message : messages.messages) {
            System.out.println("消息ID: " + message.id);
        }
    }
});
```

---

## 标记未读消息为已读

使用 `ViewMessages` API 来通知 TDLib 用户正在查看消息，这会自动将消息标记为已读。

### API 类

```java
TdApi.ViewMessages
```

### 构造方法

```java
public ViewMessages(
    long chatId,                      // 频道的聊天ID
    long[] messageIds,                // 要标记为已读的消息ID数组
    TdApi.MessageSource source,       // 消息查看来源
    boolean forceRead                 // 是否强制标记为已读
)
```

### 参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `chatId` | `long` | 频道的聊天标识符 |
| `messageIds` | `long[]` | 要查看的消息ID数组 |
| `source` | `TdApi.MessageSource` | 消息查看来源。传 `null` 让 TDLib 根据聊天打开状态自动判断 |
| `forceRead` | `boolean` | 设置为 `true` 可以强制标记消息为已读，即使聊天窗口已关闭 |

### 返回值

返回 `TdApi.Ok` 表示操作成功。

### 重要说明

- 调用 `ViewMessages` 会通知 TDLib 用户正在查看这些消息
- 这个操作会触发多个有用的活动，包括：
  - 标记消息为已读
  - 增加查看计数
  - 更新查看计数器
  - 在超级群组和频道中移除已删除的消息
- **对于赞助消息**：只有当消息的完整文本显示在屏幕上时（不包括按钮）才应标记为已查看
- 许多有用的活动取决于消息当前是否正在被查看

### 使用示例

```java
// 标记指定消息为已读
long channelChatId = 123456789L;
long[] messageIds = {1001L, 1002L, 1003L};

TdApi.ViewMessages request = new TdApi.ViewMessages(
    channelChatId,  // 频道ID
    messageIds,     // 消息ID数组
    null,           // 自动判断来源
    true            // 强制标记为已读
);

client.send(request, result -> {
    if (result.getConstructor() == TdApi.Ok.CONSTRUCTOR) {
        System.out.println("消息已成功标记为已读");
    }
});
```

---

## 完整使用示例

### 示例 1：基础消息读取

以下是一个完整的示例，展示如何获取频道的未读消息并将它们标记为已读。

```java
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.Client;
import java.util.ArrayList;
import java.util.List;

public class ChannelMessageReader {
    
    private final Client client;
    
    public ChannelMessageReader(Client client) {
        this.client = client;
    }
    
    /**
     * 获取频道的未读消息并标记为已读
     * 
     * @param channelChatId 频道的聊天ID
     * @param maxMessages 最多获取的消息数量
     */
    public void readChannelMessages(long channelChatId, int maxMessages) {
        // 1. 获取频道的消息历史
        TdApi.GetChatHistory getChatHistory = new TdApi.GetChatHistory(
            channelChatId,
            0,              // 从最新消息开始
            0,              // 不使用偏移
            maxMessages,    // 获取指定数量的消息
            false           // 从服务器获取
        );
        
        client.send(getChatHistory, result -> {
            if (result.getConstructor() == TdApi.Messages.CONSTRUCTOR) {
                TdApi.Messages messages = (TdApi.Messages) result;
                
                System.out.println("获取到 " + messages.messages.length + " 条消息");
                
                // 收集所有消息的ID
                List<Long> messageIdList = new ArrayList<>();
                for (TdApi.Message message : messages.messages) {
                    messageIdList.add(message.id);
                    
                    // 可以在这里添加业务逻辑来判断哪些消息是未读的
                    // 例如：检查 message.isOutgoing 或其他属性
                }
                
                // 2. 标记这些消息为已读
                if (!messageIdList.isEmpty()) {
                    markMessagesAsRead(channelChatId, messageIdList);
                } else {
                    System.out.println("没有消息需要标记为已读");
                }
            } else if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                TdApi.Error error = (TdApi.Error) result;
                System.err.println("获取消息失败: " + error.message);
            }
        });
    }
    
    /**
     * 标记消息为已读
     * 
     * @param chatId 聊天ID
     * @param messageIds 消息ID列表
     */
    private void markMessagesAsRead(long chatId, List<Long> messageIds) {
        // 将 List<Long> 转换为 long[]
        long[] messageIdsArray = messageIds.stream()
            .mapToLong(Long::longValue)
            .toArray();
        
        TdApi.ViewMessages viewMessages = new TdApi.ViewMessages(
            chatId,
            messageIdsArray,
            null,    // 自动判断消息来源
            true     // 强制标记为已读，即使聊天已关闭
        );
        
        client.send(viewMessages, result -> {
            if (result.getConstructor() == TdApi.Ok.CONSTRUCTOR) {
                System.out.println("成功标记 " + messageIds.size() + " 条消息为已读");
            } else if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                TdApi.Error error = (TdApi.Error) result;
                System.err.println("标记消息为已读失败: " + error.message);
            }
        });
    }
    
    /**
     * 获取频道的未读消息数量
     * 
     * @param chatId 聊天ID
     */
    public void getUnreadCount(long chatId) {
        TdApi.GetChat getChat = new TdApi.GetChat(chatId);
        
        client.send(getChat, result -> {
            if (result.getConstructor() == TdApi.Chat.CONSTRUCTOR) {
                TdApi.Chat chat = (TdApi.Chat) result;
                System.out.println("未读消息数量: " + chat.unreadCount);
                System.out.println("未读提及数量: " + chat.unreadMentionCount);
            }
        });
    }
}
```

### 示例 2：自动获取特定频道的未读消息列表

以下是一个更完整的示例，展示如何自动获取特定频道的未读消息列表，包括消息详情和智能过滤。

```java
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.Client;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 自动获取频道未读消息的工具类
 * 
 * 特性：
 * - 自动分批次获取所有未读消息，不限制总数量
 * - 单次请求限制为 50 条，避免触发 API 速率限制
 * - 递归获取直到所有未读消息获取完成
 */
public class UnreadMessageFetcher {
    
    private final Client client;
    
    // 单次从服务器获取的最大消息数量，避免触发 API 限制
    private static final int BATCH_SIZE = 50;
    
    public UnreadMessageFetcher(Client client) {
        this.client = client;
    }
    
    /**
     * 未读消息信息
     */
    public static class UnreadMessageInfo {
        public long messageId;
        public long chatId;
        public String chatTitle;
        public int date;
        public String content;
        public String senderName;
        public boolean isChannel;
        
        @Override
        public String toString() {
            return String.format(
                "消息ID: %d, 频道: %s, 发送者: %s, 时间: %d, 内容: %s",
                messageId, chatTitle, senderName, date, content
            );
        }
    }
    
    // 单次从服务器获取的最大消息数量，避免触发 API 限制
    private static final int BATCH_SIZE = 50;
    
    /**
     * 自动获取特定频道的所有未读消息列表
     * 会分批次获取，直到获取完所有未读消息
     * 
     * @param channelChatId 频道的聊天ID
     * @param callback 回调函数，接收未读消息列表
     */
    public void fetchUnreadMessages(long channelChatId, Consumer<List<UnreadMessageInfo>> callback) {
        // 第一步：获取聊天信息，确定未读消息数量
        TdApi.GetChat getChat = new TdApi.GetChat(channelChatId);
        
        client.send(getChat, chatResult -> {
            if (chatResult.getConstructor() == TdApi.Chat.CONSTRUCTOR) {
                TdApi.Chat chat = (TdApi.Chat) chatResult;
                int unreadCount = chat.unreadCount;
                
                System.out.println("频道: " + chat.title);
                System.out.println("未读消息数量: " + unreadCount);
                
                if (unreadCount == 0) {
                    System.out.println("没有未读消息");
                    callback.accept(new ArrayList<>());
                    return;
                }
                
                // 第二步：分批次获取所有未读消息
                List<UnreadMessageInfo> allUnreadMessages = new ArrayList<>();
                fetchMessagesRecursively(chatId, chat, 0, unreadCount, allUnreadMessages, callback);
                
            } else if (chatResult.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                TdApi.Error error = (TdApi.Error) chatResult;
                System.err.println("获取聊天信息失败: " + error.message);
                callback.accept(new ArrayList<>());
            }
        });
    }
    
    /**
     * 递归获取消息，直到获取完所有未读消息
     * 
     * @param chatId 聊天ID
     * @param chat 聊天对象
     * @param fromMessageId 从哪条消息开始获取（0表示最新）
     * @param remainingCount 还需要获取的未读消息数量
     * @param collectedMessages 已收集的消息列表
     * @param callback 最终回调
     */
    private void fetchMessagesRecursively(
            long chatId,
            TdApi.Chat chat,
            long fromMessageId,
            int remainingCount,
            List<UnreadMessageInfo> collectedMessages,
            Consumer<List<UnreadMessageInfo>> callback) {
        
        if (remainingCount <= 0) {
            // 所有未读消息已获取完成
            System.out.println("总共获取到 " + collectedMessages.size() + " 条未读消息");
            callback.accept(collectedMessages);
            return;
        }
        
        // 计算本次获取的数量：取剩余数量和批次大小的较小值
        int fetchLimit = Math.min(remainingCount, BATCH_SIZE);
        
        System.out.println("正在获取消息，剩余: " + remainingCount + "，本次获取: " + fetchLimit);
        
        TdApi.GetChatHistory getChatHistory = new TdApi.GetChatHistory(
            chatId,
            fromMessageId,  // 从指定消息开始
            0,              // 不使用偏移
            fetchLimit,     // 本次获取的数量
            false           // 从服务器获取
        );
        
        client.send(getChatHistory, messagesResult -> {
            if (messagesResult.getConstructor() == TdApi.Messages.CONSTRUCTOR) {
                TdApi.Messages messages = (TdApi.Messages) messagesResult;
                
                System.out.println("本次获取到 " + messages.messages.length + " 条消息");
                
                if (messages.messages.length == 0) {
                    // 没有更多消息了
                    System.out.println("没有更多消息，获取结束");
                    callback.accept(collectedMessages);
                    return;
                }
                
                // 提取消息信息
                for (TdApi.Message message : messages.messages) {
                    UnreadMessageInfo info = extractMessageInfo(message, chat);
                    collectedMessages.add(info);
                }
                
                // 获取最后一条消息的ID，作为下次获取的起点
                long lastMessageId = messages.messages[messages.messages.length - 1].id;
                
                // 计算还需要获取的数量
                int newRemainingCount = remainingCount - messages.messages.length;
                
                // 继续获取剩余的消息
                fetchMessagesRecursively(
                    chatId, 
                    chat, 
                    lastMessageId, 
                    newRemainingCount, 
                    collectedMessages, 
                    callback
                );
                
            } else if (messagesResult.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                TdApi.Error error = (TdApi.Error) messagesResult;
                System.err.println("获取消息历史失败: " + error.message);
                // 即使出错，也返回已收集的消息
                callback.accept(collectedMessages);
            }
        });
    }
    
    /**
     * 提取消息信息
     */
    private UnreadMessageInfo extractMessageInfo(TdApi.Message message, TdApi.Chat chat) {
        UnreadMessageInfo info = new UnreadMessageInfo();
        info.messageId = message.id;
        info.chatId = message.chatId;
        info.chatTitle = chat.title;
        info.date = message.date;
        info.isChannel = chat.type.getConstructor() == TdApi.ChatTypeSupergroup.CONSTRUCTOR &&
                        ((TdApi.ChatTypeSupergroup) chat.type).isChannel;
        
        // 提取发送者名称
        info.senderName = extractSenderName(message);
        
        // 提取消息内容
        info.content = extractMessageContent(message.content);
        
        return info;
    }
    
    /**
     * 提取发送者名称
     */
    private String extractSenderName(TdApi.Message message) {
        if (message.senderId.getConstructor() == TdApi.MessageSenderUser.CONSTRUCTOR) {
            long userId = ((TdApi.MessageSenderUser) message.senderId).userId;
            return "用户ID: " + userId;
        } else if (message.senderId.getConstructor() == TdApi.MessageSenderChat.CONSTRUCTOR) {
            long chatId = ((TdApi.MessageSenderChat) message.senderId).chatId;
            return "频道ID: " + chatId;
        }
        return "未知发送者";
    }
    
    /**
     * 提取消息内容摘要
     */
    private String extractMessageContent(TdApi.MessageContent content) {
        switch (content.getConstructor()) {
            case TdApi.MessageText.CONSTRUCTOR:
                TdApi.MessageText textMessage = (TdApi.MessageText) content;
                return textMessage.text.text;
                
            case TdApi.MessagePhoto.CONSTRUCTOR:
                TdApi.MessagePhoto photoMessage = (TdApi.MessagePhoto) content;
                return "[图片]" + (photoMessage.caption != null ? " " + photoMessage.caption.text : "");
                
            case TdApi.MessageVideo.CONSTRUCTOR:
                TdApi.MessageVideo videoMessage = (TdApi.MessageVideo) content;
                return "[视频]" + (videoMessage.caption != null ? " " + videoMessage.caption.text : "");
                
            case TdApi.MessageDocument.CONSTRUCTOR:
                TdApi.MessageDocument docMessage = (TdApi.MessageDocument) content;
                return "[文档] " + docMessage.document.fileName;
                
            case TdApi.MessageAudio.CONSTRUCTOR:
                TdApi.MessageAudio audioMessage = (TdApi.MessageAudio) content;
                return "[音频] " + audioMessage.audio.fileName;
                
            case TdApi.MessageVoiceNote.CONSTRUCTOR:
                return "[语音消息]";
                
            case TdApi.MessageVideoNote.CONSTRUCTOR:
                return "[视频消息]";
                
            case TdApi.MessageSticker.CONSTRUCTOR:
                TdApi.MessageSticker stickerMessage = (TdApi.MessageSticker) content;
                return "[贴纸] " + stickerMessage.sticker.emoji;
                
            case TdApi.MessageAnimation.CONSTRUCTOR:
                return "[动画]";
                
            case TdApi.MessageLocation.CONSTRUCTOR:
                return "[位置]";
                
            case TdApi.MessagePoll.CONSTRUCTOR:
                TdApi.MessagePoll pollMessage = (TdApi.MessagePoll) content;
                return "[投票] " + pollMessage.poll.question.text;
                
            default:
                return "[其他类型消息]";
        }
    }
    
    /**
     * 获取未读消息并自动标记为已读
     * 
     * @param channelChatId 频道的聊天ID
     */
    public void fetchAndMarkAsRead(long channelChatId) {
        fetchUnreadMessages(channelChatId, unreadMessages -> {
            if (unreadMessages.isEmpty()) {
                System.out.println("没有未读消息需要标记");
                return;
            }
            
            // 提取消息ID
            long[] messageIds = unreadMessages.stream()
                .mapToLong(msg -> msg.messageId)
                .toArray();
            
            // 标记为已读
            TdApi.ViewMessages viewMessages = new TdApi.ViewMessages(
                channelChatId,
                messageIds,
                null,
                true
            );
            
            client.send(viewMessages, result -> {
                if (result.getConstructor() == TdApi.Ok.CONSTRUCTOR) {
                    System.out.println("成功标记 " + messageIds.length + " 条消息为已读");
                } else if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                    TdApi.Error error = (TdApi.Error) result;
                    System.err.println("标记消息为已读失败: " + error.message);
                }
            });
        });
    }
    
    /**
     * 批量获取多个频道的未读消息
     * 
     * @param channelChatIds 频道ID列表
     * @param callback 回调函数，接收所有未读消息
     */
    public void fetchUnreadMessagesFromMultipleChannels(
            List<Long> channelChatIds,
            Consumer<List<UnreadMessageInfo>> callback) {
        
        List<UnreadMessageInfo> allUnreadMessages = new ArrayList<>();
        final int[] completedCount = {0};
        
        for (long chatId : channelChatIds) {
            fetchUnreadMessages(chatId, unreadMessages -> {
                synchronized (allUnreadMessages) {
                    allUnreadMessages.addAll(unreadMessages);
                    completedCount[0]++;
                    
                    // 所有频道都处理完成
                    if (completedCount[0] == channelChatIds.size()) {
                        System.out.println("总共获取到 " + allUnreadMessages.size() + " 条未读消息");
                        callback.accept(allUnreadMessages);
                    }
                }
            });
        }
    }
}
```

### 使用方法

#### 基础用法：读取单个频道的未读消息

```java
// 创建 TDLib 客户端
Client client = Client.create(...);

// 创建未读消息获取器
UnreadMessageFetcher fetcher = new UnreadMessageFetcher(client);

// 获取频道ID（可以通过搜索频道或其他方式获得）
long channelChatId = 123456789L;

// 方式1：仅获取未读消息列表（自动分批次获取所有未读消息）
fetcher.fetchUnreadMessages(channelChatId, unreadMessages -> {
    System.out.println("获取到 " + unreadMessages.size() + " 条未读消息");
    
    for (UnreadMessageFetcher.UnreadMessageInfo msg : unreadMessages) {
        System.out.println("消息: " + msg.content);
        System.out.println("发送者: " + msg.senderName);
        System.out.println("时间: " + msg.date);
        System.out.println("---");
    }
});

// 方式2：获取未读消息并自动标记为已读
fetcher.fetchAndMarkAsRead(channelChatId);
```

**注意**：`fetchUnreadMessages` 会自动分批次获取所有未读消息：
- 单次从服务器获取最多 50 条消息（避免 API 限制）
- 如果未读消息超过 50 条，会自动递归获取剩余消息
- 例如：如果有 150 条未读消息，会分 3 次获取（50 + 50 + 50）

#### 高级配置：自定义批次大小

如果需要调整批次大小以适应不同的使用场景，可以修改 `UnreadMessageFetcher` 类：

```java
/**
 * 支持自定义批次大小的未读消息获取器
 */
public class ConfigurableUnreadMessageFetcher extends UnreadMessageFetcher {
    
    private final int batchSize;
    
    /**
     * @param client TDLib 客户端
     * @param batchSize 单次获取的消息数量（建议 20-100 之间）
     */
    public ConfigurableUnreadMessageFetcher(Client client, int batchSize) {
        super(client);
        // 限制批次大小在合理范围内
        this.batchSize = Math.max(10, Math.min(batchSize, 100));
    }
    
    // 重写批次大小常量的使用...
}

// 使用示例
// 对于网络较慢的环境，使用较小的批次
ConfigurableUnreadMessageFetcher slowNetworkFetcher = 
    new ConfigurableUnreadMessageFetcher(client, 20);

// 对于网络良好的环境，使用较大的批次
ConfigurableUnreadMessageFetcher fastNetworkFetcher = 
    new ConfigurableUnreadMessageFetcher(client, 100);
```

**批次大小选择建议**：
- **20-30 条**：网络不稳定或 API 限制严格的情况
- **50 条**（默认）：平衡性能和稳定性的推荐值
- **80-100 条**：网络良好且需要快速获取大量消息时

#### 高级用法：批量处理多个频道

```java
// 创建未读消息获取器
UnreadMessageFetcher fetcher = new UnreadMessageFetcher(client);

// 定义多个频道ID
List<Long> channelIds = Arrays.asList(
    123456789L,
    987654321L,
    555666777L
);

// 批量获取所有频道的未读消息
fetcher.fetchUnreadMessagesFromMultipleChannels(channelIds, allUnreadMessages -> {
    System.out.println("从 " + channelIds.size() + " 个频道获取到 " + 
                       allUnreadMessages.size() + " 条未读消息");
    
    // 按频道分组显示
    Map<String, List<UnreadMessageFetcher.UnreadMessageInfo>> messagesByChannel = 
        allUnreadMessages.stream()
            .collect(Collectors.groupingBy(msg -> msg.chatTitle));
    
    for (Map.Entry<String, List<UnreadMessageFetcher.UnreadMessageInfo>> entry : 
         messagesByChannel.entrySet()) {
        System.out.println("\n频道: " + entry.getKey());
        System.out.println("未读消息数: " + entry.getValue().size());
        
        for (UnreadMessageFetcher.UnreadMessageInfo msg : entry.getValue()) {
            System.out.println("  - " + msg.content);
        }
    }
});
```

#### 实际应用场景：定时检查未读消息

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UnreadMessageMonitor {
    
    private final UnreadMessageFetcher fetcher;
    private final List<Long> monitoredChannels;
    private final ScheduledExecutorService scheduler;
    
    public UnreadMessageMonitor(Client client, List<Long> channelIds) {
        this.fetcher = new UnreadMessageFetcher(client);
        this.monitoredChannels = channelIds;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * 启动定时监控
     * @param intervalMinutes 检查间隔（分钟）
     */
    public void startMonitoring(int intervalMinutes) {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("开始检查未读消息...");
            
            fetcher.fetchUnreadMessagesFromMultipleChannels(
                monitoredChannels, 
                unreadMessages -> {
                    if (!unreadMessages.isEmpty()) {
                        System.out.println("发现 " + unreadMessages.size() + " 条新消息");
                        processUnreadMessages(unreadMessages);
                    } else {
                        System.out.println("没有新消息");
                    }
                }
            );
        }, 0, intervalMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * 处理未读消息（可以根据需要自定义）
     */
    private void processUnreadMessages(
            List<UnreadMessageFetcher.UnreadMessageInfo> messages) {
        
        for (UnreadMessageFetcher.UnreadMessageInfo msg : messages) {
            // 根据消息内容执行不同的操作
            if (msg.content.contains("重要")) {
                System.out.println("⚠️ 重要消息: " + msg.content);
                // 发送通知、记录日志等
            }
            
            // 可以根据频道进行不同处理
            if (msg.chatTitle.contains("工作群")) {
                // 工作相关消息的特殊处理
            }
        }
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        scheduler.shutdown();
    }
}

// 使用示例
Client client = Client.create(...);
List<Long> channelIds = Arrays.asList(123456789L, 987654321L);

UnreadMessageMonitor monitor = new UnreadMessageMonitor(client, channelIds);
monitor.startMonitoring(5); // 每5分钟检查一次

// 程序结束时停止监控
// monitor.stopMonitoring();
```

#### 与基础示例的对比

```java
// 如果只需要简单的消息读取功能，使用 ChannelMessageReader
ChannelMessageReader reader = new ChannelMessageReader(client);
reader.readChannelMessages(channelChatId, 100);
reader.getUnreadCount(channelChatId);

// 如果需要更详细的未读消息信息和批量处理，使用 UnreadMessageFetcher
UnreadMessageFetcher fetcher = new UnreadMessageFetcher(client);
fetcher.fetchUnreadMessages(channelChatId, messages -> {
    // 可以访问每条消息的详细信息
    for (UnreadMessageFetcher.UnreadMessageInfo msg : messages) {
        System.out.println(msg.senderName + ": " + msg.content);
    }
});
```

---

## 相关 API

### 获取聊天信息

```java
TdApi.GetChat getChat = new TdApi.GetChat(chatId);
```

返回 `TdApi.Chat` 对象，包含：
- `unreadCount`: 未读消息数量
- `unreadMentionCount`: 未读提及数量
- `unreadReactionCount`: 未读反应数量

### 更新事件

当消息被标记为已读时，TDLib 会发送以下更新事件：

- `UpdateChatReadInbox`: 收到的消息被读取或未读消息数量改变
- `UpdateChatReadOutbox`: 发出的消息被读取

监听这些更新以保持应用状态同步：

```java
client.send(new TdApi.GetCurrentState(), result -> {
    // 处理当前状态
});

// 在结果处理器中监听更新
if (result.getConstructor() == TdApi.UpdateChatReadInbox.CONSTRUCTOR) {
    TdApi.UpdateChatReadInbox update = (TdApi.UpdateChatReadInbox) result;
    System.out.println("聊天 " + update.chatId + " 的未读数量: " + update.unreadCount);
}
```

---

## 注意事项

1. **API 速率限制**：
   - TDLib 有 API 调用频率限制，避免短时间内大量请求
   - `UnreadMessageFetcher` 使用批次大小为 50 条消息，在性能和限制之间取得平衡
   - 对于大量未读消息（如 500+ 条），会自动分多次获取，每次间隔由 TDLib 内部控制
   
2. **批量处理**：尽量批量标记消息为已读，而不是逐条标记

3. **错误处理**：始终检查返回结果的类型，处理可能的错误情况

4. **异步操作**：所有 TDLib API 调用都是异步的，使用回调处理结果

5. **消息来源**：对于不同的消息来源（通知、聊天列表、聊天历史等），可以指定不同的 `MessageSource`

6. **内存管理**：
   - 如果频道有大量未读消息（如数千条），考虑分批处理而不是一次性加载所有消息到内存
   - 可以在回调中逐步处理消息，而不是等待所有消息获取完成

7. **网络状况**：
   - 批次获取依赖网络连接，如果网络不稳定可能导致部分批次失败
   - 建议添加重试机制或错误恢复逻辑

---

## 参考文档

- TDLib 官方文档：[https://core.telegram.org/tdlib](https://core.telegram.org/tdlib)
- TDLib Java 示例：[https://github.com/tdlib/td/tree/master/example/java](https://github.com/tdlib/td/tree/master/example/java)
- API 参考：`docs/t3d/tdlib/`
