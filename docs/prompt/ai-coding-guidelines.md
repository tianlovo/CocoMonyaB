# AI 编码规范 - 响应规范化开发约束

## 文档说明

本文档为大模型（AI）编写代码时提供响应规范化开发的约束和指导，确保生成的代码符合项目的统一响应标准。

---

## 1. 核心原则

### 1.1 统一响应结构
- **所有 API 接口必须返回 `ApiResponse<T>` 或 `PageResponse<T>` 类型**
- **禁止直接返回业务对象或基本类型**
- **禁止返回 Spring 的 `ResponseEntity` 包装响应体**

### 1.2 错误处理原则
- **禁止在 Controller 中使用 try-catch 返回错误响应**
- **必须通过抛出 `BusinessException` 来处理业务错误**
- **依赖全局异常处理器统一处理异常并返回规范化响应**

### 1.3 响应码规则
- **成功响应：code 必须为正数（通常为 200）**
- **失败响应：code 必须为负数**
- **必须使用 `ResponseCode` 枚举定义的响应码**
- **禁止硬编码响应码数值**

### 1.4 DTO 和 VO 规范
- **请求参数必须使用 DTO（Data Transfer Object）**
  - 存放位置：`domain.dto` 包下
  - 命名规则：以 `DTO` 结尾，如 `UserCreateDTO`、`UserUpdateDTO`、`UserQueryDTO`
  - 必须包含参数校验注解（`@NotNull`、`@NotBlank`、`@Valid` 等）
  - 用途：接收客户端请求参数
  
- **响应数据必须使用 VO（View Object）**
  - 存放位置：`domain.vo` 包下
  - 命名规则：以 `VO` 结尾，如 `UserVO`、`UserDetailVO`、`UserListVO`
  - 只包含需要展示的字段，不包含敏感信息（如密码、token、内部ID等）
  - 用途：返回给客户端的数据视图

- **禁止直接使用实体类（Entity）作为请求或响应对象**
- **禁止在 Controller 中直接使用 Entity、DO 等持久化对象**

---

## 2. Controller 层开发规范

### 2.1 DTO 和 VO 使用规范

#### ✅ 正确示例

```java
package org.xlyo.cocomonyab.controller;

import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.UserCreateDTO;
import org.xlyo.cocomonyab.domain.dto.UserUpdateDTO;
import org.xlyo.cocomonyab.domain.dto.UserQueryDTO;
import org.xlyo.cocomonyab.domain.vo.UserVO;
import org.xlyo.cocomonyab.domain.vo.UserDetailVO;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    // 创建 - 使用 DTO 接收，返回 VO
    @PostMapping
    public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserVO vo = userService.create(dto);
        return ApiResponse.success(vo);
    }
    
    // 更新 - 使用 DTO 接收，返回 VO
    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO dto) {
        UserVO vo = userService.update(id, dto);
        return ApiResponse.success(vo);
    }
    
    // 查询单个 - 返回详细 VO
    @GetMapping("/{id}")
    public ApiResponse<UserDetailVO> getUser(@PathVariable Long id) {
        UserDetailVO vo = userService.getById(id);
        return ApiResponse.success(vo);
    }
    
    // 查询列表 - 返回简化 VO
    @GetMapping("/list")
    public ApiResponse<List<UserVO>> listUsers(@Valid UserQueryDTO dto) {
        List<UserVO> list = userService.list(dto);
        return ApiResponse.success(list);
    }
    
    // 分页查询 - 返回 VO
    @GetMapping("/page")
    public PageResponse<UserVO> pageUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid UserQueryDTO dto) {
        List<UserVO> records = userService.page(current, size, dto);
        Long total = userService.count(dto);
        return PageResponse.success(records, current, size, total);
    }
    
    // 删除 - 无返回数据
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ApiResponse.success();
    }
}
```

#### DTO 示例

```java
package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户创建请求 DTO
 */
@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄必须大于0")
    @Max(value = 150, message = "年龄必须小于150")
    private Integer age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

/**
 * 用户更新请求 DTO
 */
@Data
public class UserUpdateDTO {
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @Min(value = 1, message = "年龄必须大于0")
    @Max(value = 150, message = "年龄必须小于150")
    private Integer age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

/**
 * 用户查询请求 DTO
 */
@Data
public class UserQueryDTO {
    private String username;
    private Integer minAge;
    private Integer maxAge;
}
```

