plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.xlyo"
version = "0.0.1-INDEV"
description = "【后端】基于 TG Userbot 监控与多级审核，实现媒体资源自动化筛选、编辑及本地结构化存储的存档系统。"

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
    implementation("de.svenkubiak:embedded-mongodb:9.0.1")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 环境配置
    implementation(platform("me.paulschwarz:spring-dotenv-bom:5.1.0"))
    implementation("me.paulschwarz:springboot4-dotenv:5.1.0")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

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
