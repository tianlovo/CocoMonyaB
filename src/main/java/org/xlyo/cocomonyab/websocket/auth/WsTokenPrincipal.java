package org.xlyo.cocomonyab.websocket.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

/**
 * 基于Token的认证主体
 * 用于在 WebSocket 会话中标识已认证的客户端
 */
@RequiredArgsConstructor
public class WsTokenPrincipal implements Principal {
    // token的唯一标识（比如客户端ID）
    @Getter
    private final String name;
}
