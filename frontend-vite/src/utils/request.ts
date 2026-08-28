import axios from 'axios'
import router from '@/router'
import { mockSkills, mockAgents, mockEvalTasks, mockPipelines } from './mock'

const LOCAL_DEMO_TOKEN = 'local-demo-token'

const request = axios.create({
  baseURL: '/race-api',
  timeout: 30000,
})

// 获取mock数据
const getMockData = (url: string) => {
  if (url.includes('/skills/reviews')) return null
  if (url.includes('/skills/') && url.includes('operation-logs')) return null
  if (url.includes('/skills/') && url.includes('versions')) return null
  if (url.includes('/skills')) return mockSkills
  if (url.includes('/agents')) return mockAgents
  if (url.includes('/eval-tasks')) return mockEvalTasks
  if (url.includes('/pipelines')) return mockPipelines
  return null
}

// 检查是否开发模式 + 本地token
const isLocalDemo = (token: string) => token === LOCAL_DEMO_TOKEN

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === '0') return payload.data
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload
  },
  (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    const token = localStorage.getItem('token')

    // 开发模式 + 本地token + 后端不可用 = 使用mock数据
    if (import.meta.env.DEV && isLocalDemo(token || '') && (status === 500 || status === 404 || status === 503)) {
      const mockData = getMockData(url)
      if (mockData) {
        console.log('🔶 使用 Mock 数据:', url)
        return mockData
      }
    }

    const message = error.response?.data?.message || error.response?.data?.detail || error.message || '请求失败'

    // 401: token 过期或无效
    if (status === 401) {
      // 如果是本地演示token，不跳转登录
      if (isLocalDemo(token || '')) {
        console.log('🔶 本地演示模式不过期')
        return Promise.resolve({})
      }
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (router.currentRoute.value.name !== 'Login') {
        router.push('/login')
      }
    }
    return Promise.reject(new Error(message))
  }
)

export default request
