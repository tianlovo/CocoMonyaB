# Coco API UI Server

轻量级Node.js后端服务，用于API转发、监控和认证。

## 功能特性

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
- Token在`config.yaml`中配置
- 支持登录页面和路由守卫

### 4. 可视化配置
- 提供Web配置页面（`http://localhost:10722`）
- 可配置Bark通知、监控参数、服务端点等
- 实时查看监控状态

## 快速开始

### 安装依赖

```bash
cd server
npm install
```

### 开发模式启动

```bash
npm run dev
```

### 生产模式

```bash
npm run build
npm start
```

### 使用启动脚本

Windows:
```bash
# 命令行
start-server.bat

# PowerShell
.\start-server.ps1
```

## 配置文件

配置文件位于 `server/config.yaml`，首次启动会自动创建。

```yaml
server:
  port: 10722                    # Node.js服务端口
  javaBackendUrl: "http://127.0.0.1:10721"  # Java后端地址
  frontendToken: "coco-api-ui-token"        # 前端访问令牌

bark:
  enabled: false                 # 是否启用Bark通知
  key: ""                        # Bark Key
  server: "https://api.day.app"  # Bark服务器地址

monitor:
  javaOfflineCheck:
    enabled: true                # 启用Java后端掉线检测
    intervalMinutes: 30          # 检测间隔（分钟）
  tgLoginCheck:
    enabled: true                # 启用TG登录态检测
    intervalMinutes: 60          # 检测间隔（分钟）
    maxFailures: 3               # 最大连续失败次数
```

## Bark通知配置

1. 下载Bark App（iOS）
2. 获取Bark Key
3. 在配置页面启用Bark通知并填入Key
4. 点击"测试通知"验证配置

## API端点

### 认证
- `POST /auth/login` - 登录
- `POST /auth/verify` - 验证Token

### 配置管理
- `GET /config` - 获取配置
- `PUT /config` - 更新配置
- `POST /config/bark/test` - 测试Bark通知

### 监控
- `GET /monitor/status` - 获取监控状态
- `POST /monitor/restart` - 重启监控服务

### API转发
- `/*` - 转发到Java后端

## 项目结构

```
server/
├── src/
│   ├── config/          # 配置管理
│   ├── middleware/      # 中间件
│   ├── routes/          # 路由
│   ├── services/        # 服务
│   ├── types/           # 类型定义
│   └── index.ts         # 入口文件
├── public/              # 静态文件（配置页面）
│   ├── index.html
│   └── login.html
├── package.json
├── tsconfig.json
└── README.md
```

## 环境要求

- Node.js >= 18
- npm >= 9

## 许可证

MIT
