package org.xlyo.cocomonyab.config.mongo;

import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.Channel;

import java.time.LocalDateTime;

/**
 * MongoDB 频道实体生命周期事件监听器。
 * 自动管理 createTime 和 updateTime 时间戳。
 */
@Component
public class ChannelEntityListener extends AbstractMongoEventListener<@NonNull Channel> {
    
    @Override
    public void onBeforeConvert(BeforeConvertEvent<@NonNull Channel> event) {
        Channel channel = event.getSource();
        LocalDateTime now = LocalDateTime.now();
        
        // 仅在 createTime 为 null 时设置（new entity）
        if (channel.getCreateTime() == null) {
            channel.setCreateTime(now);
        }
        
        // 总是更新 updateTime
        channel.setUpdateTime(now);
    }
}
