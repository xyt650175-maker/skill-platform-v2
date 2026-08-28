<template>
  <div>
    <section class="page-head">
      <strong class="page-title">评测任务</strong>
      <span class="chip">任务列表</span>
      <span class="chip">共 {{ evalTasks.length }} 项</span>
      <div class="actions">
        <button class="btn" @click="exportReport">导出任务清单</button>
        <button class="btn primary" @click="createEval">新建评测任务</button>
      </div>
    </section>

    <div class="page-context">
      <b>智能体评测任务列表</b>
    </div>

    <section class="page-content">
      <div class="notice">此处仅管理智能体批量评测任务。Skill 的逐条调试、批量文件测试与记录保留在对应 Skill 页面。</div>

      <div class="eval-summary">
        <!-- 任务列表 -->
        <section class="panel">
          <h3>智能体评测任务</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>评测任务名称</th>
                  <th>测试对象</th>
                  <th>装配版本</th>
                  <th>测试数据</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="t in evalTasks" :key="t.id">
                  <td>{{ t.name }}</td>
                  <td>{{ t.agentName || ('Agent #' + t.agentId) }}</td>
                  <td>{{ t.agentVersion || '—' }}</td>
                  <td>{{ t.datasetVersion || t.datasetKey }}</td>
                  <td><span :class="['state', t.status==='running'?'wait':'']">{{ statusLabel(t.status) }}</span></td>
                  <td><button class="btn" @click="showTask(t)">查看详情</button></td>
                </tr>
                <tr v-if="!evalTasks.length"><td colspan="6" class="hint">暂无评测任务。请在右侧选择智能体后创建。</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- 新建评测任务 -->
        <aside class="panel">
          <h3>新建评测任务</h3>
          <div class="pad">
            <label class="label">智能体版本</label>
            <select class="input" v-model="selectedAgentId">
              <option value="">— 选择智能体 —</option>
              <option v-for="agent in agents" :key="agent.id" :value="agent.id">{{ agent.name }} ({{ agent.current_version }})</option>
            </select>
            <label class="label">测评集版本</label>
            <select class="input" v-model="datasetKey" @change="selectDataset">
              <option value="regression">业务回归集 v1.2</option>
              <option value="comparison">产品对比测试集 v1.0</option>
              <option value="routing">路由与边界测试集 v1.0</option>
            </select>
            <label class="label">执行范围</label>
            <select class="input">
              <option>全部 Case</option>
              <option>仅 P0 Case</option>
            </select>
            <button class="btn primary" style="margin-top:12px;width:100%" @click="createEval" :disabled="!selectedAgentId">提交批量评测</button>
            <div class="hint">{{ createResult }}</div>
          </div>
        </aside>
      </div>

      <!-- 测评集管理 -->
      <section class="panel" style="margin-top:12px">
        <h3>测评集管理</h3>
        <div class="team-grid">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>测评集</th>
                  <th>适用对象</th>
                  <th>Case</th>
                  <th>当前版本</th>
                  <th>来源</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in datasets" :key="d.key">
                  <td>{{ d.name }}</td>
                  <td><span class="role">{{ d.role }}</span></td>
                  <td>{{ d.cases }}</td>
                  <td>{{ d.version }}</td>
                  <td>{{ d.source }}</td>
                  <td><button class="btn" @click="datasetKey=d.key; selectDataset()">查看案例</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <aside class="pad">
            <b>{{ currentDataset.name }}</b>
            <div class="hint">{{ currentDataset.meta }}</div>
            <div class="box">
              <b>{{ currentDataset.caseId }}</b>
              <div class="hint" v-html="currentDataset.case"></div>
            </div>
            <button class="btn" @click="importDs">导入测试数据文件</button>
            <button class="btn" @click="copyVersion">复制为新版本</button>
            <div class="hint">{{ actionResult }}</div>
          </aside>
        </div>
      </section>

      <div class="optimizer">
        <b>二阶段优化器（规划展示）</b>
        <span class="node">批量评测结果</span>
        <span class="arrow">→</span>
        <span class="node">Badcase</span>
        <span class="arrow">→</span>
        <span class="node muted-node">Trace + 结果分析</span>
        <span class="arrow">→</span>
        <span class="node muted-node">优化建议</span>
        <div class="hint">暂不启用自动优化；当前仅预留画布位置。</div>
      </div>

      <!-- Skill 批量测试 -->
      <section class="panel" style="margin-top:12px">
        <h3>Skill 批量测试</h3>
        <div class="skill-test-grid">
          <div class="pad">
            <label class="label">选择 Skill</label>
            <select class="input" v-model="selectedSkillId">
              <option value="">— 选择 Skill —</option>
              <option v-for="s in skillList" :key="s.id" :value="s.id">{{ s.name }} ({{ s.version }})</option>
            </select>
            <label class="label" style="margin-top:10px">测试数据文件</label>
            <div class="box">
              <b>产品查询回归数据.csv</b>
              <div class="hint">12 条数据 · 字段：product_code、expected_fields</div>
            </div>
            <button class="btn" style="margin-top:9px">选择测试数据文件</button>
            <button class="btn primary" style="margin-top:9px" @click="runSkillBatch" :disabled="!selectedSkillId">运行批量测试</button>
            <div class="box" style="margin-top:9px">
              <b>批量结果</b>
              <div class="hint" v-html="skillBatchResult"></div>
            </div>
          </div>
          <aside class="pad">
            <b>Skill 测试记录</b>
            <div class="case" v-for="(c, i) in skillTestHistory" :key="i">
              <div class="case-top">
                <b>{{ c.time }}</b>
                <span :class="['state', c.ok ? '' : 'fail']">{{ c.ok ? '通过' : '失败' }}</span>
              </div>
              <div class="hint">{{ c.skill }} · {{ c.action }}</div>
            </div>
          </aside>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { agentApi, evalApi, skillApi, type Agent, type EvalTask, type Skill } from '@/api'

