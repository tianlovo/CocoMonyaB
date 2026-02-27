# TelegramClientManager 使用指南

## 概述

`TelegramClientManager` 是一个全局单例的 Spring Bean 组件，负责管理 Telegram 客户端的完整生命周期，包括：

- 配置验证
- 自动登录
- 客户端操作
- 资源释放

## 特性

✅ **全局单例**: 整个应用只有一个实例，通过依赖注入使用  
✅ **自动初始化**: 在 Spring 容器启动时自动执行配置验证和登录  
✅ **依赖管理**: 确保在配置组件初始化后才开始初始化  
✅ **自动清理**: 在应用关闭时自动登出并释放资源  
✅ **线程安全**: 单例模式保证线程安全  
✅ **状态检查**: 提供 `isReady()` 方法检查客户端是否就绪  
✅ **多种登录方式**: 支持自动登录、验证码登录、二维码登录

## 登录方式

系统支持自动登录和两种手动登录方式：

### 1. 自动登录

如果存在有效的登录会话（session），应用会自动登录，无需任何操作。

### 2. 验证码登录（code，默认）

验证码发送到其他已登录的 Telegram 设备，适合日常使用。

如果配置了手机号会自动使用：
```env
TG_PHONE=+8613800138000
```

### 3. 二维码登录（qrcode）

在控制台显示二维码，使用手机扫码登录，无需配置手机号。

详细说明请参考：[登录方式指南](登录方式指南.md)

## 初始化流程

```
应用启动
    ↓
加载配置 (TgEnvProperties, TelegramProperties)
    ↓
TelegramClientManager 初始化 (@PostConstruct)
    ↓
1. 验证配置 (validateConfiguration)
    ├─ 验证 API_ID
    ├─ 验证 API_HASH
    ├─ 验证 TG_PHONE
    └─ 验证 TG_2FA (可选)
    ↓
2. 自动登录 (login)
    ├─ 初始化 TDLight 原生库
    ├─ 创建客户端工厂
    ├─ 配置 TDLib 设置
    ├─ 构建客户端
    ├─ 等待登录完成
    └─ 保存当前用户信息
    ↓
应用就绪
```

## 使用方法

### 1. 依赖注入

在任何 Spring 管理的组件中注入 `TelegramClientManager`：

```java
@Component
@RequiredArgsConstructor
public class MyService {
    
    private final TelegramClientManager telegramClientManager;
    
    public void doSomething() {
        // 使用 Telegram 客户端
    }
}
```

### 2. 检查客户端状态

在使用客户端前，建议先检查是否就绪：

```java
if (telegramClientManager.isReady()) {
    // 客户端已就绪，可以使用
} else {
    // 客户端尚未初始化或登录失败
}
```

### 3. 获取当前用户信息

```java
TdApi.User currentUser = telegramClientManager.getCurrentUser();
System.out.println("用户名: " + currentUser.firstName);
System.out.println("用户 ID: " + currentUser.id);
```

### 4. 使用 Telegram 客户端

```java
SimpleTelegramClient client = telegramClientManager.getClient();

// 发送 API 请求（异步回调方式）
client.send(new TdApi.GetMe(), result -> {
    if (result.isError()) {
        System.err.println("错误: " + result.getError().message);
        return;
    }
    
    TdApi.User user = result.get();
    System.out.println("用户: " + user.firstName);
});

// 发送 API 请求（CompletableFuture 方式）
CompletableFuture<TdApi.User> future = client.send(new TdApi.GetMe());
future.thenAccept(user -> {
    System.out.println("用户: " + user.firstName);
}).exceptionally(ex -> {
    System.err.println("错误: " + ex.getMessage());
    return null;
});
```

## 常用操作示例

### 获取聊天列表

```java
client.send(new TdApi.GetChats(new TdApi.ChatListMain(), 100), result -> {
    if (result.isError()) {
        System.err.println("错误: " + result.getError().message);
        return;
    }
    
    TdApi.Chats chats = result.get();
    for (long chatId : chats.chatIds) {
        // 处理每个聊天
    }
});
```

### 发送消息

