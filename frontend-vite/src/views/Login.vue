<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">
        <span class="logo-icon">🦐</span>
        <span class="logo-text">云虾 AgentOps</span>
      </div>
      <p class="subtitle">企业级智能体全链路平台 · LangChain + DeepAgent</p>
      <el-form :model="form" @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="admin123" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">
          登录
        </el-button>
      </el-form>
      <p class="hint">默认账号：admin / admin123</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/skills')
  } catch (e: any) {
    ElMessage.error('登录失败：' + e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.2);
  .logo {
    text-align: center;
    font-size: 24px;
    font-weight: 700;
    .logo-icon { font-size: 36px; }
  }
  .subtitle {
    text-align: center;
    color: #64748b;
    margin: 8px 0 24px;
    font-size: 13px;
  }
  .hint {
    text-align: center;
    color: #94a3b8;
    font-size: 12px;
    margin-top: 16px;
  }
}
</style>
