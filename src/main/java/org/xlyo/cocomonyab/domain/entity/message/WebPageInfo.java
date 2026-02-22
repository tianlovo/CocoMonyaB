package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;

/**
 * WebPage信息
 * 用于Telegraph文章和其他网页预览
 */
@Data
public class WebPageInfo {
    /**
     * 网页URL
     */
    private String url;
    
    /**
     * 网页标题
     */
    private String title;
    
    /**
     * 作者
     */
    private String author;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 网站名称
     */
    private String siteName;
    
    /**
     * 是否有即时预览
     */
    private Boolean hasInstantView;
    
    /**
     * 即时预览版本号
     */
    private Integer instantViewVersion;
}
