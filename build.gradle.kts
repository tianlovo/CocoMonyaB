plugins {
    id("com.google.protobuf") version "0.9.6"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    java
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
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 数据库
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

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
    
    // Property-based testing
    testImplementation("net.jqwik:jqwik:1.9.2")
    
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mongodb:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")

    // TDLight 依赖
    implementation(platform("it.tdlight:tdlight-java-bom:3.4.0+td.1.8.26"))
    implementation("it.tdlight:tdlight-java")
    runtimeOnly(group = "it.tdlight", name = "tdlight-natives", classifier = "windows_amd64")

    // Protobuf
    implementation("com.google.protobuf:protobuf-java:${property("protobufVersion")}")
    implementation("com.google.protobuf:protobuf-java-util:${property("protobufVersion")}")
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

protobuf {
    protoc {
        // 指定 protoc 编译器版本，与依赖版本一致
        artifact = "com.google.protobuf:protoc:${property("protobufVersion")}"
    }
    generateProtoTasks {
        ofSourceSet("main").forEach { task ->
            task.builtins {
                // 如果已存在则获取，否则创建
                maybeCreate("java").apply {
                    // 设置选项，例如：
                    // option("optimize_for = SPEED")
                }
            }
        }
    }
    // 生成的 Java 文件存放目录（默认在 `build/generated/source/proto/main`）
}

// 使 IntelliJ 能识别生成的源文件
sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}