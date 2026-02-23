package org.xlyo.cocomonyab.plugin.impl.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaFile;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.MessageBroadcastDTO;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket消息广播插件集成测试
 * 测试端到端的消息广播流程：Plugin → SimpMessagingTemplate → WebSocket → Client
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "plugin.websocket-broadcast.enabled=true",
    "plugin.websocket-broadcast.topic-prefix=/topic/channel/real",
    "websocket.trusted-token=test-token-12345"
})
class WebSocketBroadcastPluginIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcastPluginIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketBroadcastPlugin plugin;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    @BeforeEach
    void setup() throws Exception {
        // 创建WebSocket STOMP客户端
        stompClient = new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        stompClient.setMessageConverter(new StringMessageConverter());

        // 连接到WebSocket服务器
        String wsUrl = "ws://localhost:" + port + "/ws";
        log.info("连接到WebSocket服务器: {}", wsUrl);

        stompSession = stompClient.connectAsync(
            wsUrl,
            new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    log.info("WebSocket连接成功");
                }

                @Override
                public void handleException(StompSession session, StompCommand command, 
                                          StompHeaders headers, byte[] payload, Throwable exception) {
                    log.error("WebSocket异常", exception);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    log.error("WebSocket传输错误", exception);
                }
            }
        ).get(5, TimeUnit.SECONDS);

        assertThat(stompSession.isConnected()).isTrue();
    }

    @Test
    void shouldReceiveBroadcastedTextMessage() throws Exception {
        // Given: 创建文本消息实体
        Long chatId = -1001234567890L;
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(123L);
        entity.setChatId(chatId);
        entity.setChannelUsername("test_channel");
        entity.setChannelTitle("Test Channel");
        entity.setDate((int) (System.currentTimeMillis() / 1000));
        entity.setTextContent("Hello from integration test!");
        entity.setViews(100);
        entity.setForwards(10);

        // 订阅topic
        String topic = "/topic/channel/real/" + chatId;
        CompletableFuture<MessageBroadcastDTO> future = new CompletableFuture<>();
        
        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageBroadcastDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("收到广播消息: {}", payload);
                future.complete((MessageBroadcastDTO) payload);
            }
        });

        // When: 触发插件处理消息
        plugin.handle(entity, new PluginContext(null));

        // Then: 验证客户端接收到广播消息
        MessageBroadcastDTO received = future.get(5, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.getMessageId()).isEqualTo(123L);
        assertThat(received.getChatId()).isEqualTo(chatId);
        assertThat(received.getChannelUsername()).isEqualTo("test_channel");
        assertThat(received.getChannelTitle()).isEqualTo("Test Channel");
        assertThat(received.getTextContent()).isEqualTo("Hello from integration test!");
        assertThat(received.getContentType()).isEqualTo("TEXT");
        assertThat(received.getViews()).isEqualTo(100);
        assertThat(received.getForwards()).isEqualTo(10);
    }

    @Test
    void shouldReceiveBroadcastedPhotoMessage() throws Exception {
        // Given: 创建图片消息实体
        Long chatId = -1001234567890L;
        PhotoMessageEntity entity = new PhotoMessageEntity();
        entity.setMessageId(456L);
        entity.setChatId(chatId);
        entity.setChannelUsername("test_channel");
        entity.setChannelTitle("Test Channel");
        entity.setDate((int) (System.currentTimeMillis() / 1000));
        entity.setCaption("Photo caption");
        
        // 添加图片信息
        MediaFile photo = new MediaFile();
        photo.setFileId(123456);
        photo.setFileUniqueId("photo_unique_id_123");
        photo.setFileSize(1024000L);
        photo.setMimeType("image/jpeg");
        photo.setWidth(1920);
        photo.setHeight(1080);
        entity.setPhotos(List.of(photo));

        // 订阅topic
        String topic = "/topic/channel/real/" + chatId;
        CompletableFuture<MessageBroadcastDTO> future = new CompletableFuture<>();
        
        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageBroadcastDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("收到图片消息: {}", payload);
                future.complete((MessageBroadcastDTO) payload);
            }
        });

        // When: 触发插件处理消息
        plugin.handle(entity, new PluginContext(null));

        // Then: 验证客户端接收到广播消息
        MessageBroadcastDTO received = future.get(5, TimeUnit.SECONDS);
        
        assertThat(received).isNotNull();
        assertThat(received.getMessageId()).isEqualTo(456L);
        assertThat(received.getChatId()).isEqualTo(chatId);
        assertThat(received.getContentType()).isEqualTo("PHOTO");
        assertThat(received.getTextContent()).isEqualTo("Photo caption");
        assertThat(received.getPhotos()).isNotNull();
        assertThat(received.getPhotos()).hasSize(1);
        assertThat(received.getPhotos().get(0).getFileId()).isEqualTo(123456);
        assertThat(received.getPhotos().get(0).getWidth()).isEqualTo(1920);
        assertThat(received.getPhotos().get(0).getHeight()).isEqualTo(1080);
    }

    @Test
    void shouldHandleMultipleSubscribersToSameTopic() throws Exception {
        // Given: 创建文本消息实体
        Long chatId = -1001234567890L;
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(999L);
        entity.setChatId(chatId);
        entity.setChannelUsername("test_channel");
        entity.setChannelTitle("Test Channel");
        entity.setDate((int) (System.currentTimeMillis() / 1000));
        entity.setTextContent("Message for multiple subscribers");

        // 订阅topic - 订阅者1
        String topic = "/topic/channel/real/" + chatId;
        CompletableFuture<MessageBroadcastDTO> future1 = new CompletableFuture<>();
        
        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageBroadcastDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("订阅者1收到消息: {}", payload);
                future1.complete((MessageBroadcastDTO) payload);
            }
        });

        // 订阅topic - 订阅者2
        CompletableFuture<MessageBroadcastDTO> future2 = new CompletableFuture<>();
        
        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MessageBroadcastDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("订阅者2收到消息: {}", payload);
                future2.complete((MessageBroadcastDTO) payload);
            }
        });

        // When: 触发插件处理消息
        plugin.handle(entity, new PluginContext(null));

        // Then: 验证两个订阅者都收到消息
        MessageBroadcastDTO received1 = future1.get(5, TimeUnit.SECONDS);
        MessageBroadcastDTO received2 = future2.get(5, TimeUnit.SECONDS);
        
        assertThat(received1).isNotNull();
        assertThat(received2).isNotNull();
        assertThat(received1.getMessageId()).isEqualTo(999L);
        assertThat(received2.getMessageId()).isEqualTo(999L);
        assertThat(received1.getTextContent()).isEqualTo("Message for multiple subscribers");
        assertThat(received2.getTextContent()).isEqualTo("Message for multiple subscribers");
    }
}
