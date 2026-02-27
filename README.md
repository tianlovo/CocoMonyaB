# CocoMonyaB

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green.svg)](https://www.mongodb.com/)

> 【后端】基于 TG Userbot 监控与多级审核，实现媒体资源自动化筛选、编辑及本地结构化存储的存档系统。

## 目录

- [背景](#背景)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [安装](#安装)
  - [环境要求](#环境要求)
  - [快速开始](#快速开始)
- [使用说明](#使用说明)
  - [配置说明](#配置说明)
  - [启动应用](#启动应用)
  - [API 文档](#api-文档)
- [核心模块](#核心模块)
- [开发指南](#开发指南)
- [维护者](#维护者)
- [如何贡献](#如何贡献)
- [许可证](#许可证)

## 背景

CocoMonyaB 是一个基于 Telegram Userbot 的频道消息监控与处理系统。系统通过 TDLight（Telegram Database Library）实现对指定频道的实时消息监控，并提供灵活的过滤器链和插件机制，支持消息的自动化筛选、标签匹配、转发和本地存储。

### 主要应用场景

- **频道内容监控**：实时监控多个 Telegram 频道的消息更新
- **智能内容过滤**：基于标签库（作者、原作、角色）的智能内容筛选
- **自动化转发**：根据标签匹配规则自动转发消息到目标频道
- **数据归档**：结构化存储频道消息，支持全文检索和数据分析
- **媒体管理**：支持媒体组（相册）的完整性处理和批量操作

## 功能特性

### 核心功能

- ✅ **Telegram 客户端管理**
  - 基于 TDLight 的 Userbot 实现
  - 自动登录和会话管理
  - 支持两步验证（2FA）
  - 实时消息更新监听

- ✅ **频道监控系统**
  - 动态频道监控配置（REST API）
  - 支持多频道并发监控
  - 实时监控状态切换
  - 频道信息自动同步

- ✅ **消息处理框架**
  - 可扩展的过滤器链机制
  - 插件化的消息处理架构
  - 支持媒体组（相册）完整性处理
  - Telegraph 文章自动识别
  - 消息去重和空消息过滤

- ✅ **标签数据库系统**
  - 作者库（Author）：管理创作者信息
  - 原作库（Work）：管理作品信息
  - 角色库（Character）：管理角色信息
  - 支持别名系统和全局唯一性约束
  - 标签过滤配置（白名单/黑名单模式）

- ✅ **消息转发系统**
  - 基于标签匹配的自动转发
  - 支持媒体组原子性转发
  - FIFO 队列保证消息顺序
  - 失败重试机制
  - 转发状态跟踪

- ✅ **数据存储**
  - MongoDB 原始消息存储
  - 结构化消息数据
  - 支持嵌入式和远程 MongoDB
  - 自动索引优化
  - TTL 自动清理

- ✅ **RESTful API**
  - 频道管理 API
  - 消息查询 API
  - 标签库管理 API
  - 系统状态监控 API
  - 统一响应格式

### 高级特性

- 🔒 **并发安全**
  - 媒体组状态机管理
  - 分段锁（Striped Lock）机制
  - 线程安全的缓存策略
  - 防止消息重复处理

- 📊 **监控与统计**
  - 消息处理统计
  - 过滤器执行监控
  - 转发成功率统计
  - 系统健康检查

- 🔄 **消息来源生成器**
  - 支持多种消息来源
  - 未读消息自动检测
  - 消息缓冲区机制
  - 速率限制保护

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Telegram Server                            │
└────────────────────────┬────────────────────────────────────┘
                         │ TDLib Protocol
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              TelegramClientManager                           │
│  - 客户端初始化                                               │
│  - 认证管理                                                   │
│  - Update 分发                                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│           ChannelMonitorService                              │
│  - 消息接收与分发                                             │
│  - 媒体组缓冲处理                                             │
│  - 过滤器链执行                                               │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Filter Chain │  │   Storage    │  │   Parser     │
│  - 频道监控  │  │  - 原始消息  │  │  - 类型检测  │
│  - 类型过滤  │  │  - 结构化    │  │  - 内容解析  │
│  - 重复检测  │  │  - 索引优化  │  │  - 实体创建  │
└──────┬───────┘  └──────────────┘  └──────┬───────┘
       │                                    │
       └────────────────┬───────────────────┘
                        ↓
              ┌─────────────────────┐
              │   PluginManager     │
              │  - 插件调度          │
              │  - 优先级管理        │
              └──────────┬───────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Storage    │  │   Forward    │  │   Console    │
│   Plugin     │  │   Plugin     │  │   Printer    │
└──────────────┘  └──────────────┘  └──────────────┘
```

详细架构说明请参考：
- [消息处理框架开发者指南](docs/arch/消息处理框架开发者指南.md)
- [消息处理流程图](docs/arch/消息处理流程图.md)
- [消息来源生成器系统开发者指南](docs/arch/消息来源生成器系统开发者指南.md)

## 安装

### 环境要求

- **Java**: JDK 21 或更高版本
- **Gradle**: 8.5 或更高版本（项目自带 Gradle Wrapper）
- **MongoDB**: 7.0 或更高版本（支持嵌入式模式）
- **操作系统**: Windows
- **内存**: 建议 2GB 以上

### 快速开始

#### 1. 克隆项目

```bash
git clone https://github.com/your-org/CocoMonyaB.git
cd CocoMonyaB
```

#### 2. 配置环境变量

首次启动时，系统会自动在应用目录创建 `.env.example` 模板文件。

将 `.env.example` 重命名为 `.env` 并填写以下配置：

```env
# Telegram API 配置（必填）
API_ID=12345678
API_HASH=0123456789abcdef0123456789abcdef

# 手机号（可选，验证码登录时如果配置了会自动使用）
TG_PHONE=+8613800138000

# 两步验证密码（可选，如果启用了 2FA 建议配置）
TG_2FA=your_2fa_password
```

**获取 Telegram API 凭证：**
1. 访问 https://my.telegram.org/apps
2. 登录你的 Telegram 账号
3. 创建新应用获取 `API_ID` 和 `API_HASH`

**登录方式说明：**

应用启动时会先尝试使用已保存的会话自动登录。如果自动登录失败，会通过控制台交互让你选择登录方式：

1. **验证码登录**
   - 验证码发送到其他已登录的 Telegram 设备
   - 如果配置了 `TG_PHONE` 会自动使用，否则需要手动输入
   - 适合已在其他设备登录的用户

2. **二维码登录（推荐首次登录使用）**
   - 在控制台显示二维码，使用手机扫码登录
   - 同时保存二维码图片到 `data/tmp/telegram_qrcode.png`
   - 无需配置或输入手机号
   - 适合首次登录或验证码无法收到的情况

**重要提示：**
- **首次登录推荐使用二维码登录**，更快捷方便
- 如果验证码一直无法在所有设备收到，请使用二维码登录
- 控制台显示的二维码可能比较抽象，建议直接打开 `data/tmp/telegram_qrcode.png` 图片文件扫码
- 登录成功后，下次启动会自动登录，无需重复操作

#### 3. 构建项目

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

#### 4. 运行应用

**开发模式（IDE 中运行）：**

```bash
# Windows
gradlew.bat bootRun

# Linux/macOS
./gradlew bootRun
```

**生产模式（Windows 控制台）：**

为了解决 Windows 控制台中文乱码问题，建议使用提供的启动脚本：

```bash
# 1. 先构建项目
gradlew.bat build

# 2. 使用启动脚本运行（自动处理 jar 重命名和 UTF-8 编码）
start.bat
```

`start.bat` 脚本会自动执行以下操作：
- 查找并重命名 `CocoMonyaB-*.jar` 为 `CocoMonyaB.jar`
- 设置控制台 UTF-8 编码
- 启动应用

或者手动运行并设置编码：

```bash
# 设置控制台编码为 UTF-8
chcp 65001

# 运行应用
java -Dfile.encoding=UTF-8 -jar build\libs\CocoMonyaB-1.0.0.jar
```

首次启动时需要输入 Telegram 验证码完成登录。

#### 5. 验证安装

访问系统状态 API：

```bash
curl http://localhost:8080/api/system/status
```

预期响应：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "ready": true,
    "reason": null,
    "timestamp": 1708588800000
  }
}
```

## 使用说明

### 配置说明

#### application.yaml 配置

```yaml
# 应用配置
spring:
  application:
    name: CocoMonyaB
  
  # MongoDB 配置
  data:
    mongodb:
      mode: embedded  # embedded（嵌入式）或 remote（远程）
      embedded:
        version: 7.0.12
        port: 27017
        bind-ip: 127.0.0.1
      # 远程模式配置（可选）
      # uri: mongodb://localhost:27017/cocomonya

# Telegram 配置
telegram:
  device-model: "Coco Monya"
  login-timeout-minutes: 2
  # 登录方式将在启动时通过控制台交互选择

# 数据目录配置（可选，使用默认值即可）
app:
  data:
    database-directory: "db"
    mongo-db-directory: "db/mongo"
    session-directory: "session"
    telegram-session-directory: "session/td"
```

详细配置说明请参考：
- [配置文件使用指南](docs/arch/配置文件使用指南.md) - 完整的配置说明和最佳实践
- [DataDirectoryManager使用指南](docs/arch/DataDirectoryManager使用指南.md) - 数据目录管理
- [TelegramClientManager使用指南](docs/tg/TelegramClientManager使用指南.md) - Telegram 客户端配置

### 启动应用

#### 开发模式（IDE）

在 IDE 中直接运行或使用 Gradle：

```bash
# Windows
gradlew.bat bootRun

# Linux/macOS
./gradlew bootRun
```

#### 生产模式

**Windows 系统（推荐使用启动脚本）：**

```bash
# 1. 构建 JAR 包
gradlew.bat build

# 2. 使用启动脚本（自动处理 jar 重命名和编码问题）
start.bat
```

`start.bat` 脚本功能：
- 自动查找并重命名 `CocoMonyaB-*.jar` 为 `CocoMonyaB.jar`
- 自动设置 UTF-8 编码避免中文乱码
- 启动应用并等待用户确认

**手动启动（所有系统）：**

```bash
# 构建 JAR 包
./gradlew build

# Windows - 设置 UTF-8 编码
chcp 65001
java -Dfile.encoding=UTF-8 -jar build/libs/CocoMonyaB-1.0.0.jar

# Linux/macOS
java -jar build/libs/CocoMonyaB-1.0.0.jar
```

> **注意**：Windows 系统在控制台运行时，如果出现中文乱码，请使用 `start.bat` 脚本或手动设置 UTF-8 编码。IDE 中运行不受影响。

### API 文档

系统提供完整的 RESTful API，详细文档请参考：

- [API 接口文档](docs/api/api.md)
- [API 响应规范文档](docs/api/API 响应规范文档.md)

#### 常用 API 示例

**1. 添加监控频道**

```bash
curl -X POST http://localhost:8080/api/channel \
  -H "Content-Type: application/json" \
  -d '{
    "channelId": -1001234567890,
    "channelUsername": "tech_news",
    "channelTitle": "科技新闻频道",
    "monitoringStatus": true
  }'
```

**2. 查询频道列表**

```bash
curl http://localhost:8080/api/channel/list
```

**3. 查询消息**

```bash
curl "http://localhost:8080/api/message/page?current=1&size=10&chatId=-1001234567890"
```

**4. 创建作者标签**

```bash
curl -X POST http://localhost:8080/api/tag/author \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "aliases": ["作者别名1", "作者别名2"],
    "signature": "这是作者的个性签名"
  }'
```

## 核心模块

### 1. Telegram 客户端管理

- **TelegramClientManager**: 全局单例的 Telegram 客户端管理器
- **TgUpdateNewMessageHandler**: 消息更新处理器
- 支持自动登录、会话持久化、两步验证

详见：[TelegramClientManager使用指南](docs/tg/TelegramClientManager使用指南.md)

### 2. 频道监控服务

- **ChannelMonitorService**: 频道消息监控核心服务
- **ChannelMonitoringFilter**: 动态频道监控过滤器
- 支持 REST API 动态管理监控列表

### 3. 消息处理框架

- **FilterChainManager**: 过滤器链管理器
- **MessageParser**: 消息解析器
- **PluginManager**: 插件管理器
- 支持自定义过滤器和插件开发

详见：[消息处理框架开发者指南](docs/arch/消息处理框架开发者指南.md)

### 4. 标签数据库系统

- **TagAuthorService**: 作者库管理
- **TagWorkService**: 原作库管理
- **TagCharacterService**: 角色库管理
- **TagFilterConfigService**: 标签过滤配置管理

### 5. 消息转发系统

- **TagBasedMessageForwardingPlugin**: 基于标签的消息转发插件
- **ForwardQueueService**: 转发队列管理
- **ProcessedMessageService**: 已处理消息跟踪

### 6. 数据存储

- **RawMessageRepository**: 原始消息存储
- **ChannelMessageRepository**: 结构化消息存储
- **MongoDB 索引优化**: 支持高效查询

详见：[数据库结构文档](docs/arch/数据库结构文档.md)

## 开发指南

### 项目结构

```
CocoMonyaB/
├── src/
│   ├── main/
│   │   ├── java/org/xlyo/cocomonyab/
│   │   │   ├── config/          # 配置类
│   │   │   ├── controller/      # REST 控制器
│   │   │   ├── service/         # 业务服务
│   │   │   ├── repository/      # 数据访问层
│   │   │   ├── domain/          # 领域模型
│   │   │   │   ├── dto/         # 数据传输对象
│   │   │   │   ├── vo/          # 视图对象
│   │   │   │   └── entity/      # 实体类
│   │   │   ├── filter/          # 消息过滤器
│   │   │   ├── plugin/          # 消息处理插件
│   │   │   ├── source/          # 消息来源生成器
│   │   │   └── util/            # 工具类
│   │   └── resources/
│   │       ├── application.yaml # 应用配置
│   │       └── logback.xml      # 日志配置
│   └── test/                    # 测试代码
├── docs/                        # 项目文档
│   ├── api/                     # API 文档
│   ├── arch/                    # 架构文档
│   └── tg/                      # Telegram 相关文档
├── data/                        # 数据目录（运行时生成）
│   ├── config/                  # 配置文件
│   ├── db/                      # 数据库文件
│   ├── session/                 # 会话数据
│   └── logs/                    # 日志文件
├── build.gradle.kts             # Gradle 构建脚本
└── README.md                    # 本文件
```

### 开发文档

**配置与部署**：
- [配置文件使用指南](docs/arch/配置文件使用指南.md) - 完整的配置说明和故障排查
- [DataDirectoryManager使用指南](docs/arch/DataDirectoryManager使用指南.md) - 数据目录管理
- [TelegramClientManager使用指南](docs/tg/TelegramClientManager使用指南.md) - Telegram 客户端配置

**开发指南**：
- [消息处理框架开发者指南](docs/arch/消息处理框架开发者指南.md) - 过滤器和插件开发
- [消息来源生成器系统开发者指南](docs/arch/消息来源生成器系统开发者指南.md) - 自定义消息来源
- [字段命名规范与区分指南](docs/arch/字段命名规范与区分指南.md) - 避免字段混淆

**架构文档**：
- [数据库结构文档](docs/arch/数据库结构文档.md) - MongoDB 集合设计
- [消息处理流程图](docs/arch/消息处理流程图.md) - 系统流程图解

### 开发自定义过滤器

```java
@Component
public class MyCustomFilter extends AbstractMessageFilter {
    
    @Override
    public String getName() {
        return "MyCustomFilter";
    }
    
    @Override
    public int getPriority() {
        return 50; // 优先级
    }
    
    @Override
    protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
        // 实现过滤逻辑
        if (shouldReject(message)) {
            context.setRejectReason("拒绝原因");
            return FilterResult.REJECT;
        }
        return FilterResult.ACCEPT;
    }
}
```

### 开发自定义插件

```java
@Component
@RequiredArgsConstructor
public class MyCustomPlugin implements MessagePlugin {
    
    @Override
    public String getName() {
        return "MyCustomPlugin";
    }
    
    @Override
    public int getPriority() {
        return 100; // 优先级
    }
    
    @Override
    public void process(BaseMessageEntity entity, TdApi.Message originalMessage) {
        // 实现处理逻辑
        log.info("处理消息: {}", entity.getMessageId());
    }
}
```

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "org.xlyo.cocomonyab.service.ChannelServiceTest"

# 生成测试报告
./gradlew test jacocoTestReport
```

## 维护者

[@CocoMonyaB](https://github.com/tianlovo/CocoMonyaB)

## 如何贡献

非常欢迎你的加入！[提一个 Issue](https://github.com/your-org/CocoMonyaB/issues/new) 或者提交一个 Pull Request。

### 贡献指南

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

### 代码规范

- 遵循 Java 代码规范
- 使用有意义的变量和方法名
- 添加必要的注释和文档
- 编写单元测试
- 确保所有测试通过

### 贡献者

感谢以下参与项目的人：

<a href="https://github.com/tianlovo/CocoMonyaB/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=tianlovo/CocoMonyaB" />
</a>

*Made with [contrib.rocks](https://contrib.rocks).*

## 许可证

[MIT](LICENSE) © tianluoqaq

---

## 相关链接

- [TDLight 官方文档](https://tdlight-team.github.io/tdlight-docs)
- [Telegram API 文档](https://core.telegram.org/tdlib/docs)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [MongoDB 文档](https://www.mongodb.com/docs/)

## 常见问题

### Q: 如何获取 Telegram API 凭证？

A: 访问 https://my.telegram.org/apps 登录并创建应用即可获取 `API_ID` 和 `API_HASH`。

### Q: 首次启动需要输入什么？

A: 应用会先尝试自动登录。如果自动登录失败（首次启动或 session 过期），会提示你选择登录方式（1/2）：

- **1. 验证码登录**: 验证码发送到其他已登录的 Telegram 设备，需要输入验证码
- **2. 二维码登录（推荐首次登录）**: 在控制台显示二维码，同时保存到 `data/tmp/telegram_qrcode.png`，使用手机扫描即可

**推荐做法：**
- 首次登录建议选择二维码登录（输入 2），更快捷方便
- 控制台二维码可能显示不清晰，直接打开 `data/tmp/telegram_qrcode.png` 图片文件扫码
- 如果验证码一直无法在所有设备收到，请使用二维码登录

如果启用了两步验证，还需要输入 2FA 密码（或在配置文件中预先配置）。

### Q: 如何切换登录方式？

A: 如果已有有效的 session，应用会自动登录。如果想重新选择登录方式，需要先清除 session：
```bash
rm -rf data/session/td/data
```
然后重启应用，会提示选择登录方式。

### Q: 二维码显示乱码或无法识别怎么办？

A: 控制台显示的二维码可能比较抽象，系统提供了多种备用方案：

1. **使用图片文件（推荐）**：系统会自动保存二维码图片到 `data/tmp/telegram_qrcode.png`，使用图片查看器打开并扫描
2. **使用登录链接**：复制控制台显示的登录链接，在 Telegram 中打开
3. **切换登录方式**：重启应用，选择验证码登录（输入 1）

如果想改善控制台二维码显示效果：
- Windows 用户建议使用 Windows Terminal 或 PowerShell 7+
- 确保控制台字体支持 Unicode 字符（█ ▀ ▄）
- 调整控制台字体大小

### Q: 验证码一直收不到怎么办？

A: 如果验证码一直无法在所有已登录的 Telegram 设备上收到，请使用二维码登录：
1. 重启应用
2. 选择 `2`（二维码登录）
3. 打开 `data/tmp/telegram_qrcode.png` 图片文件
4. 使用手机 Telegram 扫描二维码
5. 在手机上确认登录

### Q: 如何切换到远程 MongoDB？

A: 修改 `application.yaml` 中的 `spring.data.mongodb.mode` 为 `remote`，并配置 `uri`。详见 [配置文件使用指南](docs/arch/配置文件使用指南.md#23-mongodb-配置)。

### Q: 如何添加自定义过滤器？

A: 创建类继承 `AbstractMessageFilter`，实现必要方法，并添加 `@Component` 注解即可自动注册。

### Q: Windows 控制台中文乱码怎么办？

A: 使用项目根目录的 `start.bat` 脚本启动应用，该脚本会自动设置 UTF-8 编码。或者手动执行 `chcp 65001` 后再运行 JAR 文件。IDE 中运行不受影响。

### Q: 消息转发失败怎么办？

A: 检查转发队列状态 API，查看失败原因和重试次数，必要时手动重试或调整配置。详见 [配置文件使用指南](docs/arch/配置文件使用指南.md#6-故障排查)。

### Q: 如何调整系统性能？

A: 可以调整并发安全性配置、MongoDB 连接池、JVM 内存参数等。详见 [配置文件使用指南](docs/arch/配置文件使用指南.md#64-性能调优)。

---

**开发阶段说明**

本项目目前处于开发阶段，功能持续完善中。欢迎提出建议和反馈！
