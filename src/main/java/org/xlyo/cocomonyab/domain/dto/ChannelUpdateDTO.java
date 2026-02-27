package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用于更新现有channel的DTO
 */
@Data
public class ChannelUpdateDTO {
    
    @Size(max = 100, message = "频道用户名长度不能超过100")
    private String channelUsername;
    
    @Size(min = 1, max = 200, message = "频道标题长度必须在1-200之间")
    private String channelTitle;
    
    private Boolean monitoringStatus;
}
