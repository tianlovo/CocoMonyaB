<template>
  <div class="server-config-view">
    <div class="page-header">
      <h1 class="page-title">⚙️ 后端服务配置</h1>
      <p class="page-desc">配置Node.js轻量级后端服务的各项参数</p>
    </div>

    <!-- 监控状态卡片 -->
    <el-card class="config-card" v-loading="statusLoading">
      <template #header>
        <div class="card-header">
          <span>📊 监控状态</span>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="refreshStatus" :loading="statusLoading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-button size="small" @click="restartMonitor" :loading="restarting">
              <el-icon><RefreshRight /></el-icon> 重启监控
            </el-button>
          </div>
        </div>
      </template>

      <div class="status-grid">
        <div class="status-item">
          <div class="status-label">Java后端状态</div>
          <el-tag :type="monitorStatus?.javaBackend.isOnline ? 'success' : 'danger'" size="large">
            {{ monitorStatus?.javaBackend.isOnline ? '✅ 在线' : '❌ 离线' }}
          </el-tag>
        </div>
        <div class="status-item">
          <div class="status-label">TG登录态</div>
          <el-tag :type="monitorStatus?.tgLogin.isValid ? 'success' : 'danger'" size="large">
            {{ monitorStatus?.tgLogin.isValid ? '✅ 正常' : '❌ 失效' }}
          </el-tag>
        </div>
        <div class="status-item">
          <div class="status-label">Java最后检查</div>
          <div class="status-value">{{ formatTime(monitorStatus?.javaBackend.lastCheck) }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">TG最后检查</div>
          <div class="status-value">{{ formatTime(monitorStatus?.tgLogin.lastCheck) }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">Java离线时间</div>
          <div class="status-value">{{ monitorStatus?.javaBackend.offlineSince || '-' }}</div>
        </div>
        <div class="status-item">
          <div class="status-label">TG连续失败次数</div>
          <div class="status-value">{{ monitorStatus?.tgLogin.consecutiveFailures || 0 }} 次</div>
        </div>
      </div>
    </el-card>

    <!-- 服务器配置 -->
    <el-card class="config-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>🖥️ 服务器配置</span>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="服务端口">
              <el-input :model-value="FIXED_PORT" disabled style="width: 100%" />
              <div class="form-hint">Node.js后端服务端口（固定为15088，不可修改）</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Java后端地址">
              <div class="java-backend-input">
                <el-input 
                  v-model="form.server.javaBackendUrl" 
                  placeholder="http://127.0.0.1:10721"
                  style="flex: 1"
                />
                <el-button 
                  type="primary" 
                  @click="testJavaConnection"
                  :loading="testingJavaConnection"
                  :disabled="!form.server.javaBackendUrl"
                >
                  <el-icon><Connection /></el-icon>
                  测试连接
                </el-button>
              </div>
              <div class="form-hint">Java后端服务的URL地址</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- Bark通知配置 -->
    <el-card class="config-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>📱 Bark通知配置</span>
          <el-button type="primary" size="small" @click="testBark" :loading="testingBark">
            <el-icon><Bell /></el-icon> 测试通知
          </el-button>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <el-form-item>
          <el-checkbox v-model="form.bark.enabled">启用Bark通知</el-checkbox>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Bark Key">
              <el-input
                v-model="form.bark.key"
                placeholder="输入Bark Key"
                show-password
                :disabled="!form.bark.enabled"
              />
              <div class="form-hint">从Bark App获取的推送Key</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Bark服务器地址（可选）">
              <el-input
                v-model="form.bark.server"
                placeholder="https://api.day.app"
                :disabled="!form.bark.enabled"
              />
              <div class="form-hint">自定义Bark服务器地址，留空使用默认</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 监控配置 -->
    <el-card class="config-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>🔍 监控配置</span>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <!-- Java后端监控 -->
        <div class="config-section">
          <h4 class="section-title">Java后端掉线检测</h4>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item>
                <el-checkbox v-model="form.monitor.javaOfflineCheck.enabled">启用检测</el-checkbox>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="检测间隔（分钟）">
                <el-input-number
                  v-model="form.monitor.javaOfflineCheck.intervalMinutes"
                  :min="1"
                  :max="1440"
                  style="width: 100%"
                  :disabled="!form.monitor.javaOfflineCheck.enabled"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <el-divider />

        <!-- TG登录态监控 -->
        <div class="config-section">
          <h4 class="section-title">TG登录态检测</h4>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item>
                <el-checkbox v-model="form.monitor.tgLoginCheck.enabled">启用检测</el-checkbox>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="检测间隔（分钟）">
                <el-input-number
                  v-model="form.monitor.tgLoginCheck.intervalMinutes"
                  :min="1"
                  :max="1440"
                  style="width: 100%"
                  :disabled="!form.monitor.tgLoginCheck.enabled"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最大失败次数">
                <el-input-number
                  v-model="form.monitor.tgLoginCheck.maxFailures"
                  :min="1"
                  :max="10"
                  style="width: 100%"
                  :disabled="!form.monitor.tgLoginCheck.enabled"
                />
                <div class="form-hint">连续失败达到此次数后判定为登录态失效</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </el-card>

    <!-- 保存按钮 -->
    <div class="actions-bar">
      <el-button type="primary" size="large" @click="saveConfig" :loading="saving">
        <el-icon><Check /></el-icon> 保存配置
      </el-button>
      <el-button size="large" @click="resetConfig">
        <el-icon><RefreshLeft /></el-icon> 重置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  RefreshRight,
  Bell,
  Check,
  RefreshLeft,
  Connection
} from '@element-plus/icons-vue'
import { serverConfigApi, FIXED_PORT, type ServerConfig, type MonitorStatus, type JavaConnectionTestResult } from '@/api/server-config'

// 状态
const loading = ref(false)
const saving = ref(false)
const statusLoading = ref(false)
const restarting = ref(false)
const testingBark = ref(false)
const testingJavaConnection = ref(false)
const monitorStatus = ref<MonitorStatus | null>(null)

// 表单数据
const defaultForm: ServerConfig = {
  server: {
    javaBackendUrl: 'http://127.0.0.1:10721'
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
}

const form = reactive<ServerConfig>(JSON.parse(JSON.stringify(defaultForm)))

// 自动刷新定时器
let autoRefreshTimer: number | null = null

// 加载配置
async function loadConfig() {
  loading.value = true
  try {
    const config = await serverConfigApi.getConfig()
    Object.assign(form, config)
    ElMessage.success('配置加载成功')
  } catch (error) {
    console.error('加载配置失败:', error)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 保存配置
async function saveConfig() {
  saving.value = true
  try {
    await serverConfigApi.updateConfig(form)
    ElMessage.success('配置保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败')
  } finally {
    saving.value = false
  }
}

// 重置配置
function resetConfig() {
  ElMessageBox.confirm('确定要重置所有配置吗？未保存的修改将丢失。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      loadConfig()
    })
    .catch(() => {})
}

// 测试Bark通知
async function testBark() {
  if (!form.bark.enabled) {
    ElMessage.warning('请先启用Bark通知')
    return
  }
  if (!form.bark.key) {
    ElMessage.warning('请先配置Bark Key')
    return
  }

  testingBark.value = true
  try {
    await serverConfigApi.testBark()
    ElMessage.success('测试通知已发送，请检查手机')
  } catch (error) {
    console.error('测试通知失败:', error)
    ElMessage.error('测试通知发送失败')
  } finally {
    testingBark.value = false
  }
}

// 测试Java后端连接
async function testJavaConnection() {
  if (!form.server.javaBackendUrl) {
    ElMessage.warning('请先输入Java后端地址')
    return
  }

  testingJavaConnection.value = true
  try {
    const result = await serverConfigApi.testJavaConnection(form.server.javaBackendUrl)
    if (result.connected) {
      ElMessage.success(`${result.message} (响应时间: ${result.responseTime})`)
    } else {
      ElMessage.error('连接失败，请检查地址是否正确')
    }
  } catch (error: any) {
    console.error('测试Java后端连接失败:', error)
    const errorMsg = error.response?.data?.msg || '连接失败，请检查Java后端是否已启动'
    ElMessage.error(errorMsg)
  } finally {
    testingJavaConnection.value = false
  }
}

// 刷新监控状态
async function refreshStatus() {
  statusLoading.value = true
  try {
    const status = await serverConfigApi.getMonitorStatus()
    monitorStatus.value = status
  } catch (error) {
    console.error('刷新状态失败:', error)
    ElMessage.error('刷新状态失败')
  } finally {
    statusLoading.value = false
  }
}

// 重启监控服务
async function restartMonitor() {
  restarting.value = true
  try {
    await serverConfigApi.restartMonitor()
    ElMessage.success('监控服务已重启')
    setTimeout(refreshStatus, 1000)
  } catch (error) {
    console.error('重启监控服务失败:', error)
    ElMessage.error('重启监控服务失败')
  } finally {
    restarting.value = false
  }
}

// 格式化时间
function formatTime(isoString?: string): string {
  if (!isoString) return '-'
  const date = new Date(isoString)
  return date.toLocaleString('zh-CN')
}

// 页面加载
onMounted(() => {
  loadConfig()
  refreshStatus()

  // 每30秒自动刷新状态
  autoRefreshTimer = window.setInterval(refreshStatus, 30000)
})

// 页面卸载
onUnmounted(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
  }
})
</script>

<style scoped>
.server-config-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--fluent-text-primary);
}

.page-desc {
  color: var(--fluent-text-secondary);
  font-size: 14px;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.status-item {
  text-align: center;
  padding: 16px;
  background: var(--fluent-bg-alt);
  border-radius: 8px;
}

.status-label {
  font-size: 13px;
  color: var(--fluent-text-secondary);
  margin-bottom: 8px;
}

.status-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--fluent-text-primary);
}

.config-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--fluent-text-secondary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--fluent-border);
}

.form-hint {
  font-size: 12px;
  color: var(--fluent-text-secondary);
  margin-top: 4px;
}

.java-backend-input {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.actions-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 32px;
  margin-bottom: 40px;
}

@media (max-width: 768px) {
  .status-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
