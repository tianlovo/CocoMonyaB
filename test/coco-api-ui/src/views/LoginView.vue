<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>🔐 系统登录</h1>
        <p>请输入访问令牌以继续使用</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="token">
          <el-input
            v-model="form.token"
            type="password"
            placeholder="请输入访问令牌"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="authStore.isLoading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>访问令牌由管理员在服务端配置文件中设置</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()

const form = reactive({
  token: ''
})

const rules = {
  token: [
    { required: true, message: '请输入访问令牌', trigger: 'blur' },
    { min: 1, message: '令牌不能为空', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const success = await authStore.login(form.token)
  if (success) {
    ElMessage.success('登录成功')
    router.push('/')
  } else {
    ElMessage.error('登录失败，请检查令牌是否正确')
  }
}

onMounted(async () => {
  // 如果已登录，跳转到首页
  const isAuth = await authStore.checkAuth()
  if (isAuth) {
    router.push('/')
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.login-header p {
  color: #666;
  font-size: 14px;
}

.login-button {
  width: 100%;
}

.login-hint {
  margin-top: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.login-hint .el-icon {
  color: #909399;
  flex-shrink: 0;
}
</style>
