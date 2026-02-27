plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.xlyo"
version = "0.0.1-INDEV"
description = "【后端】基于 TG Userbot 监控与多级审核，实现媒体资源自动化筛选、编辑及本地结构化存储的存档系统。"

val protobufVersion by extra("4.33.5")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven("https://mvn.mchv.eu/repository/mchv/")
}

dependencies {
    // Spring Boot 相关
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 数据库
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    
    // JSON 处理
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Guava for Striped Lock
    implementation("com.google.guava:guava:33.0.0-jre")
    
    // Resilience4j for Rate Limiting
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    
    // Caffeine for Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // Micrometer for Metrics (via Spring Boot Actuator)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 环境配置
    implementation(platform("me.paulschwarz:spring-dotenv-bom:5.1.0"))
    implementation("me.paulschwarz:springboot4-dotenv:5.1.0")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Property-based testing
    testImplementation("net.jqwik:jqwik:1.9.2")
    
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mongodb:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    
    // Awaitility for async testing
    testImplementation("org.awaitility:awaitility:4.2.0")

    // TDLight 依赖
    implementation(platform("it.tdlight:tdlight-java-bom:3.4.0+td.1.8.26"))
    implementation("it.tdlight:tdlight-java")
    runtimeOnly(group = "it.tdlight", name = "tdlight-natives", classifier = "windows_amd64")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}
