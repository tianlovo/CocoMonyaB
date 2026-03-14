import { Request, Response, NextFunction } from 'express';
import { getConfig } from '../config';
import type { ApiResponse } from '../types';

/**
 * Token认证中间件
 * 验证请求头中的Authorization或X-Access-Token
 */

// 不需要认证的路径
const PUBLIC_PATHS = [
  '/api/auth/login',
  '/api/auth/verify',
  '/api/system/status' // 系统状态检查不需要认证
];

export function authMiddleware(req: Request, res: Response, next: NextFunction): void {
  const path = req.path;

  // 检查是否是公开路径
  if (PUBLIC_PATHS.some(publicPath => path.includes(publicPath))) {
    next();
    return;
  }

  // 从请求头中获取token
  const authHeader = req.headers.authorization;
  const accessToken = req.headers['x-access-token'] as string;

  let token: string | undefined;

  if (authHeader && authHeader.startsWith('Bearer ')) {
    token = authHeader.substring(7);
  } else if (accessToken) {
    token = accessToken;
  }

  if (!token) {
    const response: ApiResponse = {
      code: -40001,
      msg: '未提供访问令牌',
      data: null
    };
    res.status(401).json(response);
    return;
  }

  // 验证token
  const config = getConfig();
  if (token !== config.server.frontendToken) {
    const response: ApiResponse = {
      code: -40001,
      msg: '访问令牌无效',
      data: null
    };
    res.status(401).json(response);
    return;
  }

  next();
}
