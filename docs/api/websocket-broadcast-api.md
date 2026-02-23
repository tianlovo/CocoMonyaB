# WebSocket消息广播API文档

## 概述

WebSocket消息广播API提供实时推送Telegram频道消息和监控事件的能力。客户端通过STOMP协议连接到WebSocket服务器，订阅特定的topic来接收消息广播。

### 主要功能

- **实时消息广播**: 将监听的Telegram频道消息实时推送给订阅的客户端
- **频道监控事件**: 通知客户端频道监控列表的变化（添加、移除、更新）
- **多消息类型支持**: 支持文本、图片、视频、文档、音频、Telegraph文章、媒体组等多种消息类型
- **可靠传输**: 基于STOMP协议，支持心跳检测和自动重连

### 技术栈

- **协议**: STOMP over WebSocket
- **传输格式**: JSON
- **认证方式**: Token-based authentication

## 认证方式

### Token认证

客户端连接WebSocket时需要提供有效的认证token。Token通过STOMP协议的`Authorization`头传递，而不是URL查询参数。

**认证流程:**

1. 客户端从服务端获取WebSocket访问token
2. 建立WebSocket连接到`/ws`端点（不需要在URL中携带token）
3. 在STOMP CONNECT帧中，通过`Authorization`头发送token（格式：`Bearer {token}`）
4. 服务端验证token有效性
5. 验证通过后建立连接，否则拒绝连接并抛出异常

**Token传递方式:**

```typescript
// 正确方式：在STOMP connectHeaders中传递
connectHeaders: {
  'Authorization': 'Bearer your-secret-token'
}

// 错误方式：不要在URL中传递token
// ❌ new SockJS('http://localhost:8080/ws?token=xxx')
```

**安全注意事项:**

- Token应该通过安全渠道获取（如HTTPS API）
- Token应该定期轮换
- 不要在客户端代码中硬编码token
- 使用环境变量或配置文件管理token
- Token在STOMP头中传递，比URL参数更安全（不会出现在服务器日志中）

## 连接端点

### WebSocket连接端点

**端点URL:**

```
ws://{host}:{port}/ws
```

**参数说明:**

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| host | String | 是 | 服务器地址 |
| port | Integer | 是 | 服务器端口（默认8080） |

**示例:**

```
ws://localhost:8080/ws
wss://api.example.com/ws
```

**注意:** Token不在URL中传递，而是在STOMP CONNECT帧的`Authorization`头中传递。

### STOMP配置

**目标前缀:**

- **应用目标前缀**: `/app` - 用于客户端发送消息到服务器（本API暂不使用）
- **订阅前缀**: `/topic` - 用于订阅广播消息
- **用户目标前缀**: `/user` - 用于点对点消息（本API暂不使用）

**心跳配置:**

- **发送心跳间隔**: 10000ms (10秒)
- **接收心跳间隔**: 10000ms (10秒)


## Topic订阅规范

### 频道消息Topic

订阅特定频道的实时消息。

**Topic格式:**

```
/topic/channel/real/{channelId}
```

**参数说明:**

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| channelId | Long | Telegram频道的唯一标识符（chatId） | -1001234567890 |

**订阅示例:**

```javascript
// 订阅频道ID为-1001234567890的消息
stompClient.subscribe('/topic/channel/real/-1001234567890', (message) => {
    const data = JSON.parse(message.body);
    console.log('收到消息:', data);
});
```

### 频道监控事件Topic

订阅频道监控列表的变化事件。

**Topic列表:**

| Topic | 说明 | 触发时机 |
|-------|------|---------|
| `/topic/channel/monitoring/added` | 频道添加事件 | 新频道被添加到监控列表 |
| `/topic/channel/monitoring/removed` | 频道移除事件 | 频道从监控列表中移除 |
| `/topic/channel/monitoring/updated` | 频道更新事件 | 频道监控状态更新 |
| `/topic/channel/monitoring/reload` | 监控列表重载事件 | 监控列表完全重新加载 |

**订阅示例:**

```javascript
// 订阅频道添加事件
stompClient.subscribe('/topic/channel/monitoring/added', (message) => {
    const notification = JSON.parse(message.body);
    console.log('频道已添加:', notification.channelId);
});

// 订阅频道移除事件
stompClient.subscribe('/topic/channel/monitoring/removed', (message) => {
    const notification = JSON.parse(message.body);
    console.log('频道已移除:', notification.channelId);
});
```


## 消息格式定义

### MessageBroadcastDTO

频道消息的数据传输对象，包含消息的所有相关信息。

**JSON结构:**

```json
{
  "messageId": 12345,
  "chatId": -1001234567890,
  "channelUsername": "example_channel",
  "channelTitle": "示例频道",
  "date": 1708704000,
  "contentType": "TEXT",
  "textContent": "这是一条文本消息",
  "views": 1000,
  "forwards": 50,
  "photos": null,
  "video": null,
  "document": null,
  "audio": null,
  "voice": null,
  "videoNote": null,
  "animation": null,
  "sticker": null,
  "webPage": null,
  "mediaAlbumId": null,
  "isMediaGroup": false,
  "itemCount": null,
  "items": null,
  "pollQuestion": null,
  "pollOptions": null
}
```