#### VO 示例

```java
package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户基础信息 VO（用于列表）
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private Integer age;
    private LocalDateTime createTime;
    // 不包含敏感字段：密码、token等
}

/**
 * 用户详细信息 VO（用于详情）
 */
@Data
public class UserDetailVO {
    private Long id;
    private String username;
    private Integer age;
    private String email;
    private String phone;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 不包含敏感字段：密码、token等
}
```

#### ❌ 错误示例

```java
// 错误：直接使用 Entity 作为请求参数
@PostMapping
public ApiResponse<User> createUser(@RequestBody User user) {
    return ApiResponse.success(userService.create(user));
}

// 错误：直接返回 Entity
@GetMapping("/{id}")
public ApiResponse<User> getUser(@PathVariable Long id) {
    return ApiResponse.success(userService.getById(id));
}

// 错误：使用 Map 接收参数
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody Map<String, Object> params) {
    return ApiResponse.success(userService.create(params));
}

// 错误：返回 Entity 列表
@GetMapping("/list")
public ApiResponse<List<User>> listUsers() {
    return ApiResponse.success(userRepository.findAll());
}

// 错误：DTO 命名不规范
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody UserRequest request) {
    // 应该命名为 UserCreateDTO
}

// 错误：VO 命名不规范
@GetMapping("/{id}")
public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
    // 应该命名为 UserVO 或 UserDetailVO
}
```

### 2.2 方法返回类型

#### ✅ 正确示例

```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    // 返回单个对象
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        UserVO vo = userService.getById(id);
        return ApiResponse.success(vo);
    }
    
    // 返回列表
    @GetMapping("/list")
    public ApiResponse<List<UserVO>> listUsers() {
        List<UserVO> users = userService.list();
        return ApiResponse.success(users);
    }
    
    // 返回分页数据
    @GetMapping("/page")
    public PageResponse<UserVO> pageUsers(
            @RequestParam Long current, 
            @RequestParam Long size) {
        List<UserVO> records = userService.page(current, size);
        Long total = userService.count();
        return PageResponse.success(records, current, size, total);
    }
    
    // 无返回数据
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ApiResponse.success();
    }
}
```

#### ❌ 错误示例

```java
// 错误：直接返回业务对象
@GetMapping("/{id}")
public UserVO getUser(@PathVariable Long id) {
    return userService.getById(id);
}

// 错误：返回 ResponseEntity
@GetMapping("/{id}")
public ResponseEntity<UserVO> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getById(id));
}

// 错误：返回 Map
@GetMapping("/{id}")
public Map<String, Object> getUser(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    result.put("code", 200);
    result.put("data", userService.getById(id));
    return result;
}
```

### 2.3 异常处理

### 2.3 异常处理

#### ✅ 正确示例

```java
@PostMapping
public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
    // 业务校验失败 - 抛出异常
    if (userService.existsByUsername(dto.getUsername())) {
        throw new BusinessException(ResponseCode.DATA_ALREADY_EXISTS, "用户名已存在");
    }
    
    // 业务操作失败 - 抛出异常
    UserVO vo = userService.create(dto);
    if (vo == null) {
        throw new BusinessException(ResponseCode.OPERATION_FAILED, "创建用户失败");
    }
    
    return ApiResponse.success(vo);
}

@GetMapping("/{id}")
public ApiResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO vo = userService.getById(id);
    
    // 数据不存在 - 抛出异常
    if (vo == null) {
        throw new BusinessException(ResponseCode.DATA_NOT_FOUND, "用户不存在");
    }
    
    return ApiResponse.success(vo);
}
```

#### ❌ 错误示例

```java
// 错误：使用 try-catch 返回错误响应
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody UserCreateDTO dto) {
    try {
        UserVO vo = userService.create(dto);
        return ApiResponse.success(vo);
    } catch (Exception e) {
        return ApiResponse.error(ResponseCode.BUSINESS_ERROR, e.getMessage());
    }
}

// 错误：直接返回错误响应
@GetMapping("/{id}")
public ApiResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO vo = userService.getById(id);
    if (vo == null) {
        return ApiResponse.error(ResponseCode.DATA_NOT_FOUND);
    }
    return ApiResponse.success(vo);
}
```

