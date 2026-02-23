package org.xlyo.cocomonyab.plugin.impl.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebPage信息数据传输对象
 * 
 * <p>该DTO用于传输Telegraph消息中的网页预览信息。
 * 当消息包含URL链接时，Telegram会自动生成网页预览，包含标题、描述、作者等信息。</p>
 * 
 * <h2>即时预览（Instant View）</h2>
 * <p>Telegram的即时预览功能可以在应用内直接显示网页内容，无需打开外部浏览器。
 * hasInstantView字段指示该网页是否支持即时预览。</p>
 * 
 * <h2>使用示例</h2>
 * <pre>
 * // 创建WebPage DTO
 * WebPageDTO webPage = WebPageDTO.builder()
 *     .url("https://example.com/article")
 *     .displayUrl("example.com/article")
 *     .type("article")
 *     .siteName("Example Site")
 *     .title("Example Article Title")
 *     .description("This is an example article description...")
 *     .author("John Doe")
 *     .hasInstantView(true)
 *     .instantViewVersion("2.0")
 *     .build();
 * </pre>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPageDTO {
    
    /**
     * 网页URL
     * <p>网页的完整URL地址。</p>
     * <p>示例: {@code "https://example.com/article"}</p>
     */
    private String url;
    
    /**
     * 显示URL
     * <p>用于显示的简化URL（通常不包含协议部分）。</p>
     * <p>示例: {@code "example.com/article"}</p>
     */
    private String displayUrl;
    
    /**
     * 网页类型
     * <p>网页的类型标识。</p>
     * <p>常见值: {@code "article"}, {@code "video"}, {@code "photo"}</p>
     */
    private String type;
    
    /**
     * 网站名称
     * <p>网页所属网站的名称。</p>
     * <p>示例: {@code "Example Site"}, {@code "Medium"}, {@code "YouTube"}</p>
     */
    private String siteName;
    
    /**
     * 网页标题
     * <p>网页的标题（通常来自HTML的title标签或og:title）。</p>
     */
    private String title;
    
    /**
     * 网页描述
     * <p>网页的描述或摘要（通常来自meta description或og:description）。</p>
     */
    private String description;
    
    /**
     * 作者
     * <p>网页内容的作者名称（如果有）。</p>
     */
    private String author;
    
    /**
     * 是否支持即时预览
     * <p>指示该网页是否支持Telegram的即时预览功能。</p>
     * <p>如果为true，用户可以在Telegram内直接查看网页内容。</p>
     */
    private Boolean hasInstantView;
    
    /**
     * 即时预览版本
     * <p>即时预览功能的版本号（如果支持即时预览）。</p>
     * <p>示例: {@code "2.0"}</p>
     */
    private String instantViewVersion;
}