**字段说明:**

#### 基础字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| messageId | Long | 是 | 消息唯一标识符 |
| chatId | Long | 是 | 频道ID（Telegram chat ID） |
| channelUsername | String | 是 | 频道用户名（不含@符号） |
| channelTitle | String | 是 | 频道标题/名称 |
| date | Integer | 是 | 消息发送时间（Unix时间戳，秒） |
| contentType | String | 是 | 消息类型（见消息类型枚举） |
| textContent | String | 否 | 文本内容（文本消息或媒体说明文字） |

#### 互动字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| views | Integer | 否 | 浏览次数 |
| forwards | Integer | 否 | 转发次数 |


#### 媒体字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| photos | Array<MediaFileDTO> | 否 | 图片列表（PHOTO类型消息） |
| video | MediaFileDTO | 否 | 视频信息（VIDEO类型消息） |
| document | MediaFileDTO | 否 | 文档信息（DOCUMENT类型消息） |
| audio | MediaFileDTO | 否 | 音频信息（AUDIO类型消息） |
| voice | MediaFileDTO | 否 | 语音信息（VOICE类型消息） |
| videoNote | MediaFileDTO | 否 | 视频笔记信息（VIDEO_NOTE类型消息） |
| animation | MediaFileDTO | 否 | 动画信息（ANIMATION类型消息） |
| sticker | MediaFileDTO | 否 | 贴纸信息（STICKER类型消息） |

#### WebPage字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| webPage | WebPageDTO | 否 | 网页预览信息（TELEGRAPH类型消息） |

#### 媒体组字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| mediaAlbumId | Long | 否 | 媒体组ID（MEDIA_GROUP类型消息） |
| isMediaGroup | Boolean | 否 | 是否为媒体组 |
| itemCount | Integer | 否 | 媒体组中的项目数量 |
| items | Array<MessageBroadcastDTO> | 否 | 媒体组中的所有消息 |

#### 投票字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| pollQuestion | String | 否 | 投票问题（POLL类型消息） |
| pollOptions | Array<String> | 否 | 投票选项列表 |


#### 消息类型枚举

| contentType | 说明 | 包含的媒体字段 |
|-------------|------|---------------|
| TEXT | 纯文本消息 | 无 |
| PHOTO | 图片消息 | photos |
| VIDEO | 视频消息 | video |
| DOCUMENT | 文档消息 | document |
| AUDIO | 音频消息 | audio |
| VOICE | 语音消息 | voice |
| VIDEO_NOTE | 视频笔记消息 | videoNote |
| ANIMATION | 动画消息 | animation |
| STICKER | 贴纸消息 | sticker |
| POLL | 投票消息 | pollQuestion, pollOptions |
| TELEGRAPH | Telegraph文章 | webPage |
| MEDIA_GROUP | 媒体组消息 | mediaAlbumId, isMediaGroup, itemCount, items |

### MediaFileDTO

媒体文件信息的数据传输对象。

**JSON结构:**

```json
{
  "fileId": "AgACAgUAAxkBAAIBY2...",
  "fileUniqueId": "AQADAgATwqkxZ3xy",
  "fileSize": 1048576,
  "mimeType": "image/jpeg",
  "fileName": "photo.jpg",
  "width": 1920,
  "height": 1080,
  "duration": null,
  "thumbnailFileId": "AAMCAgADGQEAAgFj..."
}
```

**字段说明:**

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| fileId | String | 是 | Telegram文件ID（用于下载文件） |
| fileUniqueId | String | 是 | 文件唯一标识符 |
| fileSize | Long | 否 | 文件大小（字节） |
| mimeType | String | 否 | MIME类型 |
| fileName | String | 否 | 文件名 |
| width | Integer | 否 | 宽度（图片/视频） |
| height | Integer | 否 | 高度（图片/视频） |
| duration | Integer | 否 | 时长（音频/视频，秒） |
| thumbnailFileId | String | 否 | 缩略图文件ID |


### WebPageDTO

网页预览信息的数据传输对象（用于Telegraph文章等）。

**JSON结构:**

```json
{
  "url": "https://telegra.ph/Example-Article-01-23",
  "displayUrl": "telegra.ph/Example-Article-01-23",
  "type": "article",
  "siteName": "Telegraph",
  "title": "示例文章标题",
  "description": "这是文章的简短描述...",
  "author": "作者名称",
  "hasInstantView": true,
  "instantViewVersion": "2.0"
}
```

