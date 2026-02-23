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

## 相关文档

- [API 响应规范文档](./API%20响应规范文档.md) - 了解统一的 API 响应格式和错误处理机制
- [TelegramClientManager 使用指南](../TelegramClientManager使用指南.md) - 了解 Telegram 客户端管理器的使用方法
