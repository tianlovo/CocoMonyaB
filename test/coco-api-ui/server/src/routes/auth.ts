import { Router } from 'express';
import { getConfig } from '../config';
import type { ApiResponse } from '../types';

const router: Router = Router();

/**
 * 认证相关路由
 */

// 登录
router.post('/login', (req, res) => {
  const { token } = req.body;
  const config = getConfig();

  if (!token) {
    const response: ApiResponse = {
      code: -40000,
      msg: 'Token不能为空',
      data: null
    };
    return res.status(400).json(response);
  }

  if (token === config.server.frontendToken) {
    const response: ApiResponse = {
      code: 200,
      msg: '登录成功',
      data: {
        token: config.server.frontendToken,
        expiresIn: 86400 * 7 // 7天
      }
    };
    res.json(response);
  } else {
    const response: ApiResponse = {
      code: -40001,
      msg: 'Token无效',
      data: null
    };
    res.status(401).json(response);
  }
});

// 验证Token
router.post('/verify', (req, res) => {
  const { token } = req.body;
  const config = getConfig();

  if (token === config.server.frontendToken) {
    const response: ApiResponse = {
      code: 200,
      msg: 'Token有效',
      data: { valid: true }
    };
    res.json(response);
  } else {
    const response: ApiResponse = {
      code: -40001,
      msg: 'Token无效',
      data: { valid: false }
    };
    res.status(401).json(response);
  }
});

export default router;