**字段说明:**

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| url | String | 是 | 完整URL地址 |
| displayUrl | String | 否 | 显示用的URL（通常不含协议） |
| type | String | 否 | 网页类型（article, video等） |
| siteName | String | 否 | 网站名称 |
| title | String | 否 | 网页标题 |
| description | String | 否 | 网页描述 |
| author | String | 否 | 作者名称 |
| hasInstantView | Boolean | 否 | 是否支持即时预览 |
| instantViewVersion | String | 否 | 即时预览版本 |


## 监控事件格式定义

### ChannelMonitoringNotificationDTO

频道监控事件通知的数据传输对象。

**JSON结构:**

```json
{
  "eventType": "ADDED",
  "channelId": -1001234567890,
  "monitoringStatus": true,
  "timestamp": 1708704000000
}
```

**字段说明:**

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| eventType | String | 是 | 事件类型（见事件类型枚举） |
| channelId | Long | 是 | 频道ID |
| monitoringStatus | Boolean | 是 | 监控状态（true=启用，false=禁用） |
| timestamp | Long | 是 | 事件时间戳（毫秒） |

**事件类型枚举:**

| eventType | 说明 | Topic |
|-----------|------|-------|
| ADDED | 频道被添加到监控列表 | /topic/channel/monitoring/added |
| REMOVED | 频道从监控列表移除 | /topic/channel/monitoring/removed |
| UPDATED | 频道监控状态更新 | /topic/channel/monitoring/updated |
| RELOAD_ALL | 监控列表完全重载 | /topic/channel/monitoring/reload |


## 客户端连接示例

### JavaScript/TypeScript示例

#### 使用SockJS和STOMP.js

**安装依赖:**

```bash
npm install sockjs-client @stomp/stompjs
```

**基础连接示例:**

```typescript
import SockJS from 'sockjs-client';
import { Client, StompSubscription } from '@stomp/stompjs';

class WebSocketClient {
    private client: Client;
    private subscriptions: Map<string, StompSubscription> = new Map();

    constructor(private token: string, private serverUrl: string = 'http://localhost:8080') {}

    /**
     * 连接到WebSocket服务器
     */
    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            this.client = new Client({
                // 使用SockJS连接（不在URL中传递token）
                webSocketFactory: () => new SockJS(`${this.serverUrl}/ws`),
                
                // 在STOMP CONNECT帧中传递Authorization头
                connectHeaders: {
                    'Authorization': `Bearer ${this.token}`
                },
                
                debug: (str) => {
                    console.log('STOMP Debug:', str);
                },
                
                reconnectDelay: 5000,
                heartbeatIncoming: 10000,
                heartbeatOutgoing: 10000,
                
                onConnect: (frame) => {
                    console.log('WebSocket连接成功:', frame);
                    resolve();
                },
                
                onStompError: (frame) => {
                    console.error('STOMP错误:', frame.headers['message']);
                    console.error('详细信息:', frame.body);
                    reject(new Error(frame.headers['message']));
                },
                
                onWebSocketError: (event) => {
                    console.error('WebSocket错误:', event);
                    reject(event);
                }
            });

            this.client.activate();
        });
    }

    /**
     * 断开连接
     */
    disconnect(): Promise<void> {
        return new Promise((resolve) => {
            if (this.client) {
                this.client.deactivate();
                console.log('WebSocket连接已断开');
            }
            resolve();
        });
    }

    /**
     * 订阅频道消息
     */
    subscribeToChannel(channelId: number, callback: (message: MessageBroadcastDTO) => void): void {
        const topic = `/topic/channel/real/${channelId}`;
        
        if (this.subscriptions.has(topic)) {
            console.warn(`已订阅topic: ${topic}`);
            return;
        }

        const subscription = this.client.subscribe(topic, (message) => {
            try {
                const data: MessageBroadcastDTO = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('解析消息失败:', error);
            }
        });

        this.subscriptions.set(topic, subscription);
        console.log(`已订阅频道消息: ${topic}`);
    }

    /**
     * 取消订阅频道消息
     */
    unsubscribeFromChannel(channelId: number): void {
        const topic = `/topic/channel/real/${channelId}`;
        const subscription = this.subscriptions.get(topic);
        
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(topic);
            console.log(`已取消订阅: ${topic}`);
        }
    }

    /**
     * 订阅频道监控事件
     */
    subscribeToMonitoringEvents(
        eventType: 'added' | 'removed' | 'updated' | 'reload',
        callback: (notification: ChannelMonitoringNotificationDTO) => void
    ): void {
        const topic = `/topic/channel/monitoring/${eventType}`;
        
        if (this.subscriptions.has(topic)) {
            console.warn(`已订阅topic: ${topic}`);
            return;
        }

        const subscription = this.client.subscribe(topic, (message) => {
            try {
                const data: ChannelMonitoringNotificationDTO = JSON.parse(message.body);
                callback(data);
            } catch (error) {
                console.error('解析监控事件失败:', error);
            }
        });

        this.subscriptions.set(topic, subscription);
        console.log(`已订阅监控事件: ${topic}`);
    }

    /**
     * 检查连接状态
     */
    isConnected(): boolean {
        return this.client && this.client.connected;
    }
}

// TypeScript类型定义
interface MessageBroadcastDTO {
    messageId: number;
    chatId: number;
    channelUsername: string;
    channelTitle: string;
    date: number;
    contentType: string;
    textContent?: string;
    views?: number;
    forwards?: number;
    photos?: MediaFileDTO[];
    video?: MediaFileDTO;
    document?: MediaFileDTO;
    audio?: MediaFileDTO;
    voice?: MediaFileDTO;
    videoNote?: MediaFileDTO;
    animation?: MediaFileDTO;
    sticker?: MediaFileDTO;
    webPage?: WebPageDTO;
    mediaAlbumId?: number;
    isMediaGroup?: boolean;
    itemCount?: number;
    items?: MessageBroadcastDTO[];
    pollQuestion?: string;
    pollOptions?: string[];
}

interface MediaFileDTO {
    fileId: string;
    fileUniqueId: string;
    fileSize?: number;
    mimeType?: string;
    fileName?: string;
    width?: number;
    height?: number;
    duration?: number;
    thumbnailFileId?: string;
}

interface WebPageDTO {
    url: string;
    displayUrl?: string;
    type?: string;
    siteName?: string;
    title?: string;
    description?: string;
    author?: string;
    hasInstantView?: boolean;
    instantViewVersion?: string;
}

interface ChannelMonitoringNotificationDTO {
    eventType: 'ADDED' | 'REMOVED' | 'UPDATED' | 'RELOAD_ALL';
    channelId: number;
    monitoringStatus: boolean;
    timestamp: number;
}

export { WebSocketClient, MessageBroadcastDTO, MediaFileDTO, WebPageDTO, ChannelMonitoringNotificationDTO };
```


