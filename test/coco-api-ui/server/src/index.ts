import express from 'express';
import cors from 'cors';
import { loadConfig, getConfig, FIXED_PORT } from './config';
import { monitorService } from './services/monitor';
import { authMiddleware } from './middleware/auth';

// 导入路由
import authRouter from './routes/auth';
import configRouter from './routes/config';
import monitorRouter from './routes/monitor';
import proxyRouter from './routes/proxy';

// 加载配置
loadConfig();

const app = express();

// 中间件
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// 请求日志
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// API路由统一挂载到 /api 路径下
// 公开路由 - 不需要认证
app.use('/api/auth', authRouter);

// 认证中间件（保护后续路由）
app.use(authMiddleware);

// 受保护的路由
app.use('/api/config', configRouter);
app.use('/api/monitor', monitorRouter);

// API转发路由 - 处理所有 /api/* 请求（除了上面已处理的）
app.use('/api', proxyRouter);

// 错误处理
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('[Error]', err);
  res.status(500).json({
    code: -50000,
    msg: '服务器内部错误',
    data: null
  });
});

// 启动服务器（使用固定端口15088）
const PORT = FIXED_PORT;

app.listen(PORT, () => {
  // 获取配置用于显示
  const config = getConfig();
  
  console.log(`
╔════════════════════════════════════════════════════════╗
║           Coco API UI Server                           ║
╠════════════════════════════════════════════════════════╣
║  服务端口: ${PORT.toString().padEnd(43)}║
║  Java后端: ${config.server.javaBackendUrl.padEnd(43)}║
║  监控服务: ${(config.monitor.javaOfflineCheck.enabled || config.monitor.tgLoginCheck.enabled ? '已启用' : '已禁用').padEnd(43)}║
╚════════════════════════════════════════════════════════╝
  `);

  // 启动监控服务
  if (config.monitor.javaOfflineCheck.enabled || config.monitor.tgLoginCheck.enabled) {
    monitorService.start();
  }
});

// 优雅关闭
process.on('SIGTERM', () => {
  console.log('[Server] 正在关闭...');
  monitorService.stop();
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('[Server] 正在关闭...');
  monitorService.stop();
  process.exit(0);
});
