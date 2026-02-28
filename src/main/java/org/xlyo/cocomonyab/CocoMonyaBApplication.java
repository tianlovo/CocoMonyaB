package org.xlyo.cocomonyab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xlyo.cocomonyab.config.initializer.EarlyEnvFileInitializer;

/**
 * CocoMonyaB 应用主类
 * <p>
 * 应用启动流程：
 * <ol>
 *   <li>早期环境文件初始化（在 Spring 启动前执行）</li>
 *   <li>Spring Boot 应用启动</li>
 *   <li>配置初始化阶段（ConfigurationManager）</li>
 *   <li>数据库初始化阶段（DatabaseManager）</li>
 *   <li>集合初始化阶段（CollectionInitializer）</li>
 *   <li>插件初始化阶段（PluginInitializer）</li>
 *   <li>消息源初始化阶段（MessageSourceInitializer）</li>
 *   <li>API 初始化阶段（ApiInitializer）</li>
 *   <li>应用就绪（ApplicationReadyListener）</li>
 * </ol>
 * </p>
 * <p>
 * 组件扫描配置：
 * <ul>
 *   <li>扫描基础包：org.xlyo.cocomonyab</li>
 *   <li>包含所有启动组件、配置类、服务类、控制器等</li>
 * </ul>
 * </p>
 * <p>
 * 事件驱动机制：
 * 各启动阶段通过 Spring 事件机制协调，确保按正确的依赖顺序执行。
 * 每个阶段完成后发布相应的就绪事件，下一阶段监听该事件后开始执行。
 * </p>
 *
 * @see org.xlyo.cocomonyab.config.initializer.ConfigurationManager
 * @see org.xlyo.cocomonyab.config.initializer.DatabaseManager
 * @see org.xlyo.cocomonyab.config.initializer.CollectionInitializer
 * @see org.xlyo.cocomonyab.config.initializer.PluginInitializer
 * @see org.xlyo.cocomonyab.config.initializer.MessageSourceInitializer
 * @see org.xlyo.cocomonyab.config.initializer.ApiInitializer
 * @see org.xlyo.cocomonyab.config.initializer.ApplicationReadyListener
 * @see org.xlyo.cocomonyab.event.startup.StartupEventPublisher
 * @see org.xlyo.cocomonyab.event.startup.StartupProgressTracker
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
	"org.xlyo.cocomonyab.config",           // 配置类（包括启动初始化器）
	"org.xlyo.cocomonyab.event",            // 事件类（包括启动事件）
	"org.xlyo.cocomonyab.actuator",         // Actuator 端点
	"org.xlyo.cocomonyab.controller",       // REST 控制器
	"org.xlyo.cocomonyab.service",          // 业务服务
	"org.xlyo.cocomonyab.repository",       // 数据仓库
	"org.xlyo.cocomonyab.plugin",           // 消息处理插件
	"org.xlyo.cocomonyab.source",           // 消息源
	"org.xlyo.cocomonyab.filter",           // 消息过滤器
	"org.xlyo.cocomonyab.telegram",         // Telegram 客户端
	"org.xlyo.cocomonyab.interceptor",      // 拦截器
	"org.xlyo.cocomonyab.common"            // 通用组件
})
public class CocoMonyaBApplication {

	public static void main(String[] args) {
		// 早期环境文件初始化（在 Spring 启动前执行）
		if (!EarlyEnvFileInitializer.initialize()) {
			// 如果初始化失败或需要用户配置，退出应用
			System.exit(0);
		}
		
		// 启动 Spring Boot 应用
		// 启动后会自动触发配置初始化阶段（ConfigurationManager.initialize）
		SpringApplication.run(CocoMonyaBApplication.class, args);
	}

}