**使用示例:**

```typescript
// 创建客户端实例
const wsClient = new WebSocketClient('your-secret-token', 'http://localhost:8080');

// 连接到服务器
wsClient.connect()
    .then(() => {
        console.log('连接成功！');
        
        // 订阅频道消息
        wsClient.subscribeToChannel(-1001234567890, (message) => {
            console.log('收到新消息:', message);
            
            // 根据消息类型处理
            switch (message.contentType) {
                case 'TEXT':
                    console.log('文本消息:', message.textContent);
                    break;
                case 'PHOTO':
                    console.log('图片消息，共', message.photos?.length, '张');
                    break;
                case 'VIDEO':
                    console.log('视频消息:', message.video?.fileName);
                    break;
                case 'TELEGRAPH':
                    console.log('Telegraph文章:', message.webPage?.title);
                    break;
                case 'MEDIA_GROUP':
                    console.log('媒体组，共', message.itemCount, '项');
                    break;
                default:
                    console.log('其他类型消息:', message.contentType);
            }
        });
        
        // 订阅频道添加事件
        wsClient.subscribeToMonitoringEvents('added', (notification) => {
            console.log('频道已添加:', notification.channelId);
        });
        
        // 订阅频道移除事件
        wsClient.subscribeToMonitoringEvents('removed', (notification) => {
            console.log('频道已移除:', notification.channelId);
        });
    })
    .catch((error) => {
        console.error('连接失败:', error);
    });

// 断开连接（在应用关闭时）
window.addEventListener('beforeunload', () => {
    wsClient.disconnect();
});
```


#### 使用原生WebSocket和STOMP（浏览器环境）

```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket消息广播示例</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>
</head>
<body>
    <h1>WebSocket消息广播</h1>
    <div id="status">状态: 未连接</div>
    <div id="messages"></div>

    <script>
        const token = 'your-secret-token';
        const serverUrl = 'http://localhost:8080';
        
        // 创建STOMP客户端
        const client = new StompJs.Client({
            // 使用SockJS连接（不在URL中传递token）
            webSocketFactory: () => new SockJS(`${serverUrl}/ws`),
            
            // 在STOMP CONNECT帧中传递Authorization头
            connectHeaders: {
                'Authorization': `Bearer ${token}`
            },
            
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            
            onConnect: (frame) => {
                document.getElementById('status').textContent = '状态: 已连接';
                console.log('连接成功:', frame);
                
                // 订阅频道消息
                client.subscribe('/topic/channel/real/-1001234567890', (message) => {
                    const data = JSON.parse(message.body);
                    displayMessage(data);
                });
                
                // 订阅监控事件
                client.subscribe('/topic/channel/monitoring/added', (message) => {
                    const notification = JSON.parse(message.body);
                    console.log('频道已添加:', notification);
                });
            },
            
            onStompError: (frame) => {
                document.getElementById('status').textContent = '状态: 错误';
                console.error('STOMP错误:', frame);
            },
            
            onWebSocketClose: (event) => {
                document.getElementById('status').textContent = '状态: 已断开';
                console.log('连接关闭:', event);
            }
        });
        
        // 激活客户端
        client.activate();
        
        // 显示消息
        function displayMessage(message) {
            const messagesDiv = document.getElementById('messages');
            const messageElement = document.createElement('div');
            messageElement.innerHTML = `
                <p><strong>${message.channelTitle}</strong> (${message.contentType})</p>
                <p>${message.textContent || '(无文本内容)'}</p>
                <p>浏览: ${message.views || 0} | 转发: ${message.forwards || 0}</p>
                <hr>
            `;
            messagesDiv.appendChild(messageElement);
        }
    </script>
</body>
</html>
```


