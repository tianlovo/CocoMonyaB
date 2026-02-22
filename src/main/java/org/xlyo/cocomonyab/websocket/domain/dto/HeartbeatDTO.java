package org.xlyo.cocomonyab.websocket.domain.dto;

import lombok.Data;

@Data
public class HeartbeatDTO {
    private Long clientTimestamp;
    private String clientId;
}
