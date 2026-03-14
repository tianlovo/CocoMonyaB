import axios from 'axios';
import { getConfig } from '../config';

/**
 * Bark通知服务
 * 文档: https://bark.day.app/#/tutorial
 */

export interface BarkNotification {
  title?: string;
  subtitle?: string;
  body: string;
  group?: string;
  level?: 'active' | 'timeSensitive' | 'passive' | 'critical';
  sound?: string;
  badge?: number;
  icon?: string;
  url?: string;
}

class BarkService {
  private lastNotificationTime: Map<string, number> = new Map();
  private readonly MIN_INTERVAL = 30000; // 最小通知间隔30秒

  /**
   * 发送通知
   */
  async send(notification: BarkNotification): Promise<boolean> {
    const config = getConfig();

    if (!config.bark.enabled || !config.bark.key) {
      console.log('[Bark] 通知未启用或key未配置');
      return false;
    }

    // 检查通知频率限制
    const key = notification.group || 'default';
    const lastTime = this.lastNotificationTime.get(key) || 0;
    const now = Date.now();
    if (now - lastTime < this.MIN_INTERVAL) {
      console.log('[Bark] 通知频率限制，跳过发送');
      return false;
    }

    try {
      const server = config.bark.server || 'https://api.day.app';
      const url = `${server}/push`;

      const response = await axios.post(url, {
        device_key: config.bark.key,
        title: notification.title,
        subtitle: notification.subtitle,
        body: notification.body,
        group: notification.group || 'coco-api-ui',
        level: notification.level || 'active',
        sound: notification.sound,
        badge: notification.badge,
        icon: notification.icon,
        url: notification.url
      }, {
        headers: {
          'Content-Type': 'application/json; charset=utf-8'
        },
        timeout: 10000
      });

      if (response.status === 200) {
        console.log('[Bark] 通知发送成功:', notification.title);
        this.lastNotificationTime.set(key, now);
        return true;
      }
      return false;
    } catch (error) {
      console.error('[Bark] 通知发送失败:', error);
      return false;
    }
  }

  /**
   * 发送Java后端掉线通知
   */
  async sendJavaOfflineNotification(offlineSince: string): Promise<boolean> {
    return this.send({
      title: '⚠️ Java后端服务异常',
      subtitle: '服务已离线',
      body: `Java后端服务自 ${offlineSince} 起已离线，请检查服务状态`,
      group: 'monitor-java',
      level: 'critical',
      sound: 'alarm'
    });
  }

  /**
   * 发送Java后端恢复通知
   */
  async sendJavaOnlineNotification(): Promise<boolean> {
    return this.send({
      title: '✅ Java后端服务恢复',
      subtitle: '服务已恢复正常',
      body: 'Java后端服务已恢复正常运行',
      group: 'monitor-java',
      level: 'active'
    });
  }

  /**
   * 发送TG登录态失效通知
   */
  async sendTgLoginExpiredNotification(): Promise<boolean> {
    return this.send({
      title: '⚠️ Telegram登录态失效',
      subtitle: '需要重新登录',
      body: 'Telegram登录态已失效，请重新登录以恢复功能',
      group: 'monitor-tg',
      level: 'critical',
      sound: 'alarm'
    });
  }

  /**
   * 发送TG登录态恢复通知
   */
  async sendTgLoginValidNotification(): Promise<boolean> {
    return this.send({
      title: '✅ Telegram登录态正常',
      subtitle: '登录态已恢复',
      body: 'Telegram登录态检查正常',
      group: 'monitor-tg',
      level: 'active'
    });
  }

  /**
   * 发送测试通知
   */
  async sendTestNotification(): Promise<boolean> {
    return this.send({
      title: '📱 Bark测试通知',
      subtitle: '配置测试',
      body: '这是一条测试通知，如果您收到说明Bark配置正确',
      group: 'test',
      level: 'active'
    });
  }
}

export const barkService = new BarkService();