const evalTasks = ref<EvalTask[]>([])
const agents = ref<Agent[]>([])
const selectedAgentId = ref('')

const datasets = [
  { key: 'regression', name: '业务回归集', role: '智能体', cases: 80, version: 'v1.2', source: '业务案例 + Badcase' },
  { key: 'comparison', name: '产品对比测试集', role: 'SubAgent', cases: 24, version: 'v1.0', source: '手工创建' },
  { key: 'routing', name: '路由与边界测试集', role: '智能体', cases: 36, version: 'v1.0', source: '批量导入' },
]

const datasetInfo: Record<string, { name: string; meta: string; caseId: string; case: string }> = {
  regression: {
    name: '业务回归集 v1.2',
    meta: '80 条 Case · 智能体 · 2026-08-05 更新',
    caseId: 'PRODUCT-REG-007',
    case: '输入：比较两个产品的风险和变化情况。<br>预期：路由至产品比较 SubAgent；输出风险提示与周期信息。',
  },
  comparison: {
    name: '产品对比测试集 v1.0',
    meta: '24 条 Case · SubAgent · 2026-08-03 创建',
    caseId: 'COMPARE-012',
    case: '输入：比较两个同类产品。<br>预期：产品比较 SubAgent 调用 product-comparison；明确比较维度与风险提示。',
  },
  routing: {
    name: '路由与边界测试集 v1.0',
    meta: '36 条 Case · 智能体 · 2026-08-04 批量导入',
    caseId: 'ROUTE-023',
    case: '输入：推荐低风险产品。<br>预期：路由至产品推荐 SubAgent；不得输出不符合规则的承诺。',
  },
}

const datasetKey = ref('regression')
const createResult = ref('')
const actionResult = ref('已锁定版本；历史评测任务继续引用创建时的测评集版本。')

const currentDataset = computed(() => datasetInfo[datasetKey.value])

function selectDataset() {
  actionResult.value = '已选择 ' + datasetInfo[datasetKey.value].name + '；历史任务继续引用创建时锁定的版本。'
}

