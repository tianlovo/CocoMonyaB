import { Router } from 'express';
import { monitorService } from '../services/monitor';
import type { ApiResponse } from '../types';

const router = Router();

/**
 * 监控状态路由
 */

// 获取监控状态
router.get('/status', (req, res) => {
  const status = monitorService.getStatus();

  const response: ApiResponse = {
    code: 200,
    msg: '获取成功',
    data: status
  };
  res.json(response);
});

// 重启监控服务
router.post('/restart', (req, res) => {
  try {
    monitorService.restart();

    const response: ApiResponse = {
      code: 200,
      msg: '监控服务已重启',
      data: null
    };
    res.json(response);
  } catch (error) {
    console.error('[Monitor] 重启失败:', error);
    const response: ApiResponse = {
      code: -50000,
      msg: '监控服务重启失败',
      data: null
    };
    res.status(500).json(response);
  }
});

export default router;
