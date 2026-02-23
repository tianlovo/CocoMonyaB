# DataDirectoryManager 使用指南

## 概述

`DataDirectoryManager` 是一个专门用于管理应用运行时 data 目录的组件，采用"释放型数据目录"模式，确保应用在不同环境下都能正确创建和管理数据目录。

## 设计理念

### 释放型数据目录

"释放型数据目录"是指应用运行时自动在特定位置创建 data 目录的模式：

- **开发环境（IDE运行）**：在项目根目录创建 `data/` 目录
- **生产环境（jar包运行）**：在 jar 包同级目录创建 `data/` 目录

这种设计的优势：
1. 数据与应用分离，便于备份和迁移
2. 支持多环境部署，无需修改配置
3. 自动创建目录结构，开箱即用
4. 统一管理所有数据路径，避免硬编码
5. 配置简单，所有路径由 DataDirectoryManager 统一管理

### 路径固定策略

为了确保配置文件和数据目录的一致性，以下路径是固定的，不可配置：

- **data 根目录**：固定为应用同级的 `data` 目录
- **config 目录**：固定为 `data/config`

其他子目录（如 db、session、logs 等）可以通过配置文件自定义相对路径。

## 目录结构

```
data/
├── config/          # 配置文件目录
│   ├── .env         # 环境变量配置文件
│   └── .env.example # 环境变量配置示例
├── db/              # 数据库目录
│   └── mongo/       # MongoDB 数据存储
├── session/         # 会话数据目录
│   └── td/          # Telegram 会话数据
│       ├── data/    # TDLib 数据库
│       └── downloads/ # 文件下载目录
├── bin/             # 二进制文件目录
│   └── mongo/       # MongoDB 二进制文件
├── tmp/             # 临时文件目录
└── logs/            # 日志文件目录
```

## 配置方式

### 1. 默认配置（零配置，推荐）

不需要任何配置，DataDirectoryManager 会自动检测运行环境并创建目录：

```yaml
# application.yaml
# 无需配置，DataDirectoryManager 自动工作
```

### 2. 自定义子目录

自定义各子目录的相对路径（相对于 data 根目录）：

注意：data 根目录和 config 目录路径固定，不可配置。

```yaml
app:
  data:
    database-directory: "db"
    mongo-db-directory: "db/mongo"
    session-directory: "session"
    telegram-session-directory: "session/td"
    bin-directory: "bin"
    mongo-bin-directory: "bin/mongo"
    tmp-directory: "tmp"
    logs-directory: "logs"
```

### 3. 完整配置示例

```yaml
app:
  data:
    # 自定义子目录（可选）
    mongo-db-directory: "database/mongodb"
    logs-directory: "log"
```

## 统一路径管理

所有组件的路径都由 DataDirectoryManager 统一管理，无需在各个组件中单独配置：

- **MongoDB 数据目录**：由 DataDirectoryManager 提供
- **Telegram 会话目录**：由 DataDirectoryManager 提供
- **日志目录**：由 DataDirectoryManager 提供
- **配置目录**：由 DataDirectoryManager 提供

这种设计确保了：
1. 配置简单，只需配置一处
2. 路径一致，避免配置冲突
3. 易于维护，修改路径只需改一个地方

## 早期初始化流程

应用启动前会执行 `EarlyEnvFileInitializer` 进行环境文件初始化，确保 `.env` 文件存在：

### 初始化逻辑

1. **检查 data/config/.env**：如果存在，继续启动应用
2. **检查应用同级 .env**：如果存在，迁移到 data/config/ 并继续启动
3. **首次启动**：
   - 从 resources 复制 `.env.example` 到应用同级目录
   - 提示用户填写配置
   - 退出应用，等待用户配置
4. **下次启动**：检测到同级 .env 文件，自动迁移到 data/config/

### 首次启动提示

```
================================================================================
未找到环境配置文件 (.env)
================================================================================

已在应用目录创建配置模板文件: E:\app\.env.example

请按以下步骤操作：
  1. 将 .env.example 重命名为 .env
  2. 编辑 .env 文件，填写以下必需配置：
     - API_ID: Telegram API ID（从 https://my.telegram.org/apps 获取）
     - API_HASH: Telegram API Hash
     - TG_PHONE: 登录手机号（格式：+8613800138000）
     - TG_2FA: 两步验证密码（如果启用了 2FA）
     - WS_TRUSTED_TOKEN: WebSocket 认证令牌
  3. 保存文件后重新启动应用

下次启动时，.env 文件将自动移动到 data/config/ 目录
================================================================================
```