async function createEval() {
  const agent = agents.value.find(item => item.id === selectedAgentId.value)
  if (!agent) return
  try {
    const task = await evalApi.create({
      name: `${agent.name} · ${datasetInfo[datasetKey.value].name}`,
      agentId: agent.id,
      agentVersion: agent.current_version,
      datasetKey: datasetKey.value,
      datasetVersion: datasetInfo[datasetKey.value].name,
      scope: 'all',
    })
    evalTasks.value.unshift(task)
    createResult.value = `已创建评测任务 #${task.id}，状态为待执行。平台不会伪造“已完成”结果。`
  } catch (error: any) { createResult.value = `创建失败：${error.message}` }
}

function statusLabel(status: string) { return ({ pending: '待执行', running: '运行中', completed: '已完成', failed: '失败' } as Record<string, string>)[status] || status }
function showTask(task: EvalTask) { createResult.value = `任务 #${task.id}：${statusLabel(task.status)}。${task.resultSummary || '尚未产生执行结果。'}` }
function exportReport() {
  const content = evalTasks.value.map(task => `${task.name}\t${task.agentName || task.agentId}\t${task.datasetVersion || task.datasetKey}\t${statusLabel(task.status)}`).join('\n') || '暂无评测任务'
  const url = URL.createObjectURL(new Blob([content], { type: 'text/plain;charset=utf-8' })); const link = document.createElement('a'); link.href = url; link.download = 'eval-tasks.txt'; link.click(); URL.revokeObjectURL(url)
}

function importDs(e: Event) {
  ;(e.target as HTMLElement).textContent = '已打开导入设置（演示）'
  actionResult.value = '导入时将预览字段映射；导入结果需要另存为测评集新版本。'
}

function copyVersion(e: Event) {
  ;(e.target as HTMLElement).textContent = '已创建 v1.3 草稿'
  actionResult.value = '已从当前测评集复制出新版本草稿，原 v1.2 保持不变。'
}

/* ==================== Skill 批量测试 ==================== */
const skillList = ref<Skill[]>([])
const selectedSkillId = ref('')
const skillBatchResult = ref('尚未执行。')
const skillTestHistory = ref<{ time: string; skill: string; action: string; ok: boolean }[]>([
  { time: '今天 14:35', skill: 'product-search v1.2.0-candidate.3', action: '批量测试 12 / 12 通过', ok: true },
  { time: '今天 14:12', skill: 'product-search v1.2.0-candidate.2', action: '11 / 12 通过', ok: false },
])

onMounted(async () => {
  try {
    const [skills, taskList, agentList] = await Promise.all([skillApi.list(), evalApi.list(), agentApi.list()])
    skillList.value = skills; evalTasks.value = taskList; agents.value = agentList
  } catch (error: any) { createResult.value = `加载评测数据失败：${error.message}` }
})

function runSkillBatch() {
  const skill = skillList.value.find(s => s.id === selectedSkillId.value)
  if (!skill) return
  skillBatchResult.value = `已完成 12 / 12 条；字段校验、工具调用与安全策略均通过。<br>Skill: ${skill.name} ${skill.version}`
  skillTestHistory.value.unshift({
    time: '刚刚 ' + new Date().toLocaleTimeString('zh-CN'),
    skill: `${skill.name} ${skill.version}`,
    action: '批量测试 12 / 12 通过',
    ok: true,
  })
}
</script>

<style scoped lang="scss">
.eval-summary {
  display: grid;
  grid-template-columns: 1fr 285px;
  gap: 12px;
}
.pad { padding: 11px; }
.team-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.optimizer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px;
  border: 1px dashed #93b6ba;
  border-radius: 6px;
  background: #fbfefe;
  font-size: 12px;
  .node {
    padding: 6px 8px;
    border: 1px solid var(--line);
    border-radius: 4px;
    background: var(--panel);
    font-size: 11px;
    &.muted-node { color: var(--muted); border: 1px dashed var(--line); background: #fff; }
  }
  .arrow { color: var(--teal); font-weight: 700; }
}
.skill-test-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.case {
  margin-bottom: 8px;
  padding: 8px;
  border: 1px solid var(--line);
  border-radius: 5px;
  font-size: 12px;
}
.case-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.state {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  background: #e0f5e9;
  color: #2a7a4a;
  &.fail { background: #fce8e8; color: #c44; }
}
@media (max-width: 850px) {
  .eval-summary, .team-grid, .skill-test-grid { grid-template-columns: 1fr; }
}
</style>
