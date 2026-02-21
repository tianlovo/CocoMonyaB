package org.xlyo.cocomonyab.domain.dto;

import lombok.Data;

/**
 * 用于查询带过滤器的channels的 DTO
 */
@Data
public class ChannelQueryDTO {
    
    private String channelUsername;
    
    private Boolean monitoringStatus;
}
