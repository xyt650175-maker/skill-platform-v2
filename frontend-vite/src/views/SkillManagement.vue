<template>
  <div class="skill-management">
    <section class="page-head">
      <div>
        <strong class="page-title">Skill 管理</strong>
        <p class="sub-title">统一管理公共、个人与团队 Skill；创建后进入开发工作台持续迭代。</p>
      </div>
      <div class="actions">
        <button class="btn" @click="load" :disabled="loading">刷新</button>
        <button class="btn primary" @click="openCreate">+ 创建 Skill</button>
      </div>
    </section>

    <div v-if="creationChoiceVisible" class="modal-mask" @click.self="creationChoiceVisible = false">
      <section class="creation-choice" role="dialog" aria-modal="true" aria-label="选择创建方式">
        <header class="modal-head"><h2>创建 Skill</h2><button class="close" @click="creationChoiceVisible = false">×</button></header>
        <div class="choice-cards">
          <button class="choice-card" @click="chooseCreationMode('online')"><span class="choice-icon">✎</span><b>在线创建</b><small>在平台编辑器中创建并保存</small></button>
          <button class="choice-card" @click="chooseCreationMode('local')"><span class="choice-icon">☁</span><b>从本地导入</b><small>上传本地 Skill 文件导入</small></button>
          <button class="choice-card" @click="chooseCreationMode('git')"><span class="choice-icon">⎇</span><b>从 Git 导入</b><small>从 Git 仓库导入并继续开发</small></button>
        </div>
        <footer class="modal-foot"><button class="btn" @click="creationChoiceVisible = false">关闭</button></footer>
      </section>
    </div>

    <section class="toolbar">
      <div>
        <div class="tabs filter-tabs">
          <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
            {{ tab.label }} <span>({{ tab.count }})</span>
          </button>
        </div>
        <div class="category-tabs" aria-label="Skill 分类">
          <button v-for="category in categories" :key="category" :class="{ active: activeCategory === category }" @click="activeCategory = category">{{ category }}</button>
        </div>
      </div>
      <input v-model.trim="keyword" class="input search" placeholder="搜索 Skill 名称、描述…" />
    </section>

    <div v-if="loading" class="empty-state">正在加载 Skill…</div>
    <div v-else-if="filteredSkills.length === 0" class="empty-state">
      <b>暂无符合条件的 Skill</b>
      <span>可通过“在线创建”建立一个新的 Skill，再进入开发工作台编写和调试。</span>
    </div>
    <section v-else class="skill-grid">
      <article v-for="skill in filteredSkills" :key="skill.id" class="skill-card" @click="enterWorkbench(skill)">
        <div class="skill-card-head">
          <span class="avatar" :class="visibilityClass(skill.visibility)">{{ skill.name.slice(0, 1).toUpperCase() }}</span>
          <div class="skill-name-wrap">
            <h3>{{ skill.name }}</h3>
            <span class="meta">{{ skill.language }} · {{ skill.entry_file }}</span>
          </div>
          <span class="visibility" :class="visibilityClass(skill.visibility)">{{ visibilityLabel(skill.visibility) }}</span>
        </div>
        <p class="description">{{ skill.description || '暂无简介，可进入开发工作台补充需求与实现。' }}</p>
        <div class="card-footer"><span class="category-label">{{ skillCategory(skill) }}</span>
          <span>{{ skill.version || 'v0.0.0' }}</span>
          <span>{{ statusLabel(skill.status) }}</span>
        </div>
        <div class="card-actions">
          <button v-for="action in skillActions(skill)" :key="action" :class="['card-action', { primary: isPrimaryAction(action) }]" @click.stop="handleAction(action, skill)" :disabled="action === '审核中'">{{ action }}</button>
        </div>
      </article>
    </section>

    <div v-if="detailSkill" class="modal-mask" @click.self="detailSkill = null">
      <section class="create-modal small-modal" role="dialog" aria-modal="true" aria-label="Skill 详情">
        <header class="modal-head"><h2>{{ detailSkill.name }}</h2><button class="close" @click="detailSkill = null">×</button></header>
        <div class="modal-body detail-list">
          <p><b>简介：</b>{{ detailSkill.description || '暂无简介' }}</p>
          <p><b>可见范围：</b>{{ visibilityLabel(detailSkill.visibility) }}</p>
          <p><b>状态：</b>{{ statusLabel(detailSkill.status) }}</p>
          <p><b>版本：</b>{{ detailSkill.version }}</p>
          <p><b>入口文件：</b>{{ detailSkill.entry_file }}</p>
          <p><b>最近修改：</b>{{ detailSkill.updated_at || '—' }}</p>
        </div>
        <footer class="modal-foot"><button class="btn" @click="detailSkill = null">关闭</button><button v-if="detailSkill.visibility === 'private'" class="btn primary" @click="enterWorkbench(detailSkill)">进入开发</button></footer>
      </section>
    </div>

    <div v-if="trialSkill" class="modal-mask" @click.self="trialSkill = null">
      <section class="create-modal small-modal" role="dialog" aria-modal="true" aria-label="在线试用">
        <header class="modal-head"><h2>在线试用 · {{ trialSkill.name }}</h2><button class="close" @click="trialSkill = null">×</button></header>
        <div class="modal-body"><label class="form-row"><span>测试输入</span><textarea v-model="trialInput" class="input textarea" rows="4" /></label><pre v-if="trialResult" class="trial-result">{{ trialResult }}</pre></div>
        <footer class="modal-foot"><button class="btn" @click="trialSkill = null">关闭</button><button class="btn primary" @click="runTrial">运行试用</button></footer>
      </section>
    </div>

    <div v-if="createVisible" class="modal-mask" @click.self="createVisible = false">
      <section class="create-modal" role="dialog" aria-modal="true" aria-label="创建 Skill">
        <header class="modal-head">
          <h2>创建 Skill</h2>
          <button class="close" @click="createVisible = false" aria-label="关闭">×</button>
        </header>
        <div class="modal-body">
          <label class="form-row inline"><span><i>*</i> 可见范围</span>
            <div class="radio-group">
              <label><input v-model="form.visibility" type="radio" value="private" /> 私有</label>
              <label><input v-model="form.visibility" type="radio" value="team" /> 团队</label>
            </div>
          </label>
          <label class="form-row"><span><i>*</i> 名称</span>
            <input v-model.trim="form.name" class="input" maxlength="128" placeholder="例如：产品信息查询" />
          </label>
          <label class="form-row"><span><i>*</i> 简介</span>
            <textarea v-model.trim="form.description" class="input textarea" maxlength="512" rows="3" placeholder="说明 Skill 能解决的问题、输入与输出。" />
          </label>
          <label class="form-row"><span>Git 仓库</span>
            <input v-model.trim="form.gitRepoPath" class="input" placeholder="https://code.example.com/team/my-skill.git（可稍后配置）" />
            <small>用于后续版本管理；在线创建时可先留空，系统会初始化 Skill 文件。</small>
          </label>
          <label class="form-row inline"><span><i>*</i> 创建方式</span>
            <div class="radio-group">
              <label><input v-model="form.creationMode" type="radio" value="model" /> 模型驱动</label>
              <label><input v-model="form.creationMode" type="radio" value="template" /> 模板驱动</label>
              <label><input v-model="form.creationMode" type="radio" value="custom" /> 自定义</label>
            </div>
          </label>
          <label v-if="form.creationMode === 'model'" class="form-row"><span>模型选择</span>
            <select v-model="form.model" class="input"><option>Qwen3.7-Plus</option><option>Think-Medium-Flash</option></select>
          </label>
          <p class="mode-hint">{{ modeHint }}</p>
        </div>
        <footer class="modal-foot">
          <button class="btn" @click="createVisible = false">取消</button>
          <button class="btn primary" :disabled="creating" @click="createSkill">{{ creating ? '创建中…' : '创建并进入开发' }}</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { skillApi, type Skill } from '@/api'
