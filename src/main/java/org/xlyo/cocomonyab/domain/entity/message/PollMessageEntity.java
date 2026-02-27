package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.util.List;

/**
 * 投票消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PollMessageEntity extends BaseMessageEntity {
    /**
     * 投票问题
     */
    private String question;
    
    /**
     * 投票选项列表
     */
    private List<String> options;
    
    /**
     * 总投票数
     */
    private Integer totalVoterCount;
    
    /**
     * 是否匿名
     */
    private Boolean isAnonymous;
    
    /**
     * 是否已关闭
     */
    private Boolean isClosed;
    
    @Override
    public MessageType getType() {
        return MessageType.POLL;
    }
}