---

## 3. Service 层开发规范

### 3.1 DTO 和 VO 转换

#### ✅ 正确示例

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 根据ID查询用户
     * Entity -> VO
     */
    public UserVO getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, 
                        "用户不存在: " + id));
        
        // Entity 转 VO
        return convertToVO(user);
    }
    
    /**
     * 创建用户
     * DTO -> Entity -> VO
     */
    public UserVO create(UserCreateDTO dto) {
        // 业务校验
        if (existsByUsername(dto.getUsername())) {
            throw new BusinessException(
                    ResponseCode.DATA_ALREADY_EXISTS, 
                    "用户名已存在: " + dto.getUsername());
        }
        
        // DTO 转 Entity
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setAge(dto.getAge());
        user.setEmail(dto.getEmail());
        
        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            throw new BusinessException(
                    ResponseCode.OPERATION_FAILED, 
                    "创建用户失败", e);
        }
        
        // Entity 转 VO
        return convertToVO(user);
    }
    
    /**
     * 更新用户
     * DTO -> Entity -> VO
     */
    public UserVO update(Long id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND, 
                        "用户不存在"));
        
        // 更新字段
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getAge() != null) {
            user.setAge(dto.getAge());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        
        user = userRepository.save(user);
        return convertToVO(user);
    }
    
    /**
     * 分页查询
     * Entity List -> VO List
     */
    public List<UserVO> page(Long current, Long size, UserQueryDTO dto) {
        // 构建查询条件
        // ...
        List<User> users = userRepository.findAll();
        
        // Entity List 转 VO List
        return users.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * Entity 转 VO 的转换方法
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAge(user.getAge());
        vo.setCreateTime(user.getCreateTime());
        // 不设置敏感字段
        return vo;
    }
    
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException(
                    ResponseCode.DATA_NOT_FOUND, 
                    "用户不存在");
        }
        userRepository.deleteById(id);
    }
}
```

#### ❌ 错误示例

```java
// 错误：返回 Entity 而不是 VO
public User getById(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                    ResponseCode.DATA_NOT_FOUND, 
                    "用户不存在"));
}

// 错误：接收 Entity 而不是 DTO
public User create(User user) {
    return userRepository.save(user);
}

// 错误：返回 null 而不是抛出异常
public UserVO getById(Long id) {
    return userRepository.findById(id)
            .map(this::convertToVO)
            .orElse(null);
}

// 错误：返回布尔值表示成功失败
public boolean create(UserCreateDTO dto) {
    try {
        User user = new User();
        user.setUsername(dto.getUsername());
        userRepository.save(user);
        return true;
    } catch (Exception e) {
        return false;
    }
}

// 错误：吞掉异常不处理
public void deleteById(Long id) {
    try {
        userRepository.deleteById(id);
    } catch (Exception e) {
        // 什么都不做
    }
}
```

### 3.2 异常抛出

---

## 4. 响应码使用规范

### 4.1 使用预定义响应码

#### ✅ 正确示例

```java
// 使用枚举
throw new BusinessException(ResponseCode.DATA_NOT_FOUND);
throw new BusinessException(ResponseCode.DATA_ALREADY_EXISTS);
throw new BusinessException(ResponseCode.TELEGRAM_ERROR);

// 使用枚举 + 自定义消息
throw new BusinessException(ResponseCode.BUSINESS_ERROR, "具体的业务错误描述");
```

#### ❌ 错误示例

```java
// 错误：硬编码响应码
throw new BusinessException(-60002, "数据不存在");
return ApiResponse.error(-40000, "参数错误");

