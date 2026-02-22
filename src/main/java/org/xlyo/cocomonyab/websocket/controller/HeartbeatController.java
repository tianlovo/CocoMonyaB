package org.xlyo.cocomonyab.websocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.xlyo.cocomonyab.websocket.auth.WsTokenPrincipal;
import org.xlyo.cocomonyab.websocket.protobuf.HeartbeatProtos;

@Slf4j
@Controller
public class HeartbeatController {
    /**
     * 处理客户端心跳请求，返回服务端时间戳
     * <p>
     * (<code>/app/heartbeat</code>)
     *
     * @param request        心跳请求（Protobuf 自动反序列化）
     * @param principal      认证用户信息
     * @param headerAccessor 消息头访问器，可获取 sessionId
     * @return 心跳响应
     */
    @MessageMapping("/heartbeat")
    public HeartbeatProtos.HeartbeatResponse handleHeartbeat(
            @Payload HeartbeatProtos.HeartbeatRequest request,
            @AuthenticationPrincipal WsTokenPrincipal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.info("[Heartbeat] User: {}, Session: {}, ClientTime: {}",
                principal.getName(), sessionId, request.getClientTimestamp());

        // 构建响应
        return HeartbeatProtos.HeartbeatResponse.newBuilder()
                .setServerTimestamp(System.currentTimeMillis())
                .setStatus("OK")
                .build();
    }
}
