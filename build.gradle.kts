import java.time.Instant

plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.xlyo"
// 主版本号.次版本号.修订号[-预发布标识]
version = "1.0.0"
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
    implementation("org.springframework.boot:spring-boot-starter-cache")
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

// 生成版本信息类的任务
tasks.register("generateVersionInfo") {
    group = "build"
    description = "Generate version information class from build.gradle.kts"
    
    val outputDir = layout.buildDirectory.dir("generated/sources/version/java").get().asFile
    val packageName = "org.xlyo.cocomonyab.config.version"
    val className = "VersionInfo"
    val packagePath = packageName.replace('.', '/')
    val outputFile = file("$outputDir/$packagePath/$className.java")
    
    val projectVersion = project.version.toString()
    val projectGroup = project.group.toString()
    val projectDescription = project.description ?: "CocoMonyaB Backend System"
    
    inputs.property("version", projectVersion)
    inputs.property("group", projectGroup)
    inputs.property("description", projectDescription)
    outputs.file(outputFile)
    
    doLast {
        outputFile.parentFile.mkdirs()
        
        val buildTime = Instant.now().toString()
        val javaVersion = System.getProperty("java.version")
        val gradleVersion = gradle.gradleVersion
        
        outputFile.writeText("""
package $packageName;

/**
 * 版本信息类（自动生成）
 * <p>
 * 此类由Gradle任务自动生成，包含项目的版本、构建时间等信息。
 * 请勿手动修改此文件。
 * </p>
 * 
 * @see org.xlyo.cocomonyab.controller.SystemStatusController
 */
public final class VersionInfo {
    
    /**
     * 项目版本号
     */
    public static final String VERSION = "$projectVersion";
    
    /**
     * 项目组ID
     */
    public static final String GROUP = "$projectGroup";
    
    /**
     * 项目描述
     */
    public static final String DESCRIPTION = "$projectDescription";
    
    /**
     * 构建时间（ISO 8601格式）
     */
    public static final String BUILD_TIME = "$buildTime";
    
    /**
     * Java版本
     */
    public static final String JAVA_VERSION = "$javaVersion";
    
    /**
     * Gradle版本
     */
    public static final String GRADLE_VERSION = "$gradleVersion";
    
    /**
     * 项目名称
     */
    public static final String PROJECT_NAME = "CocoMonyaB";
    
    private VersionInfo() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 获取完整的版本信息字符串
     * 
     * @return 格式化的版本信息
     */
    public static String getFullVersionInfo() {
        return String.format("%s v%s (Built: %s, Java: %s)", 
            PROJECT_NAME, VERSION, BUILD_TIME, JAVA_VERSION);
    }
}
        """.trimIndent())
        
        println("Generated version info class: $outputFile")
    }
}

// 将生成的源代码添加到源集
sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/version/java"))
        }
    }
}

// 确保在编译前生成版本信息
tasks.compileJava {
    dependsOn("generateVersionInfo")
}
