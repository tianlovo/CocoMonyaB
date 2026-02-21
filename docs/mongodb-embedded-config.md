# 嵌入式 MongoDB 配置说明

## 概述

本应用支持嵌入式 MongoDB 模式，可以自动下载、配置和启动 MongoDB 实例，无需手动安装 MongoDB。

## 配置选项

在 `application.yaml` 中配置嵌入式 MongoDB：

```yaml
spring:
  data:
    mongodb:
      # 运行模式：embedded（嵌入式）或 remote（远程）
      mode: embedded
      
      # 嵌入式 MongoDB 配置
      embedded:
        # MongoDB 版本号（默认：7.0.12）
        version: 7.0.12
        
        # MongoDB 监听端口（默认：27017）
        port: 27017
        
        # MongoDB 绑定 IP 地址（默认：127.0.0.1）
        bind-ip: 127.0.0.1
        
        storage:
          # 数据存储目录（默认：data/db/mongo）
          directory: data/db/mongo
```

## 配置说明

### mode
- **类型**: String
- **可选值**: `embedded` 或 `remote`
- **默认值**: `embedded`
- **说明**: 选择 MongoDB 运行模式
  - `embedded`: 使用嵌入式 MongoDB，应用启动时自动下载和启动
  - `remote`: 连接到远程 MongoDB 服务器

### embedded.version
- **类型**: String
- **默认值**: `7.0.12`
- **说明**: 指定要下载的 MongoDB 版本号
- **注意**: 仅支持 Windows x86_64 版本

### embedded.port
- **类型**: Integer
- **默认值**: `27017`
- **说明**: MongoDB 监听的端口号
- **范围**: 1024-65535

### embedded.bind-ip
- **类型**: String
- **默认值**: `127.0.0.1`
- **说明**: MongoDB 绑定的 IP 地址
- **常用值**:
  - `127.0.0.1`: 仅本地访问（推荐）
  - `0.0.0.0`: 允许所有网络接口访问（不推荐用于生产环境）

### embedded.storage.directory
- **类型**: String
- **默认值**: `data/db/mongo`
- **说明**: MongoDB 数据文件存储目录
- **注意**: 
  - 目录会自动创建
  - 确保应用有写入权限
  - 数据会持久化保存

## 目录结构

嵌入式 MongoDB 使用以下目录结构：

```
project-root/
├── data/
│   ├── bin/
│   │   └── mongo/          # MongoDB 二进制文件
│   │       └── bin/
│   │           ├── mongod.exe
│   │           └── ...
│   ├── db/
│   │   └── mongo/          # MongoDB 数据文件（可配置）
│   └── tmp/                # 临时下载目录（下载后自动清理）
```

## 启动流程

1. **检查二进制文件**: 检查 `data/bin/mongo/bin/mongod.exe` 是否存在
2. **下载 MongoDB**: 如果不存在，从官网下载指定版本
   - 显示实时下载进度条
   - 下载到 `data/tmp` 目录
3. **解压文件**: 解压 MongoDB 压缩包
   - 显示实时解压进度条
   - 仅解压 bin 目录下的文件
4. **清理临时文件**: 删除下载的压缩包
5. **创建数据目录**: 创建配置的数据存储目录
6. **启动 MongoDB**: 使用配置的参数启动 mongod 进程
7. **等待就绪**: 通过端口连接测试，确认 MongoDB 已启动
8. **日志输出**: 
   - 进度条仅在控制台显示，不记录到日志文件
   - 完成/失败信息记录到日志文件

## 生命周期管理

### 启动
- 应用启动时自动启动 MongoDB（`@PostConstruct`）
- 添加 JVM 关闭钩子，确保异常退出时也能清理进程

### 关闭
- 应用关闭时自动停止 MongoDB（`@PreDestroy`）
- 优雅关闭：先发送 SIGTERM，等待 10 秒
- 强制关闭：如果 10 秒后仍未退出，强制终止进程
- 资源释放：清理进程句柄和相关资源

## 日志说明

### 控制台输出
- 下载进度条：`下载: [=====>    ] 50% (25.5/51.0 MB)`
- 解压进度条：`解压: [=========>] 90% (450/500)`

### 日志文件记录
- MongoDB 启动信息
- 配置参数
- 启动耗时
- 错误信息
- 关闭信息

### MongoDB 进程日志
- MongoDB 进程的标准输出以 DEBUG 级别记录
- 日志前缀：`MongoDB: `

## 常见问题

### Q: 首次启动很慢？
A: 首次启动需要下载 MongoDB（约 50-100 MB），后续启动会直接使用已下载的版本。

### Q: 如何更换 MongoDB 版本？
A: 修改 `embedded.version` 配置，删除 `data/bin/mongo` 目录，重启应用。

### Q: 数据会丢失吗？
A: 不会。数据存储在配置的目录中，应用重启后数据仍然存在。

### Q: 如何清理数据？
A: 删除配置的数据存储目录（默认 `data/db/mongo`）。

### Q: 可以在生产环境使用吗？
A: 嵌入式模式主要用于开发和测试。生产环境建议使用 `remote` 模式连接到独立的 MongoDB 服务器。

### Q: 支持其他操作系统吗？
A: 当前仅支持 Windows。Linux 和 macOS 支持需要修改下载 URL 和可执行文件名。

## 切换到远程模式

如果需要连接到远程 MongoDB 服务器：

```yaml
spring:
  data:
    mongodb:
      mode: remote
      uri: mongodb://username:password@host:port/database
```

## 性能建议

1. **开发环境**: 使用嵌入式模式，方便快捷
2. **测试环境**: 可以使用嵌入式模式或远程模式
3. **生产环境**: 强烈建议使用远程模式，连接到专业的 MongoDB 集群

## 安全建议

1. 嵌入式模式默认绑定到 `127.0.0.1`，仅本地访问
2. 不要在生产环境暴露嵌入式 MongoDB 到公网
3. 定期备份数据存储目录
4. 使用远程模式时，配置强密码和访问控制
