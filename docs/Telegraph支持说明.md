# Telegraph 支持说明

## 概述

系统现已支持 Telegraph 文章的监控和解析。Telegraph 是 Telegram 的即时预览平台，允许用户创建和分享富文本文章。

## Telegraph 识别机制

### 什么是 Telegraph 消息？

Telegraph 消息在 Telegram 中以 `MessageText` 类型出现，但包含一个 `webPage` 字段，其中包含：
- 文章标题
- 作者信息
- 文章描述
- 即时预览（Instant View）信息

### 自动识别

系统会自动识别 Telegraph 文章：

1. **消息类型判断**：
   - 基础类型：`MessageText`
   - 包含 `webPage` 字段
   - `webPage.instantViewVersion` 不为空

2. **类型标记**：
   - 普通文本消息：`contentType = "text"`
   - Telegraph 文章：`contentType = "telegraph"`

## 数据结构

### ChannelMessage 实体

```java
@Document(collection = "channel_messages")
public class ChannelMessage {
    // ... 基础字段 ...
    
    private String contentType;      // "telegraph" 表示 Telegraph 文章
    private String textContent;      // 消息文本（通常是文章链接）
    
    // Telegraph/WebPage 信息
    private WebPageInfo webPage;
}
```

### WebPageInfo 结构

```java
public static class WebPageInfo {
    private String url;                  // 文章URL（telegraph.ph/...）
    private String displayUrl;           // 显示URL
    private String type;                 // 类型：通常是 "article"
    private String siteName;             // 网站名称："Telegraph"
    private String title;                // 文章标题
    private String description;          // 文章描述/摘要
    private String author;               // 作者名称
    private Integer duration;            // 时长（视频/音频）
    private Boolean hasInstantView;      // 是否有即时预览
    private String instantViewVersion;   // 即时预览版本
}
```

## 控制台输出示例

### Telegraph 文章消息

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📨 收到新消息
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
频道: 技术分享频道 (@tech_channel)
消息ID: 123456
频道ID: -1001234567890
类型: telegraph
时间: 2024-02-22T20:30:45
内容: https://telegra.ph/My-Article-02-22
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🌐 WebPage 信息:
  📰 Telegraph 文章
  标题: 如何使用 TDLib 开发 Telegram 应用
  网站: Telegraph
  作者: 张三
  描述: 本文介绍了如何使用 TDLib 库开发 Telegram 客户端应用，包括环境配置、API 调用、消息处理等内容...
  链接: https://telegra.ph/How-to-use-TDLib-02-22
  即时预览: 可用 (版本: 2.0)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 数据库ID: 65d7f8a9b1c2d3e4f5a6b7c8
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 普通网页链接

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📨 收到新消息
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
频道: 新闻频道 (@news_channel)
消息ID: 123457
频道ID: -1001234567890
类型: text
时间: 2024-02-22T20:35:12
内容: https://example.com/article
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🌐 WebPage 信息:
  标题: 示例文章标题
  网站: Example.com
  描述: 这是一篇示例文章的描述...
  链接: https://example.com/article
  即时预览: 不可用
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 数据库ID: 65d7f8a9b1c2d3e4f5a6b7c9
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## MongoDB 数据示例

### Telegraph 文章记录

```json
{
  "_id": "65d7f8a9b1c2d3e4f5a6b7c8",
  "messageId": 123456,
  "chatId": -1001234567890,
  "channelUsername": "tech_channel",
  "channelTitle": "技术分享频道",
  "date": 1708617045,
  "contentType": "telegraph",
  "textContent": "https://telegra.ph/My-Article-02-22",
  "webPage": {
    "url": "https://telegra.ph/How-to-use-TDLib-02-22",
    "displayUrl": "telegra.ph/How-to-use-TDLib-02-22",
    "type": "article",
    "siteName": "Telegraph",
    "title": "如何使用 TDLib 开发 Telegram 应用",
    "description": "本文介绍了如何使用 TDLib 库开发 Telegram 客户端应用...",
    "author": "张三",
    "hasInstantView": true,
    "instantViewVersion": "2.0"
  },
  "status": "PENDING",
  "createTime": "2024-02-22T20:30:45",
  "updateTime": "2024-02-22T20:30:45"
}
```

## 查询示例

### 查询所有 Telegraph 文章

```javascript
db.channel_messages.find({
    contentType: "telegraph"
}).sort({createTime: -1})
```

### 查询特定作者的 Telegraph 文章

```javascript
db.channel_messages.find({
    contentType: "telegraph",
    "webPage.author": "张三"
}).sort({createTime: -1})
```

### 统计 Telegraph 文章数量

```javascript
db.channel_messages.aggregate([
    {$match: {contentType: "telegraph"}},
    {$group: {
        _id: "$channelTitle",
        count: {$sum: 1}
    }},
    {$sort: {count: -1}}
])
```

### 查询包含特定关键词的 Telegraph 文章