## 错误处理和重连策略

### 常见错误类型

#### 1. 认证失败

**错误表现:**
- 连接立即断开
- STOMP错误帧包含"无效或缺失的认证 token"信息
- 服务端日志显示"Token 验证失败"

**处理方式:**

```typescript
onStompError: (frame) => {
    const errorMessage = frame.headers['message'] || '';
    
    if (errorMessage.includes('token') || 
        errorMessage.includes('认证') ||
        errorMessage.includes('Authentication') || 
        errorMessage.includes('Unauthorized')) {
        console.error('认证失败，请检查token是否有效');
        console.error('错误详情:', frame.body);
        
        // 不要自动重连，需要用户提供新的token
        client.deactivate();
        
        // 通知用户需要重新获取token
        alert('认证失败，请重新登录获取新的token');
    }
}
```

**常见原因:**
- Token不正确或已过期
- Token格式错误（应为`Bearer {token}`）
- 未在`connectHeaders`中设置`Authorization`头
- Token在URL中传递而不是在STOMP头中（错误做法）

#### 2. 网络连接中断

**错误表现:**
- WebSocket连接突然关闭
- 心跳超时

**处理方式:**

```typescript
const client = new Client({
    // 启用自动重连
    reconnectDelay: 5000, // 5秒后重连
    
    onWebSocketClose: (event) => {
        console.log('连接关闭，将在5秒后自动重连');
    },
    
    onConnect: (frame) => {
        console.log('重新连接成功');
        // 重新订阅所有topic
        resubscribeAll();
    }
});
```

#### 3. 消息解析失败

**错误表现:**
- JSON.parse()抛出异常
- 数据格式不符合预期

**处理方式:**

```typescript
client.subscribe(topic, (message) => {
    try {
        const data = JSON.parse(message.body);
        
        // 验证必需字段
        if (!data.messageId || !data.chatId) {
            console.error('消息缺少必需字段:', data);
            return;
        }
        
        handleMessage(data);
    } catch (error) {
        console.error('解析消息失败:', error, message.body);
    }
});
```


### 重连策略最佳实践

#### 指数退避重连

```typescript
class ReconnectManager {
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 10;
    private baseDelay = 1000; // 1秒
    private maxDelay = 60000; // 60秒

    getReconnectDelay(): number {
        const delay = Math.min(
            this.baseDelay * Math.pow(2, this.reconnectAttempts),
            this.maxDelay
        );
        this.reconnectAttempts++;
        return delay;
    }

    reset(): void {
        this.reconnectAttempts = 0;
    }

    shouldReconnect(): boolean {
        return this.reconnectAttempts < this.maxReconnectAttempts;
    }
}

// 使用示例
const reconnectManager = new ReconnectManager();

const client = new Client({
    beforeConnect: () => {
        if (!reconnectManager.shouldReconnect()) {
            console.error('达到最大重连次数，停止重连');
            return Promise.reject(new Error('Max reconnect attempts reached'));
        }
        return Promise.resolve();
    },
    
    reconnectDelay: () => reconnectManager.getReconnectDelay(),
    
    onConnect: () => {
        reconnectManager.reset(); // 连接成功后重置计数器
        console.log('连接成功');
    }
});
```

#### 订阅管理和恢复

```typescript
class SubscriptionManager {
    private subscriptions: Map<string, (message: any) => void> = new Map();

    addSubscription(topic: string, callback: (message: any) => void): void {
        this.subscriptions.set(topic, callback);
    }

    removeSubscription(topic: string): void {
        this.subscriptions.delete(topic);
    }

    resubscribeAll(client: Client): void {
        console.log(`重新订阅${this.subscriptions.size}个topic`);
        
        this.subscriptions.forEach((callback, topic) => {
            client.subscribe(topic, (message) => {
                try {
                    const data = JSON.parse(message.body);
                    callback(data);
                } catch (error) {
                    console.error(`解析消息失败 [${topic}]:`, error);
                }
            });
        });
    }
}

// 使用示例
const subManager = new SubscriptionManager();

const client = new Client({
    onConnect: () => {
        subManager.resubscribeAll(client);
    }
});

// 添加订阅
subManager.addSubscription('/topic/channel/real/-1001234567890', (message) => {
    console.log('收到消息:', message);
});
```


