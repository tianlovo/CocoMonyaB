package org.xlyo.cocomonyab.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 代表 Telegram channel 监控配置的channel实体
 */
@Document(collection = "telegram_channels")
@Data
public class Channel {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private Long channelId;
    
    private String channelUsername;
    
    private String channelTitle;
    
    @Indexed
    private Boolean monitoringStatus;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
