package org.xlyo.cocomonyab.websocket.auth;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xlyo.cocomonyab.config.websocket.WebsocketProperties;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsTokenChannelInterceptor implements ChannelInterceptor {
    private final WebsocketProperties websocketProperties;

    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_PREFIX = "Bearer ";
    private static final String WS_CLIENT_PREFIX = "ws-client+";

    /**
     * 在消息发送到客户端入站通道之前调用
     * 在这里进行认证和授权
     *
     * @param message 接收到的消息
     * @param channel 消息通道
     * @return 处理后的消息（可能包含新的认证信息），如果返回null则中断消息处理
     */
    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            log.debug("无法获取 {}，消息直接放行", StompHeaderAccessor.class.getName());
            return message;
        }

        String sessionId = accessor.getSessionId();
        StompCommand command = accessor.getCommand();
        log.debug("拦截到消息，sessionId: {}, command: {}", sessionId, command);

        // 1. 仅拦截 CONNECT 命令，确保只在连接建立时进行一次认证
        if (StompCommand.CONNECT.equals(command)) {
            log.info("处理 CONNECT 请求，sessionId: {}", sessionId);

            // 2. 从STOMP头中获取token
            List<String> authorization = accessor.getNativeHeader(AUTHORIZATION);
            String token = null;
            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.getFirst();
                if (bearerToken.startsWith(AUTHORIZATION_PREFIX)) {
                    token = bearerToken.substring(7);
                    log.debug("从 {} 头中提取 Bearer token: {}", AUTHORIZATION, maskToken(token));
                } else {
                    token = bearerToken;
                    log.debug("从 {} 头中直接提取 token: {}", AUTHORIZATION, maskToken(token));
                }
            } else {
                log.warn("CONNECT 请求中未携带 {} 头，sessionId: {}", AUTHORIZATION, sessionId);
            }

            // 记录 token 前缀用于调试（避免记录完整 token）
            String tokenPrefix = token != null && token.length() > 8 ? token.substring(0, 8) + "..." : "null";
            log.debug("提取到的 token 前缀: {}", tokenPrefix);

            // 3. 验证Token
            if (StringUtils.hasText(token) && Objects.equals(token, websocketProperties.getTrustedToken())) {
                // 认证成功：创建Principal并绑定到Session
                WsTokenPrincipal principal = new WsTokenPrincipal(WS_CLIENT_PREFIX + UUID.randomUUID());
                accessor.setUser(principal);
                accessor.setLeaveMutable(true);

                log.info("Token 验证成功，为 sessionId: {} 创建 Principal: {}", sessionId, principal.getName());

                // 构建新的消息，携带认证信息
                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
            } else {
                // 认证失败：记录警告并抛出异常
                log.warn("Token 验证失败，sessionId: {}, token前缀: {}", sessionId, tokenPrefix);
                throw new IllegalArgumentException("Invalid or missing authentication token.");
            }
        }

        // 2. 对于非CONNECT命令，验证会话是否已认证
        if (accessor.getUser() == null) {
            log.warn("未认证的客户端尝试发送消息，拒绝处理，sessionId: {}, command: {}", sessionId, command);
            return null; // 拒绝未认证消息，断开连接
        }

        log.debug("消息通过认证检查，sessionId: {}, command: {}", sessionId, command);
        return message;
    }

    /**
     * 对 token 进行掩码处理，仅显示前4位和后4位，中间用 "****" 代替。
     * 若 token 长度 ≤ 8，则完全掩码为 "****"。
     */
    private String maskToken(String token) {
        if (token == null) {
            return "null";
        }
        int length = token.length();
        if (length <= 8) {
            return "****";
        }
        int keep = 4;
        String prefix = token.substring(0, keep);
        String suffix = token.substring(length - keep);
        return prefix + "****" + suffix;
    }
}