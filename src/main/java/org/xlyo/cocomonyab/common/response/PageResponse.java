package org.xlyo.cocomonyab.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.xlyo.cocomonyab.common.enums.ResponseCode;

import java.util.List;

/**
 * 分页响应结构
 * @param <T> 数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> extends ApiResponse<PageResponse.PageData<T>> {
    
    /**
     * 分页数据包装类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageData<T> {
        /**
         * 数据列表
         */
        private List<T> records;
        
        /**
         * 当前页码
         */
        private Long current;
        
        /**
         * 每页大小
         */
        private Long size;
        
        /**
         * 总记录数
         */
        private Long total;
        
        /**
         * 总页数
         */
        private Long pages;
    }
    
    /**
     * 成功分页响应
     */
    public static <T> PageResponse<T> success(List<T> records, Long current, Long size, Long total) {
        PageData<T> pageData = new PageData<>();
        pageData.setRecords(records);
        pageData.setCurrent(current);
        pageData.setSize(size);
        pageData.setTotal(total);
        pageData.setPages((total + size - 1) / size); // 计算总页数
        
        PageResponse<T> response = new PageResponse<>();
        response.setCode(ResponseCode.SUCCESS.getCode());
        response.setMsg("操作成功");
        response.setData(pageData);
        return response;
    }
    
    /**
     * 空分页响应
     */
    public static <T> PageResponse<T> empty(Long current, Long size) {
        return success(List.of(), current, size, 0L);
    }
}
