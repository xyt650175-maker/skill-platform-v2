import request from '@/utils/request'

const LOCAL_DEMO_TOKEN = 'local-demo-token'
const LOCAL_SKILLS_KEY = 'local-demo-skills'

export interface Skill {
  id: string
  name: string
  description: string
  version: string
  status: string
  language: string
  entry_file: string
  code_path?: string
  author: string
  visibility?: 'private' | 'team'
  tags?: string[]
  created_at: string
  updated_at: string
}

interface PlatformSkill {
  id: number
  name: string
  description?: string
  runtime?: string
  gitRepoPath?: string
  currentVersion?: string
  status?: string
  entryFile?: string
  creatorName?: string
  visibility?: 'private' | 'team'
  createTime?: string
  updateTime?: string
}

const toSkill = (skill: PlatformSkill): Skill => ({
  id: String(skill.id),
  name: skill.name,
  description: skill.description || '',
  version: skill.currentVersion || '0.0.0',
  status: skill.status || 'draft',
  language: skill.runtime || 'python',
  entry_file: skill.entryFile || 'scripts/main.py',
  code_path: skill.gitRepoPath,
  author: skill.creatorName || '',
  visibility: skill.visibility || 'private',
  created_at: skill.createTime || '',
  updated_at: skill.updateTime || '',
})

interface LocalSkill extends Skill {
  files?: Record<string, string>
}

const isLocalDemo = () => import.meta.env.DEV && localStorage.getItem('token') === LOCAL_DEMO_TOKEN

const readLocalSkills = (): LocalSkill[] => {
  try {
    return JSON.parse(localStorage.getItem(LOCAL_SKILLS_KEY) || '[]')
  } catch {
    return []
  }
}

const writeLocalSkills = (skills: LocalSkill[]) => {
  localStorage.setItem(LOCAL_SKILLS_KEY, JSON.stringify(skills))
}

export interface Agent {
  id: string
  name: string
  description: string
  current_version: string
  currentVersion?: string
  status: string
  model_name?: string
  modelName?: string
  system_prompt?: string
  systemPrompt?: string
  canvasConfig?: string
  creatorId?: number
  versions?: AgentVersion[]
  created_at: string
  updated_at: string
}

export interface SubAgent {
  id: string
  agent_id: string
  name: string
  description: string
  system_prompt: string
  model_name: string
}

export interface SkillMounting {
  id: string
  agent_id: string
  sub_agent_id?: string
  skill_id: string
  skill_alias?: string
  enabled: boolean
}

export interface EnterpriseGitCredential {
  id: string
  name: string
  repoUrl: string
  authType: string
  username: string
  tokenMasked: string
  connectionStatus: string
}

export interface EnterpriseGitRefs {
  branches: string[]
  tags: string[]
}

export interface EnterpriseGitSkill {
  path: string
  name: string
  version: string
  description: string
  files: Record<string, string>
}

export const enterpriseGitApi = {
  list: () => request.get('/enterprise-git/credentials') as Promise<EnterpriseGitCredential[]>,
  save: (data: { id?: string; name: string; repoUrl: string; username: string; token?: string }) =>
    request.post('/enterprise-git/credentials', { ...data, id: data.id ? Number(data.id) : undefined }) as Promise<EnterpriseGitCredential>,
  delete: (id: string) => request.delete(`/enterprise-git/credentials/${id}`) as Promise<void>,
  test: (id: string) => request.post(`/enterprise-git/credentials/${id}/test`) as Promise<EnterpriseGitCredential>,
  refs: (id: string) => request.get(`/enterprise-git/credentials/${id}/refs`) as Promise<EnterpriseGitRefs>,
  loadSkills: (id: string, ref: string) => request.get(`/enterprise-git/credentials/${id}/skills`, { params: { ref } }) as Promise<EnterpriseGitSkill[]>,
}