### 注意事项

- `EarlyEnvFileInitializer` 在 Spring 启动前执行，不依赖 Spring 容器
- 不使用日志框架（因为日志系统还未初始化），使用 `System.out` 和 `System.err`
- 自动处理 .env 文件的迁移，用户无需手动操作

## 迁移指南

### 从硬编码路径迁移

**之前（硬编码）：**
```java
private String configDirectory = "data/config";
```

**之后（使用 DataDirectoryManager）：**
```java
private final DataDirectoryManager dataDirectoryManager;

public String getConfigDirectory() {
    return dataDirectoryManager.getConfigPath().toString();
}
```

### 配置文件迁移

**之前（application.yaml）：**
```yaml
app:
  config-directory: "data/config"

telegram:
  database-directory: "data/session/td/data"
  download-directory: "data/session/td/downloads"
```

**之后（application.yaml）：**
```yaml
# 由 DataDirectoryManager 自动管理，无需配置
app: {}

telegram:
  device-model: "Coco Monya"
  login-timeout-minutes: 2
```

## 注意事项

1. **初始化顺序**：`DataDirectoryManager` 使用 `@PostConstruct` 初始化，确保在其他组件使用前完成初始化

2. **测试环境**：测试类需要导入 `TestDataDirectoryConfiguration` 以提供 `DataDirectoryManager` 实例

3. **路径分隔符**：使用 `Path` 对象可以自动处理不同操作系统的路径分隔符

4. **目录创建**：所有目录在初始化时自动创建，无需手动创建

5. **日志配置**：logback 配置文件需要使用系统属性 `${LOG_PATH}` 来获取日志路径

## 相关组件

- `TelegramProperties`：Telegram 配置属性，使用 DataDirectoryManager 管理会话目录
- `MongoDBProperties`：MongoDB 配置属性，使用 DataDirectoryManager 管理数据库目录
- `AppProperties`：应用配置属性，使用 DataDirectoryManager 管理配置目录
- `SpringDotenvConfiguration`：动态设置 .env 文件目录
- `LogbackConfiguration`：动态设置日志文件目录

## 示例场景

### 场景1：添加新的数据子目录

如果需要添加新的数据子目录（如 `data/cache/`），可以：

1. 在 `DataDirectoryManager` 中添加字段和初始化逻辑：

```java
@Getter
private Path cachePath;

private void initializeSubDirectories() {
    // ... 现有代码 ...
    cachePath = dataRootPath.resolve("cache");
}

private void createDirectories() throws IOException {
    // ... 现有代码 ...
    createDirectoryIfNotExists(cachePath, "缓存目录");
}
```

2. 在需要的地方使用：

```java
Path cacheDir = dataDirectoryManager.getCachePath();
```

### 场景2：在不同环境使用不同路径

DataDirectoryManager 会自动检测运行环境：

- IDE 运行：使用项目根目录的 `data/`
- jar 包运行：使用 jar 包同级目录的 `data/`

无需任何配置，自动适配。

## 常见问题

### Q: 如何在测试中使用不同的 data 目录？

A: 可以在测试配置中覆盖 `DataDirectoryManager` bean，或使用 `@TestPropertySource` 设置系统属性。

### Q: 如何备份 data 目录？

A: 直接备份整个 `data/` 目录即可，包含所有配置、数据库和会话信息。

### Q: 如何在 Docker 中使用？

A: 可以将 `data/` 目录挂载为 volume：

```dockerfile
VOLUME /app/data
```

### Q: 如何清理临时文件？

A: 可以定期清理 `data/tmp/` 目录，或在应用启动时清理。

## 总结

`DataDirectoryManager` 提供了统一、灵活的数据目录管理方案，消除了硬编码路径，提高了代码的可维护性和可移植性。通过依赖注入的方式使用，确保了组件的解耦和测试友好性。
