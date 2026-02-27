package org.xlyo.cocomonyab.domain.vo.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入结果视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultVO {
    /**
     * 成功导入的数量
     */
    private Integer successCount;
    
    /**
     * 失败的数量
     */
    private Integer failureCount;
    
    /**
     * 错误详情列表
     */
    private List<ImportError> errors;
    
    /**
     * 导入错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        /**
         * 数据在数组中的索引（从0开始）
         */
        private Integer index;
        
        /**
         * 实体名称
         */
        private String name;
        
        /**
         * 错误信息
         */
        private String error;
    }
}
