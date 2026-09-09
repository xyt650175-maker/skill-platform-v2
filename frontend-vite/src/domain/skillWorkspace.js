export const DEVELOPMENT_MODES = {
  online: { label: '在线创建', hint: '在平台编辑器中创建并保存' },
  local: { label: '从本地导入', hint: '上传本地 ZIP 包导入' },
  git: { label: '从 Git 导入', hint: '从 Git 仓库批量导入' },
}

/**
 * 调试区内置的 Mock 数据集。数据只用于本地验证，
 * 不会被当作生产数据调用。每个数据集覆盖正常、异常和边界场景。
 */
export const TEST_DATASETS = [
  {
    id: 'product-mock',
    label: '产品查询 Mock 数据',
    description: '产品信息查询的 Mock 调试数据，含正常、未知编码、参数缺失与风险提示等场景。',
    inputSchema: {
      product_code: { type: 'string', required: true, pattern: '^\\d{6}$', description: '6 位产品编码' },
      query: { type: 'string', required: false, description: '查询意图' },
    },
    outputContract: { required: ['product_code', 'product_name', 'risk_level'], optional: ['latest_status', 'change_rate', 'risk_warning', 'error'] },
    input: { product_code: '000001', query: '产品信息查询' },
    expected: { product_code: '000001', product_name: '示例产品', risk_level: 'R2', latest_status: '正常', change_rate: '2.36%' },
    testCases: [
      { id: 'normal-query', name: '正常产品查询', input: { product_code: '000001', query: '产品信息查询' }, expected: { contains: { product_code: '000001', product_name: '示例产品', risk_level: 'R2' } } },
      { id: 'unknown-product', name: '未知产品编码', input: { product_code: '999999', query: '产品信息查询' }, expected: { contains: { error: '产品不存在' } } },
      { id: 'missing-code', name: '缺失产品编码', input: { product_code: '', query: '产品信息查询' }, expected: { contains: { error: 'product_code 不能为空' } } },
      { id: 'high-risk-warning', name: '高风险产品提示', input: { product_code: '110022', query: '产品信息查询' }, expected: { contains: { risk_level: 'R5', risk_warning: '高风险产品' } } },
    ],
  },
  {
    id: 'text-processor',
    label: '文本处理数据',
    description: '通用文本处理场景：清洗、提取关键词、格式校验。适用于非金融类 Skill。',
    inputSchema: {
      text: { type: 'string', required: true, description: '待处理文本' },
      action: { type: 'string', required: true, description: '处理动作：clean/extract/validate' },
    },
    outputContract: { required: ['action', 'result'], optional: ['error', 'warnings'] },
    input: { text: '  Hello  World  ', action: 'clean' },
    expected: { action: 'clean', result: 'Hello World' },
    testCases: [
      { id: 'clean-text', name: '文本清洗', input: { text: '  Hello  World  ', action: 'clean' }, expected: { contains: { action: 'clean', result: 'Hello World' } } },
      { id: 'extract-keywords', name: '关键词提取', input: { text: '产品使用风险提示', action: 'extract' }, expected: { contains: { action: 'extract' } } },
      { id: 'empty-text', name: '空文本校验', input: { text: '', action: 'clean' }, expected: { contains: { error: 'text 不能为空' } } },
      { id: 'invalid-action', name: '非法动作', input: { text: 'hello', action: 'unknown' }, expected: { contains: { error: '不支持的操作' } } },
    ],
  },
  {
    id: 'data-converter',
    label: '数据格式转换数据',
    description: 'JSON 格式转换与字段映射校验，适用于数据集成类 Skill。',
    inputSchema: {
      source_data: { type: 'object', required: true, description: '源数据对象' },
      target_format: { type: 'string', required: true, description: '目标格式：flat/nested/filtered' },
    },
    outputContract: { required: ['converted', 'target_format'], optional: ['error'] },
    input: { source_data: { user: { name: '张三', age: 30 } }, target_format: 'flat' },
    expected: { converted: { 'user.name': '张三', 'user.age': 30 }, target_format: 'flat' },
    testCases: [
      { id: 'flatten-object', name: '对象扁平化', input: { source_data: { user: { name: '张三', age: 30 } }, target_format: 'flat' }, expected: { contains: { target_format: 'flat' } } },
      { id: 'empty-data', name: '空数据校验', input: { source_data: {}, target_format: 'flat' }, expected: { contains: { error: 'source_data 不能为空' } } },
      { id: 'invalid-format', name: '非法格式', input: { source_data: { a: 1 }, target_format: 'xml' }, expected: { contains: { error: '不支持的格式' } } },
    ],
  },
]

