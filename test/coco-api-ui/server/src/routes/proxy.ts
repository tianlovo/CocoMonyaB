import { Router } from 'express';
import axios, { AxiosError } from 'axios';
import { getConfig } from '../config';
import type { ApiResponse } from '../types';

const router: Router = Router();

/**
 * API转发路由
 * 将所有 /api/* 请求转发到Java后端
 */

// 创建axios实例用于转发请求
const createProxyAxios = () => {
  const config = getConfig();
  return axios.create({
    baseURL: config.server.javaBackendUrl,
    timeout: 60000, // 60秒超时
    maxBodyLength: Infinity,
    maxContentLength: Infinity
  });
};

// 处理所有API请求
router.all('*', async (req, res) => {
  const config = getConfig();
  const path = req.path;
  const method = req.method.toLowerCase();

  console.log(`[Proxy] ${req.method} ${path}`);

  try {
    const proxyAxios = createProxyAxios();

    // 准备请求配置
    const requestConfig: any = {
      url: `/api${path}`,
      method: method,
      headers: {
        'Content-Type': req.headers['content-type'] || 'application/json'
      }
    };

    // 处理查询参数
    if (Object.keys(req.query).length > 0) {
      requestConfig.params = req.query;
    }

    // 处理请求体
    if (['post', 'put', 'patch'].includes(method) && req.body) {
      requestConfig.data = req.body;
    }

    // 发送请求到Java后端
    const response = await proxyAxios.request(requestConfig);

    // 特殊处理：/system/status 直接返回数据
    if (path === '/system/status') {
      return res.status(response.status).json(response.data);
    }

    // 返回Java后端的响应
    res.status(response.status).json(response.data);

  } catch (error) {
    const axiosError = error as AxiosError;

    if (axiosError.response) {
      // Java后端返回了错误响应
      console.error(`[Proxy] Java后端返回错误: ${axiosError.response.status}`);
      res.status(axiosError.response.status).json(axiosError.response.data);
    } else if (axiosError.request) {
      // 无法连接到Java后端
      console.error('[Proxy] 无法连接到Java后端');
      const errorResponse: ApiResponse = {
        code: -50003,
        msg: 'Java后端服务暂时不可用，请稍后重试',
        data: null
      };
      res.status(503).json(errorResponse);
    } else {
      // 其他错误
      console.error('[Proxy] 请求转发失败:', axiosError.message);
      const errorResponse: ApiResponse = {
        code: -50000,
        msg: '服务器内部错误',
        data: null
      };
      res.status(500).json(errorResponse);
    }
  }
});

export default router;
