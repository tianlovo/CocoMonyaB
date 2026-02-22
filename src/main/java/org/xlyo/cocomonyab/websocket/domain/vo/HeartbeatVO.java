package org.xlyo.cocomonyab.websocket.domain.vo;

import lombok.Data;

@Data
public class HeartbeatVO {
    private Long serverTimestamp;
    private String status;
}
