package org.xlyo.cocomonyab.config.mongo;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.TagFilterConfig;

import java.time.LocalDateTime;

/**
 * MongoDB 标签过滤配置实体生命周期事件监听器。
 * 自动管理 createTime 和 updateTime 时间戳。
 */
@Component
public class TagFilterConfigEntityListener extends AbstractMongoEventListener<TagFilterConfig> {
    
    @Override
    public void onBeforeConvert(BeforeConvertEvent<TagFilterConfig> event) {
        TagFilterConfig config = event.getSource();
        LocalDateTime now = LocalDateTime.now();
        
        // 仅在 createTime 为 null 时设置（new entity）
        if (config.getCreateTime() == null) {
            config.setCreateTime(now);
        }
        
        // 总是更新 updateTime
        config.setUpdateTime(now);
    }
}