### 心跳监控

```typescript
class HeartbeatMonitor {
    private lastHeartbeat: number = Date.now();
    private heartbeatTimeout: number = 30000; // 30秒
    private checkInterval: NodeJS.Timeout | null = null;

    start(onTimeout: () => void): void {
        this.checkInterval = setInterval(() => {
            const elapsed = Date.now() - this.lastHeartbeat;
            
            if (elapsed > this.heartbeatTimeout) {
                console.warn('心跳超时，连接可能已断开');
                onTimeout();
            }
        }, 5000); // 每5秒检查一次
    }

    stop(): void {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
        }
    }

    update(): void {
        this.lastHeartbeat = Date.now();
    }
}

// 使用示例
const heartbeatMonitor = new HeartbeatMonitor();

const client = new Client({
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    
    onConnect: () => {
        heartbeatMonitor.start(() => {
            console.log('心跳超时，尝试重连');
            client.forceDisconnect();
        });
    },
    
    onWebSocketClose: () => {
        heartbeatMonitor.stop();
    },
    
    // 每次收到消息时更新心跳
    onStompError: () => {
        heartbeatMonitor.update();
    }
});
```


## 常见问题FAQ

### Q1: 如何获取WebSocket访问token？

**A:** Token由服务端管理员分配。在生产环境中，应该通过安全的API端点获取token，而不是硬编码在客户端代码中。

```typescript
// 示例：通过API获取token
async function getWebSocketToken(): Promise<string> {
    const response = await fetch('https://api.example.com/auth/ws-token', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer your-api-token',
            'Content-Type': 'application/json'
        }
    });
    
    const data = await response.json();
    return data.wsToken;
}

// 使用token连接（注意：token在connectHeaders中传递，不在URL中）
const token = await getWebSocketToken();
const wsClient = new WebSocketClient(token);
```

**重要提示:** Token必须在STOMP `connectHeaders`的`Authorization`头中传递，格式为`Bearer {token}`，而不是在WebSocket URL的查询参数中。

### Q2: Token应该如何传递？为什么不能在URL中传递？

**A:** Token必须在STOMP CONNECT帧的`Authorization`头中传递，而不是在WebSocket URL中。

**正确方式:**
```typescript
const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    connectHeaders: {
        'Authorization': 'Bearer your-secret-token'  // ✓ 正确
    }
});
```

**错误方式:**
```typescript
// ❌ 错误：不要在URL中传递token
const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws?token=xxx')
});
```

**原因:**
1. **安全性**: URL参数会出现在服务器日志、浏览器历史记录和代理服务器日志中
2. **协议规范**: STOMP协议设计为在CONNECT帧头中传递认证信息
3. **服务端实现**: 服务端的`WsTokenChannelInterceptor`从STOMP头中读取token，不从URL读取

### Q3: 如何订阅多个频道？

**A:** 可以多次调用`subscribeToChannel()`方法订阅不同的频道。

```typescript
// 订阅多个频道
const channelIds = [-1001234567890, -1009876543210, -1001111222333];

channelIds.forEach(channelId => {
    wsClient.subscribeToChannel(channelId, (message) => {
        console.log(`频道${channelId}收到消息:`, message);
    });
});
```

### Q4: 消息是否会丢失？

**A:** WebSocket连接断开期间的消息不会被缓存。重连后只能接收新的消息。如果需要历史消息，应该：

1. 通过REST API获取历史消息
2. 实现客户端消息缓存
3. 记录最后接收的messageId，重连后请求缺失的消息

### Q5: 如何处理大量消息？

**A:** 对于高频消息场景，建议：

1. **消息队列**: 使用队列缓冲消息，避免UI阻塞

```typescript
class MessageQueue {
    private queue: MessageBroadcastDTO[] = [];
    private processing = false;

    add(message: MessageBroadcastDTO): void {
        this.queue.push(message);
        this.process();
    }

    private async process(): Promise<void> {
        if (this.processing || this.queue.length === 0) return;
        
        this.processing = true;
        
        while (this.queue.length > 0) {
            const message = this.queue.shift()!;
            await this.handleMessage(message);
            
            // 避免阻塞UI
            await new Promise(resolve => setTimeout(resolve, 10));
        }
        
        this.processing = false;
    }

    private async handleMessage(message: MessageBroadcastDTO): Promise<void> {
        // 处理消息逻辑
        console.log('处理消息:', message.messageId);
    }
}
```

2. **消息过滤**: 只订阅需要的频道
3. **批量处理**: 累积一定数量后批量更新UI


### Q6: 如何判断连接是否正常？

**A:** 可以通过以下方式监控连接状态：

