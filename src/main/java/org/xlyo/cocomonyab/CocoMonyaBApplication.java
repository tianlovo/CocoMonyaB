package org.xlyo.cocomonyab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xlyo.cocomonyab.config.initializer.EarlyEnvFileInitializer;

@SpringBootApplication
@EnableScheduling
public class CocoMonyaBApplication {

	public static void main(String[] args) {
		// 早期环境文件初始化（在 Spring 启动前执行）
		if (!EarlyEnvFileInitializer.initialize()) {
			// 如果初始化失败或需要用户配置，退出应用
			System.exit(0);
		}
		
		// 启动 Spring Boot 应用
		SpringApplication.run(CocoMonyaBApplication.class, args);
	}

}