export function getTestDataset(id) {
  return TEST_DATASETS.find(item => item.id === id) || TEST_DATASETS[0]
}

export function getDatasetTestCases(dataset) {
  if (Array.isArray(dataset?.testCases) && dataset.testCases.length) return dataset.testCases
  return [{ id: `${dataset?.id || 'dataset'}-default`, name: '默认样例', input: dataset?.input || {}, expected: dataset?.expected || {} }]
}

/** 业务人员可直接填写并随 Skill 保存的测试数据文件格式。 */
export function buildBusinessTestData(dataset) {
  return {
    version: '1.0',
    id: dataset.id,
    name: dataset.label,
    description: dataset.description,
    inputSchema: dataset.inputSchema || {},
    outputContract: dataset.outputContract || {},
    testCases: getDatasetTestCases(dataset),
  }
}

export function validateBusinessTestData(data) {
  if (!data || typeof data !== 'object') throw new Error('测试数据必须是 JSON 对象')
  if (!data.name || typeof data.name !== 'string') throw new Error('测试数据缺少 name')
  if (!Array.isArray(data.testCases) || !data.testCases.length) throw new Error('至少需要提供一条 testCases 测试用例')
  data.testCases.forEach((item, index) => {
    if (!item || typeof item !== 'object' || !item.id || !item.name) throw new Error(`第 ${index + 1} 条测试用例缺少 id 或 name`)
    if (!item.input || Array.isArray(item.input) || typeof item.input !== 'object') throw new Error(`第 ${index + 1} 条测试用例 input 必须是 JSON 对象`)
  })
  return data
}

export function buildSkillScaffold(name = '未命名 Skill', description = '', version = '0.0.0') {
  const safeName = (name || '未命名 Skill').replace(/\s+/g, '-').toLowerCase()
  return {
    'SKILL.md': `---\nname: ${safeName}\nname_zh: ${name || '未命名 Skill'}\ndescription: ${description || '待补充 Skill 描述'}\nversion: ${version}\ntags: []\nrunEnv: python\ndigestValue: \n---\n\n# ${name || '未命名 Skill'}\n\n## 简介\n${description || '待补充 Skill 描述'}\n\n## 输入\n- 请在此描述输入字段与校验规则。\n\n## 输出\n- 请在此描述返回字段与错误结构。\n\n## 使用说明\n请在 scripts/main.py 中实现 Skill 逻辑。\n`,
    'requirements.txt': '# 当前 Skill 仅使用 Python 标准库；如引入第三方库，请在此逐行声明具体版本。\n',
    'references/implementation-notes.md': `# ${name} 参考说明\n\n- 维护输入字段、输出契约、Mock 数据来源与业务规则。\n- 业务测试数据保存在 references/test-data.json。\n- 不在此目录保存 YAML 文件。\n`,
    'references/data-source.json': '{\n  "type": "mock",\n  "databaseApiUrl": "",\n  "credentialEnv": "SKILL_DATABASE_API_TOKEN",\n  "description": "生产环境可配置数据库 API 地址；密钥只通过后端环境变量注入，不写入 Skill 文件。"\n}\n',
    'scripts/main.py': 'from scripts.validators import validate_input\nfrom scripts.mock_data import build_mock_result\n\n\ndef handle(input_data: dict) -> dict:\n    """Skill 入口函数：协调校验和业务处理。"""\n    error = validate_input(input_data)\n    if error:\n        return {"error": error}\n    return build_mock_result(input_data)\n',
    'scripts/validators.py': 'def validate_input(input_data: dict) -> str:\n    """校验输入参数，合法时返回空字符串，否则返回错误信息。"""\n    if not isinstance(input_data, dict):\n        return "input_data 必须是 JSON 对象"\n    return ""\n',
    'scripts/mock_data.py': '# Mock 数据表，开发阶段使用，后续可替换为数据库 API\n_MOCK_DATA = {}\n\n\ndef build_mock_result(input_data: dict) -> dict:\n    """根据输入返回 Mock 数据。"""\n    return dict(input_data)\n',
  }
}

