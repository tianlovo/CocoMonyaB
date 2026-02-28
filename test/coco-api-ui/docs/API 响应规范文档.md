# API 响应规范文档

## 1. 概述

本项目采用统一的 JSON 响应格式，所有 API 接口都遵循相同的响应结构，以确保前后端交互的一致性和可预测性。

## 2. 响应结构

### 2.1 基础响应结构

所有 API 响应都采用以下 JSON 结构：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 响应状态码，成功为正数（通常为200），失败为负数 |
| msg | String | 响应消息，描述操作结果 |
| data | Object/null | 响应数据，成功时包含具体数据，失败时为 null |

### 2.2 分页响应结构

分页查询接口使用扩展的响应结构：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [],
    "current": 1,
    "size": 10,
    "total": 100,
    "pages": 10
  }
}
```

**data 字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| records | Array | 当前页的数据列表 |
| current | Long | 当前页码 |
| size | Long | 每页大小 |
| total | Long | 总记录数 |
| pages | Long | 总页数 |

## 3. 响应码规范

### 3.1 响应码规则

- **成功响应**：code 为正数，通常为 200
- **失败响应**：code 为负数，按错误类型分类

### 3.2 标准响应码

| 响应码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 操作成功 | 所有成功的操作 |
| -40000 | 请求参数错误 | 参数格式不正确、缺少必需参数等 |
| -40001 | 未授权 | 未登录或 token 无效 |
| -40003 | 禁止访问 | 没有权限访问该资源 |
| -40004 | 资源不存在 | 请求的资源不存在 |
| -40005 | 请求方法不允许 | HTTP 方法不支持 |
| -40006 | 参数校验失败 | 参数验证不通过 |
| -50000 | 服务器内部错误 | 系统异常 |
| -50003 | 服务暂时不可用 | 服务维护或过载 |
| -60000 | 业务处理失败 | 通用业务错误 |
| -60001 | Telegram操作失败 | Telegram 相关操作失败 |
| -60002 | 数据不存在 | 查询的数据不存在 |
| -60003 | 数据已存在 | 创建时数据已存在 |
| -60004 | 操作失败 | 其他操作失败 |

## 4. 响应示例

### 4.1 成功响应（无数据）

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 4.2 成功响应（带数据）

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "name": "测试数据",
    "timestamp": 1708588800000
  }
}
```

### 4.3 失败响应

```json
{
  "code": -60000,
  "msg": "业务处理失败",
  "data": null
}
```

### 4.4 参数校验失败响应

```json
{
  "code": -40006,
  "msg": "用户名不能为空; 年龄不能为空",
  "data": null
}
```

### 4.5 分页响应

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "测试数据 1",
        "description": "这是第 1 条测试数据"
      },
      {
        "id": 2,
        "name": "测试数据 2",
        "description": "这是第 2 条测试数据"
      }
    ],
    "current": 1,
    "size": 10,
    "total": 100,
    "pages": 10
  }
}
```

## 5. 异常处理机制

### 5.1 业务异常

所有业务异常都继承自 `BusinessException` 类，通过全局异常处理器统一捕获并返回规范化的 JSON 响应。

**使用示例：**

```java
// 抛出业务异常
throw new BusinessException(ResponseCode.BUSINESS_ERROR, "操作失败");

// 使用预定义的响应码
throw new BusinessException(ResponseCode.TELEGRAM_ERROR);
```

### 5.2 系统异常

系统异常（如 NullPointerException、IOException 等）会被全局异常处理器捕获，统一返回：

```json
{
  "code": -50000,
  "msg": "系统内部错误，请联系管理员",
  "data": null
}
```

### 5.3 参数校验异常

使用 Spring Validation 进行参数校验，校验失败时自动返回：

```json
{
  "code": -40006,
  "msg": "参数校验失败的具体原因",
  "data": null
}
```

## 6. 开发指南

### 7.1 返回成功响应

```java
// 无数据
return ApiResponse.success();

// 带数据
return ApiResponse.success(data);

// 自定义消息
return ApiResponse.success("创建成功", data);
```

### 7.2 返回失败响应

```java
// 使用预定义响应码
return ApiResponse.error(ResponseCode.BUSINESS_ERROR);

// 自定义消息
return ApiResponse.error(ResponseCode.BUSINESS_ERROR, "具体错误原因");

// 完全自定义
return ApiResponse.error(-99999, "自定义错误");
```

### 7.3 抛出业务异常

```java
// 使用预定义响应码
throw new BusinessException(ResponseCode.DATA_NOT_FOUND);

// 自定义消息
throw new BusinessException(ResponseCode.BUSINESS_ERROR, "用户不存在");

// 完全自定义
throw new BusinessException(-99999, "自定义业务异常");
```

### 7.4 返回分页响应

```java
// 成功分页
return PageResponse.success(records, current, size, total);

// 空分页
return PageResponse.empty(current, size);
```

### 7.5 DTO 和 VO 规范

#### 请求对象（DTO - Data Transfer Object）
- **位置**：`domain.dto` 包下
- **命名**：以 `DTO` 结尾，如 `UserCreateDTO`、`UserUpdateDTO`
- **用途**：接收客户端请求参数
- **特点**：包含参数校验注解

```java
package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄必须大于0")
    private Integer age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

#### 响应对象（VO - View Object）
- **位置**：`domain.vo` 包下
- **命名**：以 `VO` 结尾，如 `UserVO`、`UserDetailVO`
- **用途**：返回给客户端的数据
- **特点**：只包含需要展示的字段，不包含敏感信息

```java
package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private Integer age;
    private String email;
    private LocalDateTime createTime;
    // 不包含密码等敏感字段
}
```

#### 使用示例

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @PostMapping
    public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserVO vo = userService.create(dto);
        return ApiResponse.success(vo);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        UserVO vo = userService.getById(id);
        return ApiResponse.success(vo);
    }
    
    @GetMapping("/page")
    public PageResponse<UserVO> pageUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        List<UserVO> records = userService.page(current, size);
        Long total = userService.count();
        return PageResponse.success(records, current, size, total);
    }
}
```

## 8. 注意事项

1. **错误码一致性**：所有错误响应的 code 必须为负数
2. **data 字段**：失败响应时 data 必须为 null
3. **异常处理**：业务逻辑中应抛出 BusinessException，而不是直接返回错误响应
4. **参数校验**：使用 Spring Validation 注解进行参数校验
5. **日志记录**：异常处理器会自动记录日志，业务代码中也应适当记录
6. **HTTP 状态码**：除特殊情况外，HTTP 状态码通常为 200，具体错误通过响应体中的 code 字段表示

## 9. 扩展说明

### 9.1 添加新的响应码

在 `ResponseCode` 枚举中添加新的响应码：

```java
NEW_ERROR(-60005, "新的错误类型");
```

### 9.2 创建特定业务异常

继承 `BusinessException` 创建特定业务异常：

```java
public class TelegramException extends BusinessException {
    public TelegramException(String message) {
        super(ResponseCode.TELEGRAM_ERROR, message);
    }
}
```

### 9.3 自定义响应结构

如需特殊的响应结构，可以继承 `ApiResponse` 类进行扩展。

## 10. 相关文档

- [频道管理 API 文档](./api.md) - 查看频道管理相关的 API 接口文档
