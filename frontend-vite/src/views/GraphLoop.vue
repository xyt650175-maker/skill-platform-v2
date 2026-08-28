<template>
  <div>
    <section class="page-head">
      <strong class="page-title">图感知闭环</strong>
      <span class="chip warn">Badcase #07</span>
      <span class="chip">{{ pendingCount }} 个节点任务</span>
      <div class="actions">
        <button class="btn">查看图版本</button>
        <button class="btn primary" @click="release" :disabled="!released">{{ releaseBtn }}</button>
      </div>
    </section>

    <div class="page-context">
      <b>主 Agent—SubAgent—Skill 路径定位与节点优化</b>
    </div>

    <section class="page-content">
      <div class="notice">图感知闭环：先还原 Badcase 实际经过的主 Agent—SubAgent—Skill 路径，再按问题节点拆分任务；只有相关节点验证完成，才重组新的完整云虾图版本。</div>

      <!-- 失败路径 -->
      <section class="panel">
        <h3>Badcase #07 失败路径</h3>
        <div class="pad">
          <div class="box">
            <b>用户输入</b>
            <div class="hint">"比较两个产品的风险和变化情况。"</div>
          </div>
          <div class="relation">
            <span class="node main">产品服务主 Agent</span>
            <span class="arrow">路由：产品比较</span>
            <span class="node">产品比较 SubAgent</span>
            <span class="arrow">调用</span>
            <span class="node">product-comparison</span>
            <span class="arrow">调用</span>
            <span class="node">product-search 副本</span>
          </div>
          <div class="hint">定位结果：主 Agent 路由正确；产品比较 SubAgent 提示词缺少风险提示；product-search 副本缺少周期字段。</div>
        </div>
      </section>

      <!-- 多节点优化任务 + 分层验证 -->
      <div class="team-grid" style="margin-top:12px">
        <section class="panel">
          <h3>多节点优化任务</h3>
          <div class="work">
            <div class="work-top">
              <b>Task A：产品比较 SubAgent 提示词</b>
              <span :class="['state', taskA==='通过'?'':'wait']">{{ taskA }}</span>
            </div>
            <div class="hint">负责人：SubAgent 管理员。产物：SubAgent Prompt Candidate 4。</div>
            <button class="btn" @click="doA" :disabled="!!doneA">{{ btnA }}</button>
          </div>
          <div class="work">
            <div class="work-top">
              <b>Task B：product-search 绑定副本</b>
              <span :class="['state', taskB==='通过'?'':'wait']">{{ taskB }}</span>
            </div>
            <div class="hint">负责人：Skill 开发者。产物：Skill Candidate 4。</div>
            <button class="btn" @click="doB" :disabled="!!doneB">{{ btnB }}</button>
          </div>
          <div class="work">
            <div class="work-top">
              <b>Task C：绑定关系与云虾图版本</b>
              <span :class="['state', taskC==='通过'?'':'wait']">{{ taskC }}</span>
            </div>
            <div class="hint">负责人：系统管理员。将 Candidate 4 绑定到"产品比较 SubAgent"，生成新的装配图。</div>
            <button class="btn primary" @click="doC" :disabled="!canC || !!doneC">{{ btnC }}</button>
          </div>
        </section>

        <section class="panel">
          <h3>分层验证</h3>
          <div class="work">
            <b>1. Skill 单测</b>
            <div class="hint">只验证 product-search Candidate 4 的输入输出、工具调用与安全。</div>
          </div>
          <div class="work">
            <b>2. SubAgent 场景测试</b>
            <div class="hint">验证产品比较 SubAgent 的提示词、职责边界和其下 Skill 调用。</div>
          </div>
          <div class="work">
            <b>3. 主 Agent — SubAgent 集成测试</b>
            <div class="hint">验证"产品比较"意图是否正确路由至目标 SubAgent。</div>
          </div>
          <div class="work">
            <b>4. 完整云虾批量回归</b>
            <div class="hint">{{ testState }}</div>
            <button class="btn" @click="runRegression" :disabled="!doneC || !!regressionDone">{{ runBtn }}</button>
          </div>
        </section>
      </div>

      <!-- 云虾图版本与定版 -->
      <section class="panel" style="margin-top:12px">
        <h3>云虾图版本与定版</h3>
        <div class="pad">
          <div class="box">
            <b>v2.0 图版本变更</b>
            <div class="hint">仅变更两个节点：产品比较 SubAgent Prompt Candidate 4、product-search 绑定副本 Candidate 4；主 Agent、产品推荐 SubAgent、Harness 和其他 Skill 保持 v1.0 基线版本。</div>
          </div>
          <div class="box">
            <b>定版条件</b>
            <div class="hint">{{ releaseMsg }}</div>
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const doneA = ref<boolean>(false)
const doneB = ref<boolean>(false)
const doneC = ref<boolean>(false)
const regressionDone = ref<boolean>(false)
const released = ref<boolean>(false)

const btnA = ref('验证 SubAgent 场景测试')
const btnB = ref('完成 Skill 单测')
const btnC = ref('重组云虾 v2.0 图版本')
const runBtn = ref('执行图版本回归')
const releaseBtn = ref('管理员定版 v2.0')

const taskA = computed(() => doneA.value ? '通过' : '待处理')
const taskB = computed(() => doneB.value ? '通过' : '待处理')
const taskC = computed(() => {
  if (doneC.value) return '通过'
  if (doneA.value && doneB.value) return '可重组'
  return '等待 A / B'
})
const canC = computed(() => doneA.value && doneB.value)
const pendingCount = computed(() => {
  let n = 0
  if (!doneA.value) n++
  if (!doneB.value) n++
  if (!doneC.value) n++
  return n
})

const testState = computed(() => {
  if (regressionDone.value) return '图版本回归通过；准确率 92%、平均耗时 1.7s、无新增严重 Badcase。'
  if (doneC.value) return '图版本已重组；可执行 SubAgent 场景、路由集成和完整云虾回归。'
  return '等待图版本重组后，使用原 Case #07 和全量集成测试集回归。'
})

const releaseMsg = computed(() => {
  if (released.value) return 'v2.0 图版本已定版发布。'
  if (regressionDone.value) return '全部节点任务与分层测试通过；准确率 92%、平均耗时 1.7s、无新增严重 Badcase，允许系统管理员定版。'
  return '等待：Task A、Task B、Task C 均完成；SubAgent 场景测试、路由集成测试、完整云虾回归全部通过；准确率 ≥ 90%、平均耗时 ≤ 2.0s、无新增严重 Badcase。'
})

function doA() {
  doneA.value = true
  btnA.value = 'SubAgent 场景测试通过'
}
function doB() {
  doneB.value = true
  btnB.value = 'Skill 单测通过'
}
function doC() {
  if (!canC.value) return
  doneC.value = true
  btnC.value = 'v2.0 图版本已重组'
}
function runRegression() {
  if (!doneC.value) return
  regressionDone.value = true
  runBtn.value = '图版本回归通过'
}
function release() {
  if (!regressionDone.value || released.value) return
  released.value = true
  releaseBtn.value = 'v2.0 已定版发布'
}
</script>

<style scoped lang="scss">
.pad { padding: 11px; }
.team-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.work {
  padding: 10px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
  &:last-of-type { border-bottom: 0; }
}
.work-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.relation {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 7px;
  flex-wrap: wrap;
}
.node {
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--panel);
  font-size: 11px;
  &.main { border-color: var(--teal); background: var(--teal); color: #fff; }
}
.arrow { color: var(--teal); font-weight: 700; }
@media (max-width: 850px) {
  .team-grid { grid-template-columns: 1fr; }
}
</style>
