package org.xlyo.cocomonyab.websocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.xlyo.cocomonyab.websocket.auth.WsTokenPrincipal;
import org.xlyo.cocomonyab.websocket.domain.dto.HeartbeatDTO;
import org.xlyo.cocomonyab.websocket.domain.vo.HeartbeatVO;

@Slf4j
@Controller
public class HeartbeatController {

    @MessageMapping("/heartbeat")
    public HeartbeatVO handleHeartbeat(
            @Payload HeartbeatDTO request,
            @AuthenticationPrincipal WsTokenPrincipal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.info("[Heartbeat] User: {}, Session: {}, ClientTime: {}",
                principal.getName(), sessionId, request.getClientTimestamp());

        HeartbeatVO response = new HeartbeatVO();
        response.setServerTimestamp(System.currentTimeMillis());
        response.setStatus("OK");
        return response;
    }
}
