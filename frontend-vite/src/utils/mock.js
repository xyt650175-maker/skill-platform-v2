// Mock 数据 - 用于后端未启动时演示
export const mockSkills = [
  { id: '1', name: 'product-query', description: '产品信息查询', version: '1.0.0', status: 'released', runtime: 'python', entry_file: 'scripts/main.py', author: 'admin', created_at: '2024-01-01', updated_at: '2024-01-15' },
  { id: '2', name: 'content-analysis', description: '内容分析', version: '0.5.0', status: 'testing', runtime: 'python', entry_file: 'scripts/main.py', author: 'developer', created_at: '2024-01-05', updated_at: '2024-01-10' },
  { id: '3', name: 'risk评估', description: '风险评估', version: '0.0.1', status: 'draft', runtime: 'python', entry_file: 'scripts/main.py', author: 'admin', created_at: '2024-01-08', updated_at: '2024-01-08' },
]

export const mockAgents = [
  { id: '1', name: '产品服务助手', description: '产品信息服务', current_version: '1.0.0', status: 'active', model_name: 'gpt-4', created_at: '2024-01-01', updated_at: '2024-01-15' },
  { id: '2', name: '客服助手', description: '智能客服', current_version: '0.8.0', status: 'active', model_name: 'gpt-3.5', created_at: '2024-01-05', updated_at: '2024-01-12' },
]

export const mockEvalTasks = [
  { name: '产品服务助手 v0.1.1 评测', target: '产品服务助手', version: 'v0.1.1', dataset: '业务回归集 v1.2', state: '已完成' },
  { name: '客服助手 v0.8 评测', target: '客服助手', version: 'v0.8', dataset: '路由与边界测试集 v1.0', state: '运行中' },
  { name: '新功能回归测试', target: '产品服务助手', version: 'v1.0.0', dataset: '业务回归集 v1.2', state: '待开始' },
]

export const mockPipelines = [
  { id: '1', name: '工厂侧交付流水线', type: 'factory', stage_count: 7, version: '2.3.1', status: 'active' },
  { id: '2', name: 'Skill 构建流水线', type: 'skill', stage_count: 4, version: '1.0.0', status: 'active' },
]
