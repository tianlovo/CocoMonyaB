import fs from 'fs';
import path from 'path';
import yaml from 'js-yaml';
import type { ServerConfig } from '../types';

const CONFIG_FILE = path.join(process.cwd(), 'config.yaml');

// 默认配置
const defaultConfig: ServerConfig = {
  server: {
    port: 10722,
    javaBackendUrl: 'http://127.0.0.1:10721',
    frontendToken: 'coco-api-ui-token'
  },
  bark: {
    enabled: false,
    key: '',
    server: 'https://api.day.app'
  },
  monitor: {
    javaOfflineCheck: {
      enabled: true,
      intervalMinutes: 30
    },
    tgLoginCheck: {
      enabled: true,
      intervalMinutes: 60,
      maxFailures: 3
    }
  }
};

let config: ServerConfig = { ...defaultConfig };

// 加载配置
export function loadConfig(): ServerConfig {
  try {
    if (fs.existsSync(CONFIG_FILE)) {
      const fileContent = fs.readFileSync(CONFIG_FILE, 'utf8');
      const loadedConfig = yaml.load(fileContent) as Partial<ServerConfig>;
      config = mergeConfig(defaultConfig, loadedConfig);
      console.log('[Config] 配置文件加载成功');
    } else {
      console.log('[Config] 配置文件不存在，使用默认配置');
      saveConfig(config);
    }
  } catch (error) {
    console.error('[Config] 加载配置文件失败:', error);
    config = { ...defaultConfig };
  }
  return config;
}

// 保存配置
export function saveConfig(newConfig: ServerConfig): void {
  try {
    const yamlContent = yaml.dump(newConfig, {
      indent: 2,
      lineWidth: -1,
      noRefs: true
    });
    fs.writeFileSync(CONFIG_FILE, yamlContent, 'utf8');
    config = newConfig;
    console.log('[Config] 配置文件保存成功');
  } catch (error) {
    console.error('[Config] 保存配置文件失败:', error);
    throw error;
  }
}

// 获取当前配置
export function getConfig(): ServerConfig {
  return config;
}

// 更新配置
export function updateConfig(newConfig: Partial<ServerConfig>): ServerConfig {
  config = mergeConfig(config, newConfig);
  saveConfig(config);
  return config;
}

// 合并配置
function mergeConfig(defaults: ServerConfig, override: Partial<ServerConfig>): ServerConfig {
  return {
    server: { ...defaults.server, ...override.server },
    bark: { ...defaults.bark, ...override.bark },
    monitor: {
      javaOfflineCheck: { ...defaults.monitor.javaOfflineCheck, ...override.monitor?.javaOfflineCheck },
      tgLoginCheck: { ...defaults.monitor.tgLoginCheck, ...override.monitor?.tgLoginCheck }
    }
  };
}
