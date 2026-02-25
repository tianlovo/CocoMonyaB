package org.xlyo.cocomonyab.domain.dto.tag;

import lombok.Data;

/**
 * 作者查询DTO
 */
@Data
public class AuthorQueryDTO {
    /**
     * 搜索关键词（名称或别名）
     */
    private String keyword;
}
