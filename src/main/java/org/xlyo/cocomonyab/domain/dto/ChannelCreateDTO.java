package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用于创建新 channel 的 DTO
 */
@Data
public class ChannelCreateDTO {
    
    @NotNull(message = "频道ID不能为空")
    private Long channelId;
    
    @NotBlank(message = "频道用户名不能为空")
    @Size(min = 1, max = 100, message = "频道用户名长度必须在1-100之间")
    private String channelUsername;
    
    @NotBlank(message = "频道标题不能为空")
    @Size(min = 1, max = 200, message = "频道标题长度必须在1-200之间")
    private String channelTitle;
    
    private Boolean monitoringStatus = true;
}