// 错误：使用字符串作为响应码
throw new BusinessException("DATA_NOT_FOUND", "数据不存在");
```

### 4.2 响应码选择指南

| 场景 | 使用的响应码 | 示例 |
|------|-------------|------|
| 数据不存在 | `ResponseCode.DATA_NOT_FOUND` | 查询用户不存在 |
| 数据已存在 | `ResponseCode.DATA_ALREADY_EXISTS` | 用户名重复 |
| 通用业务错误 | `ResponseCode.BUSINESS_ERROR` | 业务规则校验失败 |
| 操作失败 | `ResponseCode.OPERATION_FAILED` | 创建/更新/删除失败 |
| Telegram 相关 | `ResponseCode.TELEGRAM_ERROR` | TG API 调用失败 |
| 参数错误 | 由全局异常处理器自动处理 | 使用 `@Valid` 注解 |

---

## 5. 参数校验规范

### 5.1 使用 Validation 注解

#### ✅ 正确示例

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
    @Max(value = 150, message = "年龄必须小于150")
    private Integer age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

@PostMapping
public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
    // 参数校验失败会自动被全局异常处理器捕获
    UserVO vo = userService.create(dto);
    return ApiResponse.success(vo);
}
```

#### ❌ 错误示例

```java
// 错误：手动校验参数并返回错误响应
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody UserCreateDTO dto) {
    if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
        return ApiResponse.error(ResponseCode.VALIDATION_ERROR, "用户名不能为空");
    }
    if (dto.getAge() == null) {
        return ApiResponse.error(ResponseCode.VALIDATION_ERROR, "年龄不能为空");
    }
    // ...
}
```

---

## 6. 分页查询规范

### 6.1 分页响应

#### ✅ 正确示例

```java
@GetMapping("/page")
public PageResponse<UserVO> pageUsers(
        @RequestParam(defaultValue = "1") Long current,
        @RequestParam(defaultValue = "10") Long size) {
    
    // 查询数据
    List<UserVO> records = userService.page(current, size);
    Long total = userService.count();
    
    // 返回分页响应
    return PageResponse.success(records, current, size, total);
}

// 空数据场景
@GetMapping("/page")
public PageResponse<UserVO> pageUsers(
        @RequestParam(defaultValue = "1") Long current,
        @RequestParam(defaultValue = "10") Long size) {
    
    List<UserVO> records = userService.page(current, size);
    if (records.isEmpty()) {
        return PageResponse.empty(current, size);
    }
    
    Long total = userService.count();
    return PageResponse.success(records, current, size, total);
}
```

#### ❌ 错误示例

```java
// 错误：使用 ApiResponse 返回分页数据
@GetMapping("/page")
public ApiResponse<Map<String, Object>> pageUsers(
        @RequestParam Long current,
        @RequestParam Long size) {
    Map<String, Object> result = new HashMap<>();
    result.put("records", userService.page(current, size));
    result.put("total", userService.count());
    return ApiResponse.success(result);
}

// 错误：自定义分页结构
@GetMapping("/page")
public ApiResponse<PageResult<UserVO>> pageUsers(
        @RequestParam Long current,
        @RequestParam Long size) {
    PageResult<UserVO> result = new PageResult<>();
    // ...
    return ApiResponse.success(result);
}
```

---

## 7. 代码生成检查清单

在生成 Controller 代码时，请确保：

- [ ] 所有方法返回类型为 `ApiResponse<T>` 或 `PageResponse<T>`
- [ ] 请求参数使用 DTO（存放在 `domain.dto` 包，以 `DTO` 结尾）
- [ ] 响应数据使用 VO（存放在 `domain.vo` 包，以 `VO` 结尾）
- [ ] 没有直接使用 Entity、DO 等持久化对象作为请求或响应
- [ ] 没有使用 try-catch 返回错误响应
- [ ] 业务错误通过抛出 `BusinessException` 处理
- [ ] 使用 `ResponseCode` 枚举定义的响应码
- [ ] 参数校验使用 `@Valid` 和 Validation 注解
- [ ] 分页查询使用 `PageResponse` 返回
- [ ] 没有硬编码响应码数值
- [ ] DTO 包含完整的参数校验注解
- [ ] VO 不包含敏感信息（密码、token等）

---

## 8. 常见错误模式识别

### 8.1 反模式：Controller 中的 try-catch