```typescript
class ConnectionMonitor {
    private client: Client;
    private statusCallback: (status: string) => void;

    constructor(client: Client, statusCallback: (status: string) => void) {
        this.client = client;
        this.statusCallback = statusCallback;
        this.setupMonitoring();
    }

    private setupMonitoring(): void {
        this.client.onConnect = () => {
            this.statusCallback('connected');
        };

        this.client.onWebSocketClose = () => {
            this.statusCallback('disconnected');
        };

        this.client.onStompError = () => {
            this.statusCallback('error');
        };
    }

    getStatus(): string {
        if (this.client.connected) {
            return 'connected';
        } else if (this.client.active) {
            return 'connecting';
        } else {
            return 'disconnected';
        }
    }
}
```

### Q7: 媒体文件如何下载？

**A:** 消息中的`fileId`可以用于通过Telegram Bot API下载文件。需要：

1. 使用Bot API的`getFile`方法获取文件路径
2. 通过`https://api.telegram.org/file/bot<token>/<file_path>`下载文件

注意：这需要服务端支持，客户端不能直接使用fileId下载。

### Q8: 如何处理媒体组消息？

**A:** 媒体组消息的`items`字段包含组内所有消息：

```typescript
function handleMediaGroup(message: MessageBroadcastDTO): void {
    if (message.isMediaGroup && message.items) {
        console.log(`媒体组包含${message.itemCount}项`);
        
        message.items.forEach((item, index) => {
            console.log(`项目${index + 1}:`, item.contentType);
            
            // 处理每个媒体项
            if (item.photos) {
                console.log('  - 图片:', item.photos.length, '张');
            } else if (item.video) {
                console.log('  - 视频:', item.video.fileName);
            }
        });
    }
}
```

### Q9: 支持哪些浏览器？

**A:** 支持所有现代浏览器：

- Chrome/Edge 88+
- Firefox 85+
- Safari 14+
- Opera 74+

对于旧版浏览器，SockJS提供了降级方案（如长轮询）。

### Q10: 如何在React中使用？

**A:** 使用React Hooks封装WebSocket客户端：

```typescript
import { useEffect, useState, useCallback } from 'react';
import { WebSocketClient, MessageBroadcastDTO } from './WebSocketClient';

export function useWebSocket(token: string, channelId: number) {
    const [client, setClient] = useState<WebSocketClient | null>(null);
    const [messages, setMessages] = useState<MessageBroadcastDTO[]>([]);
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        const wsClient = new WebSocketClient(token);
        
        wsClient.connect()
            .then(() => {
                setConnected(true);
                setClient(wsClient);
                
                // 订阅频道
                wsClient.subscribeToChannel(channelId, (message) => {
                    setMessages(prev => [...prev, message]);
                });
            })
            .catch(error => {
                console.error('连接失败:', error);
            });

        // 清理
        return () => {
            wsClient.disconnect();
        };
    }, [token, channelId]);

    return { messages, connected, client };
}

// 使用示例
function ChannelMessages({ channelId }: { channelId: number }) {
    const { messages, connected } = useWebSocket('your-token', channelId);

    return (
        <div>
            <p>状态: {connected ? '已连接' : '未连接'}</p>
            {messages.map(msg => (
                <div key={msg.messageId}>
                    <p>{msg.textContent}</p>
                </div>
            ))}
        </div>
    );
}
```


### Q11: 如何调试WebSocket连接？

**A:** 启用调试日志和使用浏览器开发者工具：

```typescript
// 1. 启用STOMP调试日志
const client = new Client({
    debug: (str) => {
        console.log('[STOMP Debug]', new Date().toISOString(), str);
    }
});

// 2. 使用浏览器开发者工具
// - Network标签 -> WS过滤器 -> 查看WebSocket帧
// - Console标签 -> 查看日志输出

// 3. 添加详细的事件监听
client.onConnect = (frame) => {
    console.log('✓ 连接成功', frame);
};

client.onWebSocketClose = (event) => {
    console.log('✗ 连接关闭', event.code, event.reason);
};

client.onStompError = (frame) => {
    console.error('✗ STOMP错误', frame.headers, frame.body);
};

client.onWebSocketError = (event) => {
    console.error('✗ WebSocket错误', event);
};
```

## 性能优化建议

### 1. 减少订阅数量

只订阅必要的频道和事件，避免订阅过多topic导致消息处理压力。

```typescript
// 不推荐：订阅所有监控事件
['added', 'removed', 'updated', 'reload'].forEach(eventType => {
    wsClient.subscribeToMonitoringEvents(eventType, handleEvent);
});

// 推荐：只订阅需要的事件
wsClient.subscribeToMonitoringEvents('added', handleChannelAdded);
```

### 2. 消息去重

对于可能重复的消息，使用messageId进行去重：

```typescript
const processedMessages = new Set<number>();

function handleMessage(message: MessageBroadcastDTO): void {
    if (processedMessages.has(message.messageId)) {
        console.log('重复消息，跳过:', message.messageId);
        return;
    }
    
    processedMessages.add(message.messageId);
    
    // 处理消息
    displayMessage(message);
    
    // 定期清理旧消息ID（避免内存泄漏）
    if (processedMessages.size > 10000) {
        const oldestIds = Array.from(processedMessages).slice(0, 5000);
        oldestIds.forEach(id => processedMessages.delete(id));
    }
}
```

