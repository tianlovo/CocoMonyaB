# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-03-01

### Fixed

#### 媒体组消息处理问题修复

- **修复媒体组消息重复存储导致的 MongoDB 重复键错误**
  - 问题：媒体组消息可能被多次添加到缓冲区，导致尝试保存已存在的消息时触发 `E11000 duplicate key error`
  - 修复：
    - 在 `ChannelMonitorService.handleMediaGroupMessage()` 中添加重复检查，防止同一消息多次加入缓冲区
    - 在 `ChannelMessageStoragePlugin.handleMediaGroup()` 中过滤掉已存在的消息，只保存新消息
  - 影响文件：
    - `src/main/java/org/xlyo/cocomonyab/service/ChannelMonitorService.java`
    - `src/main/java/org/xlyo/cocomonyab/plugin/channelmessage/ChannelMessageStoragePlugin.java`

- **修复媒体组消息转发时的消息 ID 顺序错误**
  - 问题：媒体组中存在重复的 messageId，导致 Telegram API 报错 "Message identifiers must be in a strictly increasing order"
  - 修复：
    - 在 `ChannelMonitorService.createMediaGroupEntity()` 中对 messageIds 去重并排序
    - 在 `TagBasedMessageForwardingPlugin.doHandle()` 中对收集的 messageIds 去重并排序
  - 影响文件：
    - `src/main/java/org/xlyo/cocomonyab/service/ChannelMonitorService.java`
    - `src/main/java/org/xlyo/cocomonyab/plugin/tagforward/TagBasedMessageForwardingPlugin.java`

#### 系统启动状态显示错误修复 (Spec: startup-status-and-json-export-fix)

- **修复系统启动完成后状态字段显示不一致的问题**
  - 问题：系统成功启动后，`ready: true` 但 `status: NOT_STARTED` 和 `currentPhase: "未启动"`，状态信息自相矛盾
  - 根本原因：`SystemReadyService.checkSystemReadiness()` 方法只更新了 `systemReady` 和 `notReadyReason`，但忘记更新 `currentStatus` 和 `currentPhase` 字段
  - 修复：在 `checkSystemReadiness()` 方法中添加缺失的状态更新，确保所有状态字段保持一致
  - 影响文件：`src/main/java/org/xlyo/cocomonyab/service/SystemReadyService.java`

#### JSON 导出格式问题修复 (Spec: startup-status-and-json-export-fix)

- **修复导出 JSON 数据时包含 Java 类名的问题**
  - 问题：导出作者、原作、角色数据时，生成的 JSON 包含 Java 类型信息（如 `["java.util.ArrayList", [...]]`），导致无法导入
  - 根本原因：全局 `ObjectMapper` 启用了多态类型处理（`activateDefaultTyping`），影响了所有使用该 ObjectMapper 的地方
  - 修复：为导出功能创建独立的 `ObjectMapper` 实例，不启用多态类型处理，生成标准 JSON 格式
  - 影响文件：
    - `src/main/java/org/xlyo/cocomonyab/config/core/JacksonConfiguration.java`
    - `src/main/java/org/xlyo/cocomonyab/service/tag/impl/AuthorServiceImpl.java`
    - `src/main/java/org/xlyo/cocomonyab/service/tag/impl/WorkServiceImpl.java`
    - `src/main/java/org/xlyo/cocomonyab/service/tag/impl/CharacterServiceImpl.java`

### Improved

#### 应用启动流程重构 (Spec: application-startup-flow-refactor)

- **建立清晰的启动阶段划分和依赖管理机制**
  - 实现基于 Spring 事件的启动流程：配置初始化 → 数据库初始化 → 数据库集合准备 → 消息处理插件初始化 → 消息源生成器初始化 → RESTful API 初始化 → 应用准备就绪
  - 添加启动进度跟踪和耗时统计功能
  - 实现优雅的错误处理和资源释放机制
  - 提供健康检查端点（`/actuator/health` 和 `/actuator/startup`）查询系统启动状态

### Technical Details

- **并发安全性增强**：媒体组处理使用分段锁（Striped Lock）机制，减少锁竞争
- **数据完整性保证**：通过去重和排序确保媒体组消息 ID 列表的唯一性和严格递增顺序
- **配置隔离**：导出功能使用独立的 ObjectMapper 配置，避免与 TdApi 序列化配置冲突
- **状态一致性**：确保系统就绪时所有状态字段（ready、status、currentPhase、progress）保持一致

## [1.0.0] - 2026-02-28

### Added

- 初始版本发布
- 基于 TDLight 的 Telegram 频道监控功能
- 消息处理插件系统
- 标签过滤和转发功能
- MongoDB 数据存储
- RESTful API 接口
- 未读消息检测和处理

---

## 版本号说明

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

- **主版本号（MAJOR）**：当你做了不兼容的 API 修改
- **次版本号（MINOR）**：当你做了向下兼容的功能性新增
- **修订号（PATCH）**：当你做了向下兼容的问题修正

格式：`主版本号.次版本号.修订号[-预发布标识]`

例如：`1.0.0`、`1.1.0-beta`、`2.0.0`
