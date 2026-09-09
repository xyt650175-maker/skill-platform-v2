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
        <button class="nav-group nav-group-toggle" @click="skillMenuOpen = !skillMenuOpen">技能 <span>{{ skillMenuOpen ? '⌄' : '›' }}</span></button>
        <div v-show="skillMenuOpen">
          <button :class="['nav-btn nav-child', { active: activeNav === 'skill-management' }]" @click="selectNav('skill-management', '/skills')">技能管理</button>
          <button :class="['nav-btn nav-child', { active: activeNav === 'skill-development' }]" @click="selectNav('skill-development', '/skill-workbench?new=1')">技能开发</button>
          <button :class="['nav-btn nav-child', { active: activeNav === 'review-center' }]" @click="selectNav('review-center', '/reviews')">审核中心</button>
        </div>
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
  '/reviews': 'review-center',
}
// 导航高亮由当前路由唯一决定。业务页面内部 router.push/replace 后也会同步，
// 避免“创建 Skill → 我的开发”仍保留 Skill 管理高亮。
const activeNav = computed(() => initialNavByPath[route.path] || '')
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
