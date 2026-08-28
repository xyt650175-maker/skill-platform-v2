import request from '@/utils/request'

const LOCAL_DEMO_TOKEN = 'local-demo-token'
const LOCAL_DEMO_USER: UserInfo = {
  id: 1,
  username: 'admin',
  displayName: '本地演示管理员',
  role: 'admin',
  permissions: ['*'],
}

export const authApi = {
  async login(username: string, password: string) {
    try {
      return await request.post('/auth/login', { username, password }) as {
        accessToken: string
        user: UserInfo
      }
    } catch (error: any) {
      // 仅在开发环境且服务根本不可达时使用本地演示；不能把有效的后端 JWT
      // 替换为本地 token，否则后续创建/模型接口会持续返回 401。
      const backendUnavailable = !error?.response && ['ERR_NETWORK', 'ECONNABORTED'].includes(error?.code)
      if (import.meta.env.DEV && backendUnavailable) {
        console.warn('后端不可达，切换到本地演示模式')
        return { accessToken: LOCAL_DEMO_TOKEN, user: LOCAL_DEMO_USER }
      }
      throw error
    }
  },

  me(token: string) {
    if (token === LOCAL_DEMO_TOKEN) {
      return Promise.resolve(LOCAL_DEMO_USER)
    }
    return request.get('/auth/me', {
      headers: { Authorization: `Bearer ${token}` },
    }) as Promise<any>
  },
}

export interface UserInfo {
  id: number
  username: string
  displayName?: string
  email?: string
  role: string
  permissions: string[]
}
