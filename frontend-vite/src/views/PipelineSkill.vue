<template>
  <div>
    <section class="page-head">
      <strong class="page-title">流水线2 · Skill侧</strong>
      <span class="chip ok">6 阶段</span>
      <span class="chip">v1.8.0</span>
      <div class="actions">
        <button class="btn">查看历史</button>
        <button class="btn primary">触发流水线</button>
      </div>
    </section>

    <div class="page-context">
      <b>Skill 侧交付流水线</b>　/　从定版到智家登记的完整链路
    </div>

    <section class="page-content">
      <div class="notice">
        Skill 侧 6 阶段流水线：Skill 定版 → 推送 Git → 质量流水线 → 打 zip 包 → 推送 zpk → 智家登记。
        其中 ⑤ 推送 zpk 是流水线1（工厂侧）的关键依赖产物。
      </div>

      <!-- 流水线全貌 -->
      <section class="panel">
        <h3>流水线全貌</h3>
        <div class="pad">
          <div class="flow">
            <template v-for="(s, i) in stages" :key="s.key">
              <div :class="['flow-node', s.status]">
                <div class="flow-num">{{ s.index }}</div>
                <div class="flow-label">{{ s.short }}</div>
                <div v-if="s.key === 's5'" class="flow-dep">⇐ 工厂侧依赖</div>
              </div>
              <div v-if="i < stages.length - 1" class="flow-arrow">→</div>
            </template>
          </div>
        </div>
      </section>

      <!-- 各阶段详细说明 -->
      <section class="panel" style="margin-top:12px">
        <h3>各阶段详细说明</h3>
        <div class="pad">
          <div v-for="s in stages" :key="s.key" class="stage-card">
            <div class="stage-head">
              <span class="stage-num">{{ s.index }}</span>
              <span class="stage-title">{{ s.full }}</span>
              <span :class="['stage-status', s.status]">{{ statusText(s.status) }}</span>
            </div>
            <div class="stage-body">
              <div class="stage-col">
                <b>输入</b>
                <div class="hint">{{ s.input }}</div>
              </div>
              <div class="stage-col">
                <b>输出</b>
                <div class="hint">{{ s.output }}</div>
              </div>
              <div class="stage-col">
                <b>责任人</b>
                <div class="hint">{{ s.owner }}</div>
              </div>
              <div class="stage-col gate">
                <b>质量门禁</b>
                <div class="hint">{{ s.gate }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 质量门禁与协同 -->
      <section class="panel" style="margin-top:12px">
        <h3>质量门禁与协同</h3>
        <div class="pad">
          <div class="qm-grid">
            <div class="qm-card">
              <b>质检项</b>
              <ul class="qm-list">
                <li>功能测试：Skill 输入输出正确性</li>
                <li>安全扫描：代码无高危漏洞、无敏感信息泄露</li>
                <li>性能基线：单次执行耗时 ≤ 500ms</li>
                <li>依赖合规：第三方库版本合规、无许可证风险</li>
                <li>文档完整性：SKILL.md 描述清晰、示例可用</li>
              </ul>
            </div>
            <div class="qm-card">
              <b>版本可追溯（三元绑定）</b>
              <ul class="qm-list">
                <li><b>Git Tag</b>：<code>skill-{name}-v{version}</code>，对应 Skill 源码快照</li>
                <li><b>zpk</b>：包含 Skill 代码 + SKILL.md + requirements.txt 的压缩包</li>
                <li><b>智家登记</b>：记录 zpk 版本号、Git Commit、发布时间的注册表</li>
              </ul>
              <div class="hint" style="margin-top:6px">
                通过 Git Tag → zpk → 智家登记 三元绑定，任意版本可一键溯源。
              </div>
            </div>
            <div class="qm-card">
              <b>协同流水线1</b>
              <ul class="qm-list">
                <li>⑤ 推送 zpk 完成后，自动通知流水线1 可用</li>
                <li>流水线1 ③ 流水线注入 校验 zpk 的 Git Tag 与智家登记一致</li>
                <li>若 zpk 校验失败，流水线1 暂停并回滚</li>
                <li>版本冲突时以 Git Tag 为准，智家登记为准</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <!-- 双流水线汇合关系 -->
      <section class="panel" style="margin-top:12px">
        <h3>双流水线汇合关系</h3>
        <div class="pad">
          <div class="merge-diagram">
            <div class="merge-col">
              <div class="merge-label">流水线2 · Skill侧</div>
              <div class="merge-item">① Skill定版</div>
              <div class="merge-item">② 推送Git</div>
              <div class="merge-item">③ 质量流水线</div>
              <div class="merge-item">④ 打zip包</div>
              <div class="merge-item highlight">⑤ 推送zpk ← 汇合点</div>
              <div class="merge-item">⑥ 智家登记</div>
            </div>
            <div class="merge-arrow">⇅ zpk 产物依赖</div>
            <div class="merge-col">
              <div class="merge-label">流水线1 · 工厂侧</div>
              <div class="merge-item">① 工厂提交定版</div>
              <div class="merge-item">② 自动提取</div>
              <div class="merge-item highlight">③ 流水线注入 ← 汇合点</div>
              <div class="merge-item">④ 应用镜像生成</div>
              <div class="merge-item">⑤ 部署验证</div>
              <div class="merge-item">⑥ 晋级生产</div>
              <div class="merge-item">⑦ 变更验证</div>
            </div>
          </div>
          <div class="hint" style="margin-top:10px">
            关键依赖：流水线2的 ⑤推送zpk 产物是流水线1 ③流水线注入 的前置条件，
            通过 zpk 版本号 + Git Tag + 智家登记 三元绑定保证版本一致性与可追溯性。
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

type StageStatus = 'done' | 'running' | 'waiting'

interface Stage {
  key: string
  index: number
  short: string
  full: string
  input: string
  output: string
  owner: string
  gate: string
  status: StageStatus
}

const stages = ref<Stage[]>([
  {
    key: 's1', index: 1, short: 'Skill定版', full: '① Skill 定版',
    input: '需求文档、Skill 代码、测试用例',
    output: '定版 Skill（含版本号 v1.8.0）',
    owner: 'Skill 开发者',
    gate: '代码审查通过、单元测试 100% 通过、SKILL.md 完整',
    status: 'done',
  },
  {
    key: 's2', index: 2, short: '推送Git', full: '② 推送 Git',
    input: '定版 Skill、Git 仓库地址',
    output: 'Git Commit（含 Tag: skill-product-search-v1.8.0）',
    owner: 'Skill 开发者',
    gate: 'Git Tag 命名规范、Commit Message 规范',
    status: 'done',
  },
  {
    key: 's3', index: 3, short: '质量流水线', full: '③ 质量流水线',
    input: 'Git Commit、CI 配置',
    output: '质检报告（功能/安全/性能/依赖）',
    owner: 'CI Bot + QA',
    gate: '全部质检项通过、无高危漏洞、性能达标',
    status: 'done',
  },
  {
    key: 's4', index: 4, short: '打zip包', full: '④ 打 zip 包',
    input: '质检通过的 Git Commit',
    output: 'Skill zip 包（含 SKILL.md + scripts/ + references/ + requirements.txt）',
    owner: 'CI Bot',
    gate: 'zip 包结构合规、大小 ≤ 10MB、校验和匹配',
    status: 'running',
  },
  {
    key: 's5', index: 5, short: '推送zpk', full: '⑤ 推送 zpk',
    input: 'Skill zip 包、Git Tag',
    output: 'zpk 产物（zip + 元数据 JSON）',
    owner: '流水线工程师',
    gate: 'zpk 校验通过、Git Tag 匹配、智家登记成功',
    status: 'waiting',
  },
  {
    key: 's6', index: 6, short: '智家登记', full: '⑥ 智家登记',
    input: 'zpk 产物、版本元数据',
    output: '智家注册表记录（zpk 版本号 + Git Commit + 发布时间）',
    owner: '系统自动',
    gate: '三元绑定完成（Git Tag ↔ zpk ↔ 智家登记）',
    status: 'waiting',
  },
])

function statusText(s: StageStatus) {
  return { done: '已完成', running: '进行中', waiting: '等待中' }[s]
}
</script>

<style scoped lang="scss">
.pad { padding: 11px; }

/* 流程图 */
.flow {
  display: flex;
  align-items: center;
  gap: 0;
  overflow-x: auto;
  padding: 10px 0;
}
.flow-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 90px;
  padding: 10px 8px;
  border: 2px solid var(--line);
  border-radius: 8px;
  background: #fafcfa;
  text-align: center;
  position: relative;
  &.done { border-color: var(--teal); background: var(--soft); }
  &.running {
    border-color: var(--teal);
    background: var(--soft);
    box-shadow: 0 0 0 3px rgba(8, 126, 133, 0.15);
    animation: glow 2s ease-in-out infinite;
  }
  &.waiting { opacity: 0.55; }
}
@keyframes glow {
  0%, 100% { box-shadow: 0 0 0 3px rgba(8, 126, 133, 0.15); }
  50% { box-shadow: 0 0 0 6px rgba(8, 126, 133, 0.08); }
}
.flow-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--teal);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.flow-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
}
.flow-dep {
  font-size: 10px;
  color: var(--warn);
  margin-top: 2px;
}
.flow-arrow {
  color: var(--teal);
  font-weight: 700;
  margin: 0 6px;
  font-size: 14px;
}

