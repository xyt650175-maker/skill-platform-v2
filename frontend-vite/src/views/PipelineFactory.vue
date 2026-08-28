<template>
  <div>
    <section class="page-head">
      <strong class="page-title">流水线1 · 工厂侧</strong>
      <span class="chip ok">7 阶段</span>
      <span class="chip">v2.3.1</span>
      <div class="actions">
        <button class="btn">查看历史</button>
        <button class="btn primary">触发流水线</button>
      </div>
    </section>

    <div class="page-context">
      <b>工厂侧交付流水线</b>　/　从定版到生产的完整链路
    </div>

    <section class="page-content">
      <div class="notice">
        工厂侧 7 阶段流水线：工厂提交定版 → 自动提取 → 流水线注入 → 应用镜像生成 → 部署验证 → 晋级生产 → 变更验证。其中 ③ 流水线注入 依赖流水线2的 zpk 产物。
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
                <div v-if="s.dep" class="flow-dep">⚠ 依赖流水线2</div>
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

      <!-- 版本与回滚策略 -->
      <section class="panel" style="margin-top:12px">
        <h3>版本与回滚策略</h3>
        <div class="pad">
          <div class="ver-grid">
            <div class="ver-card">
              <b>镜像版本标签</b>
              <div class="hint">
                采用 <code>release-{date}-{commit}</code> 格式，如 <code>release-20260811-a3f2c1</code>。
                每个生产镜像对应唯一 Git Commit，支持溯源。
              </div>
            </div>
            <div class="ver-card">
              <b>失败回滚</b>
              <div class="hint">
                部署验证失败时自动回滚到上一版本镜像；
                若连续 3 次失败，暂停流水线并通知负责人人工介入。
              </div>
            </div>
            <div class="ver-card">
              <b>灰度策略</b>
              <div class="hint">
                新版本先灰度 10% → 观察 30 分钟 → 50% → 观察 1 小时 → 100%。
                灰度期间错误率超过 1% 立即回滚。
              </div>
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
            关键依赖：流水线1的 ③流水线注入 需等待流水线2的 ⑤推送zpk 产物，
            通过 zpk 版本号 + Git Tag + 智家登记三元绑定保证版本可追溯。
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
  dep?: boolean
}

const stages = ref<Stage[]>([
  {
    key: 's1', index: 1, short: '工厂定版', full: '① 工厂提交定版',
    input: '工厂侧需求文档、变更申请单、测试报告',
    output: '定版申请单（含版本号、变更范围）',
    owner: '工厂侧负责人',
    gate: '需求文档完整性、测试报告通过率 100%',
    status: 'done',
  },
  {
    key: 's2', index: 2, short: '自动提取', full: '② 自动提取',
    input: '定版申请单、Git 仓库地址',
    output: '代码快照、依赖清单、配置文件',
    owner: '系统自动（CI Bot）',
    gate: '代码扫描无高危漏洞、依赖版本合规',
    status: 'done',
  },
  {
    key: 's3', index: 3, short: '流水线注入', full: '③ 流水线注入（依赖线路二）',
    input: '代码快照 + zpk 产物（来自流水线2）',
    output: '注入后的代码包、版本关联元数据',
    owner: '流水线工程师',
    gate: 'zpk 产物校验通过、Git Tag 匹配',
    status: 'running', dep: true,
  },
  {
    key: 's4', index: 4, short: '镜像生成', full: '④ 应用镜像生成',
    input: '注入后的代码包、Dockerfile',
    output: '生产镜像（含版本标签）',
    owner: '镜像构建服务',
    gate: '镜像扫描通过、大小 ≤ 500MB',
    status: 'waiting',
  },
  {
    key: 's5', index: 5, short: '部署验证', full: '⑤ 部署验证',
    input: '生产镜像、部署配置',
    output: '部署验证报告（功能/性能/安全）',
    owner: 'QA 团队',
    gate: '功能用例 100% 通过、P95 延迟 ≤ 2s',
    status: 'waiting',
  },
  {
    key: 's6', index: 6, short: '晋级生产', full: '⑥ 晋级生产',
    input: '部署验证通过的镜像',
    output: '生产环境镜像、灰度发布计划',
    owner: 'SRE 团队',
    gate: '灰度 10% → 50% → 100% 逐级放量',
    status: 'waiting',
  },
  {
    key: 's7', index: 7, short: '变更验证', full: '⑦ 变更验证',
    input: '生产环境运行数据',
    output: '变更验证报告、关闭变更单',
    owner: '工厂侧 + QA',
    gate: '线上错误率 < 0.1%、核心指标无回退',
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

/* 版本策略 */
.ver-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.ver-card {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fafcfa;
  b { color: var(--teal); font-weight: 600; display: block; margin-bottom: 6px; }
  code {
    padding: 1px 5px;
    background: var(--soft);
    border-radius: 3px;
    font-size: 11px;
    color: var(--teal);
  }
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
  .ver-grid { grid-template-columns: 1fr; }
  .merge-diagram { flex-direction: column; }
  .merge-arrow { min-height: 40px; }
}
</style>
