package org.xlyo.cocomonyab.config.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebsocketProperties {
    
    /**
     * WS客户端认证的Token
     */
    private String trustedToken;

    /**
     * 注册的STOMP端点，客户端将使用此端点进行连接
     */
    private String connectPrefix = "/ws";

    /**
     * 应用目的地前缀（客户端发送消息的路径前缀）
     */
    private String appDstPrefix = "/app";

    /**
     * 简单代理，将消息转发给订阅了特定前缀的客户端
     */
    private String brokerPrefix = "/topic";

    /**
     * 用户目的地前缀，用于点对点消息
     */
    private String userDstPrefix = "/user";
}