/**
 * 运行目标平台可选项。用于 Skill 声明它支持在哪些 CPU 架构/OS 上运行，
 * 类似 Docker 镜像的 platform 声明，便于分发与部署匹配。
 */
export const PLATFORM_OPTIONS = [
  { value: 'x86_64', label: 'x86_64', desc: 'Intel/AMD 64 位' },
  { value: 'arm64', label: 'arm64', desc: 'ARM 64 位（如 Apple Silicon / 鲲鹏）' },
  { value: 'linux-x86_64', label: 'linux-x86_64', desc: 'Linux x86_64' },
  { value: 'linux-arm64', label: 'linux-arm64', desc: 'Linux ARM64' },
  { value: 'windows', label: 'windows', desc: 'Windows' },
  { value: 'macos', label: 'macos', desc: 'macOS' },
]

const REQ_LINE_RE = /^\s*([A-Za-z0-9_.\-]+)\s*(==|>=|<=|~=|!=|>|<)?\s*([A-Za-z0-9_.\-+*,]*)\s*$/

/**
 * 解析 requirements.txt 内容，返回依赖列表。
 * 支持 package==1.0 / package>=1.0 / package / # 注释 / -r other.txt。
 * 与后端 SkillCodeLogic.parseRequirements 保持一致。
 */
export function parseRequirements(content) {
  const deps = []
  if (!content) return deps
  for (const raw of String(content).split(/\r?\n/)) {
    const line = (raw.split('#')[0] || '').trim()
    if (!line || line.startsWith('-')) continue
    const m = REQ_LINE_RE.exec(line)
    if (!m) continue
    const op = m[2]
    const ver = m[3]
    deps.push({
      name: m[1],
      versionConstraint: op ? `${op}${ver || ''}` : '',
      source: 'requirements.txt',
    })
  }
  return deps
}

/**
 * 解析 SKILL.md 前言区 dependency 字段，格式：
 * dependency:
 *   python:
 *     - jinja2>=3.1.0
 */
export function parseSkillFrontmatterDependency(skillMd) {
  const deps = []
  if (!skillMd) return deps
  const fm = skillMd.match(/^---\s*\n([\s\S]*?)\n---\s*(?:\n|$)/)
  if (!fm) return deps
  const lines = fm[1].split(/\r?\n/)
  let inDep = false
  for (const line of lines) {
    if (/^\s*dependency\s*:/.test(line)) { inDep = true; continue }
    if (inDep) {
      if (!/^\s/.test(line) && line.trim() !== '') { inDep = false; continue }
      const item = line.replace(/^\s*-\s*/, '').trim()
      if (!item) continue
      const m = REQ_LINE_RE.exec(item)
      if (!m) continue
      const op = m[2]
      const ver = m[3]
      deps.push({
        name: m[1],
        versionConstraint: op ? `${op}${ver || ''}` : '',
        source: 'SKILL.md',
      })
    }
  }
  return deps
}

