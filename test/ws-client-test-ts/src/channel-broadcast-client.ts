/**
 * WebSocket 广播消息接收器
 * 
 * 连接到服务端的 WebSocket 广播插件，接收并显示：
 * 1. 频道消息广播
 * 2. 频道监控事件通知
 * 
 * Requirements: 8.4
 */

import { Client, StompConfig, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import * as dotenv from 'dotenv';

// 加载环境变量
dotenv.config();

// ============================================================================
// Type Definitions (matching server-side DTOs)
// ============================================================================

/**
 * Message broadcast DTO - matches MessageBroadcastDTO.java
 */
interface MessageBroadcastDTO {
  // Basic fields
  messageId: number;
  chatId: number;
  channelUsername: string;
  channelTitle: string;
  date: number;
  contentType: string;  // MessageType enum: TEXT, PHOTO, VIDEO, etc.
  textContent?: string;
  
  // Interaction fields
  views?: number;
  forwards?: number;
  
  // Media fields (optional, depends on contentType)
  photos?: MediaFileDTO[];
  video?: MediaFileDTO;
  document?: MediaFileDTO;
  audio?: MediaFileDTO;
  voice?: MediaFileDTO;
  videoNote?: MediaFileDTO;
  animation?: MediaFileDTO;
  sticker?: MediaFileDTO;
  
  // WebPage fields (for TELEGRAPH type)
  webPage?: WebPageDTO;
  
  // Media group fields (for MEDIA_GROUP type)
  mediaAlbumId?: number;
  isMediaGroup?: boolean;
  itemCount?: number;
  items?: MessageBroadcastDTO[];
  
  // Poll fields (for POLL type)
  pollQuestion?: string;
  pollOptions?: string[];
}

/**
 * Media file DTO - matches MediaFileDTO.java
 */
interface MediaFileDTO {
  fileId: string;
  fileUniqueId: string;
  fileSize?: number;
  mimeType?: string;
  fileName?: string;
  
  // Image/Video specific
  width?: number;
  height?: number;
  duration?: number;
  
  // Thumbnail
  thumbnailFileId?: string;
}

/**
 * WebPage DTO - matches WebPageDTO.java
 */
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

/**
 * Channel monitoring notification DTO - matches ChannelMonitoringNotificationDTO.java
 */
interface ChannelMonitoringNotificationDTO {
  eventType: 'CHANNEL_ADDED' | 'CHANNEL_REMOVED' | 'CHANNEL_UPDATED' | 'RELOAD_ALL';
  channelId: number;
  monitoringStatus?: boolean;
  timestamp: number;
}

// ============================================================================
// WebSocket Client Configuration
// ============================================================================

/**
 * WebSocket 客户端配置
 */
interface WebSocketClientConfig {
  serverUrl: string;           // 服务器地址，例如 'http://localhost:8080'
  token: string;               // 认证 token
  reconnectDelay?: number;     // 重连延迟（毫秒），默认 5000
  heartbeatIncoming?: number;  // 接收心跳间隔（毫秒），默认 10000
  heartbeatOutgoing?: number;  // 发送心跳间隔（毫秒），默认 10000
  debug?: boolean;             // 启用调试日志
}

// ============================================================================
// WebSocket Client Class
// ============================================================================

/**
 * WebSocket 广播客户端
 * 
 * 处理与 WebSocket 服务器的连接，提供订阅频道消息和监控事件的方法
 */
class WebSocketBroadcastClient {
  private client: Client;
  private config: WebSocketClientConfig;
  private subscriptions: Map<string, any> = new Map();

  constructor(config: WebSocketClientConfig) {
    this.config = {
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: false,
      ...config
    };

    // 配置 STOMP 客户端
    const stompConfig: StompConfig = {
      // 使用 SockJS 连接
      webSocketFactory: () => {
        return new SockJS(`${this.config.serverUrl}/ws`);
      },
      
      // 连接选项 - 在 STOMP CONNECT 帧中发送 Authorization 头
      connectHeaders: {
        'Authorization': `Bearer ${this.config.token}`
      },
      
      // 心跳配置
      heartbeatIncoming: this.config.heartbeatIncoming!,
      heartbeatOutgoing: this.config.heartbeatOutgoing!,
      
      // 重连配置
      reconnectDelay: this.config.reconnectDelay!,
      
      // 连接回调
      onConnect: (frame) => {
        console.log('✓ 已连接到 WebSocket 服务器');
        this.onConnected(frame);
      },
      
      onDisconnect: (frame) => {
        console.log('✗ 已断开与 WebSocket 服务器的连接');
        this.onDisconnected(frame);
      },
      
      onStompError: (frame) => {
        console.error('✗ STOMP 错误:', frame.headers['message']);
        console.error('详情:', frame.body);
      },
      
      onWebSocketError: (event) => {
        console.error('✗ WebSocket 错误:', event);
      }
    };
    
    // 如果启用调试模式，设置 debug 函数
    if (this.config.debug) {
      stompConfig.debug = (str: string) => {
        console.log('[STOMP Debug]', str);
      };
    }

    this.client = new Client(stompConfig);
  }

  /**
   * 连接到 WebSocket 服务器
   */
  connect(): void {
    console.log('正在连接到 WebSocket 服务器...');
    this.client.activate();
  }

  /**
   * 断开与 WebSocket 服务器的连接
   */
  disconnect(): void {
    console.log('正在断开连接...');
    
    // 取消所有订阅
    this.subscriptions.forEach((subscription, topic) => {
      subscription.unsubscribe();
      console.log(`已取消订阅: ${topic}`);
    });
    this.subscriptions.clear();
    
    // 停用客户端
    this.client.deactivate();
  }

  /**
   * 订阅特定频道的消息
   * 
   * @param channelId - Telegram 频道 ID（例如 -1001234567890）
   * @param callback - 处理接收到的消息的回调函数
   * @returns 订阅 ID，用于后续取消订阅
   */
  subscribeToChannel(
    channelId: number,
    callback: (message: MessageBroadcastDTO) => void
  ): string {
    const topic = `/topic/channel/real/${channelId}`;
    
    if (this.subscriptions.has(topic)) {
      console.warn(`已订阅: ${topic}`);
      return topic;
    }

    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const dto: MessageBroadcastDTO = JSON.parse(message.body);
        callback(dto);
      } catch (error) {
        console.error('解析消息失败:', error);
      }
    });

    this.subscriptions.set(topic, subscription);
    console.log(`✓ 已订阅频道: ${channelId} (${topic})`);
    
    return topic;
  }

  /**
   * 取消订阅特定频道的消息
   * 
   * @param channelId - Telegram 频道 ID
   */
  unsubscribeFromChannel(channelId: number): void {
    const topic = `/topic/channel/real/${channelId}`;
    
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
      console.log(`✓ 已取消订阅频道: ${channelId}`);
    } else {
      console.warn(`未订阅频道: ${channelId}`);
    }
  }

  /**
   * 订阅频道监控事件
   * 
   * @param eventType - 事件类型: 'added', 'removed', 'updated' 或 'reload'
   * @param callback - 处理监控事件的回调函数
   * @returns 订阅 ID，用于后续取消订阅
   */
  subscribeToMonitoringEvents(
    eventType: 'added' | 'removed' | 'updated' | 'reload',
    callback: (notification: ChannelMonitoringNotificationDTO) => void
  ): string {
    const topic = `/topic/channel/monitoring/${eventType}`;
    
    if (this.subscriptions.has(topic)) {
      console.warn(`已订阅: ${topic}`);
      return topic;
    }

    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const dto: ChannelMonitoringNotificationDTO = JSON.parse(message.body);
        callback(dto);
      } catch (error) {
        console.error('解析监控事件失败:', error);
      }
    });

    this.subscriptions.set(topic, subscription);
    console.log(`✓ 已订阅监控事件: ${eventType} (${topic})`);
    
    return topic;
  }

  /**
   * 取消订阅监控事件
   * 
   * @param eventType - 要取消订阅的事件类型
   */
  unsubscribeFromMonitoringEvents(eventType: 'added' | 'removed' | 'updated' | 'reload'): void {
    const topic = `/topic/channel/monitoring/${eventType}`;
    
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
      console.log(`✓ 已取消订阅监控事件: ${eventType}`);
    } else {
      console.warn(`未订阅监控事件: ${eventType}`);
    }
  }

  /**
   * 检查客户端是否已连接
   */
  isConnected(): boolean {
    return this.client.connected;
  }

  /**
   * 连接建立时调用
   */
  private onConnected(frame: any): void {
    console.log('连接已成功建立');
  }

  /**
   * 连接断开时调用
   */
  private onDisconnected(frame: any): void {
    console.log('连接已断开');
  }
}