/* 阶段卡片 */
.stage-card {
  border: 1px solid var(--line);
  border-radius: 6px;
  margin-bottom: 10px;
  overflow: hidden;
  &:last-child { margin-bottom: 0; }
}
.stage-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f4f9f9;
  border-bottom: 1px solid var(--line);
}
.stage-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--teal);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stage-title { flex: 1; font-weight: 600; font-size: 13px; }
.stage-status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  background: var(--soft);
  color: var(--teal);
  &.done { background: var(--soft); color: var(--teal); }
  &.running {
    background: var(--warn-bg);
    color: var(--warn);
    animation: pulse-status 1.5s ease-in-out infinite;
  }
  &.waiting { background: #eef2f2; color: var(--muted); }
}
@keyframes pulse-status {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
.stage-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  padding: 10px 12px;
}
.stage-col {
  padding: 4px 8px;
  font-size: 12px;
  b { color: var(--teal); font-weight: 600; display: block; margin-bottom: 3px; }
  &.gate b { color: var(--warn); }
}

/* 质量门禁卡片 */
.qm-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.qm-card {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fafcfa;
  b { color: var(--teal); font-weight: 600; display: block; margin-bottom: 8px; }
  code {
    padding: 1px 5px;
    background: var(--soft);
    border-radius: 3px;
    font-size: 11px;
    color: var(--teal);
  }
}
.qm-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink);
}

/* 汇合关系图 */
.merge-diagram {
  display: flex;
  align-items: stretch;
  gap: 12px;
}
.merge-col {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 10px;
}
.merge-label {
  font-weight: 600;
  color: var(--teal);
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}
.merge-item {
  padding: 5px 8px;
  font-size: 12px;
  border-bottom: 1px dashed #e0ebeb;
  &:last-child { border-bottom: 0; }
  &.highlight {
    background: var(--soft);
    color: var(--teal);
    font-weight: 600;
    border-radius: 4px;
    border-bottom: 0;
    margin: 2px 0;
  }
}
.merge-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--teal);
  font-size: 13px;
  min-width: 90px;
}

@media (max-width: 850px) {
  .stage-body { grid-template-columns: 1fr; }
  .qm-grid { grid-template-columns: 1fr; }
  .merge-diagram { flex-direction: column; }
  .merge-arrow { min-height: 40px; }
}
</style>
