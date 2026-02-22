package org.xlyo.cocomonyab.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.xlyo.cocomonyab.websocket.auth.WsTokenChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebsocketConfiguration implements WebSocketMessageBrokerConfigurer {
    private final WsTokenChannelInterceptor tokenChannelInterceptor;
    private final WebsocketProperties websocketProperties;
    private final TaskScheduler wsHeartbeatTaskScheduler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端将使用此端点进行连接
        // 支持SockJS以实现浏览器回退
        registry.addEndpoint(websocketProperties.getConnectPrefix())
                .setAllowedOrigins("*") // 生产环境应严格配置
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 配置应用目的地前缀（客户端发送消息的路径前缀）
        registry.setApplicationDestinationPrefixes(websocketProperties.getAppDstPrefix());
        // 配置简单代理，将消息转发给订阅了特定前缀的客户端
        registry.enableSimpleBroker(websocketProperties.getBrokerPrefix())
                .setHeartbeatValue(new long[]{
                        websocketProperties.getHeartbeat().getOutgoing(),
                        websocketProperties.getHeartbeat().getIncoming()
                })
                .setTaskScheduler(wsHeartbeatTaskScheduler);
        // 配置用户目的地前缀，用于点对点消息
        registry.setUserDestinationPrefix(websocketProperties.getUserDstPrefix());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 将自定义拦截器注册到客户端入站通道
        // 拦截器执行顺序：数字越小，优先级越高
        registration.interceptors(tokenChannelInterceptor);
    }
}