```javascript
db.channel_messages.find({
    contentType: "telegraph",
    $or: [
        {"webPage.title": {$regex: "TDLib", $options: "i"}},
        {"webPage.description": {$regex: "TDLib", $options: "i"}}
    ]
}).sort({createTime: -1})
```

## 技术实现细节

### 1. 消息类型判断

```java
case TdApi.MessageText text -> {
    message.setContentType("text");
    message.setTextContent(text.text.text);
    
    // 检查是否包含 WebPage
    if (text.webPage != null) {
        message.setWebPage(parseWebPage(text.webPage));
        
        // 如果有即时预览，标记为 telegraph 类型
        if (text.webPage.instantViewVersion != null && 
            !text.webPage.instantViewVersion.isEmpty()) {
            message.setContentType("telegraph");
        }
    }
}
```

### 2. WebPage 解析

```java
private ChannelMessage.WebPageInfo parseWebPage(TdApi.WebPage webPage) {
    ChannelMessage.WebPageInfo info = new ChannelMessage.WebPageInfo();
    
    info.setUrl(webPage.url);
    info.setDisplayUrl(webPage.displayUrl);
    info.setType(webPage.type);
    info.setSiteName(webPage.siteName);
    info.setTitle(webPage.title);
    
    // 描述可能是 FormattedText 对象
    if (webPage.description != null && webPage.description.text != null) {
        info.setDescription(webPage.description.text);
    }
    
    info.setAuthor(webPage.author);
    
    // 检查是否有即时预览
    if (webPage.instantViewVersion != null && 
        !webPage.instantViewVersion.isEmpty()) {
        info.setHasInstantView(true);
        info.setInstantViewVersion(webPage.instantViewVersion);
    }
    
    return info;
}
```

## 扩展功能建议

### 1. 获取完整的即时预览内容

可以使用 `GetWebPageInstantView` API 获取完整的文章内容：

```java
TdApi.GetWebPageInstantView request = new TdApi.GetWebPageInstantView();
request.url = webPage.url;
request.forceFull = true;

client.send(request, result -> {
    if (!result.isError()) {
        TdApi.WebPageInstantView instantView = result.get();
        // 处理即时预览内容
        // instantView.pageBlocks 包含文章的所有块（段落、图片等）
    }
});
```

### 2. 下载 Telegraph 文章中的图片

Telegraph 文章可能包含图片，可以遍历 `pageBlocks` 并下载：

```java
for (TdApi.PageBlock block : instantView.pageBlocks) {
    if (block instanceof TdApi.PageBlockPhoto) {
        TdApi.PageBlockPhoto photoBlock = (TdApi.PageBlockPhoto) block;
        // 下载图片
        downloadPhoto(photoBlock.photo);
    }
}
```

### 3. 文章内容提取和存储

可以将 Telegraph 文章的完整内容提取并存储到数据库：

```java
@Data
public static class TelegraphArticle {
    private String title;
    private String author;
    private List<ArticleBlock> blocks;  // 文章块列表
}

@Data
public static class ArticleBlock {
    private String type;      // paragraph, image, video, etc.
    private String content;   // 文本内容
    private String imageUrl;  // 图片URL
}
```

### 4. Telegraph 文章搜索

实现全文搜索功能：

```java
// 创建文本索引
db.channel_messages.createIndex({
    "webPage.title": "text",
    "webPage.description": "text",
    "webPage.author": "text"
})

// 全文搜索
db.channel_messages.find({
    $text: {$search: "TDLib Telegram"}
})
```

## 注意事项

1. **即时预览版本**：
   - Telegraph 的即时预览版本可能会更新
   - 建议记录版本号以便追踪变化

2. **URL 格式**：
   - Telegraph URL 格式：`https://telegra.ph/Article-Title-MM-DD`
   - 可能包含语言代码：`https://telegra.ph/zh/Article-Title-MM-DD`

3. **作者信息**：
   - 并非所有 Telegraph 文章都有作者信息
   - 匿名文章的 `author` 字段为空

4. **描述长度**：
   - 描述可能很长，建议在显示时截断
   - 完整内容需要通过 `GetWebPageInstantView` 获取

5. **性能考虑**：
   - 获取完整即时预览内容是额外的 API 调用
   - 建议按需获取，而不是自动获取所有文章

## 测试建议

### 1. 创建测试 Telegraph 文章

访问 https://telegra.ph/ 创建测试文章

### 2. 在监控频道中分享

将 Telegraph 文章链接发送到监控的频道

### 3. 验证数据

检查控制台输出和数据库记录，确认：
- `contentType` 为 "telegraph"
- `webPage` 字段包含完整信息
- `hasInstantView` 为 true

## 参考资料

- [Telegraph 官网](https://telegra.ph/)
- [TDLib WebPage 文档](docs/t3d/tdlib/org/drinkless/tdlib/TdApi.WebPage.html)
- [TDLib GetWebPageInstantView 文档](docs/t3d/tdlib/org/drinkless/tdlib/TdApi.GetWebPageInstantView.html)