import { matchesSkillFilter, parseDebugInput, skillActions, skillCategory } from '@/domain/skillWorkspace'

type TabKey = 'all' | 'public' | 'private' | 'team' | 'released'

const router = useRouter()
const skills = ref<Skill[]>([])
const keyword = ref('')
const activeTab = ref<TabKey>('all')
const activeCategory = ref('全部')
const categories = ['全部', '金融类', '资讯类', '工具类', '风控类', '客服类']
const loading = ref(false)
const createVisible = ref(false)
const creationChoiceVisible = ref(false)
const creating = ref(false)
const detailSkill = ref<Skill | null>(null)
const trialSkill = ref<Skill | null>(null)
const trialInput = ref('{"query":"示例查询"}')
const trialResult = ref('')
const form = reactive({
  visibility: 'private' as NonNullable<Skill['visibility']>,
  name: '',
  description: '',
  gitRepoPath: '',
  creationMode: 'model',
  model: 'Qwen3.7-Plus',
})

const tabs = computed(() => [
  { key: 'all' as const, label: '全部', count: skills.value.length },
  { key: 'private' as const, label: '我的 Skill', count: skills.value.filter(s => (s.visibility || 'private') === 'private').length },
  { key: 'team' as const, label: '团队 Skill', count: skills.value.filter(s => s.visibility === 'team').length },
  { key: 'released' as const, label: '已发布', count: skills.value.filter(s => s.status === 'released').length },
])

