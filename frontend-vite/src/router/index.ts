import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/skills', name: 'SkillManagement', component: () => import('@/views/SkillManagement.vue') },
  { path: '/skill-workbench', name: 'SkillWorkbench', component: () => import('@/views/Skills.vue') },
  { path: '/agents', name: 'Agents', component: () => import('@/views/Agents.vue') },
  { path: '/eval', name: 'Eval', component: () => import('@/views/Eval.vue') },
  { path: '/reviews', name: 'Reviews', component: () => import('@/views/Reviews.vue') },
  { path: '/teamagents', name: 'TeamAgents', component: () => import('@/views/TeamAgents.vue') },
  { path: '/team', name: 'Team', component: () => import('@/views/Team.vue') },
  { path: '/loop', name: 'Loop', component: () => import('@/views/Loop.vue') },
  { path: '/graphloop', name: 'GraphLoop', component: () => import('@/views/GraphLoop.vue') },
  { path: '/pipeline-factory', name: 'PipelineFactory', component: () => import('@/views/PipelineFactory.vue') },
  { path: '/pipeline-skill', name: 'PipelineSkill', component: () => import('@/views/PipelineSkill.vue') },
  { path: '/', redirect: '/skills' },
  { path: '/:pathMatch(.*)*', redirect: '/skills' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  if (to.meta.public || auth.token) {
    next()
  } else {
    next('/login')
  }
})

export default router