/**
 * 解析 skill 内 scripts/*.py 之间的 import 调用关系，用于内部模块架构图。
 * 识别 from scripts.xxx import ... 与 import scripts.xxx。
 * 返回 [{ from: 'scripts/main.py', to: 'scripts/validators.py' }]
 */
export function parsePythonImports(files) {
  const edges = []
  if (!files) return edges
  for (const [path, content] of Object.entries(files)) {
    if (!path.endsWith('.py') || !content) continue
    const fromModule = path.replace(/\.py$/, '').replace(/\//g, '.')
    const text = String(content)
    // from scripts.xxx import y / from .xxx import y / from scripts import y
    const fromRe = /^\s*from\s+((?:scripts|\.)[\w.]*)\s+import/gm
    let m
    while ((m = fromRe.exec(text)) !== null) {
      const target = resolveModulePath(m[1], path)
      if (target && target !== path) edges.push({ from: path, to: target })
    }
    // import scripts.xxx / import scripts
    const impRe = /^\s*import\s+(scripts[\w.]*)/gm
    while ((m = impRe.exec(text)) !== null) {
      const target = resolveModulePath(m[1], path)
      if (target && target !== path) edges.push({ from: path, to: target })
    }
    // 忽略 fromModule 自身变量未使用告警
    void fromModule
  }
  // 去重
  const seen = new Set()
  return edges.filter(e => {
    const k = `${e.from}->${e.to}`
    if (seen.has(k)) return false
    seen.add(k)
    return true
  })
}

function resolveModulePath(moduleName, fromPath) {
  // scripts.validators -> scripts/validators.py
  // .validators -> 同目录 validators.py
  let parts
  if (moduleName.startsWith('.')) {
    const dir = fromPath.includes('/') ? fromPath.replace(/\/[^/]+$/, '') : ''
    parts = moduleName.replace(/^\./, '').split('.')
    const leaf = parts.filter(Boolean).join('/')
    return leaf ? `${dir ? dir + '/' : ''}${leaf}.py` : null
  }
  parts = moduleName.split('.').filter(Boolean)
  if (parts[0] !== 'scripts') return null
  return 'scripts/' + parts.slice(1).join('/') + '.py'
}

export function skillCategory(skill) {
  const text = `${skill.name || ''} ${skill.description || ''}`.toLowerCase()
  if (/产品|product|catalog|item/.test(text)) return '业务类'
  if (/新闻|资讯|news|report/.test(text)) return '资讯类'
  if (/风控|合规|compliance|risk/.test(text)) return '风控类'
  if (/客服|问答|support|service/.test(text)) return '客服类'
  return '工具类'
}

export function matchesSkillFilter(skill, { scope = 'all', category = '全部', keyword = '' } = {}) {
  const visibility = skill.visibility || 'private'
  const scopeMatched = scope === 'all'
    || (scope === 'released' ? skill.status === 'released' : visibility === scope)
  const categoryMatched = category === '全部' || skillCategory(skill) === category
  const term = keyword.trim().toLowerCase()
  const searchMatched = !term || `${skill.name || ''} ${skill.description || ''} ${skill.language || ''}`.toLowerCase().includes(term)
  return scopeMatched && categoryMatched && searchMatched
}

export function skillActions(skill) {
  const visibility = skill.visibility || 'private'
  if (visibility === 'public') return ['查看', '在线试用', '复制到我的']
  if (visibility === 'team') return ['查看', '申请编辑']
  if (skill.status === 'testing') return ['审核中', '查看记录']
  if (skill.status === 'released') return ['进入开发', '版本历史', '下线']
  return ['进入开发', '版本历史', '删除']
}

export function parseDebugInput(input) {
  const parsed = JSON.parse(input || '{}')
  if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('测试输入必须是 JSON 对象')
  }
  return parsed
}

export function canSubmitForReview(status, debugPassed) {
  return Boolean(debugPassed) && (status === 'draft' || status === 'reviewing' || status === 'rejected')
}
