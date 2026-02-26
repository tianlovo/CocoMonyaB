package org.xlyo.cocomonyab.plugin.impl.console;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 控制台打印插件配置属性
 * 
 * <p>该配置类用于管理控制台打印插件的配置项。
 * 通过Spring Boot的{@code @ConfigurationProperties}机制，
 * 可以在application.yml或application.properties文件中配置这些属性。</p>
 * 
 * <h2>配置前缀</h2>
 * <p>所有配置项使用 {@code plugin.console-printer} 作为前缀。</p>
 * 
 * <h2>配置示例</h2>
 * <pre>
 * # application.yml
 * plugin:
 *   console-printer:
 *     enabled: true
 * </pre>
 * 
 * <h2>默认值</h2>
 * <ul>
 *   <li>{@code enabled}: true（插件默认启用）</li>
 * </ul>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin.console-printer")
public class ConsolePrinterProperties {
    
    /**
     * 是否启用插件
     * 
     * <p>控制控制台打印插件是否启用。</p>
     * <ul>
     *   <li>{@code true}: 插件启用，会将消息打印到控制台</li>
     *   <li>{@code false}: 插件禁用，不会打印消息</li>
     * </ul>
     * 
     * <p><strong>配置项:</strong> {@code plugin.console-printer.enabled}</p>
     * <p><strong>默认值:</strong> {@code true}</p>
     */
    private boolean enabled = true;
}
