import axios from 'axios';
import cron from 'node-cron';
import { getConfig } from '../config';
import { barkService } from './bark';
import type { MonitorStatus } from '../types';

/**
 * 监控服务
 * 监控Java后端状态和TG登录态
 */

class MonitorService {
  private javaCheckJob: cron.ScheduledTask | null = null;
  private tgCheckJob: cron.ScheduledTask | null = null;
  private status: MonitorStatus = {
    javaBackend: {
      isOnline: true,
      lastCheck: new Date().toISOString(),
      notified: false
    },
    tgLogin: {
      isValid: true,
      lastCheck: new Date().toISOString(),
      consecutiveFailures: 0,
      notified: false
    }
  };

  /**
   * 启动监控服务
   */
  start(): void {
    console.log('[Monitor] 启动监控服务...');
    this.stop(); // 先停止现有的任务

    const config = getConfig();

    // Java后端状态监控
    if (config.monitor.javaOfflineCheck.enabled) {
      const javaInterval = config.monitor.javaOfflineCheck.intervalMinutes;
      // 使用 cron 表达式：每N分钟执行一次
      const cronExpression = `*/${javaInterval} * * * *`;
      this.javaCheckJob = cron.schedule(cronExpression, () => {
        this.checkJavaBackend();
      });
      console.log(`[Monitor] Java后端监控已启动，间隔: ${javaInterval}分钟`);
    }

    // TG登录态监控
    if (config.monitor.tgLoginCheck.enabled) {
      const tgInterval = config.monitor.tgLoginCheck.intervalMinutes;
      const cronExpression = `*/${tgInterval} * * * *`;
      this.tgCheckJob = cron.schedule(cronExpression, () => {
        this.checkTgLogin();
      });
      console.log(`[Monitor] TG登录态监控已启动，间隔: ${tgInterval}分钟`);
    }

    // 立即执行一次检查
    this.checkJavaBackend();
    this.checkTgLogin();
  }

  /**
   * 停止监控服务
   */
  stop(): void {
    if (this.javaCheckJob) {
      this.javaCheckJob.stop();
      this.javaCheckJob = null;
      console.log('[Monitor] Java后端监控已停止');
    }
    if (this.tgCheckJob) {
      this.tgCheckJob.stop();
      this.tgCheckJob = null;
      console.log('[Monitor] TG登录态监控已停止');
    }
  }

  /**
   * 重启监控服务
   */
  restart(): void {
    console.log('[Monitor] 重启监控服务...');
    this.start();
  }

  /**
   * 获取监控状态
   */
  getStatus(): MonitorStatus {
    return { ...this.status };
  }

  /**
   * 检查Java后端状态
   */
  private async checkJavaBackend(): Promise<void> {
    const config = getConfig();
    const url = `${config.server.javaBackendUrl}/api/system/status`;

    try {
      const response = await axios.get(url, { timeout: 10000 });
      const isOnline = response.status === 200;

      const wasOffline = !this.status.javaBackend.isOnline;
      this.status.javaBackend.isOnline = isOnline;
      this.status.javaBackend.lastCheck = new Date().toISOString();

      if (isOnline) {
        // 服务恢复
        if (wasOffline) {
          console.log('[Monitor] Java后端服务已恢复');
          this.status.javaBackend.notified = false;
          delete this.status.javaBackend.offlineSince;
          await barkService.sendJavaOnlineNotification();
        }
      }
    } catch (error) {
      // 服务离线
      const wasOnline = this.status.javaBackend.isOnline;
      this.status.javaBackend.isOnline = false;
      this.status.javaBackend.lastCheck = new Date().toISOString();

      if (wasOnline) {
        // 服务刚离线
        console.log('[Monitor] Java后端服务已离线');
        this.status.javaBackend.offlineSince = new Date().toLocaleString('zh-CN');
      }

      // 发送通知（只发送一次）
      if (!this.status.javaBackend.notified) {
        const offlineSince = this.status.javaBackend.offlineSince || new Date().toLocaleString('zh-CN');
        const sent = await barkService.sendJavaOfflineNotification(offlineSince);
        if (sent) {
          this.status.javaBackend.notified = true;
        }
      }
    }
  }

  /**
   * 检查TG登录态
   * 使用 forceRefresh 参数强制刷新获取频道列表
   */
  private async checkTgLogin(): Promise<void> {
    const config = getConfig();
    const url = `${config.server.javaBackendUrl}/api/channel/tg/logged-in`;

    try {
      const response = await axios.get(url, {
        params: {
          current: 1,
          size: 1,
          forceRefresh: true
        },
        timeout: 30000 // TG操作可能需要较长时间
      });

      // 检查响应是否成功
      const isSuccess = response.status === 200 &&
        (response.data.code === 200 || !response.data.code); // 有些接口直接返回数据

      if (isSuccess) {
        // 登录态有效
        const wasInvalid = !this.status.tgLogin.isValid;
        this.status.tgLogin.isValid = true;
        this.status.tgLogin.consecutiveFailures = 0;
        this.status.tgLogin.lastCheck = new Date().toISOString();

        if (wasInvalid) {
          console.log('[Monitor] TG登录态已恢复');
          this.status.tgLogin.notified = false;
          await barkService.sendTgLoginValidNotification();
        }
      } else {
        // 响应不成功，可能是登录态问题
        await this.handleTgLoginFailure();
      }
    } catch (error: any) {
      // 如果Java后端离线，不增加TG失败次数
      if (!this.status.javaBackend.isOnline) {
        console.log('[Monitor] Java后端离线，跳过TG登录态检查');
        return;
      }
      await this.handleTgLoginFailure();
    }
  }

  /**
   * 处理TG登录失败
   */
  private async handleTgLoginFailure(): Promise<void> {
    const config = getConfig();
    const maxFailures = config.monitor.tgLoginCheck.maxFailures;

    this.status.tgLogin.consecutiveFailures++;
    this.status.tgLogin.lastCheck = new Date().toISOString();

    console.log(`[Monitor] TG登录态检查失败，连续失败次数: ${this.status.tgLogin.consecutiveFailures}`);

    // 连续失败达到阈值，判定为登录态失效
    if (this.status.tgLogin.consecutiveFailures >= maxFailures) {
      const wasValid = this.status.tgLogin.isValid;
      this.status.tgLogin.isValid = false;

      if (wasValid && !this.status.tgLogin.notified) {
        console.log('[Monitor] TG登录态已失效');
        const sent = await barkService.sendTgLoginExpiredNotification();
        if (sent) {
          this.status.tgLogin.notified = true;
        }
      }
    }
  }
}

export const monitorService = new MonitorService();