```java
// ❌ 不要这样做
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody UserCreateDTO dto) {
    try {
        UserVO vo = userService.create(dto);
        return ApiResponse.success(vo);
    } catch (BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    } catch (Exception e) {
        return ApiResponse.error(ResponseCode.INTERNAL_ERROR);
    }
}

// ✅ 应该这样做
@PostMapping
public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
    UserVO vo = userService.create(dto);
    return ApiResponse.success(vo);
}
```

### 8.2 反模式：条件判断返回错误

```java
// ❌ 不要这样做
@GetMapping("/{id}")
public ApiResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO vo = userService.getById(id);
    if (vo == null) {
        return ApiResponse.error(ResponseCode.DATA_NOT_FOUND);
    }
    return ApiResponse.success(vo);
}

// ✅ 应该这样做
@GetMapping("/{id}")
public ApiResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO vo = userService.getById(id);
    // Service 层会在数据不存在时抛出异常
    return ApiResponse.success(vo);
}
```

### 8.3 反模式：手动参数校验

```java
// ❌ 不要这样做
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody UserCreateDTO dto) {
    if (StringUtils.isBlank(dto.getUsername())) {
        throw new BusinessException(ResponseCode.VALIDATION_ERROR, "用户名不能为空");
    }
    if (dto.getAge() == null || dto.getAge() < 1) {
        throw new BusinessException(ResponseCode.VALIDATION_ERROR, "年龄无效");
    }
    // ...
}

// ✅ 应该这样做
@PostMapping
public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
    // 使用 @Valid 和 DTO 中的 Validation 注解自动校验
    UserVO vo = userService.create(dto);
    return ApiResponse.success(vo);
}
```

### 8.4 反模式：直接使用 Entity

```java
// ❌ 不要这样做
@PostMapping
public ApiResponse<User> createUser(@RequestBody User user) {
    return ApiResponse.success(userRepository.save(user));
}

@GetMapping("/{id}")
public ApiResponse<User> getUser(@PathVariable Long id) {
    return ApiResponse.success(userRepository.findById(id).orElse(null));
}

// ✅ 应该这样做
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
```

### 8.5 反模式：DTO/VO 命名不规范

```java
// ❌ 不要这样做
public class UserRequest { }  // 应该是 UserCreateDTO 或 UserUpdateDTO
public class UserResponse { }  // 应该是 UserVO
public class UserInfo { }  // 应该是 UserVO
public class UserParam { }  // 应该是 UserQueryDTO

// ✅ 应该这样做
public class UserCreateDTO { }  // 创建请求
public class UserUpdateDTO { }  // 更新请求
public class UserQueryDTO { }  // 查询请求
public class UserVO { }  // 基础响应
public class UserDetailVO { }  // 详细响应
```

---

## 9. 特殊场景处理

### 9.1 文件下载

```java
// 文件下载不使用统一响应格式
@GetMapping("/download/{id}")
public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
    Resource file = fileService.loadAsResource(id);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + file.getFilename() + "\"")
            .body(file);
}
```

### 9.2 重定向

```java
// 重定向不使用统一响应格式
@GetMapping("/redirect")
public String redirect() {
    return "redirect:/api/user/list";
}
```

### 9.3 SSE

```java
// SSE 不使用统一响应格式
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamEvents() {
    return eventService.getEventStream();
}
```

---

## 10. 总结

### 核心要点

1. **统一返回类型**：`ApiResponse<T>` 或 `PageResponse<T>`
2. **DTO 和 VO 分离**：请求用 DTO（`domain.dto`），响应用 VO（`domain.vo`）
3. **异常驱动**：通过抛出 `BusinessException` 处理错误
4. **枚举响应码**：使用 `ResponseCode` 枚举
5. **自动校验**：使用 `@Valid` 和 Validation 注解
6. **全局处理**：依赖全局异常处理器统一处理
7. **禁用 Entity**：Controller 和 Service 对外接口禁止使用 Entity

### 记住这些原则

> **"成功返回 ApiResponse，失败抛出 BusinessException"**
> 
> **"请求用 DTO，响应用 VO，禁用 Entity"**
> 
> **"DTO 在 domain.dto，VO 在 domain.vo"**

这是本项目响应规范化开发的核心原则。
