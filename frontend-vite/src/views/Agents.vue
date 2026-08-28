<template>
  <div>
    <!-- 智能体列表视图（当没有选择智能体时显示） -->
    <div v-if="!agentId">
      <section class="page-head">
        <strong class="page-title">智能体中心</strong>
        <div class="actions">
          <button class="btn" @click="loadAgents">刷新</button>
          <button class="btn primary" @click="showCreateAgent = true">+ 创建智能体</button>
        </div>
      </section>
      <section class="page-content">
        <div v-if="agentsLoading" class="pad hint">加载中...</div>
        <div v-else-if="!agents.length" class="pad hint">暂无智能体，请创建第一个智能体。</div>
        <div v-else class="agent-list">
          <div class="agent-card" v-for="a in agents" :key="a.id" @click="selectAgent(a.id)">
            <div class="agent-card-head">
              <b>{{ a.name }}</b>
              <span class="chip" :class="a.status === 'draft' ? 'warn' : 'ok'">{{ a.status === 'draft' ? '开发中' : a.status }}</span>
            </div>
            <p>{{ a.description || '暂无描述' }}</p>
            <div class="agent-card-foot">
              <span>版本: {{ a.currentVersion || '0.0.0' }}</span>
              <span>模型: {{ a.modelName || '未配置' }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 智能体详情视图（选择智能体后显示） -->
    <div v-else>
      <section class="page-head">
        <div class="head-left">
          <button class="btn sm" @click="agentId = ''; loadAgents()">← 返回列表</button>
          <strong class="page-title">{{ agent?.name || '智能体' }}</strong>
          <span class="chip warn" v-if="agent?.status === 'draft'">开发中</span>
          <span class="chip ok" v-else>{{ agent?.status }}</span>
          <!-- 版本选择器 -->
          <div class="version-selector">
            <label>版本：</label>
            <select class="input version-select" v-model="currentVersion" @change="onVersionChange">
              <option v-for="v in versions" :key="v.version" :value="v.version">{{ v.version }}</option>
            </select>
            <button class="btn sm" @click="showCreateVersion = true">+ 新建版本</button>
          </div>
        </div>
        <div class="actions">
          <button class="btn" @click="saveDraft" :disabled="saving">保存草稿</button>
          <button class="btn primary" @click="goEval">提交到评测任务</button>
        </div>
      </section>

    <div class="page-context">
      <b>智能体开发与装配</b>　来自：智能体中心
    </div>

    <!-- 新建版本对话框 -->
    <div v-if="showCreateVersion" class="modal-mask" @click.self="showCreateVersion = false">
      <div class="modal-box">
        <div class="modal-head">
          <b>创建新版本</b>
          <button class="close-btn" @click="showCreateVersion = false">×</button>
        </div>
        <div class="modal-body">
          <label class="form-label">版本号 <small>x.y.z 格式</small></label>
          <input class="input" v-model="newVersionForm.version" placeholder="例如：1.0.0" />
          <label class="form-label">变更说明</label>
          <textarea class="input textarea" v-model="newVersionForm.changeSummary" rows="3" placeholder="描述本次版本的主要变更"></textarea>
        </div>
        <div class="modal-foot">
          <button class="btn" @click="showCreateVersion = false">取消</button>
          <button class="btn primary" @click="createVersion" :disabled="creatingVersion">{{ creatingVersion ? '创建中...' : '创建' }}</button>
        </div>
      </div>
    </div>

    <section class="page-content">
      <div class="notice">这里展示完整的"主智能体 → SubAgent → Skill"装配关系。切换版本可查看不同版本的 Skill 挂载情况。</div>

      <div class="agent-grid">
        <!-- 左：装配清单 -->
        <section class="panel">
          <h3>装配清单 <small v-if="currentVersion">（{{ currentVersion }}）</small></h3>
          <div v-if="loadingMountings" class="pad hint">加载中...</div>
          <div v-else-if="!mountings.length" class="pad hint">当前版本暂无 Skill 挂载</div>
          <template v-else>
            <div class="skill-item" v-for="item in mountings" :key="item.id">
              <div class="skill-item-head">
                <b>{{ item.skillAlias || item.skillName }}</b>
                <span class="version-tag" v-if="item.agentVersion">{{ item.agentVersion }}</span>
              </div>
              <div class="hint">Skill：{{ item.skillName }}<br>状态：{{ item.enabled ? '启用' : '禁用' }}</div>
            </div>
          </template>
          <div class="pad">
            <button class="btn primary">管理挂载</button>
          </div>
        </section>

        <!-- 中：智能体编排画布 -->
        <section class="panel">
          <h3>智能体编排</h3>
          <div class="canvas">
            <div class="agent-node main">
              {{ agent?.name || '主智能体' }}<br>
              <small>{{ agent?.modelName || '未配置模型' }}</small>
              <span class="sub-skill">当前版本：{{ currentVersion }}</span>
            </div>
            <div class="line"></div>
            <div class="branch">
              <div class="agent-node" v-for="sub in subAgents" :key="sub.id">
                {{ sub.name }}<br>
                <small>{{ sub.modelName || '默认模型' }}</small>
              </div>
            </div>
          </div>
        </section>

        <!-- 右：提示词与权限 -->
        <section class="panel">
          <h3>提示词与权限</h3>
          <div class="pad">
            <label class="label">当前配置对象</label>
            <select class="input" v-model="subagentTarget" @change="onSubagentChange">
              <option value="main">主智能体系统提示词</option>
              <option v-for="sub in subAgents" :key="sub.id" :value="'sub-' + sub.id">{{ sub.name }}</option>
            </select>
            <textarea class="area" v-model="subagentPrompt" style="height:150px" placeholder="输入系统提示词..."></textarea>
            <div class="hint">{{ subagentOwner }}</div>
            <button class="btn" @click="savePrompt">保存提示词</button>
            <div class="sub-config">
              <b>权限边界</b>
              <div class="hint">{{ subagentRight }}</div>
            </div>
            <button class="btn primary" style="margin-top:10px">提交装配</button>
          </div>
        </section>
      </div>
    </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { agentApi } from '@/api'

const router = useRouter()
const route = useRoute()

const agents = ref<any[]>([])
const agentsLoading = ref(false)
const showCreateAgent = ref(false)
const agentId = ref<string>(route.query.agentId as string || '')
const agent = ref<any>(null)
const versions = ref<any[]>([])
const currentVersion = ref('')
const subAgents = ref<any[]>([])
const mountings = ref<any[]>([])
const loadingMountings = ref(false)

const showCreateVersion = ref(false)
const creatingVersion = ref(false)
const newVersionForm = reactive({ version: '', changeSummary: '' })

const saving = ref(false)

const subagentTarget = ref('main')
const subagentPrompt = ref('')
const subagentOwner = ref('维护角色：系统管理员')
const subagentRight = ref('系统管理员：配置 Harness、主智能体、SubAgent、权限并统一装配。')

function onSubagentChange() {
  if (subagentTarget.value === 'main') {
    subagentPrompt.value = agent.value?.systemPrompt || '# 主智能体角色\n你是智能体助手。'
    subagentOwner.value = '维护角色：系统管理员'
    subagentRight.value = '系统管理员：配置 Harness、主智能体、SubAgent、权限并统一装配。'
  } else {
    const subId = subagentTarget.value.replace('sub-', '')
    const sub = subAgents.value.find(s => String(s.id) === subId)
    subagentPrompt.value = sub?.systemPrompt || ''
    subagentOwner.value = `维护角色：${sub?.name || ''} 管理员`
    subagentRight.value = 'SubAgent 管理员：只配置自己负责的 SubAgent，并装配其下 Skill。'
  }
}

async function loadAgent() {
  if (!agentId.value) return
  try {
    agent.value = await agentApi.getById(agentId.value)
    versions.value = agent.value.versions || []
    currentVersion.value = agent.value.currentVersion || (versions.value[0]?.version) || ''
    subAgents.value = await agentApi.listSubAgents(agentId.value)
  } catch (e: any) {
    console.error('加载智能体失败:', e)
  }
}

async function loadMountings() {
  if (!agentId.value) return
  loadingMountings.value = true
  try {
    mountings.value = await agentApi.listMountings(agentId.value, currentVersion.value || undefined)
  } catch (e: any) {
    console.error('加载挂载列表失败:', e)
  } finally {
    loadingMountings.value = false
  }
}

async function onVersionChange() {
  await loadMountings()
}

async function createVersion() {
  if (!newVersionForm.version) {
    alert('请填写版本号')
    return
  }
  if (!/^\d+\.\d+\.\d+$/.test(newVersionForm.version)) {
    alert('版本号需使用 x.y.z 格式')
    return
  }
  creatingVersion.value = true
  try {
    const version = await agentApi.createVersion(agentId.value, newVersionForm)
    versions.value.unshift(version)
    currentVersion.value = version.version
    showCreateVersion.value = false
    newVersionForm.version = ''
    newVersionForm.changeSummary = ''
    await loadMountings()
  } catch (e: any) {
    alert('创建版本失败：' + e.message)
  } finally {
    creatingVersion.value = false
  }
}

async function saveDraft() {
  saving.value = true
  try {
    await agentApi.update({
      id: String(agent.value?.id || agentId.value),
      systemPrompt: subagentPrompt.value,
    } as any)
    alert('已保存草稿')
  } catch (e: any) {
    alert('保存失败：' + e.message)
  } finally {
    saving.value = false
  }
}

function savePrompt(e: Event) {
  ;(e.target as HTMLElement).textContent = '已保存'
}

async function loadAgents() {
  agentsLoading.value = true
  try {
    agents.value = await agentApi.list()
  } catch (e: any) {
    console.error('加载智能体列表失败:', e)
  } finally {
    agentsLoading.value = false
  }
}

function selectAgent(id: string) {
  agentId.value = id
  loadAgent()
  loadMountings()
}

function goCreateAgent() {
  showCreateAgent.value = false
}

function goEval() {
  router.push('/eval')
}

onMounted(async () => {
  if (agentId.value) {
    await loadAgent()
    await loadMountings()
    onSubagentChange()
  } else {
    await loadAgents()
  }
})
</script>

<style scoped lang="scss">
.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.version-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  label { font-size: 13px; color: #607087; }
}
.version-select {
  width: 120px;
  padding: 5px 8px;
  font-size: 13px;
}
.agent-grid {
  display: grid;
  grid-template-columns: 27% minmax(0, 1fr) 27%;
  gap: 12px;
}
.pad { padding: 11px; }
.skill-item {
  padding: 10px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
  &:last-of-type { border-bottom: 0; }
}
.skill-item-head {
  display: flex;
  align-items: center;
  gap: 6px;
}
.version-tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  background: #eef1f5;
  color: #56657a;
}
.canvas {
  min-height: 370px;
  padding: 34px 12px;
  background-image: radial-gradient(var(--line) 1px, transparent 1px);
  background-size: 16px 16px;
}
.agent-node {
  width: 138px;
  margin: 0 auto;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fff;
  text-align: center;
  font-size: 12px;
  &.main {
    border-color: var(--teal);
    background: var(--teal);
    color: #fff;
  }
}
.line {
  width: 1px;
  height: 40px;
  margin: auto;
  background: var(--teal);
}
.branch {
  display: flex;
  justify-content: center;
  gap: 8px;
  position: relative;
  &:before {
    position: absolute;
    top: 0;
    left: 18%;
    right: 18%;
    border-top: 1px solid var(--teal);
    content: '';
  }
  .agent-node {
    width: 122px;
    margin-top: 16px;
  }
}
.sub-skill {
  display: block;
  margin-top: 7px;
  padding: 5px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--soft);
  color: var(--teal);
  font-size: 10px;
  .agent-node.main & {
    border-color: rgba(255,255,255,0.4);
    background: rgba(255,255,255,0.15);
    color: #fff;
  }
}
.sub-config {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}
.modal-mask {
  position: fixed;
  z-index: 20;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 20px;
  background: #12213a66;
}
.modal-box {
  width: min(480px, 100%);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 40px #0e1b3180;
}
.modal-head, .modal-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
}
.modal-head { border-bottom: 1px solid #e6ebf2; }
.modal-body { display: grid; gap: 12px; padding: 20px; }
.modal-foot { justify-content: flex-end; gap: 8px; border-top: 1px solid #e6ebf2; }
.form-label {
  display: block;
  font-size: 13px;
  color: #45556c;
  small { color: #8a96a6; }
}
.textarea { resize: vertical; }
.close-btn {
  border: 0;
  background: none;
  color: #7d899a;
  cursor: pointer;
  font-size: 24px;
}
@media (max-width: 850px) {
  .agent-grid { grid-template-columns: 1fr; }
}

/* 智能体列表 */
.agent-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
.agent-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  cursor: pointer;
  transition: transform .15s, box-shadow .15s;
}
.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px #263b5a20;
}
.agent-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.agent-card-head b {
  font-size: 16px;
  color: #253650;
}
.agent-card p {
  margin: 8px 0;
  color: #607087;
  font-size: 13px;
  line-height: 1.5;
}
.agent-card-foot {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  color: #8490a2;
}
</style>