export const skillApi = {
  list: async () => isLocalDemo()
    ? readLocalSkills()
    : (await request.get('/skills') as PlatformSkill[]).map(toSkill),
  get: async (id: string) => {
    if (isLocalDemo()) {
      const skill = readLocalSkills().find(item => item.id === id)
      if (!skill) throw new Error('Skill 不存在')
      return skill
    }
    return toSkill(await request.get(`/skills/${id}`) as PlatformSkill)
  },
  create: async (data: Partial<Skill>) => {
    if (isLocalDemo()) {
      const now = new Date().toISOString()
      const skill: LocalSkill = {
        id: `demo-${Date.now()}`,
        name: data.name || '未命名 Skill',
        description: data.description || '',
        version: data.version || '0.0.0',
        status: 'draft',
        language: data.language || 'python',
        entry_file: data.entry_file || 'scripts/main.py',
        author: '本地演示管理员',
        visibility: data.visibility === 'team' ? 'team' : 'private',
        created_at: now,
        updated_at: now,
        files: {},
      }
      writeLocalSkills([skill, ...readLocalSkills()])
      return skill
    }
    return toSkill(await request.post('/skills', {
    name: data.name,
    description: data.description,
    runtime: data.language,
    gitRepoPath: data.code_path,
    entryFile: data.entry_file,
    visibility: data.visibility === 'team' ? 'team' : 'private',
    version: data.version,
    }) as PlatformSkill)
  },
  update: async (id: string, data: Partial<Skill>) => {
    if (isLocalDemo()) {
      const skills = readLocalSkills()
      const index = skills.findIndex(item => item.id === id)
      if (index < 0) throw new Error('Skill 不存在')
      skills[index] = { ...skills[index], ...data, updated_at: new Date().toISOString() }
      writeLocalSkills(skills)
      return skills[index]
    }
    return toSkill(await request.put('/skills', {
    id: Number(id),
    description: data.description,
    runtime: data.language,
    gitRepoPath: data.code_path,
    status: data.status,
    entryFile: data.entry_file,
    }) as PlatformSkill)
  },
  delete: async (id: string) => {
    if (isLocalDemo()) {
      writeLocalSkills(readLocalSkills().filter(item => item.id !== id))
      return
    }
    await request.delete(`/skills/${id}`)
  },
  saveCode: async (id: string, code: string, entryFile: string = 'scripts/main.py', expectedRevision?: number) => {
    if (isLocalDemo()) {
      const skills = readLocalSkills()
      const skill = skills.find(item => item.id === id)
      if (!skill) throw new Error('Skill 不存在')
      skill.files = { ...skill.files, [entryFile]: code }
      skill.updated_at = new Date().toISOString()
      writeLocalSkills(skills)
      return { code, files: skill.files }
    }
    return request.post(`/skills/${id}/code`, { code, entryFile, expectedRevision }) as Promise<any>
  },
  getCode: async (id: string) => {
    if (isLocalDemo()) {
      const skill = readLocalSkills().find(item => item.id === id)
      if (!skill) throw new Error('Skill 不存在')
      const files = skill.files || {}
      return {
        code: files[skill.entry_file] || '',
        skills_md: files['SKILL.md'] || '',
        files,
      }
    }
    return request.get(`/skills/${id}/code`) as Promise<{ code: string; skills_md?: string; files?: Record<string, string> }>
  },
  debug: async (id: string, inputJson: string) => {
    if (isLocalDemo()) {
      throw new Error('本地演示模式不提供代码执行；请登录已启动的后端服务。')
    }
    return request.post(`/skills/${id}/debug`, { inputJson }) as Promise<{
      status: 'PASS' | 'FAILED'
      executionMode: string
      durationMs: number
      exitCode?: number | null
      dependencyCalls: number
      output: string
      errorMessage?: string
      stdout: string
      stderr: string
      promptTokens?: number
      completionTokens?: number
      totalTokens?: number
    }>
  },
  finalizeVersion: async (id: string, version: string) => {
    if (isLocalDemo()) return skillApi.update(id, { version })
    await request.put(`/skills/${id}/version`, null, { params: { version } })
  },
  submitReview: async (id: string, debugResult?: Record<string, any>) => request.post(`/skills/${id}/reviews`, debugResult || {}) as Promise<SkillReview>,
  listReviews: async (status?: string) => request.get('/skills/reviews', { params: { status } }) as Promise<SkillReview[]>,
  decideReview: async (reviewId: string, decision: 'approved' | 'rejected', comment?: string) =>
    request.put(`/skills/reviews/${reviewId}`, { decision, comment }) as Promise<SkillReview>,
  listOperationLogs: async (skillId: string, page = 1, size = 10) =>
    request.get(`/skills/${skillId}/operation-logs`, { params: { page, size } }) as Promise<any>,
  listVersions: async (skillId: string) =>
    request.get(`/skills/${skillId}/versions`) as Promise<any[]>,
  diffVersions: async (skillId: string, v1: string, v2: string) =>
    request.get(`/skills/${skillId}/versions/diff`, { params: { v1, v2 } }) as Promise<any>,
}

