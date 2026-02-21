package org.xlyo.cocomonyab.config;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.Channel;

import java.time.LocalDateTime;

/**
 * MongoDB event listener for Channel entity lifecycle events.
 * Handles automatic timestamp management for createTime and updateTime.
 */
@Component
public class ChannelEntityListener extends AbstractMongoEventListener<Channel> {
    
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Channel> event) {
        Channel channel = event.getSource();
        LocalDateTime now = LocalDateTime.now();
        
        // Set createTime only if it's null (new entity)
        if (channel.getCreateTime() == null) {
            channel.setCreateTime(now);
        }
        
        // Always update updateTime
        channel.setUpdateTime(now);
    }
}