### 3. 虚拟滚动

对于大量消息的展示，使用虚拟滚动技术：

```typescript
// 使用react-window或react-virtualized
import { FixedSizeList } from 'react-window';

function MessageList({ messages }: { messages: MessageBroadcastDTO[] }) {
    const Row = ({ index, style }: any) => (
        <div style={style}>
            <MessageItem message={messages[index]} />
        </div>
    );

    return (
        <FixedSizeList
            height={600}
            itemCount={messages.length}
            itemSize={100}
            width="100%"
        >
            {Row}
        </FixedSizeList>
    );
}
```

### 4. 连接池管理

对于多个WebSocket连接，使用连接池管理：

```typescript
class WebSocketPool {
    private connections: Map<string, WebSocketClient> = new Map();
    private maxConnections = 5;

    getConnection(token: string): WebSocketClient {
        if (!this.connections.has(token)) {
            if (this.connections.size >= this.maxConnections) {
                throw new Error('达到最大连接数');
            }
            
            const client = new WebSocketClient(token);
            this.connections.set(token, client);
        }
        
        return this.connections.get(token)!;
    }

    closeAll(): void {
        this.connections.forEach(client => client.disconnect());
        this.connections.clear();
    }
}
```


## 安全最佳实践

### 1. Token管理

```typescript
class TokenManager {
    private token: string | null = null;
    private tokenExpiry: number | null = null;

    setToken(token: string, expiresIn: number): void {
        this.token = token;
        this.tokenExpiry = Date.now() + expiresIn * 1000;
    }

    getToken(): string | null {
        if (this.isExpired()) {
            console.warn('Token已过期');
            return null;
        }
        return this.token;
    }

    isExpired(): boolean {
        if (!this.tokenExpiry) return true;
        return Date.now() >= this.tokenExpiry;
    }

    clear(): void {
        this.token = null;
        this.tokenExpiry = null;
    }
}
```

### 2. 输入验证

```typescript
function validateMessage(message: any): message is MessageBroadcastDTO {
    return (
        typeof message === 'object' &&
        typeof message.messageId === 'number' &&
        typeof message.chatId === 'number' &&
        typeof message.channelUsername === 'string' &&
        typeof message.channelTitle === 'string' &&
        typeof message.contentType === 'string'
    );
}

// 使用
client.subscribe(topic, (message) => {
    const data = JSON.parse(message.body);
    
    if (!validateMessage(data)) {
        console.error('无效的消息格式:', data);
        return;
    }
    
    handleMessage(data);
});
```

### 3. XSS防护

```typescript
function sanitizeText(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function displayMessage(message: MessageBroadcastDTO): void {
    const safeText = sanitizeText(message.textContent || '');
    const safeTitle = sanitizeText(message.channelTitle);
    
    // 安全地插入到DOM
    element.innerHTML = `
        <div class="message">
            <h3>${safeTitle}</h3>
            <p>${safeText}</p>
        </div>
    `;
}
```

## 配置参考

### 服务端配置

```yaml
# application.yml
plugin:
  websocket-broadcast:
    enabled: true
    topic-prefix: /topic/channel/real
    monitoring-topic-prefix: /topic/channel/monitoring
    async-broadcast: false

websocket:
  trusted-token: ${WEBSOCKET_TOKEN}
  connect-prefix: /ws
  app-dst-prefix: /app
  broker-prefix: /topic
  user-dst-prefix: /user
  heartbeat:
    outgoing: 10000
    incoming: 10000

spring:
  websocket:
    message-size-limit: 65536      # 64KB
    send-buffer-size-limit: 524288 # 512KB
    send-time-limit: 20000         # 20秒
```

### 客户端配置建议

```typescript
const config = {
    // 连接配置
    serverUrl: process.env.WS_SERVER_URL || 'http://localhost:8080',
    token: process.env.WS_TOKEN,
    
    // 重连配置
    reconnectDelay: 5000,
    maxReconnectAttempts: 10,
    
    // 心跳配置
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    
    // 超时配置
    connectionTimeout: 30000,
    
    // 调试配置
    debug: process.env.NODE_ENV === 'development'
};
```

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| 1.0.0 | 2024-02-23 | 初始版本，支持消息广播和监控事件 |

## 相关资源

- [STOMP协议规范](https://stomp.github.io/)
- [SockJS文档](https://github.com/sockjs/sockjs-client)
- [@stomp/stompjs文档](https://stomp-js.github.io/stomp-websocket/)
- [WebSocket API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)

## 技术支持

如有问题或建议，请联系开发团队或提交Issue。

---

**文档版本**: 1.0.0  
**最后更新**: 2024-02-23  
**维护者**: CocoMonyaB开发团队
