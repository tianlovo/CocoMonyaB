import { Client, StompHeaders } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import 'dotenv/config';

// ==================== 配置 ====================
const SOCKJS_URL = 'http://localhost:10721/ws';
const TOKEN = process.env.WS_TRUSTED_TOKEN;

// 检查 token 是否存在，若不存在则抛出错误（避免空连接）
if (!TOKEN) {
    throw new Error('❌ WS_TRUSTED_TOKEN is not defined in .env file');
}

// ==================== 创建 STOMP 客户端 ====================
const client = new Client({
    webSocketFactory: () => new SockJS(SOCKJS_URL) as WebSocket,
    connectHeaders: {
        Authorization: `Bearer ${TOKEN}`,
    } as StompHeaders,
    // debug: (msg: string) => console.log(`[STOMP] ${msg}`),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000, // 与服务器心跳配置匹配（可选）
    heartbeatOutgoing: 10000,
});

// ==================== 事件监听 ====================
client.onConnect = (frame) => {
    console.log('✅ Connected to WebSocket (SockJS)');

    // 订阅公共主题（示例）
    client.subscribe('/topic/heartbeat', (message) => {
        // TODO: 处理消息（JSON）
        console.log('Received message:', message.body);
    });

    // 定时发送心跳（JSON）
    setInterval(() => {
        const heartbeatRequest = {
            clientTimestamp: Date.now(),
            clientId: 'node-client',
        };
        client.publish({
            destination: '/app/heartbeat',
            body: JSON.stringify(heartbeatRequest),
            headers: {'content-type': 'application/json'},
        });
        console.log('💓 Heartbeat sent');
    }, 30000); // 30秒发送一次
};

client.onStompError = (frame) => {
    console.error('❌ STOMP error:', frame.headers.message);
};

// 掉线检测：监听 WebSocket 底层关闭
client.onWebSocketClose = (event) => {
    console.log('🔌 WebSocket connection closed. Code:', event.code, 'Reason:', event.reason);
    // TODO: 实现重连逻辑或通知用户
    console.log('TODO: Implement reconnection logic or user notification');
};

client.onDisconnect = () => {
    console.log('🔌 STOMP disconnected');
    // TODO: 清理资源或触发重连
};

// ==================== 启动连接 ====================
client.activate();

// 优雅退出处理
process.on('SIGINT', () => {
    console.log('Received SIGINT, deactivating client...');
    client.deactivate();
    process.exit(0);
});