export interface SkillReview {
  id: string
  skillId: string
  skillName: string
  version?: string
  status: 'pending' | 'approved' | 'rejected'
  comment?: string
  applicantId: string
  applicantName?: string
  reviewerId?: string
  reviewerName?: string
  createTime?: string
  updateTime?: string
  debugResult?: string
  debugPassed?: boolean
  debugInput?: string
  totalTokens?: number
  testCaseCount?: number
  testPassCount?: number
  debugSummary?: string
}

export interface EvalTask {
  id: string
  name: string
  agentId: string
  agentName?: string
  agentVersion?: string
  datasetKey: string
  datasetVersion?: string
  scope?: string
  status: string
  resultSummary?: string
  createTime?: string
}

export const evalApi = {
  list: (params?: { status?: string; agentId?: string }) => request.get('/eval-tasks', { params }) as Promise<EvalTask[]>,
  create: (data: { name: string; agentId: string; agentVersion?: string; datasetKey: string; datasetVersion?: string; scope?: string }) =>
    request.post('/eval-tasks', { ...data, agentId: Number(data.agentId) }) as Promise<EvalTask>,
  updateStatus: (id: string, status: string) => request.put(`/eval-tasks/${id}/status`, null, { params: { status } }) as Promise<void>,
}

export const agentApi = {
  list: () => request.get('/agents') as Promise<Agent[]>,
  get: (id: string) => request.get(`/agents/${id}`) as Promise<Agent>,
  getById: (id: string) => request.get(`/agents/${id}`) as Promise<Agent>,
  create: (data: Partial<Agent>) => request.post('/agents', data) as Promise<Agent>,
  update: (data: Partial<Agent>) => request.put('/agents', data) as Promise<Agent>,
  delete: (id: string) => request.delete(`/agents/${id}`) as Promise<void>,
  listSubAgents: (agentId: string) => request.get(`/agents/${agentId}/sub-agents`) as Promise<SubAgent[]>,
  createSubAgent: (agentId: string, data: Partial<SubAgent>) => request.post(`/agents/${agentId}/sub-agents`, data) as Promise<SubAgent>,
  listMountings: (agentId: string, version?: string) => request.get(`/agents/${agentId}/mountings`, { params: { version } }) as Promise<SkillMounting[]>,
  createMounting: (agentId: string, data: Partial<SkillMounting>) => request.post(`/agents/${agentId}/mountings`, data) as Promise<SkillMounting>,
  updateMounting: (agentId: string, mountingId: string, data: Partial<SkillMounting>) => request.put(`/agents/${agentId}/mountings/${mountingId}`, data) as Promise<SkillMounting>,
  listVersions: (agentId: string) => request.get(`/agents/${agentId}/versions`) as Promise<AgentVersion[]>,
  createVersion: (agentId: string, data: { version: string; changeSummary?: string }) => request.post(`/agents/${agentId}/versions`, data) as Promise<AgentVersion>,
  switchVersion: (agentId: string, version: string) => request.put(`/agents/${agentId}/versions/${version}`) as Promise<Agent>,
  // DAG 配置接口（HTTP 解耦）
  getConfig: (agentId: string, version?: string) =>
    request.get(`/agents/${agentId}/config`, { params: { version } }) as Promise<{
      success: boolean
      agent_config: any
      source: string
    }>,
  submitCanvas: (agentId: string, canvas: any) =>
    request.put(`/agents/${agentId}/canvas`, { canvas }) as Promise<any>,
}

export interface AgentVersion {
  id: number
  agentId: number
  version: string
  changeSummary?: string
  creatorId?: number
  createTime?: string
}
