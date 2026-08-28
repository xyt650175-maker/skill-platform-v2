import test from 'node:test'
import assert from 'node:assert/strict'
import { DEVELOPMENT_MODES, TEST_DATASETS, buildBusinessTestData, buildSkillScaffold, canSubmitForReview, getTestDataset, matchesSkillFilter, parseDebugInput, skillActions, skillCategory, validateBusinessTestData } from '../src/domain/skillWorkspace.js'

const publicProduct = { name: '产品查询', description: '产品数据查询', visibility: 'public', status: 'released', language: 'python' }

test('开发模式提供在线、本地与 Git 三种入口', () => {
  assert.deepEqual(Object.keys(DEVELOPMENT_MODES), ['online', 'local', 'git'])
  const scaffold = buildSkillScaffold('测试 Skill')
  assert.deepEqual(Object.keys(scaffold), ['SKILL.md', 'requirements.txt', 'references/implementation-notes.md', 'references/data-source.json', 'scripts/main.py', 'scripts/validators.py', 'scripts/mock_data.py'])
  assert.equal(scaffold['SKILL.md'].startsWith('# 测试 Skill'), true)
  assert.equal(scaffold['SKILL.md'].includes('---'), false)
  assert.equal(scaffold['requirements.txt'].length > 0, true)
  assert.equal(scaffold['references/implementation-notes.md'].length > 0, true)
  assert.equal(scaffold['references/data-source.json'].includes('databaseApiUrl'), true)
  assert.equal(scaffold['scripts/main.py'].includes('from scripts.validators'), true)
  assert.equal(Object.keys(scaffold).some(path => path.endsWith('.gitkeep')), false)
})

test('管理页能按范围、分类和关键词组合过滤', () => {
  assert.equal(skillCategory(publicProduct), '业务类')
  assert.equal(matchesSkillFilter(publicProduct, { scope: 'public', category: '业务类', keyword: '数据' }), true)
  assert.equal(matchesSkillFilter(publicProduct, { scope: 'private', category: '业务类' }), false)
})

test('公共、团队与个人 Skill 显示不同的可操作项', () => {
  assert.deepEqual(skillActions(publicProduct), ['查看', '在线试用', '复制到我的'])
  assert.deepEqual(skillActions({ visibility: 'team' }), ['查看', '申请编辑'])
  assert.deepEqual(skillActions({ visibility: 'private', status: 'testing' }), ['审核中', '查看记录'])
})

test('调试输入必须是 JSON 对象，草稿调试通过后才能提交评审', () => {
  assert.deepEqual(parseDebugInput('{"product_code":"000001"}'), { product_code: '000001' })
  assert.throws(() => parseDebugInput('[]'), /JSON 对象/)
  assert.equal(canSubmitForReview('draft', true), true)
  assert.equal(canSubmitForReview('draft', false), false)
})

test('测试数据集可作为模型生成与调试的统一基准输入', () => {
  const dataset = getTestDataset('product-mock')
  assert.equal(TEST_DATASETS.length >= 5, true)
  assert.equal(dataset.label, '产品查询 Mock 数据')
  assert.deepEqual(dataset.input, { product_code: '000001', query: '产品信息查询' })
  assert.equal(getTestDataset('product-comparison').input.product_codes.length, 2)
  assert.equal(getTestDataset('privacy-mask').expected.masked_phone, '138****5678')
  assert.deepEqual(getTestDataset('unknown').input, dataset.input)
})

test('业务人员可填写并校验可随 Skill 保存的多用例测试数据格式', () => {
  const businessData = buildBusinessTestData(getTestDataset('product-mock'))
  assert.equal(businessData.testCases.length, 4)
  assert.equal(validateBusinessTestData(businessData).name, '产品查询 Mock 数据')
  assert.throws(() => validateBusinessTestData({ name: '无用例', testCases: [] }), /至少需要/)
  assert.throws(() => validateBusinessTestData({ name: '缺失输入', testCases: [{ id: 'case-1', name: '用例' }] }), /input 必须/)
})
