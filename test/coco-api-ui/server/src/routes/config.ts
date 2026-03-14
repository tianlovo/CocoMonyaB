import { Router } from 'express';
import { getConfig, updateConfig } from '../config';
import { barkService } from '../services/bark';
import { monitorService } from '../services/monitor';
import type { ApiResponse, ServerConfig } from '../types';

const router = Router();

/**
 * 配置管理路由
 */

// 获取配置
router.get('/', (req, res) => {
  const config = getConfig();

  // 返回配置时隐藏敏感信息
  const safeConfig = {
    server: {
      port: config.server.port,
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

export default router;