// ============================================================================
// 辅助函数
// ============================================================================

/**
 * 格式化文件大小
 */
function formatFileSize(bytes?: number): string {
  if (!bytes) return '未知';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

/**
 * 格式化时间戳
 */
function formatTimestamp(timestamp: number): string {
  return new Date(timestamp * 1000).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
}

// ============================================================================
// 消息处理器
// ============================================================================

/**
 * 处理接收到的频道消息
 */
function handleMessage(message: MessageBroadcastDTO): void {
  console.log('\n' + '='.repeat(80));
  console.log(`📨 收到新消息`);
  console.log('='.repeat(80));
  console.log(`频道: ${message.channelTitle} (@${message.channelUsername})`);
  console.log(`类型: ${message.contentType}`);
  console.log(`时间: ${formatTimestamp(message.date)}`);
  console.log(`messageId: ${message.messageId}`);
  console.log(`chatId: ${message.chatId}`);
  
  // 文本内容
  if (message.textContent) {
    console.log(`\n内容:\n${message.textContent}`);
  }
  
  // 互动统计
  if (message.views || message.forwards) {
    console.log(`\n统计信息:`);
    if (message.views) console.log(`  👁️  浏览量: ${message.views}`);
    if (message.forwards) console.log(`  ↗️  转发量: ${message.forwards}`);
  }
  
  // 处理不同类型的媒体
  switch (message.contentType) {
    case 'PHOTO':
      if (message.photos && message.photos.length > 0) {
        console.log(`\n📷 图片: ${message.photos.length} 张`);
        message.photos.forEach((photo, index) => {
          console.log(`  [${index + 1}] ${photo.width}x${photo.height}, ${formatFileSize(photo.fileSize)}`);
        });
      }
      break;
      
    case 'VIDEO':
      if (message.video) {
        console.log(`\n🎥 视频: ${message.video.fileName || '视频文件'}`);
        console.log(`   大小: ${formatFileSize(message.video.fileSize)}`);
        if (message.video.duration) {
          console.log(`   时长: ${message.video.duration}秒`);
        }
        if (message.video.width && message.video.height) {
          console.log(`   分辨率: ${message.video.width}x${message.video.height}`);
        }
      }
      break;
      
    case 'DOCUMENT':
      if (message.document) {
        console.log(`\n📄 文档: ${message.document.fileName || '文档文件'}`);
        console.log(`   大小: ${formatFileSize(message.document.fileSize)}`);
        if (message.document.mimeType) {
          console.log(`   类型: ${message.document.mimeType}`);
        }
      }
      break;
      
    case 'AUDIO':
      if (message.audio) {
        console.log(`\n🎵 音频: ${message.audio.fileName || '音频文件'}`);
        console.log(`   大小: ${formatFileSize(message.audio.fileSize)}`);
        if (message.audio.duration) {
          console.log(`   时长: ${message.audio.duration}秒`);
        }
      }
      break;
      
    case 'VOICE':
      if (message.voice) {
        console.log(`\n🎤 语音消息`);
        console.log(`   大小: ${formatFileSize(message.voice.fileSize)}`);
        if (message.voice.duration) {
          console.log(`   时长: ${message.voice.duration}秒`);
        }
      }
      break;
      
    case 'VIDEO_NOTE':
      if (message.videoNote) {
        console.log(`\n🎬 视频消息`);
        console.log(`   大小: ${formatFileSize(message.videoNote.fileSize)}`);
        if (message.videoNote.duration) {
          console.log(`   时长: ${message.videoNote.duration}秒`);
        }
      }
      break;
      
    case 'ANIMATION':
      if (message.animation) {
        console.log(`\n🎞️ 动画: ${message.animation.fileName || 'GIF'}`);
        console.log(`   大小: ${formatFileSize(message.animation.fileSize)}`);
      }
      break;
      
    case 'STICKER':
      if (message.sticker) {
        console.log(`\n🎨 贴纸`);
        console.log(`   大小: ${formatFileSize(message.sticker.fileSize)}`);
      }
      break;
      
    case 'TELEGRAPH':
      if (message.webPage) {
        console.log(`\n🔗 WebPage:`);
        if (message.webPage.title) {
          console.log(`   标题: ${message.webPage.title}`);
        }
        console.log(`   URL: ${message.webPage.url}`);
        if (message.webPage.description) {
          console.log(`   描述: ${message.webPage.description}`);
        }
        if (message.webPage.author) {
          console.log(`   作者: ${message.webPage.author}`);
        }
        if (message.webPage.siteName) {
          console.log(`   站点: ${message.webPage.siteName}`);
        }
      }
      break;
      
    case 'MEDIA_GROUP':
      if (message.isMediaGroup) {
        console.log(`\n📁 媒体组: ${message.itemCount} 个项目`);
        if (message.mediaAlbumId) {
          console.log(`   mediaAlbumId: ${message.mediaAlbumId}`);
        }
      }
      break;
      
    case 'POLL':
      if (message.pollQuestion) {
        console.log(`\n📊 投票: ${message.pollQuestion}`);
        if (message.pollOptions && message.pollOptions.length > 0) {
          console.log(`   选项:`);
          message.pollOptions.forEach((option, index) => {
            console.log(`     ${index + 1}. ${option}`);
          });
        }
      }
      break;
  }
  
  console.log('='.repeat(80) + '\n');
}

/**
 * 处理频道监控事件
 */
function handleMonitoringEvent(notification: ChannelMonitoringNotificationDTO): void {
  const eventEmoji = {
    'CHANNEL_ADDED': '✅',
    'CHANNEL_REMOVED': '❌',
    'CHANNEL_UPDATED': '🔄',
    'RELOAD_ALL': '🔃'
  }[notification.eventType] || '📢';
  
  const eventName = {
    'CHANNEL_ADDED': '频道已添加',
    'CHANNEL_REMOVED': '频道已移除',
    'CHANNEL_UPDATED': '频道已更新',
    'RELOAD_ALL': '重新加载所有频道'
  }[notification.eventType] || notification.eventType;

  console.log(`\n${eventEmoji} 监控事件: ${eventName}`);
  console.log(`   channelId: ${notification.channelId}`);
  if (notification.monitoringStatus !== undefined) {
    console.log(`   监控状态: ${notification.monitoringStatus ? '开启' : '关闭'}`);
  }
  console.log(`   时间: ${new Date(notification.timestamp).toLocaleString('zh-CN')}\n`);
}

// ============================================================================
// 主程序
// ============================================================================

/**
 * 主函数 - 启动广播消息接收器
 */
function main() {
  console.log('\n' + '='.repeat(80));
  console.log('WebSocket 广播消息接收器');
  console.log('='.repeat(80) + '\n');

  // 从环境变量读取配置
  const serverUrl = process.env.WS_SERVER_URL || 'http://localhost:8080';
  const token = process.env.WS_TOKEN || 'your-secret-token';
  const channelIdsStr = process.env.WS_CHANNEL_IDS || '';
  const debug = process.env.WS_DEBUG === 'true';

  // 解析频道 ID 列表
  const channelIds: number[] = channelIdsStr
    .split(',')
    .map(id => id.trim())
    .filter(id => id.length > 0)
    .map(id => parseInt(id, 10))
    .filter(id => !isNaN(id));

  if (channelIds.length === 0) {
    console.error('错误: 未配置频道 ID');
    console.error('请在 .env 文件中设置 WS_CHANNEL_IDS 环境变量');
    console.error('示例: WS_CHANNEL_IDS=-1001234567890,-1009876543210');
    process.exit(1);
  }

  console.log('配置信息:');
  console.log(`  服务器地址: ${serverUrl}`);
  console.log(`  Token: ${token.substring(0, 10)}...`);
  console.log(`  监听频道: ${channelIds.join(', ')}`);
  console.log(`  调试模式: ${debug ? '开启' : '关闭'}`);
  console.log('');

  // 创建客户端
  const client = new WebSocketBroadcastClient({
    serverUrl,
    token,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug
  });

  // 处理进程终止信号
  process.on('SIGINT', () => {
    console.log('\n\n正在关闭...');
    client.disconnect();
    process.exit(0);
  });

  process.on('SIGTERM', () => {
    console.log('\n\n正在关闭...');
    client.disconnect();
    process.exit(0);
  });

  // 连接到服务器
  client.connect();

  // 等待连接建立后订阅
  setTimeout(() => {
    if (!client.isConnected()) {
      console.error('错误: 无法连接到 WebSocket 服务器');
      console.error('请检查服务器地址和 token 是否正确');
      process.exit(1);
    }

    console.log('开始订阅...\n');

    // 订阅所有配置的频道
    channelIds.forEach(channelId => {
      client.subscribeToChannel(channelId, handleMessage);
    });

    // 订阅所有监控事件
    const monitoringEvents: Array<'added' | 'removed' | 'updated'> = ['added', 'removed', 'updated'];
    monitoringEvents.forEach(eventType => {
      client.subscribeToMonitoringEvents(eventType, handleMonitoringEvent);
    });

    console.log('\n✓ 所有订阅已激活，等待消息...\n');
    console.log('按 Ctrl+C 退出\n');
  }, 2000);
}

// ============================================================================
// 启动程序
// ============================================================================

// 运行主程序
main();

// 导出类型和客户端类供其他模块使用
export {
  WebSocketBroadcastClient,
  MessageBroadcastDTO,
  MediaFileDTO,
  WebPageDTO,
  ChannelMonitoringNotificationDTO,
  WebSocketClientConfig
};