const filteredSkills = computed(() => {
  const term = keyword.value.toLowerCase()
  return skills.value.filter(skill => matchesSkillFilter(skill, { scope: activeTab.value, category: activeCategory.value, keyword: term }))
})

const modeHint = computed(() => ({
  model: '创建后将进入开发工作台，可直接通过 AI 对话生成首版文件。',
  template: '创建后将进入开发工作台，可从标准 Skill 文件结构开始补充。',
  custom: '创建后将进入开发工作台，按自己的目录与代码方式编写。',
})[form.creationMode])

onMounted(load)

async function load() {
  loading.value = true
  try {
    skills.value = await skillApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  router.push({ path: '/skill-workbench', query: { new: '1' } })
}

function chooseCreationMode(mode: 'online' | 'local' | 'git') {
  creationChoiceVisible.value = false
  if (mode !== 'online') {
    router.push({ path: '/skill-workbench', query: { mode } })
    return
  }
  Object.assign(form, { visibility: 'private', name: '', description: '', gitRepoPath: '', creationMode: 'model', model: 'Qwen3.7-Plus' })
  createVisible.value = true
}

async function createSkill() {
  if (!form.name) return window.alert('请填写 Skill 名称')
  if (!form.description) return window.alert('请填写 Skill 简介')
  creating.value = true
  try {
    const skill = await skillApi.create({
      name: form.name,
      description: form.description,
      language: 'python',
      entry_file: 'scripts/main.py',
      code_path: form.gitRepoPath || undefined,
      visibility: form.visibility,
    })
    createVisible.value = false
    router.push({ path: '/skill-workbench', query: { skillId: skill.id, mode: form.creationMode, model: form.model } })
  } catch (error: any) {
    window.alert(error?.message || '创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

function enterWorkbench(skill: Skill) {
  router.push({ path: '/skill-workbench', query: { skillId: skill.id } })
}

async function handleAction(action: string, skill: Skill) {
  if (action === '查看' || action === '版本历史' || action === '查看记录') { detailSkill.value = skill; return }
  if (action === '在线试用') { trialSkill.value = skill; trialResult.value = ''; return }
  if (action === '进入开发') { enterWorkbench(skill); return }
  if (action === '复制到我的') {
    const copy = await skillApi.create({ name: `${skill.name}-副本-${Date.now().toString(36)}`, description: skill.description, language: skill.language, entry_file: skill.entry_file, visibility: 'private' })
    const source = await skillApi.getCode(skill.id).catch(() => null)
    if (source?.files) await Promise.all(Object.entries(source.files).map(([path, content]) => skillApi.saveCode(copy.id, String(content), path)))
    await load()
    enterWorkbench(copy)
    return
  }
  if (action === '申请编辑') window.alert('已提交编辑权限申请，管理员审核后会通知你。')
  if (action === '下线') window.alert('下线功能需接入审核服务；当前原型仅展示权限入口。')
  if (action === '删除') {
    const confirmed = window.confirm(`确认删除草稿 Skill“${skill.name}”？删除后无法恢复。`)
    if (!confirmed) return
    try {
      await skillApi.delete(skill.id)
      if (detailSkill.value?.id === skill.id) detailSkill.value = null
      await load()
      window.alert(`已删除 Skill“${skill.name}”。`)
    } catch (error: any) {
      window.alert(error?.message || '删除失败，请稍后重试')
    }
  }
}

function runTrial() {
  try {
    const input = parseDebugInput(trialInput.value)
    trialResult.value = JSON.stringify({ status: 'ok', skill: trialSkill.value?.name, input, output: { message: '在线试用通过（最小模拟执行）' } }, null, 2)
  } catch (error: any) {
    trialResult.value = `输入解析失败：${error.message}`
  }
}

function isPrimaryAction(action: string) { return ['进入开发', '查看', '在线试用'].includes(action) }

function visibilityLabel(value?: Skill['visibility']) {
  return ({ public: '公共', team: '团队', private: '私有' })[value || 'private']
}

function visibilityClass(value?: Skill['visibility']) {
  return `is-${value || 'private'}`
}

function statusLabel(status: string) {
  return ({ draft: '草稿', testing: '测试中', released: '已发布' })[status] || status
}
</script>

<style scoped>
.skill-management { min-height: 100%; padding: 20px 24px; color: #17233c; }
.page-head { display:flex; align-items:flex-start; justify-content:space-between; gap:18px; padding:2px 0 18px; border-bottom:1px solid #e7edf5; }
.page-title { font-size:22px; line-height:30px; }
.sub-title { margin:5px 0 0; color:#7a8799; font-size:13px; }
.actions { display:flex; gap:8px; }
.btn { border:1px solid #cfd9e6; border-radius:4px; padding:7px 13px; background:#fff; color:#34445d; cursor:pointer; font-size:13px; }
.btn.primary { border-color:#276eea; background:#276eea; color:#fff; }.btn:disabled { opacity:.6; cursor:not-allowed; }
.toolbar { display:flex; justify-content:space-between; align-items:center; gap:18px; margin:18px 0; }
.tabs { display:flex; gap:7px; }.tabs button { border:1px solid #d7e0eb; border-radius:4px; background:#fff; padding:6px 16px; color:#5d6b80; cursor:pointer; }.tabs button.active { color:#1f66d8; border-color:#75a7ff; background:#eef5ff; font-weight:600; }.tabs span { font-size:11px; }
.category-tabs { display:flex; gap:6px; margin-top:8px; }.category-tabs button { border:0; background:transparent; color:#738197; padding:2px 8px; cursor:pointer; font-size:12px; }.category-tabs button.active { color:#1f66d8; font-weight:700; background:#eef5ff; border-radius:3px; }
.input { box-sizing:border-box; width:100%; border:1px solid #cfd9e6; border-radius:4px; padding:8px 10px; outline:none; color:#253650; font:inherit; }.input:focus { border-color:#3c7cf2; box-shadow:0 0 0 2px #eaf2ff; }.search { max-width:300px; }
.skill-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; }
.skill-card { min-height:150px; border:1px solid #dce6f1; border-radius:10px; background:#fff; padding:16px; box-shadow:0 2px 6px #263b5a12; cursor:pointer; transition:transform .15s, box-shadow .15s; }.skill-card:hover { transform:translateY(-2px); box-shadow:0 8px 18px #263b5a20; }
.skill-card-head { display:flex; align-items:center; gap:10px; }.avatar { width:34px; height:34px; display:grid; place-items:center; flex:0 0 auto; border-radius:50%; background:#e5edff; color:#306bd7; font-weight:700; }.avatar.is-public { background:#dbf6e9; color:#178756; }.avatar.is-team { background:#f5e6ff; color:#7c43b2; }.skill-name-wrap { min-width:0; flex:1; }.skill-name-wrap h3 { margin:0 0 3px; font-size:15px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.meta { color:#8490a2; font-size:11px; }.visibility { border-radius:3px; padding:2px 6px; font-size:11px; color:#56657a; background:#eef1f5; }.visibility.is-public { color:#128452; background:#e2f7eb; }.visibility.is-team { color:#7542a5; background:#f3e8fc; }
.description { min-height:39px; margin:14px 0 10px; color:#607087; font-size:12px; line-height:19px; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; }.card-footer { display:flex; align-items:center; gap:11px; border-top:1px solid #edf1f5; padding-top:9px; color:#8a96a8; font-size:11px; }.category-label { color:#32816b; }.card-actions { display:flex; gap:7px; margin-top:10px; }.card-action { flex:1; border:1px solid #d7e0eb; border-radius:4px; background:#fff; padding:5px 4px; color:#526176; font-size:11px; cursor:pointer; }.card-action.primary { border-color:#358c78; background:#358c78; color:#fff; }.card-action:disabled { opacity:.6; cursor:not-allowed; }
.empty-state { min-height:210px; display:flex; flex-direction:column; justify-content:center; align-items:center; gap:9px; color:#8490a2; border:1px dashed #ccd8e7; border-radius:8px; background:#fbfcfe; }.empty-state b { color:#526176; }
.modal-mask { position:fixed; z-index:20; inset:0; display:grid; place-items:center; padding:20px; background:#12213a66; }.create-modal,.creation-choice { width:min(640px,100%); border-radius:8px; background:#fff; box-shadow:0 16px 40px #0e1b3180; }.modal-head,.modal-foot { display:flex; align-items:center; justify-content:space-between; padding:15px 20px; }.modal-head { border-bottom:1px solid #e6ebf2; }.modal-head h2 { margin:0; font-size:18px; }.close { border:0; background:none; color:#7d899a; cursor:pointer; font-size:24px; line-height:1; }.modal-body { display:grid; gap:16px; padding:20px; }.form-row { display:grid; grid-template-columns:86px minmax(0,1fr); align-items:start; gap:12px; color:#45556c; font-size:13px; }.form-row > span { padding-top:8px; text-align:right; }.form-row i { color:#e15252; font-style:normal; }.form-row.inline { align-items:center; }.form-row.inline > span { padding-top:0; }.textarea { resize:vertical; }.radio-group { display:flex; flex-wrap:wrap; gap:18px; min-height:34px; align-items:center; }.radio-group label { white-space:nowrap; }.radio-group input { margin-right:5px; accent-color:#286fec; }.form-row small { grid-column:2; margin-top:-7px; color:#8a96a6; }.mode-hint { margin:0 0 0 98px; color:#6d7b90; font-size:12px; }.modal-foot { justify-content:flex-end; gap:8px; border-top:1px solid #e6ebf2; }
.choice-cards { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; padding:26px 28px 30px; }.choice-card { display:flex; min-height:150px; flex-direction:column; align-items:center; justify-content:center; gap:9px; border:1px solid #d8e6e5; border-radius:7px; background:#fff; color:#416167; cursor:pointer; }.choice-card:hover { border-color:#4ba496; background:#f0fbf9; box-shadow:0 5px 14px #317d701a; }.choice-card b { color:#254d52; font-size:15px; }.choice-card small { color:#8a9b9e; font-size:12px; }.choice-icon { display:grid; place-items:center; width:38px; height:38px; border-radius:50%; background:#e6f6f3; color:#248b7d; font-size:23px; }
.small-modal { width:min(520px,100%); }.detail-list p { margin:0; color:#526176; }.trial-result { margin:0; max-height:180px; overflow:auto; padding:10px; background:#f5f8fb; border-radius:4px; font-size:12px; }
@media (max-width:900px) { .skill-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }.toolbar { align-items:stretch; flex-direction:column; }.search { max-width:none; }.filter-tabs { overflow-x:auto; padding-bottom:2px; }.filter-tabs button { white-space:nowrap; }.page-head { align-items:flex-start; }.form-row { grid-template-columns:76px minmax(0,1fr); }.mode-hint { margin-left:88px; } }
@media (max-width:600px) { .skill-management { padding:16px; }.skill-grid { grid-template-columns:1fr; }.page-head { flex-direction:column; }.form-row { grid-template-columns:1fr; gap:5px; }.form-row > span { padding:0; text-align:left; }.form-row small { grid-column:1; margin:0; }.mode-hint { margin:0; } }
</style>
