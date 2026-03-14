# 标签管理系统前端

基于 Vue3 + TypeScript + Element Plus 的标签管理系统前端应用。

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **编程语言**: TypeScript
- **UI 组件库**: Element Plus
- **HTTP 客户端**: Axios
- **路由管理**: Vue Router
- **状态管理**: Pinia
- **构建工具**: Vite
- **包管理器**: pnpm

## 项目结构

```
├── src/
│   ├── api/              # API 接口定义
│   ├── assets/           # 静态资源
│   │   └── styles/       # 全局样式
│   ├── composables/      # 组合式函数
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── server/               # Node.js轻量级后端
│   ├── src/              # 服务端源码
│   ├── package.json
│   └── README.md
├── index.html            # HTML 模板
├── vite.config.ts        # Vite 配置
├── tsconfig.json         # TypeScript 配置
├── package.json          # 项目依赖
├── pnpm-workspace.yaml   # pnpm工作区配置
├── start-server.bat      # 服务端启动脚本(Windows)
└── start-server.ps1      # 服务端启动脚本(PowerShell)
```

## 环境要求

- Node.js >= 18.0.0
- pnpm >= 9.0.0

## 快速开始

### 1. 安装依赖

在项目根目录执行：

```bash
pnpm install
```

这将同时安装前端和后端的依赖。

### 2. 启动Node.js后端服务

后端服务提供API转发、监控告警和Token认证功能。

```bash
# 方式1: 使用启动脚本
start-server.bat

# 方式2: 手动启动
cd server
pnpm install
pnpm dev
```

后端服务将在 http://localhost:10722 启动。

### 3. 启动前端开发服务器

```bash
pnpm dev
```

前端将在 http://localhost:5173 启动。

### 4. 登录前端

首次访问前端页面时，需要使用Token登录：
- 默认Token: `coco-api-ui-token`
- 可在后端配置文件中修改

## 开发指南

### 安装依赖

```bash
# 安装所有工作区依赖
pnpm install

# 仅安装前端依赖
pnpm --filter . install

# 仅安装后端依赖
pnpm --filter server install
```

### 启动开发服务器

```bash
# 启动前端
pnpm dev

# 启动后端
cd server && pnpm dev
```

开发服务器将在 http://localhost:5173 启动

### 构建生产版本

```bash
# 构建前端
pnpm build

# 构建后端
cd server && pnpm build
```

### 运行测试

```bash
pnpm test
```

## 配置说明

### 后端 API 地址

前端默认连接Node.js后端地址为 `http://127.0.0.1:10722`，可在 `src/utils/request.ts` 中修改。

### 开发服务器端口

开发服务器端口为 5173，可在 `vite.config.ts` 中修改。

## 功能模块

- **作者库管理**: 管理作者信息，包括名称、别名、个性签名等
- **原作库管理**: 管理原作信息，包括名称、别名等
- **角色库管理**: 管理角色信息，包括名称、别名、所属原作、种族等
- **标签过滤配置**: 配置全局标签过滤规则
- **系统状态监控**: 实时监控系统启动就绪状态
- **系统信息**: 查看系统版本、构建信息和运行状态
- **后端配置**: 配置Node.js后端服务参数、Bark通知、监控设置

## Node.js后端功能

### 1. API转发
- 将所有前端API请求转发到Java后端
- 保持Java后端无需修改，功能完全一致
- 支持所有HTTP方法（GET、POST、PUT、DELETE等）

### 2. 监控告警
- **Java后端掉线检测**：每30分钟检查一次Java后端状态
- **TG登录态检测**：每小时使用`forceRefresh`参数检查TG登录态，连续失败3次则判定为失效
- **Bark通知**：异常时通过Bark推送通知到手机

### 3. Token认证
- 前端页面需要Token认证才能访问
- Token在 `server/config.yaml` 中配置
- 支持登录页面和路由守卫

### 4. 可视化配置
- 在前端"后端配置"页面进行配置
- 可配置Bark通知、监控参数、服务端点等
- 实时查看监控状态

## 系统启动就绪机制

前端已适配后端的系统启动就绪机制，确保在后端所有关键组件初始化完成后才显示应用界面。

### 功能特性

1. **启动检查**: 应用启动时自动检查后端系统状态
2. **等待就绪**: 如果系统未就绪，显示加载界面并自动轮询检查
3. **503 处理**: 自动处理后端返回的 503 Service Unavailable 错误
4. **状态指示器**: 提供可选的系统状态指示器组件

### API 接口

系统状态 API 位于 `src/api/system.ts`:

```typescript
// 获取系统状态
systemApi.getStatus()

// 健康检查
systemApi.healthCheck()
```

### 使用方式

#### 自动启动检查

应用启动时会自动检查系统状态（已在 `App.vue` 中集成），无需额外配置。

#### 手动检查系统状态

在组件中使用 `useSystemReady` composable:

```typescript
import { useSystemReady } from '@/composables/useSystemReady'

const { isReady, isChecking, reason, checkSystemStatus, waitForSystemReady } = useSystemReady()

// 检查一次
await checkSystemStatus()

// 等待系统就绪（最多重试30次，每次间隔2秒）
await waitForSystemReady()
```

#### 显示状态指示器

在页面中添加系统状态指示器:

```vue
<template>
  <SystemStatusIndicator :auto-refresh="true" :refresh-interval="30000" />
</template>

<script setup>
import SystemStatusIndicator from '@/components/common/SystemStatusIndicator.vue'
</script>
```

### 配置参数

- `maxRetries`: 最大重试次数（默认30次）
- `retryInterval`: 重试间隔（默认2000毫秒）
- `autoRefresh`: 是否自动刷新状态（状态指示器）
- `refreshInterval`: 刷新间隔（状态指示器，默认30000毫秒）

## Bark通知配置

1. 下载Bark App（iOS）
2. 获取Bark Key
3. 在前端"后端配置"页面启用Bark通知并填入Key
4. 点击"测试通知"验证配置

## 设计风格

界面采用 Microsoft Fluent Design 风格，包含：
- 亚克力材质效果
- 深度和层次感
- 流畅的动画和过渡
- 现代配色方案
