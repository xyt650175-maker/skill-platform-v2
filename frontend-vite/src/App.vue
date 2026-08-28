<template>
  <router-view v-if="route.name === 'Login'" />
  <div v-else class="app-shell">
    <header class="app-top">
      <span class="brand">智能应用协作平台</span>
      <span class="crumb">{{ crumb }}</span>
      <span class="top-right" @click="logout">退出 ◌</span>
    </header>
    <div class="app-layout">
      <aside class="app-nav">
        <button :class="['nav-btn', { active: activeNav === 'workbench' }]" @click="selectNav('workbench', '/agents')">工作台</button>
        <button :class="['nav-btn', { active: activeNav === 'my-agents' }]" @click="selectNav('my-agents', '/teamagents')">我的智能体</button>
        <button :class="['nav-btn', { active: activeNav === 'agent-center' }]" @click="selectNav('agent-center', '/agents')">智能体中心</button>
        <button :class="['nav-btn', { active: activeNav === 'mcp-market' }]" @click="selectNav('mcp-market', '/skills')">MCP 超市</button>
        <div class="nav-divider"></div>
        <button class="nav-group nav-group-toggle" @click="skillMenuOpen = !skillMenuOpen">技能 <span>{{ skillMenuOpen ? '⌄' : '›' }}</span></button>
        <div v-show="skillMenuOpen">
          <button :class="['nav-btn nav-child', { active: activeNav === 'skill-management' }]" @click="selectNav('skill-management', '/skills')">技能管理</button>
          <button :class="['nav-btn nav-child', { active: activeNav === 'skill-development' }]" @click="selectNav('skill-development', '/skill-workbench?new=1')">技能开发</button>
        </div>
        <button :class="['nav-btn', { active: activeNav === 'review-center' }]" @click="selectNav('review-center', '/reviews')">审核中心</button>
        <div class="nav-divider"></div>
        <button :class="['nav-btn', { active: activeNav === 'personal-space' }]" @click="selectNav('personal-space', '/team')">个人空间</button>
        <button :class="['nav-btn', { active: activeNav === 'system-management' }]" @click="selectNav('system-management', '/team')">系统管理</button>
        <button :class="['nav-btn', { active: activeNav === 'team-space' }]" @click="selectNav('team-space', '/teamagents')">团队空间</button>
        <button :class="['nav-btn', { active: activeNav === 'agent-evaluation' }]" @click="selectNav('agent-evaluation', '/eval')">智能体评测</button>
        <button :class="['nav-btn', { active: activeNav === 'ops-management' }]" @click="selectNav('ops-management', '/loop')">Ops 管理</button>
      </aside>
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const initialNavByPath: Record<string, string> = {
  '/skills': 'skill-management',
  '/skill-workbench': 'skill-development',
  '/agents': 'agent-center',
  '/eval': 'review-center',
  '/reviews': 'review-center',
  '/teamagents': 'my-agents',
  '/team': 'personal-space',
  '/loop': 'ops-management',
}
// 导航高亮由当前路由唯一决定。业务页面内部 router.push/replace 后也会同步，
// 避免“创建 Skill → 我的开发”仍保留 Skill 管理高亮。
const activeNav = computed(() => initialNavByPath[route.path] || 'workbench')
const skillMenuOpen = ref(true)

function selectNav(_id: string, target: string) {
  router.push(target)
}

const crumb = computed(() => {
  const map: Record<string, string> = {
    '/skills': '团队空间 / 业务服务智能体 · Skill 管理',
    '/skill-workbench': '团队空间 / 业务服务智能体 · Skill 开发工作台',
    '/agents': '团队空间 / 业务服务智能体 · 智能体中心',
    '/eval': '团队空间 / 业务服务智能体 · 评测任务',
    '/reviews': '团队空间 / 业务服务智能体 · Skill 审核中心',
    '/teamagents': '团队空间 / 团队智能体',
    '/team': '团队空间 / 团队管理',
    '/loop': '团队空间 / 闭环工作台',
    '/graphloop': '团队空间 / 图感知闭环',
    '/pipeline-factory': '流水线 / 流水线1·工厂侧',
    '/pipeline-skill': '流水线 / 流水线2·Skill侧',
  }
  return map[route.path] || '团队空间 / 业务服务智能体'
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
