<template>
  <div>
    <section class="page-head">
      <strong class="page-title">优化闭环与版本定版</strong>
      <span class="chip warn">v2.0 待回归</span>
      <span class="chip">1 个 Badcase</span>
      <div class="actions">
        <button class="btn">查看版本对比</button>
        <button class="btn primary" @click="approve" :disabled="!canApprove">管理员定版 v2.0</button>
      </div>
    </section>

    <div class="page-context">
      <b>Badcase 优化闭环</b>　/　云虾版本定版
    </div>

    <section class="page-content">
      <div class="notice">闭环工作台：Badcase → 提示词 / Skill 优化任务 → Candidate 验证 → 重新装配 → 原案例回归 → 云虾版本定版。</div>

      <div class="team-grid">
        <!-- Badcase #07 优化闭环 -->
        <section class="panel">
          <h3>优化闭环 · Badcase #07</h3>
          <div class="pad">
            <div class="box">
              <b>问题归因</b>
              <div class="hint">产品比较 SubAgent 提示词缺少风险提示；product-search 缺少周期字段。</div>
            </div>
            <div class="box">
              <b>提示词分支 · 系统管理员</b>
              <div class="hint">{{ promptMsg }}</div>
              <button class="btn" @click="verifyPrompt" :disabled="promptDone">{{ promptBtn }}</button>
            </div>
            <div class="box">
              <b>Skill 分支 · Skill 开发者</b>
              <div class="hint">{{ skillMsg }}</div>
              <button class="btn" @click="verifySkill" :disabled="skillDone">{{ skillBtn }}</button>
            </div>
            <div class="box">
              <b>重新装配与回归</b>
              <div class="hint">{{ flowMsg }}</div>
              <button class="btn primary" @click="assemble" :disabled="assembled">{{ assembleBtn }}</button>
              <button class="btn" @click="regression" :disabled="!assembled || regressionDone">{{ regressionBtn }}</button>
            </div>
          </div>
        </section>

        <!-- 完整云虾版本 BOM -->
        <section class="panel">
          <h3>完整云虾版本（BOM）</h3>
          <div class="pad">
            <div class="box">
              <b>v1.0-candidate · 基线</b>
              <div class="hint">准确率 86% · 1.8s · 1 个严重 Badcase。</div>
            </div>
            <div class="box">
              <b>v2.0-candidate · 当前优化版本</b>
              <div class="hint">Harness v1；主智能体提示词 Candidate 3；产品推荐 SubAgent：提示词 Candidate 2 + product-selection v1.0.1；产品比较 SubAgent：提示词 Candidate 4 + product-comparison v1.0.1 + product-search Candidate 4；主智能体直接挂载 product-search Candidate 4。</div>
            </div>
            <div class="box">
              <b>v3.0-candidate · 条件创建</b>
              <div class="hint">仅在 v2.0 未达到准确率、性能或安全门槛时继续迭代创建。</div>
            </div>
          </div>
        </section>
      </div>

      <!-- 定版门槛表 -->
      <section class="panel" style="margin-top:12px">
        <h3>云虾版本定版门槛</h3>
        <table>
          <thead>
            <tr><th>检查项</th><th>门槛</th><th>v2.0 结果</th></tr>
          </thead>
          <tbody>
            <tr><td>准确率</td><td>≥ 90%</td><td><span class="state">通过：92%</span></td></tr>
            <tr><td>平均耗时</td><td>≤ 2.0s</td><td><span class="state">通过：1.7s</span></td></tr>
            <tr><td>Badcase #07</td><td>必须通过</td><td><span :class="['state', regressionDone?'':'wait']">{{ regressionGate }}</span></td></tr>
            <tr><td>新增严重 Badcase</td><td>必须为 0</td><td><span :class="['state', regressionDone?'':'wait']">{{ riskGate }}</span></td></tr>
          </tbody>
        </table>
        <div class="pad">
          <div class="hint">{{ approvalMsg }}</div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const promptDone = ref(false)
const skillDone = ref(false)
const assembled = ref(false)
const regressionDone = ref(false)
const approved = ref(false)

const promptBtn = ref('创建并验证 Prompt Candidate')
const skillBtn = ref('创建 Candidate 并完成单测')
const assembleBtn = ref('重新装配 v2.0 Candidate')
const regressionBtn = ref('使用原 Badcase 回归')

const promptMsg = ref('待创建产品比较 SubAgent Prompt Candidate。')
const skillMsg = ref('待创建 product-search Skill Candidate，并进行单 Skill 测试。')
const flowMsg = ref('两条 Candidate 均通过后，系统管理员才能装配云虾 v2.0 Candidate。')

const regressionGate = computed(() => regressionDone.value ? '通过' : '待回归')
const riskGate = computed(() => regressionDone.value ? '通过：0 个' : '待检查')
const canApprove = computed(() => regressionDone.value && !approved.value)
const approvalMsg = computed(() => {
  if (approved.value) return '系统管理员已定版 v2.0，完整装配清单已锁定。'
  if (!regressionDone.value) return '当前不可定版：请先完成原 Badcase 回归和风险检查。'
  return '所有门槛满足，可由系统管理员定版。'
})

function verifyPrompt() {
  promptDone.value = true
  promptBtn.value = 'Prompt Candidate 已验证'
  promptMsg.value = '产品比较 SubAgent Prompt Candidate 4 已验证通过。'
}

function verifySkill() {
  skillDone.value = true
  skillBtn.value = 'Skill 单测通过'
  skillMsg.value = 'product-search Candidate 4 已创建，Skill 单测 12 / 12 通过。'
}

function assemble() {
  if (!promptDone.value || !skillDone.value) {
    flowMsg.value = '不能装配：提示词 Candidate 和 Skill Candidate 均须验证通过。'
    return
  }
  assembled.value = true
  assembleBtn.value = 'v2.0 Candidate 已重新装配'
  flowMsg.value = '已生成完整云虾 v2.0 Candidate，请使用原 Badcase #07 回归。'
}

function regression() {
  if (!assembled.value) {
    flowMsg.value = '不能回归：请先重新装配云虾 v2.0 Candidate。'
    return
  }
  regressionDone.value = true
  regressionBtn.value = 'Badcase #07 回归通过'
  flowMsg.value = '优化任务已关闭；所有门槛满足，可由系统管理员定版。'
}

function approve() {
  if (!canApprove.value) return
  approved.value = true
}
</script>

<style scoped lang="scss">
.pad { padding: 11px; }
.team-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@media (max-width: 850px) {
  .team-grid { grid-template-columns: 1fr; }
}
</style>