```java
TdApi.FormattedText text = new TdApi.FormattedText(
    "Hello, World!", 
    new TdApi.TextEntity[0]
);

TdApi.InputMessageText content = new TdApi.InputMessageText(
    text, null, false
);

TdApi.SendMessage request = new TdApi.SendMessage();
request.chatId = chatId;
request.inputMessageContent = content;

client.send(request, result -> {
    if (result.isError()) {
        System.err.println("发送失败: " + result.getError().message);
        return;
    }
    
    TdApi.Message message = result.get();
    System.out.println("消息已发送，ID: " + message.id);
});
```

### 监听新消息

在 `TelegramClientManager` 初始化时注册监听器：

```java
// 在 login() 方法中添加
clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, update -> {
    TdApi.Message message = update.message;
    System.out.println("收到新消息: " + message.content);
});
```

## 配置说明

### application.yaml

```yaml
telegram:
  # 设备型号标识
  device-model: "Coco Monya"
  # 登录超时时间（分钟）
  login-timeout-minutes: 2
  # 登录方式：code（验证码登录，默认）、qrcode（二维码登录）、console（控制台输入登录）
  login-type: code
```

### .env 文件

```env
API_ID=12345678
API_HASH=0123456789abcdef0123456789abcdef
TG_PHONE=+8613800138000  # 验证码登录和控制台登录需要，二维码登录不需要
TG_2FA=your_2fa_password  # 可选
```

## 生命周期管理

### 初始化

- 时机: Spring 容器启动后，通过 `@PostConstruct` 自动执行
- 顺序: 在 `TgEnvProperties` 和 `TelegramProperties` 初始化后执行
- 失败处理: 配置验证失败或登录失败会导致应用退出

### 销毁

- 时机: Spring 容器关闭前，通过 `@PreDestroy` 自动执行
- 操作:
  1. 调用 `client.sendClose()` 登出 Telegram
  2. 调用 `clientFactory.close()` 释放资源
- 异常处理: 捕获并记录异常，不影响应用关闭

## 异常处理

### IllegalStateException

当客户端未就绪时调用 `getClient()` 或 `getCurrentUser()` 会抛出此异常：

```java
try {
    SimpleTelegramClient client = telegramClientManager.getClient();
    // 使用客户端
} catch (IllegalStateException e) {
    System.err.println("客户端尚未初始化: " + e.getMessage());
}
```

建议使用 `isReady()` 方法先检查状态。

### RuntimeException

登录失败会抛出 `RuntimeException`，包含详细的错误信息。

## 最佳实践

1. **依赖注入**: 始终通过依赖注入获取 `TelegramClientManager`，不要尝试手动创建实例

2. **状态检查**: 在使用客户端前先调用 `isReady()` 检查状态

3. **异步操作**: Telegram API 调用都是异步的，使用回调或 CompletableFuture 处理结果

4. **错误处理**: 始终检查 `result.isError()` 并处理错误情况

5. **资源管理**: 不需要手动关闭客户端，`@PreDestroy` 会自动处理

6. **线程安全**: 客户端是线程安全的，可以在多个线程中使用

## 注意事项

⚠️ **单例模式**: 整个应用只有一个 Telegram 客户端实例  
⚠️ **阻塞操作**: 登录过程可能需要用户输入验证码，会阻塞应用启动  
⚠️ **会话持久化**: 首次登录后会保存 session，下次启动自动登录  
⚠️ **配置验证**: 配置错误会导致应用无法启动  
⚠️ **网络依赖**: 需要网络连接才能登录和使用

## 故障排查

### 问题: 应用启动时卡住

**原因**: 等待用户输入验证码  
**解决**: 查看控制台提示，输入验证码

### 问题: 配置验证失败

**原因**: `.env` 文件配置错误  
**解决**: 检查 `data/config/.env` 文件，确保配置正确

### 问题: 登录超时

**原因**: 超过配置的超时时间未完成登录  
**解决**: 增加 `telegram.login-timeout-minutes` 配置值

### 问题: 客户端未就绪

**原因**: 登录失败或尚未完成初始化  
**解决**: 检查日志，查看具体错误信息

## 参考资料

- [TDLight 官方文档](https://tdlight-team.github.io/tdlight-docs)
- [Telegram API 文档](https://core.telegram.org/tdlib/docs)
- [项目参考: CocoMonya-MVP](../refers/CocoMonya-MVP)
