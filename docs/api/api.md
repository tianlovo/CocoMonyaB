# API 接口文档

## 概述

本文档提供了系统所有 API 接口的详细说明，包括请求参数、响应格式和使用示例。

所有 API 接口遵循统一的响应格式规范，详见 [API 响应规范文档](./API%20响应规范文档.md)。

## 目录

- [1. 频道管理 API](#1-频道管理-api)
  - [1.1 数据结构](#11-数据结构)
  - [1.2 API 端点](#12-api-端点)
  - [1.3 常见错误场景](#13-常见错误场景)
  - [1.4 使用示例](#14-使用示例)
  - [1.5 注意事项](#15-注意事项)
- [2. Telegram 频道查询 API](#2-telegram-频道查询-api)
  - [2.1 概述](#21-概述)
  - [2.2 数据结构](#22-数据结构)
  - [2.3 API 端点](#23-api-端点)
  - [2.4 使用示例](#24-使用示例)
  - [2.5 注意事项](#25-注意事项)
- [3. 消息查询 API](#3-消息查询-api)
  - [3.1 概述](#31-概述)
  - [3.2 数据结构](#32-数据结构)
  - [3.3 API 端点](#33-api-端点)
  - [3.4 常见错误场景](#34-常见错误场景)
  - [3.5 使用示例](#35-使用示例)
  - [3.6 注意事项](#36-注意事项)
- [4. 作者库 API](#4-作者库-api)
  - [4.1 概述](#41-概述)
  - [4.2 数据结构](#42-数据结构)
  - [4.3 API 端点](#43-api-端点)
  - [4.4 常见错误场景](#44-常见错误场景)
  - [4.5 使用示例](#45-使用示例)
  - [4.6 注意事项](#46-注意事项)
- [5. 原作库 API](#5-原作库-api)
  - [5.1 概述](#51-概述)
  - [5.2 数据结构](#52-数据结构)
  - [5.3 API 端点](#53-api-端点)
  - [5.4 常见错误场景](#54-常见错误场景)
  - [5.5 使用示例](#55-使用示例)
  - [5.6 注意事项](#56-注意事项)
- [6. 角色库 API](#6-角色库-api)
  - [6.1 概述](#61-概述)
  - [6.2 数据结构](#62-数据结构)
  - [6.3 API 端点](#63-api-端点)
  - [6.4 常见错误场景](#64-常见错误场景)
  - [6.5 使用示例](#65-使用示例)
  - [6.6 注意事项](#66-注意事项)
- [7. 标签过滤配置 API](#7-标签过滤配置-api)
  - [7.1 概述](#71-概述)
  - [7.2 数据结构](#72-数据结构)
  - [7.3 API 端点](#73-api-端点)
  - [7.4 常见错误场景](#74-常见错误场景)
  - [7.5 使用示例](#75-使用示例)
  - [7.6 注意事项](#76-注意事项)

---

## 1. 频道管理 API

### 1.1 概述

频道管理 API 提供了对 Telegram 频道监控配置的完整 CRUD 操作，包括创建、更新、删除、查询和分页列表功能。

### 1.2 数据结构

#### 1.2.1 ChannelCreateDTO（创建频道请求）

用于创建新的频道监控配置。

```json
{
  "channelId": 1234567890,
  "channelUsername": "example_channel",
  "channelTitle": "示例频道标题",
  "monitoringStatus": true
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| channelId | Long | 是 | 不能为空 | Telegram 频道 ID |
| channelUsername | String | 否 | 长度不超过100 | 频道用户名（可为空） |
| channelTitle | String | 是 | 不能为空，长度 1-200 | 频道标题 |
| monitoringStatus | Boolean | 否 | - | 监控状态，默认为 true |

#### 1.2.2 ChannelUpdateDTO（更新频道请求）

用于更新现有频道配置，所有字段均为可选。

```json
{
  "channelUsername": "updated_channel",
  "channelTitle": "更新后的频道标题",
  "monitoringStatus": false
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| channelUsername | String | 否 | 长度不超过100 | 频道用户名 |
| channelTitle | String | 否 | 长度 1-200 | 频道标题 |
| monitoringStatus | Boolean | 否 | - | 监控状态 |

#### 1.2.3 ChannelQueryDTO（查询频道请求）

用于分页查询时的过滤条件，所有字段均为可选。

```json
{
  "channelUsername": "example",
  "monitoringStatus": true
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| channelUsername | String | 否 | 频道用户名（部分匹配） |
| monitoringStatus | Boolean | 否 | 监控状态（精确匹配） |

#### 1.2.4 ChannelVO（频道响应对象）

返回给客户端的频道数据。

```json
{
  "id": "65f8a1b2c3d4e5f6a7b8c9d0",
  "channelId": 1234567890,
  "channelUsername": "example_channel",
  "channelTitle": "示例频道标题",
  "monitoringStatus": true,
  "createTime": "2024-03-20T10:30:00",
  "updateTime": "2024-03-20T10:30:00"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | MongoDB 文档 ID |
| channelId | Long | Telegram 频道 ID |
| channelUsername | String | 频道用户名 |
| channelTitle | String | 频道标题 |
| monitoringStatus | Boolean | 监控状态 |
| createTime | String | 创建时间（ISO 8601 格式） |
| updateTime | String | 更新时间（ISO 8601 格式） |

### 1.3 API 端点

#### 1.3.1 创建频道

**接口地址：** `POST /api/channel`

**请求示例：**

```json
{
  "channelId": 1234567890,
  "channelUsername": "tech_news",
  "channelTitle": "科技新闻频道",
  "monitoringStatus": true
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "channelId": 1234567890,
    "channelUsername": "tech_news",
    "channelTitle": "科技新闻频道",
    "monitoringStatus": true,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

频道 ID 已存在：
```json
{
  "code": -60003,
  "msg": "频道ID已存在: 1234567890",
  "data": null
}
```

参数校验失败：
```json
{
  "code": -40006,
  "msg": "频道ID不能为空; 频道用户名不能为空; 频道标题长度必须在1-200之间",
  "data": null
}
```

#### 1.3.2 更新频道

**接口地址：** `PUT /api/channel/{id}`

**路径参数：**
- `id`: 频道的 MongoDB 文档 ID

**请求示例：**

```json
{
  "channelTitle": "更新后的科技新闻频道",
  "monitoringStatus": false
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "channelId": 1234567890,
    "channelUsername": "tech_news",
    "channelTitle": "更新后的科技新闻频道",
    "monitoringStatus": false,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T15:45:00"
  }
}
```

**错误响应示例：**

频道不存在：
```json
{
  "code": -60002,
  "msg": "频道不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

参数校验失败：
```json
{
  "code": -40006,
  "msg": "频道用户名长度必须在1-100之间",
  "data": null
}
```

#### 1.3.3 删除频道

**接口地址：** `DELETE /api/channel/{id}`

**路径参数：**
- `id`: 频道的 MongoDB 文档 ID

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

**错误响应示例：**

频道不存在：
```json
{
  "code": -60002,
  "msg": "频道不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 1.3.4 查询单个频道

**接口地址：** `GET /api/channel/{id}`

**路径参数：**
- `id`: 频道的 MongoDB 文档 ID

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "channelId": 1234567890,
    "channelUsername": "tech_news",
    "channelTitle": "科技新闻频道",
    "monitoringStatus": true,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

频道不存在：
```json
{
  "code": -60002,
  "msg": "频道不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 1.3.5 查询所有频道

**接口地址：** `GET /api/channel/list`

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": "65f8a1b2c3d4e5f6a7b8c9d0",
      "channelId": 1234567890,
      "channelUsername": "tech_news",
      "channelTitle": "科技新闻频道",
      "monitoringStatus": true,
      "createTime": "2024-03-20T10:30:00",
      "updateTime": "2024-03-20T10:30:00"
    },
    {
      "id": "65f8a1b2c3d4e5f6a7b8c9d1",
      "channelId": 9876543210,
      "channelUsername": "sports_updates",
      "channelTitle": "体育更新频道",
      "monitoringStatus": false,
      "createTime": "2024-03-20T11:00:00",
      "updateTime": "2024-03-20T11:00:00"
    }
  ]
}
```

**空列表响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": []
}
```

#### 1.3.6 分页查询频道

**接口地址：** `GET /api/channel/page`

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页大小 |
| channelUsername | String | 否 | - | 频道用户名（部分匹配） |
| monitoringStatus | Boolean | 否 | - | 监控状态（精确匹配） |

**请求示例：**

```
GET /api/channel/page?current=1&size=10&channelUsername=news&monitoringStatus=true
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d0",
        "channelId": 1234567890,
        "channelUsername": "tech_news",
        "channelTitle": "科技新闻频道",
        "monitoringStatus": true,
        "createTime": "2024-03-20T10:30:00",
        "updateTime": "2024-03-20T10:30:00"
      },
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d2",
        "channelId": 5555555555,
        "channelUsername": "world_news",
        "channelTitle": "世界新闻频道",
        "monitoringStatus": true,
        "createTime": "2024-03-20T12:00:00",
        "updateTime": "2024-03-20T12:00:00"
      }
    ],
    "current": 1,
    "size": 10,
    "total": 2,
    "pages": 1
  }
}
```

**空页响应（超出范围）：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 5,
    "size": 10,
    "total": 15,
    "pages": 2
  }
}
```

### 1.4 常见错误场景

#### 1.4.1 数据不存在（DATA_NOT_FOUND）

**响应码：** `-60002`

**触发场景：**
- 查询不存在的频道 ID
- 更新不存在的频道
- 删除不存在的频道

**响应示例：**

```json
{
  "code": -60002,
  "msg": "频道不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 1.4.2 数据已存在（DATA_ALREADY_EXISTS）

**响应码：** `-60003`

**触发场景：**
- 创建频道时，channelId 已存在于数据库中

**响应示例：**

```json
{
  "code": -60003,
  "msg": "频道ID已存在: 1234567890",
  "data": null
}
```

#### 1.4.3 参数校验失败（VALIDATION_ERROR）

**响应码：** `-40006`

**触发场景：**
- channelId 为空
- channelUsername 为空或长度超出范围（1-100）
- channelTitle 为空或长度超出范围（1-200）

**响应示例（单个错误）：**

```json
{
  "code": -40006,
  "msg": "频道ID不能为空",
  "data": null
}
```

**响应示例（多个错误）：**

```json
{
  "code": -40006,
  "msg": "频道ID不能为空; 频道用户名不能为空; 频道标题长度必须在1-200之间",
  "data": null
}
```

### 1.5 使用示例

#### 1.5.1 创建并查询频道

```bash
# 1. 创建频道
curl -X POST http://localhost:8080/api/channel \
  -H "Content-Type: application/json" \
  -d '{
    "channelId": 1234567890,
    "channelUsername": "tech_news",
    "channelTitle": "科技新闻频道",
    "monitoringStatus": true
  }'

# 响应：获取到 id 为 "65f8a1b2c3d4e5f6a7b8c9d0"

# 2. 查询该频道
curl -X GET http://localhost:8080/api/channel/65f8a1b2c3d4e5f6a7b8c9d0
```

#### 1.5.2 更新频道状态

```bash
# 停止监控某个频道
curl -X PUT http://localhost:8080/api/channel/65f8a1b2c3d4e5f6a7b8c9d0 \
  -H "Content-Type: application/json" \
  -d '{
    "monitoringStatus": false
  }'
```

#### 1.5.3 分页查询活跃频道

```bash
# 查询所有正在监控的频道，第1页，每页20条
curl -X GET "http://localhost:8080/api/channel/page?current=1&size=20&monitoringStatus=true"
```

#### 1.5.4 搜索频道

```bash
# 搜索用户名包含 "news" 的频道
curl -X GET "http://localhost:8080/api/channel/page?channelUsername=news"
```

### 1.6 注意事项

1. **频道 ID 唯一性**：每个 channelId 在系统中必须唯一，重复创建会返回 DATA_ALREADY_EXISTS 错误
2. **部分更新**：使用 PUT 更新时，只需提供需要修改的字段，未提供的字段保持不变
3. **时间戳自动管理**：createTime 在创建时自动设置，updateTime 在每次更新时自动更新
4. **分页查询**：current 从 1 开始，超出范围时返回空 records 列表但保持正确的分页元数据
5. **过滤条件**：channelUsername 使用部分匹配（包含），monitoringStatus 使用精确匹配
6. **HTTP 状态码**：所有响应的 HTTP 状态码均为 200，具体错误通过响应体中的 code 字段判断

---

## 2. Telegram 频道查询 API

### 2.1 概述

Telegram 频道查询 API 提供了查询当前已登录 Telegram 账号的频道列表功能。该 API 直接从 TDLib 获取实时数据，返回账号已加入或管理的所有频道信息。

### 2.2 数据结构

#### 2.2.1 TgChannelVO（TG频道响应对象）

返回给客户端的 Telegram 频道数据。

```json
{
  "chatId": 1234567890,
  "title": "科技新闻频道",
  "username": "tech_news",
  "type": "channel",
  "isChannel": true,
  "memberCount": 5000,
  "description": null
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| chatId | Long | 聊天ID（频道ID） |
| title | String | 频道标题 |
| username | String | 频道用户名（不含@符号） |
| type | String | 频道类型（固定为"channel"） |
| isChannel | Boolean | 是否为频道（true=频道，false=超级群组） |
| memberCount | Integer | 成员数量 |
| description | String | 频道描述（当前版本为null） |

### 2.3 API 端点

#### 2.3.1 分页查询已登录账号的频道列表

**接口地址：** `GET /api/channel/tg/logged-in`

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页大小 |

**请求示例：**

```
GET /api/channel/tg/logged-in?current=1&size=10
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "chatId": 1234567890,
        "title": "科技新闻频道",
        "username": "tech_news",
        "type": "channel",
        "isChannel": true,
        "memberCount": 5000,
        "description": null
      },
      {
        "chatId": 9876543210,
        "title": "体育更新频道",
        "username": "sports_updates",
        "type": "channel",
        "isChannel": true,
        "memberCount": 3000,
        "description": null
      }
    ],
    "current": 1,
    "size": 10,
    "total": 2,
    "pages": 1
  }
}
```

**空列表响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 1,
    "size": 10,
    "total": 0,
    "pages": 0
  }
}
```

**错误响应示例：**

Telegram客户端未就绪：
```json
{
  "code": -60001,
  "msg": "Telegram客户端未就绪",
  "data": null
}
```

参数校验失败：
```json
{
  "code": -40000,
  "msg": "页码必须大于等于1",
  "data": null
}
```

获取频道列表失败：
```json
{
  "code": -60001,
  "msg": "获取Telegram频道列表失败: Connection timeout",
  "data": null
}
```

### 2.4 使用示例

#### 2.4.1 查询第一页频道

```bash
# 查询前10个频道
curl -X GET "http://localhost:8080/api/channel/tg/logged-in?current=1&size=10"
```

#### 2.4.2 查询更多频道

```bash
# 查询第2页，每页20条
curl -X GET "http://localhost:8080/api/channel/tg/logged-in?current=2&size=20"
```

#### 2.4.3 获取所有频道

```bash
# 设置较大的size值获取所有频道（最大100）
curl -X GET "http://localhost:8080/api/channel/tg/logged-in?current=1&size=100"
```

### 2.5 注意事项

1. **实时数据**：该接口直接从 TDLib 获取实时数据，不依赖数据库
2. **登录状态**：必须确保 Telegram 客户端已成功登录，否则会返回错误
3. **性能考虑**：首次调用时会加载聊天列表，可能需要几秒钟时间
4. **分页限制**：每页最大支持 100 条记录
5. **频道筛选**：只返回频道（channel），不包括超级群组（supergroup）
6. **用户名可能为空**：某些频道可能没有设置用户名，此时 username 字段为 null
7. **描述字段**：当前版本的 description 字段始终为 null，获取描述需要额外的 API 调用
8. **超时设置**：API 调用设置了 30 秒超时，如果网络较慢可能会超时

### 2.6 与频道管理 API 的区别

| 特性 | 频道管理 API | Telegram 频道查询 API |
|------|-------------|---------------------|
| 数据来源 | MongoDB 数据库 | TDLib 实时数据 |
| 数据内容 | 监控配置信息 | Telegram 账号的频道列表 |
| 是否需要登录 | 否 | 是（需要 TG 客户端登录） |
| 支持 CRUD | 是 | 否（只读） |
| 用途 | 管理监控配置 | 查看账号已加入的频道 |

---

## 3. 消息查询 API

### 3.1 概述

消息查询 API 提供了对持久化到 MongoDB 数据库的 Telegram 消息的查询接口。系统已经将 Telegram 频道的消息持久化到 MongoDB 的 raw_messages 集合中，该 API 支持多种查询条件、分页和排序功能，方便用户高效检索和分析消息数据。

主要功能包括：
- 根据 MongoDB ID 或 Telegram ID 查询单条消息
- 支持多条件过滤的分页查询（频道、日期范围）
- 查询媒体组（相册）消息

### 3.2 数据结构

#### 3.2.1 MessageQueryDTO（消息查询请求）

用于分页查询时的过滤条件，所有字段均为可选。

```json
{
  "chatId": -1001234567890,
  "startDate": 1708588800,
  "endDate": 1708675200,
  "mediaAlbumId": 789012
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| chatId | Long | 否 | - | Telegram 频道 ID（负数） |
| startDate | Integer | 否 | ≥ 0 | 开始日期（Unix 时间戳） |
| endDate | Integer | 否 | ≥ 0 | 结束日期（Unix 时间戳） |
| mediaAlbumId | Long | 否 | - | 媒体组 ID（当前分页查询不支持此过滤条件） |

**校验规则：**
- startDate 和 endDate 必须大于等于 0
- 如果同时提供 startDate 和 endDate，系统会查询该时间范围内的消息
- 分页查询支持的过滤条件：chatId、startDate、endDate
- mediaAlbumId 字段存在于 DTO 中，但当前分页查询实现不使用此字段进行过滤

#### 3.2.2 MessageVO（消息响应对象）

返回给客户端的消息数据。

```json
{
  "id": "65f8a1b2c3d4e5f6a7b8c9d0",
  "chatId": -1001234567890,
  "messageId": 123456,
  "mediaAlbumId": 789012,
  "date": 1708588800,
  "rawJson": "{\"@type\":\"message\",\"id\":123456,...}",
  "createTime": "2024-03-20T10:30:00",
  "updateTime": "2024-03-20T10:30:00"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | MongoDB 文档 ID（24位十六进制） |
| chatId | Long | Telegram 频道 ID（负数） |
| messageId | Long | Telegram 消息 ID |
| mediaAlbumId | Long | 媒体组 ID（可能为 null） |
| date | Integer | 消息日期（Unix 时间戳） |
| rawJson | String | TDLib 原始消息 JSON |
| createTime | String | 创建时间（ISO 8601 格式） |
| updateTime | String | 更新时间（ISO 8601 格式） |

### 3.3 API 端点

#### 3.3.1 根据 MongoDB ID 查询单条消息

**接口地址：** `GET /api/message/{id}`

**路径参数：**
- `id`: MongoDB 文档 ID（String，24位十六进制）

**请求示例：**

```
GET /api/message/65f8a1b2c3d4e5f6a7b8c9d0
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "chatId": -1001234567890,
    "messageId": 123456,
    "mediaAlbumId": 789012,
    "date": 1708588800,
    "rawJson": "{\"@type\":\"message\",\"id\":123456,\"chat_id\":-1001234567890,...}",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

消息不存在：
```json
{
  "code": -60002,
  "msg": "消息不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

无效的 MongoDB ID 格式：
```json
{
  "code": -40006,
  "msg": "无效的MongoDB ID格式: invalid_id",
  "data": null
}
```

#### 3.3.2 根据 ChatId 和 MessageId 查询消息

**接口地址：** `GET /api/message/by-tg-id`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chatId | Long | 是 | Telegram 频道 ID |
| messageId | Long | 是 | Telegram 消息 ID |

**请求示例：**

```
GET /api/message/by-tg-id?chatId=-1001234567890&messageId=123456
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "chatId": -1001234567890,
    "messageId": 123456,
    "mediaAlbumId": null,
    "date": 1708588800,
    "rawJson": "{\"@type\":\"message\",...}",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

消息不存在：
```json
{
  "code": -60002,
  "msg": "消息不存在: chatId=-1001234567890, messageId=123456",
  "data": null
}
```

缺少必需参数：
```json
{
  "code": -40006,
  "msg": "chatId不能为空; messageId不能为空",
  "data": null
}
```

#### 3.3.3 分页查询消息列表

**接口地址：** `GET /api/message/page`

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码（≥ 1） |
| size | Long | 否 | 10 | 每页大小（1-100） |
| chatId | Long | 否 | - | 频道 ID 过滤 |
| startDate | Integer | 否 | - | 开始日期（Unix 时间戳） |
| endDate | Integer | 否 | - | 结束日期（Unix 时间戳） |

**注意：** MessageQueryDTO 中包含 mediaAlbumId 字段，但当前分页查询实现不支持按媒体组过滤。如需查询媒体组消息，请使用专用端点 `/api/message/media-album`。

**请求示例：**

```
GET /api/message/page?current=1&size=20&chatId=-1001234567890&startDate=1708588800&endDate=1708675200
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d0",
        "chatId": -1001234567890,
        "messageId": 123456,
        "mediaAlbumId": null,
        "date": 1708675000,
        "rawJson": "{...}",
        "createTime": "2024-03-20T10:30:00",
        "updateTime": "2024-03-20T10:30:00"
      },
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d1",
        "chatId": -1001234567890,
        "messageId": 123455,
        "mediaAlbumId": null,
        "date": 1708674000,
        "rawJson": "{...}",
        "createTime": "2024-03-20T10:25:00",
        "updateTime": "2024-03-20T10:25:00"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 100,
    "pages": 5
  }
}
```

**空页响应（超出范围）：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 10,
    "size": 20,
    "total": 100,
    "pages": 5
  }
}
```

**错误响应示例：**

无效的分页参数：
```json
{
  "code": -40006,
  "msg": "页码必须大于等于1; 每页大小必须大于等于1",
  "data": null
}
```

分页大小超出限制：
```json
{
  "code": -40000,
  "msg": "每页大小不能超过100",
  "data": null
}
```

#### 3.3.4 查询媒体组消息

**接口地址：** `GET /api/message/media-album`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chatId | Long | 是 | Telegram 频道 ID |
| mediaAlbumId | Long | 是 | 媒体组 ID |

**请求示例：**

```
GET /api/message/media-album?chatId=-1001234567890&mediaAlbumId=789012
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": "65f8a1b2c3d4e5f6a7b8c9d0",
      "chatId": -1001234567890,
      "messageId": 123456,
      "mediaAlbumId": 789012,
      "date": 1708588800,
      "rawJson": "{...}",
      "createTime": "2024-03-20T10:30:00",
      "updateTime": "2024-03-20T10:30:00"
    },
    {
      "id": "65f8a1b2c3d4e5f6a7b8c9d1",
      "chatId": -1001234567890,
      "messageId": 123457,
      "mediaAlbumId": 789012,
      "date": 1708588800,
      "rawJson": "{...}",
      "createTime": "2024-03-20T10:30:00",
      "updateTime": "2024-03-20T10:30:00"
    }
  ]
}
```

**空列表响应（媒体组不存在）：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": []
}
```

**错误响应示例：**

缺少必需参数：
```json
{
  "code": -40006,
  "msg": "chatId不能为空; mediaAlbumId不能为空",
  "data": null
}
```

### 3.4 常见错误场景

#### 3.4.1 数据不存在（DATA_NOT_FOUND）

**响应码：** `-60002`

**触发场景：**
- 查询不存在的 MongoDB ID
- 查询不存在的 ChatId + MessageId 组合

**响应示例：**

根据 MongoDB ID 查询：
```json
{
  "code": -60002,
  "msg": "消息不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

根据 Telegram ID 查询：
```json
{
  "code": -60002,
  "msg": "消息不存在: chatId=-1001234567890, messageId=123456",
  "data": null
}
```

#### 3.4.2 参数校验失败（VALIDATION_ERROR）

**响应码：** `-40006`

**触发场景：**
- MongoDB ID 格式无效（不是24位十六进制）
- 缺少必需参数（chatId、messageId、mediaAlbumId）
- 分页参数无效（current < 1 或 size < 1）
- 日期参数无效（startDate < 0 或 endDate < 0）

**响应示例（单个错误）：**

```json
{
  "code": -40006,
  "msg": "无效的MongoDB ID格式: invalid_id",
  "data": null
}
```

**响应示例（多个错误）：**

```json
{
  "code": -40006,
  "msg": "chatId不能为空; messageId不能为空",
  "data": null
}
```

**响应示例（日期校验）：**

```json
{
  "code": -40006,
  "msg": "开始日期必须大于等于0; 结束日期必须大于等于0",
  "data": null
}
```

#### 3.4.3 参数超出范围（PARAM_ERROR）

**响应码：** `-40000`

**触发场景：**
- 分页大小超过最大限制（size > 100）

**响应示例：**

```json
{
  "code": -40000,
  "msg": "每页大小不能超过100",
  "data": null
}
```

#### 3.4.4 系统内部错误（INTERNAL_ERROR）

**响应码：** `-50000`

**触发场景：**
- 数据库连接失败
- 数据库操作异常
- 其他未预期的系统错误

**响应示例：**

```json
{
  "code": -50000,
  "msg": "系统内部错误，请联系管理员",
  "data": null
}
```

### 3.5 使用示例

#### 3.5.1 查询单条消息

```bash
# 根据 MongoDB ID 查询
curl -X GET http://localhost:8080/api/message/65f8a1b2c3d4e5f6a7b8c9d0

# 根据 Telegram ID 查询
curl -X GET "http://localhost:8080/api/message/by-tg-id?chatId=-1001234567890&messageId=123456"
```

#### 3.5.2 分页查询所有消息

```bash
# 查询第1页，每页10条（使用默认参数）
curl -X GET "http://localhost:8080/api/message/page"

# 查询第2页，每页20条
curl -X GET "http://localhost:8080/api/message/page?current=2&size=20"
```

#### 3.5.3 按频道查询消息

```bash
# 查询指定频道的所有消息
curl -X GET "http://localhost:8080/api/message/page?chatId=-1001234567890"

# 查询指定频道的第1页，每页50条
curl -X GET "http://localhost:8080/api/message/page?chatId=-1001234567890&current=1&size=50"
```

#### 3.5.4 按日期范围查询消息

```bash
# 查询2024年2月22日到2月23日的消息
# startDate: 1708588800 (2024-02-22 00:00:00 UTC)
# endDate: 1708675200 (2024-02-23 00:00:00 UTC)
curl -X GET "http://localhost:8080/api/message/page?startDate=1708588800&endDate=1708675200"
```

#### 3.5.5 组合条件查询

```bash
# 查询指定频道在指定日期范围内的消息
curl -X GET "http://localhost:8080/api/message/page?chatId=-1001234567890&startDate=1708588800&endDate=1708675200&current=1&size=20"
```

#### 3.5.6 查询媒体组消息

```bash
# 查询指定频道的指定媒体组（相册）
curl -X GET "http://localhost:8080/api/message/media-album?chatId=-1001234567890&mediaAlbumId=789012"
```

### 3.6 注意事项

#### 3.6.1 分页查询

1. **页码从 1 开始**：current 参数从 1 开始，不是从 0 开始
2. **超出范围处理**：当请求的页码超出范围时，返回空 records 列表，但保持正确的分页元数据（total、pages）
3. **分页大小限制**：每页最大支持 100 条记录，超过会返回参数错误
4. **默认值**：如果不提供分页参数，默认 current=1, size=10

#### 3.6.2 过滤条件

1. **过滤条件组合**：过滤条件（chatId、startDate、endDate）可以任意组合使用
2. **日期范围查询**：startDate 和 endDate 都是 Unix 时间戳（秒），可以只提供其中一个
3. **频道 ID 格式**：Telegram 频道 ID 通常是负数（如 -1001234567890）
4. **媒体组查询**：查询媒体组时必须使用专用端点 `/api/message/media-album`，需要同时提供 chatId 和 mediaAlbumId

#### 3.6.3 排序规则

1. **分页查询排序**：消息列表按 date 字段降序排列（最新消息在前）
2. **媒体组排序**：媒体组内的消息按 messageId 升序排列（保持相册顺序）

#### 3.6.4 性能优化

1. **索引利用**：系统使用 MongoDB 索引优化查询性能
   - chat_message_unique 索引：用于根据 ChatId + MessageId 查询
   - chat_album_unique 索引：用于查询媒体组
   - chat_date_idx 索引：用于按频道和日期查询
2. **避免大结果集**：建议使用分页查询，避免一次性获取大量数据
3. **合理设置分页大小**：根据实际需求设置 size 参数，不要盲目设置为最大值
4. **rawJson 字段**：该字段可能包含大量数据，如果不需要可以考虑在客户端过滤

#### 3.6.5 错误处理

1. **HTTP 状态码**：所有响应的 HTTP 状态码均为 200，具体错误通过响应体中的 code 字段判断
2. **错误信息**：错误响应包含详细的错误信息，帮助定位问题
3. **参数校验**：所有参数都经过严格校验，无效参数会返回明确的错误信息
4. **日志记录**：所有错误都会记录到系统日志，但不会在响应中暴露敏感信息

#### 3.6.6 数据一致性

1. **实时性**：查询结果反映数据库的当前状态
2. **媒体组完整性**：媒体组查询返回该组的所有消息

#### 3.6.7 MongoDB ID 格式

1. **ID 格式**：MongoDB ID 是 24 位十六进制字符串（如 65f8a1b2c3d4e5f6a7b8c9d0）
2. **格式校验**：系统会自动校验 ID 格式，无效格式会返回 VALIDATION_ERROR
3. **大小写**：MongoDB ID 不区分大小写，但建议使用小写

---

## 标签数据库系统概述

标签数据库系统包含三个独立的数据库：作者库、原作库和角色库。这三个数据库共享以下核心特性：

### 全局唯一性约束

所有标签实体（作者、原作、角色）的名称和别名在全局范围内必须唯一，即：
- 作者名称不能与任何其他作者、原作、角色的名称或别名重复
- 原作名称不能与任何其他作者、原作、角色的名称或别名重复
- 角色名称不能与任何其他作者、原作、角色的名称或别名重复
- 所有别名在全局范围内唯一，不能与任何实体的名称或其他别名重复

### 数据库约束总结

#### 作者库约束
- **作者名称**：必填，全局唯一
- **作者别名列表**：必填但可为空数组，每个别名全局唯一
- **个性签名**：可为null
- **网址列表**：可为空数组
- **头像（BASE64）**：可为null
- **备注**：可为null

#### 原作库约束
- **原作名称**：必填，全局唯一
- **原作别名列表**：必填但可为空数组，每个别名全局唯一
- **网址列表**：可为空数组
- **头像（BASE64）**：可为null
- **备注**：可为null

#### 角色库约束
- **角色名称**：必填，全局唯一
- **角色别名列表**：必填但可为空数组，每个别名全局唯一
- **所属原作**：可为null，如果提供则必须在原作库中存在
- **种族**：必填
- **头像（BASE64）**：可为null
- **备注**：可为null

### 引用完整性

- 角色可以引用原作（通过workId），如果提供了workId，系统会验证该原作是否存在
- 删除作者、原作或角色时，系统会检查是否被其他实体或配置引用
- 支持强制删除选项，自动清理所有引用关系

---

## 4. 作者库 API

### 4.1 概述

作者库 API 提供了对作者信息的完整 CRUD 操作，包括创建、更新、删除、查询和分页列表功能。作者库支持作者名称、别名列表、个性签名、网址列表、头像和备注信息的管理。

主要功能包括：
- 作者的创建、更新、删除操作
- 支持多种查询方式（ID、名称、别名）
- 分页查询和关键词搜索
- 数据导入导出（JSON格式）
- 全局唯一性约束验证
- 引用完整性检查

### 4.2 数据结构

#### 4.2.1 AuthorCreateDTO（创建作者请求）

用于创建新的作者。

```json
{
  "name": "张三",
  "aliases": ["作者别名1", "作者别名2"],
  "signature": "这是作者的个性签名",
  "urls": ["https://example.com/author1", "https://twitter.com/author1"],
  "avatarBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
  "remark": "这是备注信息"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 是 | 不能为空，长度不超过100 | 作者名称（全局唯一） |
| aliases | List\<String\> | 是 | 不能为null，每个别名长度不超过100 | 别名列表（可以为空数组，每个别名全局唯一） |
| signature | String | 否 | 长度不超过500 | 个性签名（可为null） |
| urls | List\<String\> | 否 | 每个URL长度不超过500 | 网址列表（可为空数组） |
| avatarBase64 | String | 否 | - | BASE64编码的头像（可为null） |
| remark | String | 否 | 长度不超过1000 | 备注信息（可为null） |

**数据库约束：**
- **name**: 必填，全局唯一（跨作者库、原作库、角色库）
- **aliases**: 必填但可为空数组，每个别名全局唯一（跨作者库、原作库、角色库）
- **signature**: 可为null
- **urls**: 可为空数组
- **avatarBase64**: 可为null
- **remark**: 可为null

#### 4.2.2 AuthorUpdateDTO（更新作者请求）

用于更新现有作者，所有字段均为可选。

```json
{
  "name": "张三（更新后）",
  "aliases": ["新别名1", "新别名2"],
  "signature": "更新后的个性签名"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 否 | 长度不超过100 | 作者名称 |
| aliases | List\<String\> | 否 | 每个别名长度不超过100 | 别名列表 |
| signature | String | 否 | 长度不超过500 | 个性签名 |
| urls | List\<String\> | 否 | 每个URL长度不超过500 | 网址列表 |
| avatarBase64 | String | 否 | - | BASE64编码的头像 |
| remark | String | 否 | 长度不超过1000 | 备注信息 |

#### 4.2.3 AuthorQueryDTO（查询作者请求）

用于分页查询时的过滤条件。

```json
{
  "keyword": "张三"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（匹配名称或别名） |

#### 4.2.4 AuthorVO（作者响应对象）

返回给客户端的作者数据。

```json
{
  "id": "65f8a1b2c3d4e5f6a7b8c9d0",
  "name": "张三",
  "aliases": ["作者别名1", "作者别名2"],
  "signature": "这是作者的个性签名",
  "urls": ["https://example.com/author1"],
  "avatarBase64": "data:image/png;base64,...",
  "remark": "这是备注信息",
  "createTime": "2024-03-20T10:30:00",
  "updateTime": "2024-03-20T10:30:00"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | MongoDB 文档 ID |
| name | String | 作者名称 |
| aliases | List\<String\> | 别名列表 |
| signature | String | 个性签名 |
| urls | List\<String\> | 网址列表 |
| avatarBase64 | String | BASE64编码的头像 |
| remark | String | 备注信息 |
| createTime | String | 创建时间（ISO 8601 格式） |
| updateTime | String | 更新时间（ISO 8601 格式） |

### 4.3 API 端点

#### 4.3.1 创建作者

**接口地址：** `POST /api/config/tag/author`

**请求示例：**

```json
{
  "name": "张三",
  "aliases": ["作者别名1", "作者别名2"],
  "signature": "这是作者的个性签名",
  "urls": ["https://example.com/author1"],
  "avatarBase64": null,
  "remark": "这是备注信息"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "张三",
    "aliases": ["作者别名1", "作者别名2"],
    "signature": "这是作者的个性签名",
    "urls": ["https://example.com/author1"],
    "avatarBase64": null,
    "remark": "这是备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

名称已存在（唯一性冲突）：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=AUTHOR, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=张三",
  "data": null
}
```

别名已存在（唯一性冲突）：
```json
{
  "code": -60003,
  "msg": "别名已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d2, 别名=作者别名1",
  "data": null
}
```

参数校验失败：
```json
{
  "code": -40006,
  "msg": "作者名称不能为空; 别名列表不能为null",
  "data": null
}
```

#### 4.3.2 更新作者

**接口地址：** `PUT /api/config/tag/author/{id}`

**路径参数：**
- `id`: 作者的 MongoDB 文档 ID

**请求示例：**

```json
{
  "name": "张三（更新后）",
  "signature": "更新后的个性签名"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "张三（更新后）",
    "aliases": ["作者别名1", "作者别名2"],
    "signature": "更新后的个性签名",
    "urls": ["https://example.com/author1"],
    "avatarBase64": null,
    "remark": "这是备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T15:45:00"
  }
}
```

**错误响应示例：**

作者不存在：
```json
{
  "code": -60002,
  "msg": "作者不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

名称冲突：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=AUTHOR, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=张三（更新后）",
  "data": null
}
```

#### 4.3.3 删除作者

**接口地址：** `DELETE /api/config/tag/author/{id}`

**路径参数：**
- `id`: 作者的 MongoDB 文档 ID

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| force | Boolean | 否 | false | 是否强制删除（忽略引用检查） |

**请求示例：**

```
DELETE /api/config/tag/author/65f8a1b2c3d4e5f6a7b8c9d0
DELETE /api/config/tag/author/65f8a1b2c3d4e5f6a7b8c9d0?force=true
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

**错误响应示例：**

作者不存在：
```json
{
  "code": -60002,
  "msg": "作者不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

存在引用关系（引用完整性检查失败）：
```json
{
  "code": -60004,
  "msg": "无法删除：该作者被2个角色引用，被1个过滤配置引用",
  "data": {
    "referencedByCharacters": ["65f8a1b2c3d4e5f6a7b8c9d3", "65f8a1b2c3d4e5f6a7b8c9d4"],
    "referencedByConfigs": ["65f8a1b2c3d4e5f6a7b8c9e0"]
  }
}
```

#### 4.3.4 查询单个作者（通过ID）

**接口地址：** `GET /api/config/tag/author/{id}`

**路径参数：**
- `id`: 作者的 MongoDB 文档 ID

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "张三",
    "aliases": ["作者别名1", "作者别名2"],
    "signature": "这是作者的个性签名",
    "urls": ["https://example.com/author1"],
    "avatarBase64": null,
    "remark": "这是备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

作者不存在：
```json
{
  "code": -60002,
  "msg": "作者不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 4.3.5 查询单个作者（通过名称）

**接口地址：** `GET /api/config/tag/author/name/{name}`

**路径参数：**
- `name`: 作者名称

**请求示例：**

```
GET /api/config/tag/author/name/张三
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "张三",
    "aliases": ["作者别名1", "作者别名2"],
    "signature": "这是作者的个性签名",
    "urls": ["https://example.com/author1"],
    "avatarBase64": null,
    "remark": "这是备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

作者不存在：
```json
{
  "code": -60002,
  "msg": "作者不存在: name=张三",
  "data": null
}
```

#### 4.3.6 分页查询作者

**接口地址：** `GET /api/config/tag/author/page`

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页大小 |
| keyword | String | 否 | - | 搜索关键词（匹配名称或别名） |

**请求示例：**

```
GET /api/config/tag/author/page?current=1&size=20&keyword=张三
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d0",
        "name": "张三",
        "aliases": ["作者别名1", "作者别名2"],
        "signature": "这是作者的个性签名",
        "urls": ["https://example.com/author1"],
        "avatarBase64": null,
        "remark": "这是备注信息",
        "createTime": "2024-03-20T10:30:00",
        "updateTime": "2024-03-20T10:30:00"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 1,
    "pages": 1
  }
}
```

**空页响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 1,
    "size": 20,
    "total": 0,
    "pages": 0
  }
}
```

#### 4.3.7 导入作者数据

**接口地址：** `POST /api/config/tag/author/import`

**请求体：** JSON格式的作者数据数组

**请求示例：**

```json
[
  {
    "name": "作者1",
    "aliases": ["别名1", "别名2"],
    "signature": "个性签名1",
    "urls": ["https://example.com/author1"],
    "avatarBase64": null,
    "remark": "备注1"
  },
  {
    "name": "作者2",
    "aliases": ["别名3"],
    "signature": "个性签名2",
    "urls": [],
    "avatarBase64": null,
    "remark": "备注2"
  }
]
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 2,
    "failureCount": 0,
    "errors": []
  }
}
```

**部分失败响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 1,
    "failureCount": 1,
    "errors": [
      {
        "index": 1,
        "name": "作者2",
        "error": "名称已存在：冲突实体类型=AUTHOR, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=作者2"
      }
    ]
  }
}
```

**错误响应示例：**

数据格式错误：
```json
{
  "code": -40006,
  "msg": "导入数据格式错误：JSON解析失败",
  "data": null
}
```

#### 4.3.8 导出作者数据

**接口地址：** `GET /api/config/tag/author/export`

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "name": "张三",
      "aliases": ["作者别名1", "作者别名2"],
      "signature": "这是作者的个性签名",
      "urls": ["https://example.com/author1"],
      "avatarBase64": null,
      "remark": "这是备注信息"
    },
    {
      "name": "李四",
      "aliases": ["作者别名3"],
      "signature": "另一个作者的签名",
      "urls": [],
      "avatarBase64": null,
      "remark": null
    }
  ]
}
```

**空数据响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": []
}
```

### 7.4 常见错误场景

#### 7.4.1 数据不存在（DATA_NOT_FOUND）

**响应码：** `-60002`

**触发场景：**
- 查询不存在的作者 ID
- 更新不存在的作者
- 删除不存在的作者
- 通过名称或别名查询不存在的作者

**响应示例：**

```json
{
  "code": -60002,
  "msg": "作者不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 4.4.2 数据已存在（DATA_ALREADY_EXISTS）

**响应码：** `-60003`

**触发场景：**
- 创建作者时，名称已存在于作者库、原作库或角色库中
- 创建作者时，任何别名已存在于作者库、原作库或角色库中
- 更新作者时，新名称与其他实体冲突
- 更新作者时，新别名与其他实体冲突

**响应示例：**

名称冲突：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=AUTHOR, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=张三",
  "data": null
}
```

别名冲突：
```json
{
  "code": -60003,
  "msg": "别名已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d2, 别名=作者别名1",
  "data": null
}
```

#### 4.4.3 操作失败（OPERATION_FAILED）

**响应码：** `-60004`

**触发场景：**
- 删除作者时，该作者被角色库引用
- 删除作者时，该作者ID在过滤配置的作者标签中被引用

**响应示例：**

```json
{
  "code": -60004,
  "msg": "无法删除：该作者被2个角色引用，被1个过滤配置引用",
  "data": {
    "referencedByCharacters": ["65f8a1b2c3d4e5f6a7b8c9d3", "65f8a1b2c3d4e5f6a7b8c9d4"],
    "referencedByConfigs": ["65f8a1b2c3d4e5f6a7b8c9e0"]
  }
}
```

#### 4.4.4 参数校验失败（VALIDATION_ERROR）

**响应码：** `-40006`

**触发场景：**
- 作者名称为空或超过长度限制
- 别名列表为null
- 别名长度超过限制
- 个性签名、网址、备注超过长度限制

**响应示例：**

```json
{
  "code": -40006,
  "msg": "作者名称不能为空; 别名列表不能为null; 个性签名长度不能超过500",
  "data": null
}
```

### 4.5 使用示例

#### 4.5.1 创建并查询作者

```bash
# 1. 创建作者
curl -X POST http://localhost:8080/api/config/tag/author \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "aliases": ["作者别名1", "作者别名2"],
    "signature": "这是作者的个性签名",
    "urls": ["https://example.com/author1"],
    "avatarBase64": null,
    "remark": "这是备注信息"
  }'

# 响应：获取到 id 为 "65f8a1b2c3d4e5f6a7b8c9d0"

# 2. 通过ID查询该作者
curl -X GET http://localhost:8080/api/config/tag/author/65f8a1b2c3d4e5f6a7b8c9d0

# 3. 通过名称查询该作者
curl -X GET http://localhost:8080/api/config/tag/author/name/张三
```

#### 4.5.2 更新作者信息

```bash
# 更新作者的个性签名和备注
curl -X PUT http://localhost:8080/api/config/tag/author/65f8a1b2c3d4e5f6a7b8c9d0 \
  -H "Content-Type: application/json" \
  -d '{
    "signature": "更新后的个性签名",
    "remark": "更新后的备注"
  }'
```

#### 4.5.3 搜索作者

```bash
# 搜索名称或别名包含"张三"的作者
curl -X GET "http://localhost:8080/api/config/tag/author/page?keyword=张三"

# 分页查询所有作者，第1页，每页20条
curl -X GET "http://localhost:8080/api/config/tag/author/page?current=1&size=20"
```

#### 4.5.4 删除作者

```bash
# 普通删除（会检查引用关系）
curl -X DELETE http://localhost:8080/api/config/tag/author/65f8a1b2c3d4e5f6a7b8c9d0

# 强制删除（忽略引用关系，自动清理引用）
curl -X DELETE "http://localhost:8080/api/config/tag/author/65f8a1b2c3d4e5f6a7b8c9d0?force=true"
```

#### 4.5.5 导入导出作者数据

```bash
# 导出所有作者数据
curl -X GET http://localhost:8080/api/config/tag/author/export > authors.json

# 导入作者数据
curl -X POST http://localhost:8080/api/config/tag/author/import \
  -H "Content-Type: application/json" \
  -d @authors.json
```

### 4.6 注意事项

#### 4.6.1 唯一性约束

1. **全局唯一性**：作者名称在作者库、原作库、角色库之间必须全局唯一
2. **别名唯一性**：作者别名在作者库、原作库、角色库之间必须全局唯一
3. **名称与别名交叉验证**：作者名称不能与其他实体的别名冲突，作者别名不能与其他实体的名称冲突
4. **冲突信息**：唯一性冲突时，错误信息会明确指出冲突的实体类型和ID

#### 4.6.2 引用完整性

1. **引用检查**：删除作者前会检查是否被角色库或过滤配置引用
2. **引用信息**：如果存在引用，错误响应会列出所有引用该作者的实体ID
3. **强制删除**：使用 `force=true` 参数可以强制删除，系统会自动清理所有引用关系
4. **引用清理**：强制删除时，会从角色库和过滤配置中移除对该作者的所有引用

#### 4.6.3 数据验证

1. **必填字段**：name 和 aliases 是必填字段，aliases 可以是空列表但不能为null
2. **长度限制**：
   - name: 最大100字符
   - aliases: 每个别名最大100字符
   - signature: 最大500字符
   - urls: 每个URL最大500字符
   - remark: 最大1000字符
3. **空值处理**：可选字段可以为null或不提供

#### 4.6.4 查询功能

1. **多路径查询**：支持通过ID、名称、别名查询同一个作者
2. **关键词搜索**：keyword 参数支持模糊匹配名称和别名
3. **分页查询**：支持分页和关键词搜索组合使用
4. **查询一致性**：通过不同路径查询同一作者应返回相同的数据

#### 4.6.5 导入导出

1. **JSON格式**：导入导出使用标准JSON格式
2. **数据完整性**：导入时会验证数据格式和完整性
3. **唯一性验证**：导入时会检查名称和别名的唯一性约束
4. **冲突处理**：导入时如果数据冲突，会跳过该条记录并在响应中返回错误信息
5. **批量操作**：导入支持批量创建多个作者

#### 4.6.6 时间戳管理

1. **createTime**：创建时自动设置，不可修改
2. **updateTime**：每次更新时自动更新为当前时间
3. **时间格式**：ISO 8601 格式（如 `2024-03-20T10:30:00`）

#### 4.6.7 部分更新

1. **可选字段**：更新时只需提供需要修改的字段
2. **字段保留**：未提供的字段保持原值不变
3. **空值更新**：如果需要清空某个字段，可以显式传递null

#### 4.6.8 错误处理

1. **HTTP 状态码**：所有响应的 HTTP 状态码均为 200
2. **错误判断**：通过响应体中的 code 字段判断是否成功
3. **错误信息**：错误响应包含详细的错误描述
4. **参数校验**：所有参数都经过严格校验

---

## 5. 原作库 API

### 5.1 概述

原作库 API 提供了对原作信息的完整 CRUD 操作，包括创建、更新、删除、查询和分页列表功能。原作库支持原作名称、别名列表、网址列表、头像和备注信息的管理。

主要功能包括：
- 原作的创建、更新、删除操作
- 支持多种查询方式（ID、名称、别名）
- 分页查询和关键词搜索
- 数据导入导出（JSON格式）
- 全局唯一性约束验证
- 引用完整性检查

### 5.2 数据结构

#### 5.2.1 WorkCreateDTO（创建原作请求）

用于创建新的原作。

```json
{
  "name": "原作名称",
  "aliases": ["原作别名1", "原作别名2"],
  "urls": ["https://example.com/work1", "https://wiki.example.com/work1"],
  "avatarBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
  "remark": "这是原作的备注信息"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 是 | 不能为空，长度不超过100 | 原作名称（全局唯一） |
| aliases | List\<String\> | 是 | 不能为null，每个别名长度不超过100 | 别名列表（可以为空数组，每个别名全局唯一） |
| urls | List\<String\> | 否 | 每个URL长度不超过500 | 网址列表（可为空数组） |
| avatarBase64 | String | 否 | - | BASE64编码的头像（可为null） |
| remark | String | 否 | 长度不超过1000 | 备注信息（可为null） |

**数据库约束：**
- **name**: 必填，全局唯一（跨作者库、原作库、角色库）
- **aliases**: 必填但可为空数组，每个别名全局唯一（跨作者库、原作库、角色库）
- **urls**: 可为空数组
- **avatarBase64**: 可为null
- **remark**: 可为null

#### 5.2.2 WorkUpdateDTO（更新原作请求）

用于更新现有原作，所有字段均为可选。

```json
{
  "name": "原作名称（更新后）",
  "aliases": ["新别名1", "新别名2"],
  "urls": ["https://example.com/work1_updated"]
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 否 | 长度不超过100 | 原作名称 |
| aliases | List\<String\> | 否 | 每个别名长度不超过100 | 别名列表 |
| urls | List\<String\> | 否 | 每个URL长度不超过500 | 网址列表 |
| avatarBase64 | String | 否 | - | BASE64编码的头像 |
| remark | String | 否 | 长度不超过1000 | 备注信息 |

#### 5.2.3 WorkQueryDTO（查询原作请求）

用于分页查询时的过滤条件。

```json
{
  "keyword": "原作名称"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（匹配名称或别名） |

#### 5.2.4 WorkVO（原作响应对象）

返回给客户端的原作数据。

```json
{
  "id": "65f8a1b2c3d4e5f6a7b8c9d0",
  "name": "原作名称",
  "aliases": ["原作别名1", "原作别名2"],
  "urls": ["https://example.com/work1"],
  "avatarBase64": "data:image/png;base64,...",
  "remark": "这是原作的备注信息",
  "createTime": "2024-03-20T10:30:00",
  "updateTime": "2024-03-20T10:30:00"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | MongoDB 文档 ID |
| name | String | 原作名称 |
| aliases | List\<String\> | 别名列表 |
| urls | List\<String\> | 网址列表 |
| avatarBase64 | String | BASE64编码的头像 |
| remark | String | 备注信息 |
| createTime | String | 创建时间（ISO 8601 格式） |
| updateTime | String | 更新时间（ISO 8601 格式） |

### 5.3 API 端点

#### 5.3.1 创建原作

**接口地址：** `POST /api/config/tag/work`

**请求示例：**

```json
{
  "name": "原作名称",
  "aliases": ["原作别名1", "原作别名2"],
  "urls": ["https://example.com/work1"],
  "avatarBase64": null,
  "remark": "这是原作的备注信息"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "原作名称",
    "aliases": ["原作别名1", "原作别名2"],
    "urls": ["https://example.com/work1"],
    "avatarBase64": null,
    "remark": "这是原作的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

名称已存在（唯一性冲突）：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=原作名称",
  "data": null
}
```

别名已存在（唯一性冲突）：
```json
{
  "code": -60003,
  "msg": "别名已存在：冲突实体类型=AUTHOR, ID=65f8a1b2c3d4e5f6a7b8c9d2, 别名=原作别名1",
  "data": null
}
```

参数校验失败：
```json
{
  "code": -40006,
  "msg": "原作名称不能为空; 别名列表不能为null",
  "data": null
}
```

#### 5.3.2 更新原作

**接口地址：** `PUT /api/config/tag/work/{id}`

**路径参数：**
- `id`: 原作的 MongoDB 文档 ID

**请求示例：**

```json
{
  "name": "原作名称（更新后）",
  "urls": ["https://example.com/work1_updated"]
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "原作名称（更新后）",
    "aliases": ["原作别名1", "原作别名2"],
    "urls": ["https://example.com/work1_updated"],
    "avatarBase64": null,
    "remark": "这是原作的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T15:45:00"
  }
}
```

**错误响应示例：**

原作不存在：
```json
{
  "code": -60002,
  "msg": "原作不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

名称冲突：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=原作名称（更新后）",
  "data": null
}
```

#### 5.3.3 删除原作

**接口地址：** `DELETE /api/config/tag/work/{id}`

**路径参数：**
- `id`: 原作的 MongoDB 文档 ID

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| force | Boolean | 否 | false | 是否强制删除（忽略引用检查） |

**请求示例：**

```
DELETE /api/config/tag/work/65f8a1b2c3d4e5f6a7b8c9d0
DELETE /api/config/tag/work/65f8a1b2c3d4e5f6a7b8c9d0?force=true
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

**错误响应示例：**

原作不存在：
```json
{
  "code": -60002,
  "msg": "原作不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

存在引用关系（引用完整性检查失败）：
```json
{
  "code": -60004,
  "msg": "无法删除：该原作被3个角色引用，被1个过滤配置引用",
  "data": {
    "referencedByCharacters": ["65f8a1b2c3d4e5f6a7b8c9d3", "65f8a1b2c3d4e5f6a7b8c9d4", "65f8a1b2c3d4e5f6a7b8c9d5"],
    "referencedByConfigs": ["65f8a1b2c3d4e5f6a7b8c9e0"]
  }
}
```

#### 5.3.4 查询单个原作（通过ID）

**接口地址：** `GET /api/config/tag/work/{id}`

**路径参数：**
- `id`: 原作的 MongoDB 文档 ID

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "原作名称",
    "aliases": ["原作别名1", "原作别名2"],
    "urls": ["https://example.com/work1"],
    "avatarBase64": null,
    "remark": "这是原作的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

原作不存在：
```json
{
  "code": -60002,
  "msg": "原作不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 5.3.5 查询单个原作（通过名称）

**接口地址：** `GET /api/config/tag/work/name/{name}`

**路径参数：**
- `name`: 原作名称

**请求示例：**

```
GET /api/config/tag/work/name/原作名称
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d0",
    "name": "原作名称",
    "aliases": ["原作别名1", "原作别名2"],
    "urls": ["https://example.com/work1"],
    "avatarBase64": null,
    "remark": "这是原作的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

原作不存在：
```json
{
  "code": -60002,
  "msg": "原作不存在: name=原作名称",
  "data": null
}
```

#### 5.3.6 分页查询原作

**接口地址：** `GET /api/config/tag/work/page`

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页大小 |
| keyword | String | 否 | - | 搜索关键词（匹配名称或别名） |

**请求示例：**

```
GET /api/config/tag/work/page?current=1&size=20&keyword=原作
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d0",
        "name": "原作名称",
        "aliases": ["原作别名1", "原作别名2"],
        "urls": ["https://example.com/work1"],
        "avatarBase64": null,
        "remark": "这是原作的备注信息",
        "createTime": "2024-03-20T10:30:00",
        "updateTime": "2024-03-20T10:30:00"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 1,
    "pages": 1
  }
}
```

**空页响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 1,
    "size": 20,
    "total": 0,
    "pages": 0
  }
}
```

#### 5.3.7 导入原作数据

**接口地址：** `POST /api/config/tag/work/import`

**请求体：** JSON格式的原作数据数组

**请求示例：**

```json
[
  {
    "name": "原作1",
    "aliases": ["别名1", "别名2"],
    "urls": ["https://example.com/work1"],
    "avatarBase64": null,
    "remark": "备注1"
  },
  {
    "name": "原作2",
    "aliases": ["别名3"],
    "urls": [],
    "avatarBase64": null,
    "remark": "备注2"
  }
]
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 2,
    "failureCount": 0,
    "errors": []
  }
}
```

**部分失败响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 1,
    "failureCount": 1,
    "errors": [
      {
        "index": 1,
        "name": "原作2",
        "error": "名称已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=原作2"
      }
    ]
  }
}
```

**错误响应示例：**

数据格式错误：
```json
{
  "code": -40006,
  "msg": "导入数据格式错误：JSON解析失败",
  "data": null
}
```

#### 5.3.8 导出原作数据

**接口地址：** `GET /api/config/tag/work/export`

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "name": "原作名称",
      "aliases": ["原作别名1", "原作别名2"],
      "urls": ["https://example.com/work1"],
      "avatarBase64": null,
      "remark": "这是原作的备注信息"
    },
    {
      "name": "另一个原作",
      "aliases": ["原作别名3"],
      "urls": [],
      "avatarBase64": null,
      "remark": null
    }
  ]
}
```

**空数据响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": []
}
```

### 5.4 常见错误场景

#### 5.4.1 数据不存在（DATA_NOT_FOUND）

**响应码：** `-60002`

**触发场景：**
- 查询不存在的原作 ID
- 更新不存在的原作
- 删除不存在的原作
- 通过名称或别名查询不存在的原作

**响应示例：**

```json
{
  "code": -60002,
  "msg": "原作不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 5.4.2 数据已存在（DATA_ALREADY_EXISTS）

**响应码：** `-60003`

**触发场景：**
- 创建原作时，名称已存在于作者库、原作库或角色库中
- 创建原作时，任何别名已存在于作者库、原作库或角色库中
- 更新原作时，新名称与其他实体冲突
- 更新原作时，新别名与其他实体冲突

**响应示例：**

名称冲突：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d1, 名称=原作名称",
  "data": null
}
```

别名冲突：
```json
{
  "code": -60003,
  "msg": "别名已存在：冲突实体类型=CHARACTER, ID=65f8a1b2c3d4e5f6a7b8c9d2, 别名=原作别名1",
  "data": null
}
```

#### 5.4.3 操作失败（OPERATION_FAILED）

**响应码：** `-60004`

**触发场景：**
- 删除原作时，该原作被角色库引用
- 删除原作时，该原作ID在过滤配置的原作标签中被引用

**响应示例：**

```json
{
  "code": -60004,
  "msg": "无法删除：该原作被3个角色引用，被1个过滤配置引用",
  "data": {
    "referencedByCharacters": ["65f8a1b2c3d4e5f6a7b8c9d3", "65f8a1b2c3d4e5f6a7b8c9d4", "65f8a1b2c3d4e5f6a7b8c9d5"],
    "referencedByConfigs": ["65f8a1b2c3d4e5f6a7b8c9e0"]
  }
}
```

#### 5.4.4 参数校验失败（VALIDATION_ERROR）

**响应码：** `-40006`

**触发场景：**
- 原作名称为空或超过长度限制
- 别名列表为null
- 别名长度超过限制
- 网址、备注超过长度限制

**响应示例：**

```json
{
  "code": -40006,
  "msg": "原作名称不能为空; 别名列表不能为null",
  "data": null
}
```

### 5.5 使用示例

#### 5.5.1 创建并查询原作

```bash
# 1. 创建原作
curl -X POST http://localhost:8080/api/config/tag/work \
  -H "Content-Type: application/json" \
  -d '{
    "name": "原作名称",
    "aliases": ["原作别名1", "原作别名2"],
    "urls": ["https://example.com/work1"],
    "avatarBase64": null,
    "remark": "这是原作的备注信息"
  }'

# 响应：获取到 id 为 "65f8a1b2c3d4e5f6a7b8c9d0"

# 2. 通过ID查询该原作
curl -X GET http://localhost:8080/api/config/tag/work/65f8a1b2c3d4e5f6a7b8c9d0

# 3. 通过名称查询该原作
curl -X GET http://localhost:8080/api/config/tag/work/name/原作名称
```

#### 5.5.2 更新原作信息

```bash
# 更新原作的网址和备注
curl -X PUT http://localhost:8080/api/config/tag/work/65f8a1b2c3d4e5f6a7b8c9d0 \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://example.com/work1_updated"],
    "remark": "更新后的备注"
  }'
```

#### 5.5.3 搜索原作

```bash
# 搜索名称或别名包含"原作"的原作
curl -X GET "http://localhost:8080/api/config/tag/work/page?keyword=原作"

# 分页查询所有原作，第1页，每页20条
curl -X GET "http://localhost:8080/api/config/tag/work/page?current=1&size=20"
```

#### 5.5.4 删除原作

```bash
# 普通删除（会检查引用关系）
curl -X DELETE http://localhost:8080/api/config/tag/work/65f8a1b2c3d4e5f6a7b8c9d0

# 强制删除（忽略引用关系，自动清理引用）
curl -X DELETE "http://localhost:8080/api/config/tag/work/65f8a1b2c3d4e5f6a7b8c9d0?force=true"
```

#### 5.5.5 导入导出原作数据

```bash
# 导出所有原作数据
curl -X GET http://localhost:8080/api/config/tag/work/export > works.json

# 导入原作数据
curl -X POST http://localhost:8080/api/config/tag/work/import \
  -H "Content-Type: application/json" \
  -d @works.json
```

### 5.6 注意事项

#### 5.6.1 唯一性约束

1. **全局唯一性**：原作名称在作者库、原作库、角色库之间必须全局唯一
2. **别名唯一性**：原作别名在作者库、原作库、角色库之间必须全局唯一
3. **名称与别名交叉验证**：原作名称不能与其他实体的别名冲突，原作别名不能与其他实体的名称冲突
4. **冲突信息**：唯一性冲突时，错误信息会明确指出冲突的实体类型和ID

#### 5.6.2 引用完整性

1. **引用检查**：删除原作前会检查是否被角色库或过滤配置引用
2. **引用信息**：如果存在引用，错误响应会列出所有引用该原作的实体ID
3. **强制删除**：使用 `force=true` 参数可以强制删除，系统会自动清理所有引用关系
4. **引用清理**：强制删除时，会从角色库和过滤配置中移除对该原作的所有引用

#### 5.6.3 数据验证

1. **必填字段**：name 和 aliases 是必填字段，aliases 可以是空列表但不能为null
2. **长度限制**：
   - name: 最大100字符
   - aliases: 每个别名最大100字符
   - urls: 每个URL最大500字符
   - remark: 最大1000字符
3. **空值处理**：可选字段可以为null或不提供

#### 5.6.4 查询功能

1. **多路径查询**：支持通过ID、名称、别名查询同一个原作
2. **关键词搜索**：keyword 参数支持模糊匹配名称和别名
3. **分页查询**：支持分页和关键词搜索组合使用
4. **查询一致性**：通过不同路径查询同一原作应返回相同的数据

#### 5.6.5 导入导出

1. **JSON格式**：导入导出使用标准JSON格式
2. **数据完整性**：导入时会验证数据格式和完整性
3. **唯一性验证**：导入时会检查名称和别名的唯一性约束
4. **冲突处理**：导入时如果数据冲突，会跳过该条记录并在响应中返回错误信息
5. **批量操作**：导入支持批量创建多个原作

#### 5.6.6 时间戳管理

1. **createTime**：创建时自动设置，不可修改
2. **updateTime**：每次更新时自动更新为当前时间
3. **时间格式**：ISO 8601 格式（如 `2024-03-20T10:30:00`）

#### 5.6.7 部分更新

1. **可选字段**：更新时只需提供需要修改的字段
2. **字段保留**：未提供的字段保持原值不变
3. **空值更新**：如果需要清空某个字段，可以显式传递null

#### 5.6.8 错误处理

1. **HTTP 状态码**：所有响应的 HTTP 状态码均为 200
2. **错误判断**：通过响应体中的 code 字段判断是否成功
3. **错误信息**：错误响应包含详细的错误描述
4. **参数校验**：所有参数都经过严格校验

---

## 6. 角色库 API

### 6.1 概述

角色库 API 提供了对角色信息的完整 CRUD 操作，包括创建、更新、删除、查询和分页列表功能。角色库支持角色名称、别名列表、所属原作、种族、头像和备注信息的管理。

主要功能包括：
- 角色的创建、更新、删除操作
- 支持多种查询方式（ID、名称、别名、所属原作）
- 分页查询和关键词搜索
- 按原作或种族过滤
- 数据导入导出（JSON格式）
- 全局唯一性约束验证
- 原作引用有效性验证
- 引用完整性检查

### 6.2 数据结构

#### 6.2.1 CharacterCreateDTO（创建角色请求）

用于创建新的角色。

```json
{
  "name": "角色名称",
  "aliases": ["角色别名1", "角色别名2"],
  "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
  "species": "人类",
  "avatarBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
  "remark": "这是角色的备注信息"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 是 | 不能为空，长度不超过100 | 角色名称（全局唯一） |
| aliases | List\<String\> | 是 | 不能为null，每个别名长度不超过100 | 别名列表（可以为空数组，每个别名全局唯一） |
| workId | String | 否 | - | 所属原作ID（可为null，如果提供则必须在原作库中存在） |
| species | String | 是 | 不能为空，长度不超过100 | 种族 |
| avatarBase64 | String | 否 | - | BASE64编码的头像（可为null） |
| remark | String | 否 | 长度不超过1000 | 备注信息（可为null） |

**数据库约束：**
- **name**: 必填，全局唯一（跨作者库、原作库、角色库）
- **aliases**: 必填但可为空数组，每个别名全局唯一（跨作者库、原作库、角色库）
- **workId**: 可为null，如果提供则必须在原作库中存在
- **species**: 必填
- **avatarBase64**: 可为null
- **remark**: 可为null

#### 6.2.2 CharacterUpdateDTO（更新角色请求）

用于更新现有角色，所有字段均为可选。

```json
{
  "name": "角色名称（更新后）",
  "aliases": ["新别名1", "新别名2"],
  "workId": "65f8a1b2c3d4e5f6a7b8c9d1",
  "species": "精灵"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 否 | 长度不超过100 | 角色名称 |
| aliases | List\<String\> | 否 | 每个别名长度不超过100 | 别名列表 |
| workId | String | 否 | - | 所属原作ID（必须在原作库中存在） |
| species | String | 否 | 长度不超过100 | 种族 |
| avatarBase64 | String | 否 | - | BASE64编码的头像 |
| remark | String | 否 | 长度不超过1000 | 备注信息 |

#### 6.2.3 CharacterQueryDTO（查询角色请求）

用于分页查询时的过滤条件。

```json
{
  "keyword": "角色名称",
  "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
  "species": "人类"
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（匹配名称或别名） |
| workId | String | 否 | 按所属原作过滤 |
| species | String | 否 | 按种族过滤 |

#### 6.2.4 CharacterVO（角色响应对象）

返回给客户端的角色数据。

```json
{
  "id": "65f8a1b2c3d4e5f6a7b8c9d0",
  "name": "角色名称",
  "aliases": ["角色别名1", "角色别名2"],
  "workId": "65f8a1b2c3d4e5f6a7b8c9d1",
  "workName": "原作名称",
  "species": "人类",
  "avatarBase64": "data:image/png;base64,...",
  "remark": "这是角色的备注信息",
  "createTime": "2024-03-20T10:30:00",
  "updateTime": "2024-03-20T10:30:00"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | MongoDB 文档 ID |
| name | String | 角色名称 |
| aliases | List\<String\> | 别名列表 |
| workId | String | 所属原作ID |
| workName | String | 所属原作名称（冗余字段，方便展示） |
| species | String | 种族 |
| avatarBase64 | String | BASE64编码的头像 |
| remark | String | 备注信息 |
| createTime | String | 创建时间（ISO 8601 格式） |
| updateTime | String | 更新时间（ISO 8601 格式） |

### 6.3 API 端点

#### 6.3.1 创建角色

**接口地址：** `POST /api/config/tag/character`

**请求示例：**

```json
{
  "name": "角色名称",
  "aliases": ["角色别名1", "角色别名2"],
  "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
  "species": "人类",
  "avatarBase64": null,
  "remark": "这是角色的备注信息"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d2",
    "name": "角色名称",
    "aliases": ["角色别名1", "角色别名2"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "workName": "原作名称",
    "species": "人类",
    "avatarBase64": null,
    "remark": "这是角色的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

名称已存在（唯一性冲突）：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=CHARACTER, ID=65f8a1b2c3d4e5f6a7b8c9d3, 名称=角色名称",
  "data": null
}
```

别名已存在（唯一性冲突）：
```json
{
  "code": -60003,
  "msg": "别名已存在：冲突实体类型=AUTHOR, ID=65f8a1b2c3d4e5f6a7b8c9d4, 别名=角色别名1",
  "data": null
}
```

原作不存在（引用验证失败，仅当提供了workId时）：
```json
{
  "code": -60002,
  "msg": "所属原作不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

参数校验失败：
```json
{
  "code": -40006,
  "msg": "角色名称不能为空; 别名列表不能为null; 种族不能为空",
  "data": null
}
```

#### 6.3.2 更新角色

**接口地址：** `PUT /api/config/tag/character/{id}`

**路径参数：**
- `id`: 角色的 MongoDB 文档 ID

**请求示例：**

```json
{
  "name": "角色名称（更新后）",
  "species": "精灵"
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d2",
    "name": "角色名称（更新后）",
    "aliases": ["角色别名1", "角色别名2"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "workName": "原作名称",
    "species": "精灵",
    "avatarBase64": null,
    "remark": "这是角色的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T15:45:00"
  }
}
```

**错误响应示例：**

角色不存在：
```json
{
  "code": -60002,
  "msg": "角色不存在: 65f8a1b2c3d4e5f6a7b8c9d2",
  "data": null
}
```

名称冲突：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=CHARACTER, ID=65f8a1b2c3d4e5f6a7b8c9d3, 名称=角色名称（更新后）",
  "data": null
}
```

原作不存在：
```json
{
  "code": -60002,
  "msg": "原作不存在: 65f8a1b2c3d4e5f6a7b8c9d1",
  "data": null
}
```

#### 6.3.3 删除角色

**接口地址：** `DELETE /api/config/tag/character/{id}`

**路径参数：**
- `id`: 角色的 MongoDB 文档 ID

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| force | Boolean | 否 | false | 是否强制删除（忽略引用检查） |

**请求示例：**

```
DELETE /api/config/tag/character/65f8a1b2c3d4e5f6a7b8c9d2
DELETE /api/config/tag/character/65f8a1b2c3d4e5f6a7b8c9d2?force=true
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

**错误响应示例：**

角色不存在：
```json
{
  "code": -60002,
  "msg": "角色不存在: 65f8a1b2c3d4e5f6a7b8c9d2",
  "data": null
}
```

存在引用关系（引用完整性检查失败）：
```json
{
  "code": -60004,
  "msg": "无法删除：该角色被1个过滤配置引用",
  "data": {
    "referencedByConfigs": ["65f8a1b2c3d4e5f6a7b8c9e0"]
  }
}
```

#### 6.3.4 查询单个角色（通过ID）

**接口地址：** `GET /api/config/tag/character/{id}`

**路径参数：**
- `id`: 角色的 MongoDB 文档 ID

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d2",
    "name": "角色名称",
    "aliases": ["角色别名1", "角色别名2"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "workName": "原作名称",
    "species": "人类",
    "avatarBase64": null,
    "remark": "这是角色的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

角色不存在：
```json
{
  "code": -60002,
  "msg": "角色不存在: 65f8a1b2c3d4e5f6a7b8c9d2",
  "data": null
}
```

#### 6.3.5 查询单个角色（通过名称）

**接口地址：** `GET /api/config/tag/character/name/{name}`

**路径参数：**
- `name`: 角色名称

**请求示例：**

```
GET /api/config/tag/character/name/角色名称
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9d2",
    "name": "角色名称",
    "aliases": ["角色别名1", "角色别名2"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "workName": "原作名称",
    "species": "人类",
    "avatarBase64": null,
    "remark": "这是角色的备注信息",
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

角色不存在：
```json
{
  "code": -60002,
  "msg": "角色不存在: name=角色名称",
  "data": null
}
```

#### 6.3.6 查询原作的所有角色

**接口地址：** `GET /api/config/tag/character/work/{workId}`

**路径参数：**
- `workId`: 原作的 MongoDB 文档 ID

**请求示例：**

```
GET /api/config/tag/character/work/65f8a1b2c3d4e5f6a7b8c9d0
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": "65f8a1b2c3d4e5f6a7b8c9d2",
      "name": "角色1",
      "aliases": ["角色别名1"],
      "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
      "workName": "原作名称",
      "species": "人类",
      "avatarBase64": null,
      "remark": "备注1",
      "createTime": "2024-03-20T10:30:00",
      "updateTime": "2024-03-20T10:30:00"
    },
    {
      "id": "65f8a1b2c3d4e5f6a7b8c9d3",
      "name": "角色2",
      "aliases": ["角色别名2"],
      "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
      "workName": "原作名称",
      "species": "精灵",
      "avatarBase64": null,
      "remark": "备注2",
      "createTime": "2024-03-20T10:35:00",
      "updateTime": "2024-03-20T10:35:00"
    }
  ]
}
```

**空列表响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": []
}
```

#### 6.3.7 分页查询角色

**接口地址：** `GET /api/config/tag/character/page`

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| current | Long | 否 | 1 | 当前页码 |
| size | Long | 否 | 10 | 每页大小 |
| keyword | String | 否 | - | 搜索关键词（匹配名称或别名） |
| workId | String | 否 | - | 按所属原作过滤 |
| species | String | 否 | - | 按种族过滤 |

**请求示例：**

```
GET /api/config/tag/character/page?current=1&size=20&keyword=角色&workId=65f8a1b2c3d4e5f6a7b8c9d0&species=人类
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": "65f8a1b2c3d4e5f6a7b8c9d2",
        "name": "角色名称",
        "aliases": ["角色别名1", "角色别名2"],
        "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
        "workName": "原作名称",
        "species": "人类",
        "avatarBase64": null,
        "remark": "这是角色的备注信息",
        "createTime": "2024-03-20T10:30:00",
        "updateTime": "2024-03-20T10:30:00"
      }
    ],
    "current": 1,
    "size": 20,
    "total": 1,
    "pages": 1
  }
}
```

**空页响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 1,
    "size": 20,
    "total": 0,
    "pages": 0
  }
}
```

#### 6.3.8 导入角色数据

**接口地址：** `POST /api/config/tag/character/import`

**请求体：** JSON格式的角色数据数组

**请求示例：**

```json
[
  {
    "name": "角色1",
    "aliases": ["别名1", "别名2"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "species": "人类",
    "avatarBase64": null,
    "remark": "备注1"
  },
  {
    "name": "角色2",
    "aliases": ["别名3"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "species": "精灵",
    "avatarBase64": null,
    "remark": "备注2"
  }
]
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 2,
    "failureCount": 0,
    "errors": []
  }
}
```

**部分失败响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "successCount": 1,
    "failureCount": 1,
    "errors": [
      {
        "index": 1,
        "name": "角色2",
        "error": "名称已存在：冲突实体类型=CHARACTER, ID=65f8a1b2c3d4e5f6a7b8c9d3, 名称=角色2"
      }
    ]
  }
}
```

**错误响应示例：**

数据格式错误：
```json
{
  "code": -40006,
  "msg": "导入数据格式错误：JSON解析失败",
  "data": null
}
```

#### 6.3.9 导出角色数据

**接口地址：** `GET /api/config/tag/character/export`

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "name": "角色名称",
      "aliases": ["角色别名1", "角色别名2"],
      "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
      "species": "人类",
      "avatarBase64": null,
      "remark": "这是角色的备注信息"
    },
    {
      "name": "另一个角色",
      "aliases": ["角色别名3"],
      "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
      "species": "精灵",
      "avatarBase64": null,
      "remark": null
    }
  ]
}
```

**空数据响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": []
}
```

### 6.4 常见错误场景

#### 6.4.1 数据不存在（DATA_NOT_FOUND）

**响应码：** `-60002`

**触发场景：**
- 查询不存在的角色 ID
- 更新不存在的角色
- 删除不存在的角色
- 通过名称或别名查询不存在的角色
- 创建或更新角色时，指定的原作ID不存在

**响应示例：**

角色不存在：
```json
{
  "code": -60002,
  "msg": "角色不存在: 65f8a1b2c3d4e5f6a7b8c9d2",
  "data": null
}
```

原作不存在：
```json
{
  "code": -60002,
  "msg": "原作不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 6.4.2 数据已存在（DATA_ALREADY_EXISTS）

**响应码：** `-60003`

**触发场景：**
- 创建角色时，名称已存在于作者库、原作库或角色库中
- 创建角色时，任何别名已存在于作者库、原作库或角色库中
- 更新角色时，新名称与其他实体冲突
- 更新角色时，新别名与其他实体冲突

**响应示例：**

名称冲突：
```json
{
  "code": -60003,
  "msg": "名称已存在：冲突实体类型=CHARACTER, ID=65f8a1b2c3d4e5f6a7b8c9d3, 名称=角色名称",
  "data": null
}
```

别名冲突：
```json
{
  "code": -60003,
  "msg": "别名已存在：冲突实体类型=WORK, ID=65f8a1b2c3d4e5f6a7b8c9d4, 别名=角色别名1",
  "data": null
}
```

#### 6.4.3 操作失败（OPERATION_FAILED）

**响应码：** `-60004`

**触发场景：**
- 删除角色时，该角色ID在过滤配置的角色标签中被引用

**响应示例：**

```json
{
  "code": -60004,
  "msg": "无法删除：该角色被1个过滤配置引用",
  "data": {
    "referencedByConfigs": ["65f8a1b2c3d4e5f6a7b8c9e0"]
  }
}
```

#### 6.4.4 参数校验失败（VALIDATION_ERROR）

**响应码：** `-40006`

**触发场景：**
- 角色名称为空或超过长度限制
- 别名列表为null
- 别名长度超过限制
- 所属原作ID为空
- 种族、备注超过长度限制

**响应示例：**

```json
{
  "code": -40006,
  "msg": "角色名称不能为空; 别名列表不能为null; 所属原作ID不能为空",
  "data": null
}
```

### 6.5 使用示例

#### 6.5.1 创建并查询角色

```bash
# 1. 创建角色
curl -X POST http://localhost:8080/api/config/tag/character \
  -H "Content-Type: application/json" \
  -d '{
    "name": "角色名称",
    "aliases": ["角色别名1", "角色别名2"],
    "workId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "species": "人类",
    "avatarBase64": null,
    "remark": "这是角色的备注信息"
  }'

# 响应：获取到 id 为 "65f8a1b2c3d4e5f6a7b8c9d2"

# 2. 通过ID查询该角色
curl -X GET http://localhost:8080/api/config/tag/character/65f8a1b2c3d4e5f6a7b8c9d2

# 3. 通过名称查询该角色
curl -X GET http://localhost:8080/api/config/tag/character/name/角色名称
```

#### 6.5.2 更新角色信息

```bash
# 更新角色的种族和备注
curl -X PUT http://localhost:8080/api/config/tag/character/65f8a1b2c3d4e5f6a7b8c9d2 \
  -H "Content-Type: application/json" \
  -d '{
    "species": "精灵",
    "remark": "更新后的备注"
  }'
```

#### 6.5.3 查询原作的所有角色

```bash
# 查询指定原作的所有角色
curl -X GET http://localhost:8080/api/config/tag/character/work/65f8a1b2c3d4e5f6a7b8c9d0
```

#### 6.5.4 搜索和过滤角色

```bash
# 搜索名称或别名包含"角色"的角色
curl -X GET "http://localhost:8080/api/config/tag/character/page?keyword=角色"

# 按原作过滤角色
curl -X GET "http://localhost:8080/api/config/tag/character/page?workId=65f8a1b2c3d4e5f6a7b8c9d0"

# 按种族过滤角色
curl -X GET "http://localhost:8080/api/config/tag/character/page?species=人类"

# 组合过滤：搜索指定原作中的人类角色
curl -X GET "http://localhost:8080/api/config/tag/character/page?workId=65f8a1b2c3d4e5f6a7b8c9d0&species=人类"

# 分页查询所有角色，第1页，每页20条
curl -X GET "http://localhost:8080/api/config/tag/character/page?current=1&size=20"
```

#### 6.5.5 删除角色

```bash
# 普通删除（会检查引用关系）
curl -X DELETE http://localhost:8080/api/config/tag/character/65f8a1b2c3d4e5f6a7b8c9d2

# 强制删除（忽略引用关系，自动清理引用）
curl -X DELETE "http://localhost:8080/api/config/tag/character/65f8a1b2c3d4e5f6a7b8c9d2?force=true"
```

#### 6.5.6 导入导出角色数据

```bash
# 导出所有角色数据
curl -X GET http://localhost:8080/api/config/tag/character/export > characters.json

# 导入角色数据
curl -X POST http://localhost:8080/api/config/tag/character/import \
  -H "Content-Type: application/json" \
  -d @characters.json
```

### 6.6 注意事项

#### 6.6.1 唯一性约束

1. **全局唯一性**：角色名称在作者库、原作库、角色库之间必须全局唯一
2. **别名唯一性**：角色别名在作者库、原作库、角色库之间必须全局唯一
3. **名称与别名交叉验证**：角色名称不能与其他实体的别名冲突，角色别名不能与其他实体的名称冲突
4. **冲突信息**：唯一性冲突时，错误信息会明确指出冲突的实体类型和ID

#### 6.6.2 原作引用验证

1. **引用有效性**：创建或更新角色时，必须验证所属原作ID在原作库中存在
2. **引用错误**：如果原作ID不存在，操作会被拒绝并返回DATA_NOT_FOUND错误
3. **原作删除影响**：如果原作被删除（强制删除），相关角色的workId会被清理

#### 6.6.3 引用完整性

1. **引用检查**：删除角色前会检查是否被过滤配置引用
2. **引用信息**：如果存在引用，错误响应会列出所有引用该角色的配置ID
3. **强制删除**：使用 `force=true` 参数可以强制删除，系统会自动清理所有引用关系
4. **引用清理**：强制删除时，会从过滤配置中移除对该角色的所有引用

#### 6.6.4 数据验证

1. **必填字段**：name、aliases 和 workId 是必填字段，aliases 可以是空列表但不能为null
2. **长度限制**：
   - name: 最大100字符
   - aliases: 每个别名最大100字符
   - species: 最大100字符
   - remark: 最大1000字符
3. **空值处理**：可选字段可以为null或不提供

#### 6.6.5 查询功能

1. **多路径查询**：支持通过ID、名称、别名、所属原作ID查询角色
2. **关键词搜索**：keyword 参数支持模糊匹配名称和别名
3. **多维度过滤**：支持按原作、种族、关键词组合过滤
4. **分页查询**：支持分页和多种过滤条件组合使用
5. **查询一致性**：通过不同路径查询同一角色应返回相同的数据

#### 6.6.6 导入导出

1. **JSON格式**：导入导出使用标准JSON格式
2. **数据完整性**：导入时会验证数据格式和完整性
3. **唯一性验证**：导入时会检查名称和别名的唯一性约束
4. **原作引用验证**：导入时会验证所有workId的有效性
5. **冲突处理**：导入时如果数据冲突，会跳过该条记录并在响应中返回错误信息
6. **批量操作**：导入支持批量创建多个角色

#### 6.6.7 时间戳管理

1. **createTime**：创建时自动设置，不可修改
2. **updateTime**：每次更新时自动更新为当前时间
3. **时间格式**：ISO 8601 格式（如 `2024-03-20T10:30:00`）

#### 6.6.8 部分更新

1. **可选字段**：更新时只需提供需要修改的字段
2. **字段保留**：未提供的字段保持原值不变
3. **空值更新**：如果需要清空某个字段，可以显式传递null

#### 6.6.9 错误处理

1. **HTTP 状态码**：所有响应的 HTTP 状态码均为 200
2. **错误判断**：通过响应体中的 code 字段判断是否成功
3. **错误信息**：错误响应包含详细的错误描述
4. **参数校验**：所有参数都经过严格校验

#### 6.6.10 workName 冗余字段

1. **自动填充**：查询角色时，workName 字段会自动从原作库中获取并填充
2. **展示便利**：workName 是冗余字段，方便前端直接展示，无需额外查询原作信息
3. **不可修改**：workName 不能通过API直接修改，只能通过修改workId间接改变

---

## 7. 标签过滤配置 API

### 7.1 概述

标签过滤配置 API 提供了全局标签过滤规则的管理功能。系统采用全局配置模式，所有监控频道共享同一套标签过滤规则。标签按照类型分类存储（作者、角色、原作、自定义），并通过标签展开机制将标签 ID 转换为实际的标签字符串列表。

主要功能包括：
- 全局配置管理（创建/更新/查询）
- 标签分类存储（作者、角色、原作、自定义）
- 标签展开测试接口

### 7.2 数据结构

#### 7.2.1 TagFilterConfigCreateDTO（创建配置请求）

用于创建或更新全局配置。

```json
{
  "authorIds": ["author_id_1", "author_id_2"],
  "characterIds": ["char_id_1", "char_id_2"],
  "workIds": ["work_id_1", "work_id_2"],
  "customTags": {
    "custom_1": "自定义标签1",
    "custom_2": "自定义标签2"
  },
  "matchMode": "whitelist",
  "enabled": true
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| authorIds | List\<String\> | 是 | 不能为 null | 作者标签 ID 列表（可以为空列表） |
| characterIds | List\<String\> | 是 | 不能为 null | 角色标签 ID 列表（可以为空列表） |
| workIds | List\<String\> | 是 | 不能为 null | 原作标签 ID 列表（可以为空列表） |
| customTags | Map\<String, String\> | 是 | 不能为 null | 自定义标签映射（key: 自定义标签ID, value: 标签字符串） |
| matchMode | String | 是 | whitelist 或 blacklist | 匹配模式 |
| enabled | Boolean | 是 | 不能为 null | 是否启用 |

**匹配模式说明：**
- `whitelist`（白名单）：只允许包含指定标签的消息通过
- `blacklist`（黑名单）：阻止包含指定标签的消息通过

#### 7.2.2 TagFilterConfigUpdateDTO（更新配置请求）

用于更新现有配置，所有字段均为可选。

```json
{
  "authorIds": ["author_id_3"],
  "matchMode": "blacklist",
  "enabled": false
}
```

**字段说明：**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| authorIds | List\<String\> | 否 | - | 作者标签 ID 列表 |
| characterIds | List\<String\> | 否 | - | 角色标签 ID 列表 |
| workIds | List\<String\> | 否 | - | 原作标签 ID 列表 |
| customTags | Map\<String, String\> | 否 | - | 自定义标签映射 |
| matchMode | String | 否 | whitelist 或 blacklist | 匹配模式 |
| enabled | Boolean | 否 | - | 是否启用 |

#### 7.2.3 TagFilterConfigVO（配置响应对象）

返回给客户端的配置数据。

```json
{
  "id": "65f8a1b2c3d4e5f6a7b8c9d0",
  "authorIds": ["author_id_1", "author_id_2"],
  "characterIds": ["char_id_1", "char_id_2"],
  "workIds": ["work_id_1", "work_id_2"],
  "customTags": {
    "custom_1": "自定义标签1",
    "custom_2": "自定义标签2"
  },
  "matchMode": "whitelist",
  "enabled": true,
  "createTime": "2024-03-20T10:30:00",
  "updateTime": "2024-03-20T10:30:00"
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | MongoDB 文档 ID |
| authorIds | List\<String\> | 作者标签 ID 列表 |
| characterIds | List\<String\> | 角色标签 ID 列表 |
| workIds | List\<String\> | 原作标签 ID 列表 |
| customTags | Map\<String, String\> | 自定义标签映射 |
| matchMode | String | 匹配模式（whitelist 或 blacklist） |
| enabled | Boolean | 是否启用 |
| createTime | String | 创建时间（ISO 8601 格式） |
| updateTime | String | 更新时间（ISO 8601 格式） |

### 7.3 API 端点

#### 7.3.1 创建或更新全局配置

**接口地址：** `POST /api/config/tag/filter`

**请求示例：**

```json
{
  "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0", "65f8a1b2c3d4e5f6a7b8c9d1"],
  "characterIds": ["65f8a1b2c3d4e5f6a7b8c9d2"],
  "workIds": [],
  "customTags": {
    "urgent": "紧急",
    "important": "重要"
  },
  "matchMode": "whitelist",
  "enabled": true
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9e0",
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0", "65f8a1b2c3d4e5f6a7b8c9d1"],
    "characterIds": ["65f8a1b2c3d4e5f6a7b8c9d2"],
    "workIds": [],
    "customTags": {
      "urgent": "紧急",
      "important": "重要"
    },
    "matchMode": "whitelist",
    "enabled": true,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

参数校验失败：
```json
{
  "code": -40006,
  "msg": "作者标签列表不能为null",
  "data": null
}
```

匹配模式无效：
```json
{
  "code": -40006,
  "msg": "匹配模式必须是whitelist或blacklist",
  "data": null
}
```

#### 7.3.2 获取全局配置

**接口地址：** `GET /api/config/tag/filter`

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9e0",
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0"],
    "characterIds": [],
    "workIds": [],
    "customTags": {},
    "matchMode": "whitelist",
    "enabled": true,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

全局配置不存在：
```json
{
  "code": -60002,
  "msg": "全局配置不存在",
  "data": null
}
```

#### 7.3.3 更新配置

**接口地址：** `PUT /api/config/tag/filter/{id}`

**路径参数：**
- `id`: 配置的 MongoDB 文档 ID

**请求示例：**

```json
{
  "enabled": false
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9e0",
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0"],
    "characterIds": [],
    "workIds": [],
    "customTags": {},
    "matchMode": "whitelist",
    "enabled": false,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T15:45:00"
  }
}
```

**错误响应示例：**

配置不存在：
```json
{
  "code": -60002,
  "msg": "配置不存在: 65f8a1b2c3d4e5f6a7b8c9e0",
  "data": null
}
```

#### 7.3.4 通过 ID 获取配置

**接口地址：** `GET /api/config/tag/filter/{id}`

**路径参数：**
- `id`: 配置的 MongoDB 文档 ID

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "65f8a1b2c3d4e5f6a7b8c9e0",
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0"],
    "characterIds": [],
    "workIds": [],
    "customTags": {},
    "matchMode": "whitelist",
    "enabled": true,
    "createTime": "2024-03-20T10:30:00",
    "updateTime": "2024-03-20T10:30:00"
  }
}
```

**错误响应示例：**

配置不存在：
```json
{
  "code": -60002,
  "msg": "配置不存在: 65f8a1b2c3d4e5f6a7b8c9e0",
  "data": null
}
```

#### 7.3.5 展开标签（测试用）

**接口地址：** `POST /api/config/tag/filter/expand`

**说明：** 该接口用于测试标签展开功能，将配置中的所有标签 ID 展开为实际的标签字符串列表。

**请求示例：**

```json
{
  "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0"],
  "characterIds": ["65f8a1b2c3d4e5f6a7b8c9d2"],
  "workIds": [],
  "customTags": {
    "urgent": "紧急"
  },
  "matchMode": "whitelist",
  "enabled": true
}
```

**成功响应：**

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    "张三",
    "作者别名1",
    "角色名称",
    "角色别名1",
    "紧急"
  ]
}
```

**说明：**
- 作者 ID 会展开为作者名称和所有别名
- 角色 ID 会展开为角色名称和所有别名
- 原作 ID 会展开为原作名称和所有别名
- 自定义标签直接使用其值
- 返回的列表已去重

### 4.4 常见错误场景

#### 4.4.1 数据不存在（DATA_NOT_FOUND）

**响应码：** `-60002`

**触发场景：**
- 查询不存在的配置 ID
- 更新不存在的配置
- 全局配置不存在

**响应示例：**

```json
{
  "code": -60002,
  "msg": "配置不存在: 65f8a1b2c3d4e5f6a7b8c9d0",
  "data": null
}
```

#### 7.4.2 参数校验失败（VALIDATION_ERROR）

**响应码：** `-40006`

**触发场景：**
- authorIds、characterIds、workIds 或 customTags 为 null
- matchMode 不是 whitelist 或 blacklist
- enabled 为 null

**响应示例（单个错误）：**

```json
{
  "code": -40006,
  "msg": "作者标签列表不能为null",
  "data": null
}
```

**响应示例（多个错误）：**

```json
{
  "code": -40006,
  "msg": "作者标签列表不能为null; 匹配模式必须是whitelist或blacklist; 启用状态不能为null",
  "data": null
}
```

### 7.5 使用示例

#### 7.5.1 创建全局配置

```bash
# 创建全局白名单配置
curl -X POST http://localhost:8080/api/config/tag/filter \
  -H "Content-Type: application/json" \
  -d '{
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0"],
    "characterIds": [],
    "workIds": [],
    "customTags": {
      "urgent": "紧急"
    },
    "matchMode": "whitelist",
    "enabled": true
  }'
```

#### 7.5.2 更新配置

```bash
# 禁用配置
curl -X PUT http://localhost:8080/api/config/tag/filter/65f8a1b2c3d4e5f6a7b8c9e0 \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'

# 更新标签列表
curl -X PUT http://localhost:8080/api/config/tag/filter/65f8a1b2c3d4e5f6a7b8c9e0 \
  -H "Content-Type: application/json" \
  -d '{
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0", "65f8a1b2c3d4e5f6a7b8c9d1"],
    "characterIds": ["65f8a1b2c3d4e5f6a7b8c9d2"]
  }'
```

#### 7.5.3 查询配置

```bash
# 查询全局配置
curl -X GET http://localhost:8080/api/config/tag/filter

# 通过 ID 查询配置
curl -X GET http://localhost:8080/api/config/tag/filter/65f8a1b2c3d4e5f6a7b8c9e0
```

#### 7.5.4 测试标签展开

```bash
# 测试标签展开功能
curl -X POST http://localhost:8080/api/config/tag/filter/expand \
  -H "Content-Type: application/json" \
  -d '{
    "authorIds": ["65f8a1b2c3d4e5f6a7b8c9d0"],
    "characterIds": ["65f8a1b2c3d4e5f6a7b8c9d2"],
    "workIds": [],
    "customTags": {
      "urgent": "紧急"
    },
    "matchMode": "whitelist",
    "enabled": true
  }'
```

### 7.6 注意事项

#### 7.6.1 配置模式

1. **全局配置唯一**：系统中只有一个全局配置，所有监控频道共享
2. **配置创建或更新**：使用 `POST /api/config/tag/filter` 端点，如果全局配置已存在则更新，否则创建
3. **系统启动初始化**：系统启动时会自动创建默认的全局配置（如果不存在）

#### 7.6.2 标签分类存储

1. **作者标签**：存储作者库中的作者 ID 列表
2. **角色标签**：存储角色库中的角色 ID 列表
3. **原作标签**：存储原作库中的原作 ID 列表
4. **自定义标签**：存储自定义标签的 ID 和字符串映射

#### 7.6.3 标签展开机制

1. **作者展开**：作者 ID 展开为作者名称和所有别名
2. **角色展开**：角色 ID 展开为角色名称和所有别名
3. **原作展开**：原作 ID 展开为原作名称和所有别名
4. **自定义标签**：直接使用自定义标签的字符串值
5. **去重处理**：展开后的标签列表会自动去重
6. **容错处理**：如果标签 ID 在数据库中不存在，会记录警告日志并跳过

#### 7.6.4 参数验证

1. **标签列表字段**：authorIds、characterIds、workIds 不能为 null，但可以是空列表 `[]`
2. **自定义标签字段**：customTags 不能为 null，但可以是空映射 `{}`
3. **matchMode 字段**：只能是 `whitelist` 或 `blacklist`
4. **enabled 字段**：不能为 null，必须是 `true` 或 `false`

#### 7.6.5 配置管理

1. **配置更新**：使用 `PUT /api/config/tag/filter/{id}` 端点，通过 MongoDB ID 更新
2. **部分更新**：更新时只需提供需要修改的字段
3. **配置查询**：可以通过全局端点或 ID 端点查询配置

#### 7.6.6 时间戳管理

1. **createTime**：创建时自动设置，不可修改
2. **updateTime**：每次更新时自动更新为当前时间
3. **时间格式**：ISO 8601 格式（如 `2024-03-20T10:30:00`）

#### 7.6.7 错误处理

1. **HTTP 状态码**：所有响应的 HTTP 状态码均为 200
2. **错误判断**：通过响应体中的 code 字段判断是否成功
3. **错误信息**：错误响应包含详细的错误描述
4. **参数校验**：所有参数都经过严格校验

#### 7.6.8 最佳实践

1. **使用标签数据库**：建议使用作者库、原作库、角色库管理标签，而不是直接使用自定义标签
2. **测试标签展开**：使用 `/expand` 端点测试标签展开结果
3. **定期检查**：定期检查标签 ID 的有效性，清理无效的标签引用

---

## 相关文档

- [API 响应规范文档](./API%20响应规范文档.md) - 了解统一的 API 响应格式和错误处理机制
- [TelegramClientManager 使用指南](../TelegramClientManager使用指南.md) - 了解 Telegram 客户端管理器的使用方法
