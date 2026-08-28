export const DEVELOPMENT_MODES = {
  online: { label: '在线创建', hint: '在平台编辑器中创建并保存' },
  local: { label: '从本地导入', hint: '上传本地 ZIP 包导入' },
  git: { label: '从 Git 导入', hint: '从 Git 仓库批量导入' },
}

/**
 * 开发前提供给模型的基准测试数据。数据集只描述 Skill 的输入输出契约，
 * 不会被写入 Skill 源码，也不会作为生产数据调用。
 * 每个模板至少包含 3-4 条测试用例（正常、异常、边界）。
 */
export const TEST_DATASETS = [
  {
    id: 'product-mock',
    label: '产品查询 Mock 数据',
    description: '产品信息查询的业务验收模板，含正常、未知编码、参数缺失与风险提示等场景。',
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
    id: 'product-risk-boundary',
    label: '产品风险边界数据',
    description: '覆盖空产品编码、未知编码及高风险等级，要求返回可读错误或风险提示。',
    inputSchema: {
      product_code: { type: 'string', required: true, pattern: '^\\d{6}$', description: '6 位产品编码' },
      risk_level: { type: 'string', required: false, description: '风险等级' },
    },
    outputContract: { required: ['error', 'risk_warning'], optional: ['product_code'] },
    input: { product_code: '', risk_level: 'R5' },
    expected: { error: 'product_code 不能为空', risk_warning: '高风险产品，请确认使用条件' },
    testCases: [
      { id: 'empty-code', name: '空产品编码', input: { product_code: '', risk_level: 'R5' }, expected: { contains: { error: 'product_code 不能为空' } } },
      { id: 'invalid-format', name: '非法格式编码', input: { product_code: 'abc', risk_level: 'R5' }, expected: { contains: { error: '产品编码必须是6位数字' } } },
      { id: 'high-risk-check', name: '高风险等级校验', input: { product_code: '110022', risk_level: 'R5' }, expected: { contains: { risk_warning: '高风险产品' } } },
    ],
  },
  {
    id: 'product-comparison',
    label: '产品批量对比数据',
    description: '覆盖多个产品编码、指标选择与排序，适用于产品对比或筛选类 Skill。',
    inputSchema: {
      product_codes: { type: 'array', required: true, description: '产品编码列表' },
      metrics: { type: 'array', required: false, description: '查询指标列表' },
      sort_by: { type: 'string', required: false, description: '排序字段' },
    },
    outputContract: { required: ['comparison', 'sorted_by'], optional: ['error'] },
    input: {
      product_codes: ['000001', '110022'],
      metrics: ['latest_status', 'change_rate', 'risk_level'],
      sort_by: 'change_rate',
    },
    expected: {
      comparison: [
        { product_code: '000001', latest_status: '正常', change_rate: '2.36%', risk_level: 'R2' },
        { product_code: '110022', latest_status: '关注', change_rate: '1.18%', risk_level: 'R3' },
      ],
      sorted_by: 'change_rate',
    },
    testCases: [
      { id: 'normal-comparison', name: '正常批量对比', input: { product_codes: ['000001', '110022'], metrics: ['latest_status', 'change_rate', 'risk_level'], sort_by: 'change_rate' }, expected: { contains: { sorted_by: 'change_rate' } } },
      { id: 'empty-list', name: '空产品列表', input: { product_codes: [], metrics: ['latest_status'], sort_by: 'change_rate' }, expected: { contains: { error: 'product_codes 不能为空' } } },
      { id: 'single-product', name: '单个产品对比', input: { product_codes: ['000001'], metrics: ['latest_status'], sort_by: 'latest_status' }, expected: { contains: { sorted_by: 'latest_status' } } },
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
  {
    id: 'news-search',
    label: '新闻资讯检索数据',
    description: '覆盖关键词、时间范围与数量限制，适用于资讯检索、摘要与分类类 Skill。',
    inputSchema: {
      keyword: { type: 'string', required: true, description: '搜索关键词' },
      date_range: { type: 'string', required: false, description: '时间范围' },
      limit: { type: 'number', required: false, description: '返回数量上限' },
    },
    outputContract: { required: ['items', 'total'], optional: ['error'] },
    input: { keyword: '新能源', date_range: '7d', limit: 3 },
    expected: {
      items: [
        { title: '示例资讯标题', source: 'Mock News', published_at: '2026-08-12', summary: '资讯摘要' },
      ],
      total: 1,
    },
    testCases: [
      { id: 'normal-search', name: '正常搜索', input: { keyword: '新能源', date_range: '7d', limit: 3 }, expected: { contains: { total: 1 } } },
      { id: 'empty-keyword', name: '空关键词', input: { keyword: '', date_range: '7d', limit: 3 }, expected: { contains: { error: 'keyword 不能为空' } } },
      { id: 'no-results', name: '无结果搜索', input: { keyword: '不存在的关键词', date_range: '1d', limit: 5 }, expected: { contains: { total: 0 } } },
    ],
  },
  {
    id: 'privacy-mask',
    label: '敏感信息脱敏数据',
    description: '覆盖手机号与用户标识处理，要求输出符合脱敏规范，不能泄露原始敏感字段。',
    inputSchema: {
      user_id: { type: 'string', required: true, description: '用户标识' },
      phone: { type: 'string', required: true, description: '手机号' },
      action: { type: 'string', required: false, description: '操作类型' },
    },
    outputContract: { required: ['user_id', 'masked_phone'], optional: ['privacy_checked', 'error'] },
    input: { user_id: 'U10086', phone: '13812345678', action: 'profile_query' },
    expected: { user_id: 'U10086', masked_phone: '138****5678', privacy_checked: true },
    testCases: [
      { id: 'normal-mask', name: '正常脱敏', input: { user_id: 'U10086', phone: '13812345678', action: 'profile_query' }, expected: { contains: { masked_phone: '138****5678' } } },
      { id: 'empty-phone', name: '空手机号', input: { user_id: 'U10086', phone: '', action: 'profile_query' }, expected: { contains: { error: 'phone 不能为空' } } },
      { id: 'invalid-phone', name: '非法手机号', input: { user_id: 'U10086', phone: '123', action: 'profile_query' }, expected: { contains: { error: '手机号格式不正确' } } },
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
  return {
    'SKILL.md': `# ${name}\n\n## 版本\n- 当前版本：${version}\n- 发布状态：草稿\n\n## 简介\n${description || '待补充 Skill 描述'}\n\n## 输入\n- 请在此描述输入字段与校验规则。\n\n## 输出\n- 请在此描述返回字段与错误结构。\n\n## 使用说明\n请在 scripts/main.py 中实现 Skill 逻辑。\n`,
    'requirements.txt': '# 当前 Skill 仅使用 Python 标准库；如引入第三方库，请在此逐行声明具体版本。\n',
    'references/implementation-notes.md': `# ${name} 参考说明\n\n- 维护输入字段、输出契约、Mock 数据来源与业务规则。\n- 业务测试数据保存在 references/test-data.json。\n- 不在此目录保存 YAML 文件。\n`,
    'references/data-source.json': '{\n  "type": "mock",\n  "databaseApiUrl": "",\n  "credentialEnv": "SKILL_DATABASE_API_TOKEN",\n  "description": "生产环境可配置数据库 API 地址；密钥只通过后端环境变量注入，不写入 Skill 文件。"\n}\n',
    'scripts/main.py': 'from scripts.validators import validate_input\nfrom scripts.mock_data import build_mock_result\n\n\ndef handle(input_data: dict) -> dict:\n    """Skill 入口函数：协调校验和业务处理。"""\n    error = validate_input(input_data)\n    if error:\n        return {"error": error}\n    return build_mock_result(input_data)\n',
    'scripts/validators.py': 'def validate_input(input_data: dict) -> str:\n    """校验输入参数，合法时返回空字符串，否则返回错误信息。"""\n    if not isinstance(input_data, dict):\n        return "input_data 必须是 JSON 对象"\n    return ""\n',
    'scripts/mock_data.py': '# Mock 数据表，开发阶段使用，后续可替换为数据库 API\n_MOCK_DATA = {}\n\n\ndef build_mock_result(input_data: dict) -> dict:\n    """根据输入返回 Mock 数据。"""\n    return dict(input_data)\n',
  }
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
