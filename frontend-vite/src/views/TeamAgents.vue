<template>
  <div>
    <section class="page-head">
      <strong class="page-title">团队智能体</strong>
      <span class="chip">业务服务智能体团队</span>
      <span class="chip">3 个智能体</span>
      <div class="actions">
        <button class="btn">导入模板</button>
        <button class="btn primary">创建智能体</button>
      </div>
    </section>

    <div class="page-context">
      <b>团队智能体列表</b>　来自：团队空间
    </div>

    <section class="page-content">
      <section class="panel">
        <h3>团队智能体</h3>
        <div class="team-grid">
          <div class="work" v-for="w in workspaces" :key="w.name">
            <div class="work-top">
              <b>{{ w.name }}</b>
              <span :class="['state', w.state==='稳定版'?'':'wait']">{{ w.state }}</span>
            </div>
            <div class="hint">已挂载 {{ w.skillCount }} 个 Skill · 当前装配版本 {{ w.version }}</div>
            <button class="btn" style="margin-top:7px" @click="onAction(w)">{{ w.action }}</button>
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const workspaces = [
  { name: '产品服务智能体', state: '开发中', skillCount: 3, version: 'v0.1.1-candidate', action: '进入装配' },
  { name: '业务问答智能体', state: '稳定版', skillCount: 5, version: 'v1.3.0', action: '查看详情' },
  { name: '业务报告生成智能体', state: '待评测', skillCount: 2, version: 'v0.3.2-candidate', action: '进入评测任务' },
]

function onAction(w: typeof workspaces[number]) {
  if (w.action === '进入装配') router.push('/agents')
  else if (w.action === '进入评测任务') router.push('/eval')
}
</script>

<style scoped lang="scss">
.team-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px;
}
.work {
  padding: 10px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
  &:last-child { border-bottom: 0; }
}
.work-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
@media (max-width: 850px) {
  .team-grid { grid-template-columns: 1fr; }
}
</style>
