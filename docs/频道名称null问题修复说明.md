# 频道名称null问题修复说明

## 问题描述

在控制台输出中，频道名称显示为null：
```
频道: null (@null)
```

## 问题原因

`MessageParser.fillBaseFields()` 方法在填充消息基础字段时，没有设置 `channelUsername` 和 `channelTitle` 字段。这是因为 TdApi.Message 对象本身不包含频道信息，需要从数据库中查询。

## 解决方案

### 1. 修改 MessageParser

在 `MessageParser` 中添加了一个重载的 `parse` 方法，接受频道信息作为参数：

```java
/**
 * 解析消息
 */
public BaseMessageEntity parse(TdApi.Message message) {
    return parse(message, null, null);
}

/**
 * 解析消息（带频道信息）
 */
public BaseMessageEntity parse(TdApi.Message message, String channelUsername, String channelTitle) {
    MessageType type = typeDetector.detectType(message);
    
    BaseMessageEntity entity = switch (type) {
        // ... 解析逻辑
    };
    
    // 设置频道信息
    if (channelUsername != null) {
        entity.setChannelUsername(channelUsername);
    }
    if (channelTitle != null) {
        entity.setChannelTitle(channelTitle);
    }
    
    return entity;
}
```

### 2. 修改 ChannelMonitorService

在 `ChannelMonitorService` 的消息处理方法中，从数据库查询频道信息并传递给解析器：

#### processSingleMessage 方法

```java
private void processSingleMessage(TdApi.Message message) {
    // 获取频道信息
    Channel channel = channelRepository.findByChannelId(message.chatId).orElse(null);
    String channelUsername = channel != null ? channel.getChannelUsername() : null;
    String channelTitle = channel != null ? channel.getChannelTitle() : null;
    
    // 保存原始消息到数据库
    messageStorageService.saveMessage(message);
    
    // 解析消息为实体类（传入频道信息）
    try {
        BaseMessageEntity entity = messageParser.parse(message, channelUsername, channelTitle);
        
        // 使用插件管理器处理
        pluginManager.process(entity, message);
        
    } catch (Exception e) {
        log.error("解析消息失败: chatId={}, messageId={}", message.chatId, message.id, e);
    }
}
```

#### processMediaGroup 方法

```java
private void processMediaGroup(List<TdApi.Message> messages) {
    // ... 前置逻辑
    
    // 获取频道信息
    Channel channel = channelRepository.findByChannelId(chatId).orElse(null);
    String channelUsername = channel != null ? channel.getChannelUsername() : null;
    String channelTitle = channel != null ? channel.getChannelTitle() : null;
    
    // 保存和解析每条消息
    List<BaseMessageEntity> parsedMessages = new ArrayList<>();
    for (TdApi.Message message : messages) {
        messageStorageService.saveMessage(message);
        
        try {
            BaseMessageEntity entity = messageParser.parse(message, channelUsername, channelTitle);
            parsedMessages.add(entity);
        } catch (Exception e) {
            log.error("解析媒体组消息失败: chatId={}, messageId={}", 
                message.chatId, message.id, e);
        }
    }
    
    // ... 后续处理
}
```

## 修改的文件

1. `src/main/java/org/xlyo/cocomonyab/service/message/MessageParser.java`
   - 添加了重载的 `parse(message, channelUsername, channelTitle)` 方法
   - 在解析后设置频道信息

2. `src/main/java/org/xlyo/cocomonyab/service/ChannelMonitorService.java`
   - 在 `processSingleMessage()` 中查询频道信息并传递给解析器
   - 在 `processMediaGroup()` 中查询频道信息并传递给解析器

## 验证

编译成功：
```bash
./gradlew build -x test --no-daemon
BUILD SUCCESSFUL
```

## 效果

修复后，控制台输出将正确显示频道名称：
```
频道: 频道标题 (@频道用户名)
```

## 注意事项

1. 频道信息从数据库查询，确保频道已经在数据库中存在
2. 如果数据库中没有该频道信息，channelUsername 和 channelTitle 将为 null
3. 保持了向后兼容性：原有的 `parse(message)` 方法仍然可用，只是不会设置频道信息
