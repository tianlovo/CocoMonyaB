import { Router } from 'express';
import axios from 'axios';
import { getConfig, updateConfig } from '../config';
import { barkService } from '../services/bark';
import { monitorService } from '../services/monitor';
import type { ApiResponse, ServerConfig } from '../types';

const router: Router = Router();

/**
 * 配置管理路由
 */

// 获取配置
router.get('/', (req, res) => {
  const config = getConfig();

  // 返回配置时隐藏敏感信息
  const safeConfig = {
    server: {
      javaBackendUrl: config.server.javaBackendUrl,
      // 不返回 frontendToken
    },
    bark: {
      enabled: config.bark.enabled,
      key: config.bark.key ? '********' : '', // 隐藏key
      server: config.bark.server
    },
    monitor: config.monitor
  };

  const response: ApiResponse = {
    code: 200,
    msg: '获取成功',
    data: safeConfig
  };
  res.json(response);
});

// 更新配置
router.put('/', async (req, res) => {
  try {
    const newConfig: Partial<ServerConfig> = req.body;
    const currentConfig = getConfig();

    // 合并配置
    const mergedConfig: ServerConfig = {
      server: {
        ...currentConfig.server,
        ...newConfig.server
      },
      bark: {
        ...currentConfig.bark,
        ...newConfig.bark
      },
      monitor: {
        javaOfflineCheck: {
          ...currentConfig.monitor.javaOfflineCheck,
          ...newConfig.monitor?.javaOfflineCheck
        },
        tgLoginCheck: {
          ...currentConfig.monitor.tgLoginCheck,
          ...newConfig.monitor?.tgLoginCheck
        }
      }
    };

    // 如果bark.key是掩码，保留原值
    if (newConfig.bark?.key === '********') {
      mergedConfig.bark.key = currentConfig.bark.key;
    }

    updateConfig(mergedConfig);

    // 重启监控服务以应用新配置
    monitorService.restart();

    const response: ApiResponse = {
      code: 200,
      msg: '配置更新成功',
      data: null
    };
    res.json(response);
  } catch (error) {
    console.error('[Config] 更新配置失败:', error);
    const response: ApiResponse = {
      code: -50000,
      msg: '配置更新失败',
      data: null
    };
    res.status(500).json(response);
  }
});

// 测试Bark通知
router.post('/bark/test', async (req, res) => {
  try {
    const success = await barkService.sendTestNotification();

    if (success) {
      const response: ApiResponse = {
        code: 200,
        msg: '测试通知已发送',
        data: null
      };
      res.json(response);
    } else {
      const response: ApiResponse = {
        code: -50000,
        msg: '测试通知发送失败，请检查Bark配置',
        data: null
      };
      res.status(500).json(response);
    }
  } catch (error) {
    console.error('[Config] Bark测试失败:', error);
    const response: ApiResponse = {
      code: -50000,
      msg: '测试通知发送失败',
      data: null
    };
    res.status(500).json(response);
  }
});

// 测试Java后端连接
router.post('/test-java-connection', async (req, res) => {
  try {
    const { javaBackendUrl } = req.body;
    
    if (!javaBackendUrl) {
      const response: ApiResponse = {
        code: -40000,
        msg: 'Java后端地址不能为空',
        data: null
      };
      return res.status(400).json(response);
    }

    // 测试连接Java后端的system/status接口
    const testUrl = `${javaBackendUrl}/api/system/status`;
    
    try {
      const startTime = Date.now();
      const axiosResponse = await axios.get(testUrl, {
        timeout: 10000,
        validateStatus: () => true // 允许任何状态码
      });
      const responseTime = Date.now() - startTime;

      // 如果返回200或503（系统未就绪），都认为是连接成功
      if (axiosResponse.status === 200 || axiosResponse.status === 503) {
        const response: ApiResponse = {
          code: 200,
          msg: '连接成功',
          data: {
            connected: true,
            status: axiosResponse.status,
            responseTime: `${responseTime}ms`,
            message: axiosResponse.status === 200 ? 'Java后端服务正常' : 'Java后端正在启动中'
          }
        };
        return res.json(response);
      } else {
        const response: ApiResponse = {
          code: -50001,
          msg: `连接失败，HTTP状态码: ${axiosResponse.status}`,
          data: {
            connected: false,
            status: axiosResponse.status,
            responseTime: `${responseTime}ms`
          }
        };
        return res.status(500).json(response);
      }
    } catch (axiosError: any) {
      // 连接失败
      let errorMsg = '无法连接到Java后端';
      if (axiosError.code === 'ECONNREFUSED') {
        errorMsg = '连接被拒绝，请检查Java后端是否已启动';
      } else if (axiosError.code === 'ETIMEDOUT' || axiosError.code === 'ECONNABORTED') {
        errorMsg = '连接超时，请检查网络或Java后端状态';
      } else if (axiosError.code === 'ENOTFOUND') {
        errorMsg = '无法解析主机地址，请检查URL是否正确';
      }

      const response: ApiResponse = {
        code: -50002,
        msg: errorMsg,
        data: {
          connected: false,
          error: axiosError.message
        }
      };
      return res.status(500).json(response);
    }
  } catch (error) {
    console.error('[Config] 测试Java后端连接失败:', error);
    const response: ApiResponse = {
      code: -50000,
      msg: '测试连接失败',
      data: null
    };
    res.status(500).json(response);
  }
});

export default router;
