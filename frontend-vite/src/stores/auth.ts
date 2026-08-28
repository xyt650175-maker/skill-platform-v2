import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type UserInfo } from '@/api/auth'

const LOCAL_DEMO_TOKEN = 'local-demo-token'
const savedToken = localStorage.getItem('token') || ''
// 旧版本会把默认账号固定登录为本地演示 token。它不能通过 Java 后端的
// JWT 校验，刷新后要求重新登录，避免带着无效 token 在受保护接口间循环跳转。
if (savedToken === LOCAL_DEMO_TOKEN) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(savedToken === LOCAL_DEMO_TOKEN ? '' : savedToken)
  const user = ref<UserInfo | null>(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const res = await authApi.login(username, password)
    token.value = res.accessToken
    localStorage.setItem('token', res.accessToken)
    const userInfo = res.user || await authApi.me(res.accessToken)
    user.value = userInfo
    localStorage.setItem('user', JSON.stringify(userInfo))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, login, logout }
})
