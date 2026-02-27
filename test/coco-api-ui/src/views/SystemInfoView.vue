<template>
  <div class="system-info-view">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon><InfoFilled /></el-icon>
        系统信息
      </h1>
      <p class="page-description">查看系统版本、构建信息和运行状态</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container fluent-card">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="loadSystemInfo">
            <el-icon><Refresh /></el-icon>
            重新加载
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- Content -->
    <div v-else class="info-grid">
      <!-- System Status Card -->
      <div class="info-card fluent-card fluent-depth-2">
        <div class="card-header">
          <div class="card-icon" :class="statusClass">
            <el-icon :size="24">
              <component :is="statusIcon" />
            </el-icon>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">系统状态</h3>
            <p class="card-subtitle">System Status</p>
          </div>
        </div>
        <div class="card-content">
          <div class="status-badge" :class="statusClass">
            <el-icon><component :is="statusIcon" /></el-icon>
            <span>{{ statusText }}</span>
          </div>
          <p v-if="systemStatus?.reason" class="status-reason">{{ systemStatus.reason }}</p>
          <div class="status-time">
            <el-icon><Clock /></el-icon>
            <span>{{ formattedTimestamp }}</span>
          </div>
        </div>
      </div>

      <!-- Version Info Card -->
      <div class="info-card fluent-card fluent-depth-2">
        <div class="card-header">
          <div class="card-icon version-icon">
            <el-icon :size="24"><Box /></el-icon>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">版本信息</h3>
            <p class="card-subtitle">Version Information</p>
          </div>
        </div>
        <div class="card-content">
          <div class="version-main">
            <div class="project-name">{{ systemInfo?.projectName }}</div>
            <div class="version-number">v{{ systemInfo?.version }}</div>
          </div>
          <div class="version-full">{{ systemInfo?.fullVersionInfo }}</div>
        </div>
      </div>

      <!-- Project Details Card -->
      <div class="info-card fluent-card fluent-depth-2 card-wide">
        <div class="card-header">
          <div class="card-icon project-icon">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">项目详情</h3>
            <p class="card-subtitle">Project Details</p>
          </div>
        </div>
        <div class="card-content">
          <div class="detail-item">
            <span class="detail-label">项目名称</span>
            <span class="detail-value">{{ systemInfo?.projectName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Artifact ID</span>
            <span class="detail-value">{{ systemInfo?.artifact }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Group ID</span>
            <span class="detail-value">{{ systemInfo?.group }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">项目描述</span>
            <span class="detail-value description">{{ systemInfo?.description }}</span>
          </div>
        </div>
      </div>

      <!-- Build Info Card -->
      <div class="info-card fluent-card fluent-depth-2">
        <div class="card-header">
          <div class="card-icon build-icon">
            <el-icon :size="24"><Tools /></el-icon>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">构建信息</h3>
            <p class="card-subtitle">Build Information</p>
          </div>
        </div>
        <div class="card-content">
          <div class="detail-item">
            <span class="detail-label">构建时间</span>
            <span class="detail-value">{{ formattedBuildTime }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Java 版本</span>
            <span class="detail-value">{{ systemInfo?.javaVersion }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Gradle 版本</span>
            <span class="detail-value">{{ systemInfo?.gradleVersion }}</span>
          </div>
        </div>
      </div>

      <!-- Actions Card -->
      <div class="info-card fluent-card fluent-depth-2">
        <div class="card-header">
          <div class="card-icon actions-icon">
            <el-icon :size="24"><Operation /></el-icon>
          </div>
          <div class="card-title-group">
            <h3 class="card-title">快捷操作</h3>
            <p class="card-subtitle">Quick Actions</p>
          </div>
        </div>
        <div class="card-content actions-content">
          <el-button type="primary" @click="loadSystemInfo" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新信息
          </el-button>
          <el-button @click="checkHealth" :loading="healthChecking">
            <el-icon><CircleCheck /></el-icon>
            健康检查
          </el-button>
          <el-button @click="copyVersionInfo">
            <el-icon><CopyDocument /></el-icon>
            复制版本信息
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { systemApi } from '@/api/system'
import { ElMessage } from 'element-plus'
import type { SystemInfo, SystemStatus } from '@/types/api'
import {
  InfoFilled,
  Refresh,
  Clock,
  Box,
  Document,
  Tools,
  Operation,
  CircleCheck,
  CopyDocument,
  SuccessFilled,
  WarningFilled,
  Loading as LoadingIcon
} from '@element-plus/icons-vue'

const loading = ref(false)
const error = ref<string | null>(null)
const systemInfo = ref<SystemInfo | null>(null)
const systemStatus = ref<SystemStatus | null>(null)
const healthChecking = ref(false)

const statusClass = computed(() => {
  if (!systemStatus.value) return 'status-unknown'
  return systemStatus.value.ready ? 'status-ready' : 'status-not-ready'
})

const statusIcon = computed(() => {
  if (!systemStatus.value) return LoadingIcon
  return systemStatus.value.ready ? SuccessFilled : WarningFilled
})

const statusText = computed(() => {
  if (!systemStatus.value) return '检查中...'
  return systemStatus.value.ready ? '系统就绪' : '系统未就绪'
})

const formattedTimestamp = computed(() => {
  if (!systemStatus.value?.timestamp) return '-'
  return new Date(systemStatus.value.timestamp).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})

const formattedBuildTime = computed(() => {
  if (!systemInfo.value?.buildTime) return '-'
  return new Date(systemInfo.value.buildTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})

const loadSystemInfo = async () => {
  loading.value = true
  error.value = null

  try {
    const [info, status] = await Promise.all([
      systemApi.getInfo(),
      systemApi.getStatus()
    ])
    
    systemInfo.value = info
    systemStatus.value = status
  } catch (err: any) {
    const errorMessage = err.message || '加载系统信息失败'
    error.value = errorMessage
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const checkHealth = async () => {
  healthChecking.value = true
  try {
    const result = await systemApi.healthCheck()
    if (result === 'OK') {
      ElMessage.success('系统健康检查通过')
    } else {
      ElMessage.warning(`健康检查结果: ${result}`)
    }
  } catch (err: any) {
    ElMessage.error('健康检查失败: ' + err.message)
  } finally {
    healthChecking.value = false
  }
}

const copyVersionInfo = async () => {
  if (!systemInfo.value) return
  
  const text = systemInfo.value.fullVersionInfo
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('版本信息已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  loadSystemInfo()
})
</script>

<style scoped>
.system-info-view {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--fluent-text-primary);
  margin: 0 0 var(--spacing-sm) 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.page-description {
  font-size: var(--font-size-base);
  color: var(--fluent-text-secondary);
  margin: 0;
}

.loading-container,
.error-container {
  margin-top: var(--spacing-xl);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--spacing-lg);
}

.info-card {
  padding: var(--spacing-lg);
  transition: all var(--transition-normal);
}

.info-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--fluent-shadow-8);
}

.card-wide {
  grid-column: 1 / -1;
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--fluent-border);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-ready .card-icon,
.status-ready.card-icon {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: white;
}

.status-not-ready .card-icon,
.status-not-ready.card-icon {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
  color: white;
}

.version-icon {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: white;
}

.project-icon {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
  color: white;
}

.build-icon {
  background: linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%);
  color: white;
}

.actions-icon {
  background: linear-gradient(135deg, #eb2f96 0%, #f759ab 100%);
  color: white;
}

.card-title-group {
  flex: 1;
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--fluent-text-primary);
  margin: 0 0 4px 0;
}

.card-subtitle {
  font-size: var(--font-size-sm);
  color: var(--fluent-text-tertiary);
  margin: 0;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-full);
  font-weight: 500;
  font-size: var(--font-size-base);
  width: fit-content;
}

.status-ready.status-badge {
  background-color: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.status-not-ready.status-badge {
  background-color: rgba(250, 173, 20, 0.1);
  color: #faad14;
}

.status-reason {
  color: var(--fluent-text-secondary);
  font-size: var(--font-size-sm);
  margin: 0;
  padding: var(--spacing-sm);
  background-color: var(--fluent-bg-alt);
  border-radius: var(--radius-sm);
}

.status-time {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  color: var(--fluent-text-tertiary);
  font-size: var(--font-size-sm);
}

.version-main {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.project-name {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--fluent-text-primary);
}

.version-number {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: #1890ff;
  padding: 4px 12px;
  background: rgba(24, 144, 255, 0.1);
  border-radius: var(--radius-full);
}

.version-full {
  font-size: var(--font-size-sm);
  color: var(--fluent-text-secondary);
  padding: var(--spacing-sm);
  background-color: var(--fluent-bg-alt);
  border-radius: var(--radius-sm);
  font-family: 'Consolas', 'Monaco', monospace;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) 0;
}

.detail-item:not(:last-child) {
  border-bottom: 1px solid var(--fluent-border-light);
}

.detail-label {
  font-size: var(--font-size-sm);
  color: var(--fluent-text-tertiary);
  font-weight: 500;
  flex-shrink: 0;
  min-width: 100px;
}

.detail-value {
  font-size: var(--font-size-base);
  color: var(--fluent-text-primary);
  text-align: right;
  word-break: break-word;
}

.detail-value.description {
  text-align: left;
  line-height: 1.6;
}

.actions-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.actions-content .el-button {
  width: 100%;
  justify-content: flex-start;
}

/* Mobile responsive */
@media (max-width: 767px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .card-wide {
    grid-column: 1;
  }
  
  .detail-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .detail-label {
    min-width: auto;
  }
  
  .detail-value {
    text-align: left;
  }
}

/* Tablet responsive */
@media (min-width: 768px) and (max-width: 1199px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
