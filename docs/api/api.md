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
| channelUsername | String | 是 | 不能为空，长度 1-100 | 频道用户名 |
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
| channelUsername | String | 否 | 长度 1-100 | 频道用户名 |
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

## 相关文档

- [API 响应规范文档](./API%20响应规范文档.md) - 了解统一的 API 响应格式和错误处理机制
