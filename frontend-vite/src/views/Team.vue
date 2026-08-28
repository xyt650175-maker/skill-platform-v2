<template>
  <div>
    <section class="page-head">
      <strong class="page-title">业务服务智能体团队</strong>
      <span class="chip">团队管理</span>
      <span class="chip">{{ members.length }} 位成员</span>
      <div class="actions">
        <button class="btn">导入成员</button>
        <button class="btn primary">邀请成员</button>
      </div>
    </section>

    <div class="page-context">
      <b>团队管理</b>　来自：团队空间
    </div>

    <section class="page-content">
      <section class="panel">
        <h3>团队成员</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>账号</th>
                <th>角色</th>
                <th>负责模块</th>
                <th>状态</th>
                <th>加入时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in members" :key="m.id">
                <td>{{ m.name }}</td>
                <td>{{ m.account }}</td>
                <td><span class="role">{{ m.role }}</span></td>
                <td>{{ m.module }}</td>
                <td><span :class="['state', m.state==='已禁用'?'wait':'']">{{ m.state }}</span></td>
                <td>{{ m.joined }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const members = ref([
  { id: 1, name: '用户 A', account: 'admin@example.internal', role: '系统管理员', module: 'Harness / 主智能体', state: '已激活', joined: '2026-07-12' },
  { id: 2, name: '用户 B', account: 'subagent-a@example.internal', role: 'SubAgent 管理员', module: '产品推荐 SubAgent', state: '已激活', joined: '2026-07-15' },
  { id: 3, name: '用户 C', account: 'subagent-b@example.internal', role: 'SubAgent 管理员', module: '产品比较 SubAgent', state: '已激活', joined: '2026-07-15' },
  { id: 4, name: '用户 D', account: 'developer-a@example.internal', role: 'Skill 开发者', module: 'product-search / product-selection', state: '已激活', joined: '2026-07-18' },
  { id: 5, name: '用户 E', account: 'developer-b@example.internal', role: 'Skill 开发者', module: 'product-comparison', state: '已禁用', joined: '2026-07-20' },
])
</script>
