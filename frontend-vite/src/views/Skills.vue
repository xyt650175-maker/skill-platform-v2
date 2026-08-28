<template>
  <div>
    <section class="page-head">
      <strong class="page-title">{{ currentSkill?.name || '未选择 Skill' }}</strong>
      <span class="chip ok">{{ statusLabel(currentSkill?.status) }}</span>
      <span class="chip">{{ currentSkill?.version || 'v0.0.0' }}</span>
      <span class="chip">{{ developmentMode.label }}</span>
      <div class="actions">
        <button class="btn" @click="saveDraft" :disabled="!currentSkill">保存草稿</button>
        <button class="btn" @click="publishSkill" :disabled="!currentSkill">提交评审</button>
        <button class="btn primary" @click="openDevelopmentModeChooser">＋ 新建技能</button>
      </div>
    </section>

    <section v-if="pendingChanges.length" class="ai-change-banner">
      <div>
        <b>AI 修改了 {{ pendingChanges.length }} 个文件</b>
        <span class="change-files">{{ pendingChanges.map(change => `${change.path} (+${change.added} −${change.removed})`).join('　') }}</span>
      </div>
      <div class="change-actions">
        <button class="btn primary" @click="applyPendingChanges" :disabled="applyingChanges">{{ applyingChanges ? '应用中…' : '应用' }}</button>
        <button class="btn" @click="revertPendingChanges" :disabled="applyingChanges">撤销</button>
        <button class="diff-link" @click="showDiffDialog = true">查看 Diff 详情 →</button>
      </div>
    </section>

    <section class="page-content">
      <div :class="['grid3', { 'chat-collapsed': chatCollapsed }]">
        <!-- 左栏：AI 对话 -->
        <section v-if="!chatCollapsed" class="col chat-col">
          <div class="chat-header">
            <div class="chat-heading">
              <h3>AI 创建Skill</h3>
              <p>描述你想创建的 Skill，AI 会自动生成代码与配置</p>
            </div>
            <div class="chat-header-actions">
              <button class="btn sm" @click="clearChat" title="清空对话">清空对话</button>
              <button class="btn sm chat-collapse-btn" @click="chatCollapsed = true" title="收起 AI 对话" aria-label="收起 AI 对话">‹</button>
            </div>
          </div>
          <div class="chat-body" ref="chatBodyRef">
            <div v-if="chatMessages.length === 0" class="chat-empty">
              <div class="hint">输入需求，AI 将帮你生成 Skill 代码。</div>
              <div class="chat-suggestions">
                <button class="suggestion" @click="useSuggestion(s)" v-for="s in initialSuggestions" :key="s.text">
                  <span>{{ s.text }}</span><small>{{ s.templateLabel }} · {{ s.caseLabel }}</small>
                </button>
              </div>
            </div>
            <div v-for="(msg, i) in chatMessages" :key="i" :class="['msg', msg.role]">
              <div class="msg-avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
              <div class="msg-content">
                <!-- 思考过程（可折叠） -->
                <div v-if="msg.thinking" class="thinking-block">
                  <div class="thinking-toggle" @click="msg.thinkingOpen = !msg.thinkingOpen">
                    <span class="thinking-icon">{{ msg.thinkingOpen ? '▼' : '▶' }}</span>
                    思考过程{{ msg.thinkingStream ? '…' : '' }}
                  </div>
                  <div v-show="msg.thinkingOpen" class="thinking-content">{{ msg.thinking }}</div>
                </div>
                <!-- 正文 -->
                <div class="msg-text" v-html="renderMarkdown(msg.content)"></div>
                <!-- 流式光标 -->
                <span v-if="msg.streaming" class="cursor">▊</span>
              </div>
            </div>
            <div v-if="chatMessages.length && !chatStreaming" class="chat-follow-up chat-follow-up-inline">
              <b>你还可以继续：</b>
              <button class="suggestion" @click="useSuggestion(s)" v-for="s in progressiveSuggestions" :key="s.text">
                <span>{{ s.text }}</span><small>{{ s.templateLabel }} · {{ s.caseLabel }}</small>
              </button>
            </div>
          </div>
          <section class="generation-template" aria-label="测试数据模板">
            <b>测试数据模板：</b>
            <div class="template-select">
              <button class="template-select-trigger" @click="templateMenuOpen = !templateMenuOpen">{{ selectedTemplate.label }} <span>⌄</span></button>
              <div v-if="templateMenuOpen" class="template-select-menu">
                <div v-for="dataset in availableTemplates" :key="dataset.id" :class="['template-option', { selected: dataset.id === templateDatasetId }]">
                  <button class="template-option-name" @click="selectTemplate(dataset.id)">{{ dataset.id === templateDatasetId ? '✓ ' : '' }}{{ dataset.label }}</button>
                  <span class="template-option-actions">
                    <button @click="downloadTemplate(dataset)">下载</button>
                    <button @click="previewTemplate(dataset.id)">查看</button>
                  </span>
                </div>
                <button class="template-manage-option" @click="openNewTemplateManager">＋ 上传 / 管理模板</button>
              </div>
            </div>
          </section>
          <div class="chat-input-area">
            <textarea
              ref="chatInputRef"
              class="chat-input"
              v-model="chatInput"
              placeholder="描述你的需求，例如：生成一个产品信息查询 Skill…"
              @keydown.enter.exact.prevent="sendChat"
              rows="6"
            ></textarea>
            <button
              :class="['btn', 'primary', { 'is-generating': chatStreaming }]"
              @click="chatStreaming ? stopChatGeneration() : sendChat()"
              :aria-label="chatStreaming ? '终止生成' : '发送'"
              :title="chatStreaming ? '终止生成' : '发送'"
            >
              <span v-if="chatStreaming" class="stop-icon" aria-hidden="true"></span>
              <span v-else class="send-label">➤</span>
            </button>
          </div>
          <p v-if="recommendationNotice" class="recommendation-notice">{{ recommendationNotice }}</p>
        </section>

        <!-- 中栏：Skill 文件 + 调试 -->
        <section class="col">
          <div class="tabs" aria-label="工作区视图">
            <button :class="{ active: midTab === 'files' }" @click="midTab = 'files'">Skill 文件</button>
            <button :class="{ active: midTab === 'debug' }" @click="midTab = 'debug'">调试</button>
            <button :class="{ active: midTab === 'versions' }" @click="midTab = 'versions'; loadVersions()">版本历史</button>
            <button :class="{ active: midTab === 'logs' }" @click="midTab = 'logs'; loadOperationLogs(1)">日志</button>
            <button v-if="chatCollapsed" class="tabs-expand" @click="chatCollapsed = false" title="展开 AI 对话" aria-label="展开 AI 对话">⤢</button>
          </div>

          <!-- Skill 文件工作台 -->
          <div v-if="midTab === 'files'" class="workbench">
            <div class="wb-main">
              <aside class="file-explorer">
                <div class="explorer-head"><b>文件浏览器</b></div>
                <button v-for="file in rootFiles" :key="`tree-${file.path}`" :class="['tree-file', { active: activeFile === file.path, empty: file.empty }]" @click="activeFile = file.path">
                  <span>{{ file.icon }}</span>{{ file.label }}
                </button>
                <div v-for="folder in folders" :key="folder.name" class="tree-folder-group">
                  <button class="tree-folder" @click="expandedFolders[folder.name] = !expandedFolders[folder.name]">
                    <span class="folder-chevron">{{ expandedFolders[folder.name] ? '⌄' : '›' }}</span>
                    <span>📁</span><b>{{ folder.name }}/</b>
                    <small>{{ folder.files.length }}</small>
                  </button>
                  <div v-show="expandedFolders[folder.name]" class="tree-folder-children">
                    <button v-for="file in folder.files" :key="`tree-${file.path}`" :class="['tree-file', 'nested', { active: activeFile === file.path, empty: file.empty }]" @click="activeFile = file.path">
                      <span>{{ file.icon }}</span>{{ file.path.slice(folder.name.length + 1) }}
                    </button>
                  </div>
                </div>
                <p v-if="currentSkill && !fileList.length" class="explorer-empty">当前 Skill 还没有文件。请通过 AI 生成、导入本地包或从 Git 导入后开始开发。</p>
              </aside>
            <!-- 工作台主体：代码区 -->
            <div class="wb-body">
              <!-- 生成进度条 -->
              <div v-if="chatStreaming && codeStreamActive" class="wb-progress">
                <div class="wb-progress-bar"></div>
                <span class="wb-progress-text">
                  <span class="dot"></span> AI 正在写入 {{ activeFile }}…
                </span>
              </div>

              <!-- 空状态 -->
              <div v-if="!currentSkill" class="wb-placeholder">
                <div class="wb-placeholder-icon">⌘</div>
                <div>选择一个 Skill，或通过左侧 AI 对话生成代码</div>
                <div class="hint">生成的代码会在这里实时呈现</div>
              </div>

              <!-- 代码内容：行号 + 代码 -->
              <div v-else class="wb-code-area" ref="wbCodeAreaRef">
                <div class="wb-gutter">
                  <div v-for="n in lineCount" :key="n" class="wb-line-num">{{ n }}</div>
                </div>
                <pre class="wb-code"><code v-html="highlightedCode"></code><span v-if="chatStreaming && codeStreamActive" class="code-cursor">▋</span></pre>
              </div>

              <!-- 状态栏 -->
              <div class="wb-statusbar">
                <span class="wb-status-item">
                  <span :class="['wb-file-state', fileState]">{{ fileStateLabel }}</span>
                </span>
                <span class="wb-status-item">{{ lineCount }} 行</span>
                <span class="wb-status-item">{{ fileContent.length }} 字符</span>
                <span class="wb-status-item">{{ activeFile }}</span>
                <span class="wb-status-item wb-status-right">
                  <button class="wb-action" @click="copyCode" title="复制">复制</button>
                  <button class="wb-action" @click="openCodeEditor" :disabled="!currentSkill">编辑</button>
                </span>
              </div>
            </div>
            </div>

    <div class="hint" style="margin-top:8px">标准 Skill 目录：SKILL.md（含 YAML 前言区）/ scripts/ / references/ / requirements.txt · AI 生成时本区域实时更新</div>

          </div>

          <!-- 调试：执行当前 Skill 的完整工作区文件 -->
          <div v-else-if="midTab === 'debug'" class="debug-panel">
            <div class="debug-panel-head"><b>调试结果</b><span>运行输出　Token　Prompt 调试</span></div>
            <div class="debug-mode-note">受限本地运行（非容器沙箱）· 临时目录 · Python 隔离模式 · 3 秒超时</div>
            <div class="hint">执行 <code>scripts/main.py</code> 中的 <code>handle(input_data)</code>，输入必须为 JSON 对象。</div>
            <label class="label" style="margin-top:10px">调试数据来源</label>
            <select class="input" v-model="debugDataSource">
              <option value="template-sample">模板示例输入（快速验证）</option>
              <option value="template-all">模板完整用例（全量测试）</option>
              <option value="business" :disabled="!businessDataset">已上传业务测试数据{{ businessDataset ? `（${debugTestCases.length} 条）` : '（请先保存）' }}</option>
            </select>
            <div v-if="debugDataSource === 'business'" class="debug-dataset-sync">
              <label>选择测试用例</label>
              <select class="input" v-model="selectedDebugCaseId"><option v-for="item in debugTestCases" :key="item.id" :value="item.id">{{ item.name }}</option></select>
            </div>
            <label class="label" style="margin-top:10px">测试输入 (JSON)</label>
            <div class="debug-dataset-sync">{{ debugDataSource === 'template-all' ? '当前展示完整模板数据；点击"运行全部用例"将按其中 testCases 逐条执行。' : `${debugDataSourceLabel}；可直接编辑当前输入。` }}</div>
            <textarea class="area" v-model="debugInput" placeholder='{"product_code": "000001"}' style="min-height:80px"></textarea>
            <button class="btn primary" style="margin-top:10px" @click="runDebug" :disabled="!currentSkill || debugRunning">{{ debugRunning ? '运行中…' : '运行调试' }}</button>
            <div class="box" style="margin-top:10px">
              <b>运行结果</b>
              <pre class="debug-output">{{ debugResult }}</pre>
              <div v-if="lastDebugMeta" class="debug-meta">
                <span>耗时：{{ lastDebugMeta.durationMs }}ms</span>
                <span :class="lastDebugMeta.status === 'PASS' ? 'pass' : 'fail'">状态：{{ lastDebugMeta.status }}</span>
                <span>依赖调用：{{ lastDebugMeta.dependencyCalls }} 次</span>
                <span>Token：{{ lastDebugMeta.totalTokens || 0 }}</span>
              </div>
              <div v-if="debugFailureLog" class="debug-error-log">
                <b>失败原因与执行日志</b>
                <pre>{{ debugFailureLog }}</pre>
              </div>
            </div>
            <div class="box">
              <b>调试过程 Token 消耗</b>
              <div class="hint">{{ tokenUsageSummary }}</div>
              <div class="case" v-for="(c, i) in debugHistory" :key="i">
                <div class="case-top">
                  <b>{{ c.time }}</b>
                  <span :class="['state', c.ok ? '' : 'fail']">{{ c.ok ? '通过' : '失败' }}</span>
                </div>
                <div class="hint">输入: {{ c.input }} · Token: {{ c.tokens || 0 }}</div>
              </div>
            </div>
          </div>
          <!-- 版本历史 -->
          <div v-else-if="midTab === 'versions'" class="debug-panel version-panel">
            <div class="debug-panel-head"><b>版本历史</b><span>查看 Skill 历史版本与差异对比</span></div>
            <div v-if="versionsLoading" class="pad hint">加载中...</div>
            <div v-else-if="!skillVersions.length" class="pad hint">暂无历史版本。定版发布后版本将在此显示。</div>
            <div v-else>
              <div class="version-list">
                <div class="version-item" v-for="v in skillVersions" :key="v.id">
                  <div class="version-info">
                    <b>v{{ v.version }}</b>
                    <span class="version-time">{{ formatTime(v.createTime) }}</span>
                    <span class="version-summary" v-if="v.changeSummary">{{ v.changeSummary }}</span>
                  </div>
                </div>
              </div>
              <div class="version-actions" v-if="skillVersions.length >= 2">
                <label>选择版本 1：</label>
                <select v-model="diffVersion1">
                  <option v-for="v in skillVersions" :key="v.id" :value="v.version">v{{ v.version }}</option>
                </select>
                <label>选择版本 2：</label>
                <select v-model="diffVersion2">
                  <option v-for="v in skillVersions" :key="v.id" :value="v.version">v{{ v.version }}</option>
                </select>
                <button class="btn primary" @click="loadDiff">对比差异</button>
              </div>
            </div>
            <!-- Diff 视图 -->
            <div v-if="showDiffView" class="diff-view">
              <div class="diff-header">
                <b>版本对比：v{{ diffVersion1 }} vs v{{ diffVersion2 }}</b>
                <button class="btn sm" @click="showDiffView = false">关闭</button>
              </div>
              <div v-if="diffLoading" class="pad hint">计算差异中...</div>
              <div v-else>
                <div v-for="file in diffResult.files" :key="file.path" class="diff-file">
                  <div class="diff-file-header">
                    <b>{{ file.path }}</b>
                    <span :class="['diff-status', file.status]">
                      {{ file.status === 'added' ? '新增' : file.status === 'deleted' ? '删除' : file.status === 'modified' ? '修改' : '未变' }}
                    </span>
                  </div>
                  <div class="diff-content" v-if="file.status !== 'unchanged'">
                    <div class="diff-side" v-if="file.content1 !== undefined">
                      <span class="diff-label">v{{ diffVersion1 }}</span>
                      <pre>{{ file.content1 }}</pre>
                    </div>
                    <div class="diff-side" v-if="file.content2 !== undefined">
                      <span class="diff-label">v{{ diffVersion2 }}</span>
                      <pre>{{ file.content2 }}</pre>
                    </div>
                  </div>
                </div>
                <div v-if="!diffResult.files || !diffResult.files.length" class="pad hint">两个版本内容完全一致。</div>
              </div>
            </div>
          </div>

          <!-- 日志：保存、生成与调试的可追溯记录 -->
          <div v-else class="debug-panel log-panel">
            <div class="debug-panel-head"><b>操作日志</b><span>多人协作编辑历史追溯</span></div>
            <div class="operation-log-table">
              <table v-if="operationLogs.records && operationLogs.records.length">
                <thead>
                  <tr>
                    <th>操作用户</th>
                    <th>操作类型</th>
                    <th>操作备注</th>
                    <th>修改记录</th>
                    <th>状态</th>
                    <th>操作版本</th>
                    <th>操作时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="log in operationLogs.records" :key="log.id">
                    <td>{{ log.operatorName }}</td>
                    <td><span :class="['action-tag', actionTagClass(log.action)]">{{ log.actionLabel }}</span></td>
                    <td>{{ log.description || '—' }}</td>
                    <td class="change-summary">{{ log.changeSummary || '—' }}</td>
                    <td><span :class="['status-tag', log.status === 'success' ? 'success' : 'failed']">{{ log.status === 'success' ? '成功' : '失败' }}</span></td>
                    <td>{{ log.version || '—' }}</td>
                    <td>{{ formatTime(log.createTime) }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-log">暂无操作日志记录。保存、调试或提交评审后将在此显示。</div>
            </div>
            <div v-if="operationLogs.pages > 1" class="pagination">
              <button class="btn sm" :disabled="operationLogs.current <= 1" @click="loadOperationLogs(operationLogs.current - 1)">上一页</button>
              <span>第 {{ operationLogs.current }} / {{ operationLogs.pages }} 页（共 {{ operationLogs.total }} 条）</span>
              <button class="btn sm" :disabled="operationLogs.current >= operationLogs.pages" @click="loadOperationLogs(operationLogs.current + 1)">下一页</button>
            </div>
            <div v-if="debugHistory.length" class="debug-history-section" style="margin-top:20px">
              <b>调试历史（本地）</b>
              <div class="case" v-for="(c, i) in debugHistory" :key="`log-${i}`">
                <div class="case-top"><b>{{ c.time }}</b><span :class="['state', c.ok ? '' : 'fail']">{{ c.ok ? '通过' : '失败' }}</span></div>
                <div class="hint">输入: {{ c.input }} · Token: {{ c.tokens || 0 }}</div>
              </div>
            </div>
          </div>
        </section>

      </div>
    </section>

    <div v-if="showDiffDialog" class="modal-mask" @click.self="showDiffDialog = false">
      <div class="modal-box diff-modal">
        <div class="modal-head"><b>AI 修改文件预览</b><button class="icon-btn" @click="showDiffDialog = false">×</button></div>
        <div v-for="change in pendingChanges" :key="change.path" class="diff-file">
          <b>{{ change.path }}</b><span>+{{ change.added }} −{{ change.removed }}</span>
          <pre><code class="diff-remove">{{ change.before || '（新文件）' }}</code><code class="diff-add">{{ change.after || '（文件删除）' }}</code></pre>
        </div>
        <div class="modal-actions"><button class="btn" @click="showDiffDialog = false">稍后处理</button><button class="btn primary" @click="applyPendingChanges">应用变更</button></div>
      </div>
    </div>

    <div v-if="dialogModeChooser" class="modal-mask" @click.self="dialogModeChooser=false">
      <div class="modal-box creation-dialog mode-create-dialog">
        <div class="modal-head"><b>新建skill</b><button class="modal-close" @click="dialogModeChooser=false" aria-label="关闭">×</button></div>
        <div class="modal-body">
          <div class="mode-agent-row">
            <label class="label">选择智能体</label>
            <select class="input agent-picker" v-model="newSkill.agentName" @change="onAgentNameChange">
              <option v-for="agent in agentOptions" :key="agent.id" :value="agent.name">{{ agent.name }}</option>
            </select>
          </div>
          <div class="mode-agent-row" v-if="newSkill.agentName">
            <label class="label">选择版本</label>
            <select class="input agent-version-picker" v-model="newSkill.agentVersion">
              <option v-for="v in agentVersions" :key="v.version" :value="v.version">{{ v.version }}<span v-if="v.changeSummary"> - {{ v.changeSummary }}</span></option>
            </select>
            <span class="hint" v-if="!agentVersions.length">该智能体暂无版本，请先在智能体中心创建版本</span>
          </div>
          <p class="hint mode-intro">勾选 Skill 的创建方式</p>
          <div class="mode-chooser">
            <button v-for="(mode, key) in DEVELOPMENT_MODES" :key="key" :class="{ selected: selectedMode === key }" @click="selectedMode = key as 'online' | 'local' | 'git'">
              <b>{{ mode.label }}</b><small>{{ mode.hint }}</small>
              <span :class="['mode-visual', `mode-visual-${key}`]" aria-hidden="true"><i></i><i></i><em></em></span>
            </button>
          </div>
        </div>
        <div class="modal-foot"><button class="btn" @click="dialogModeChooser=false">取消</button><button class="btn primary" @click="selectDevelopmentMode(selectedMode)">下一步</button></div>
      </div>
    </div>

    <div v-if="testDataEditorVisible" class="modal-mask" @click.self="testDataEditorVisible = false">
      <div class="modal-box test-data-modal">
        <div class="modal-head"><b>编辑调试测试数据</b><button class="icon-btn" @click="testDataEditorVisible = false">×</button></div>
        <p class="hint">业务人员为已生成的 Skill 维护完整测试案例和预期结果。保存后写入当前 Skill 的 <code>references/test-data.json</code>，用于单条调试和全量测试。</p>
        <textarea v-model="testDataEditorText" class="area test-data-editor" spellcheck="false"></textarea>
        <div class="modal-actions"><button class="btn" @click="testDataEditorVisible = false">取消</button><button class="btn primary" @click="saveBusinessTestData" :disabled="testDataSaving">{{ testDataSaving ? '保存中…' : '保存并使用' }}</button></div>
      </div>
    </div>

    <div v-if="templatePreviewVisible" class="modal-mask" @click.self="templatePreviewVisible = false">
      <div class="modal-box template-preview-modal">
        <div class="modal-head"><b>测试数据模板 · {{ selectedTemplate.label }}</b><button class="icon-btn" @click="templatePreviewVisible = false">×</button></div>
        <p class="hint">该模板会作为本轮 Skill 生成的业务约束。以下是模板的实际字段、样例和用例内容。</p>
        <div class="template-preview-summary">
          <div><b>说明</b><span>{{ selectedTemplate.description }}</span></div>
          <div><b>示例输入</b><code>{{ JSON.stringify(selectedTemplate.input, null, 2) }}</code></div>
          <div><b>示例输出</b><code>{{ JSON.stringify(selectedTemplate.expected, null, 2) }}</code></div>
        </div>
        <b class="template-preview-label">完整模板数据</b>
        <pre class="template-preview-code">{{ JSON.stringify(buildBusinessTestData(selectedTemplate), null, 2) }}</pre>
        <div class="modal-actions"><button class="btn primary" @click="templatePreviewVisible = false">确认使用此模板</button></div>
      </div>
    </div>

    <div v-if="templateManagerVisible" class="modal-mask" @click.self="templateManagerVisible = false">
      <div class="modal-box template-preview-modal">
        <div class="modal-head"><b>管理测试数据模板</b><button class="icon-btn" @click="templateManagerVisible = false">×</button></div>
        <p class="hint">可编辑当前模板，或上传业务人员提供的 JSON。保存后，该模板将用于后续 AI 生成的输入、输出和业务规则约束。</p>
        <label class="label">模板名称 <span class="required-mark">*</span></label>
        <input v-model.trim="templateEditorName" class="input template-name-input" placeholder="例如：产品风险分层数据" />
        <p class="hint template-name-hint">该名称会显示在"测试数据模板"下拉列表中。</p>
        <label class="template-upload"><span>上传 JSON 模板</span><input type="file" accept="application/json,.json" @change="importTemplate" /></label>
        <textarea v-model="templateEditorText" class="area test-data-editor" spellcheck="false"></textarea>
        <div class="modal-actions"><button class="btn" @click="templateManagerVisible = false">取消</button><button class="btn primary" @click="saveTemplate">保存模板</button></div>
      </div>
    </div>

    <!-- 代码编辑器对话框 -->
    <div v-if="dialogCode" class="modal-mask" @click.self="dialogCode=false">
      <div class="modal-box" style="width:900px">
        <div class="modal-head">
          <b>编辑代码 - {{ currentSkill?.name }}</b>
          <button class="btn" @click="dialogCode=false">×</button>
        </div>
        <div class="modal-body">
          <div class="editor-toolbar">
            <button class="btn primary" @click="saveCode">保存草稿</button>
            <span class="hint">正在编辑：{{ editingFile }} · 草稿保留 3 天（TTL）</span>
          </div>
          <div class="editor-layout">
            <aside class="editor-file-list">
              <button v-for="file in fileList" :key="`editor-${file.path}`" :class="['editor-file', { active: editingFile === file.path }]" @click="selectEditorFile(file.path)">
                <span>{{ file.icon }}</span>{{ file.path }}
              </button>
            </aside>
            <div class="codemirror-wrapper">
              <div ref="editorEl" class="cm-editor-host"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 在线创建：与开发模式同一入口，不再在管理页重复选择 -->
    <div v-if="dialogCreate" class="modal-mask" @click.self="dialogCreate=false">
      <div class="modal-box creation-dialog">
        <div class="modal-head">
          <b>在线创建 Skill</b>
          <button class="btn" @click="dialogCreate=false">×</button>
        </div>
        <div class="modal-body">
          <label class="label">可见范围</label>
          <div class="radio-row"><label><input type="radio" v-model="newSkill.visibility" value="private" /> 私有</label><label><input type="radio" v-model="newSkill.visibility" value="team" /> 团队</label></div>
          <label class="label">名称 <small>仅小写字母、数字和连字符，最多 64 字符</small></label>
          <input class="input" v-model="newSkill.name" placeholder="如：product-search" />
          <label class="label">初始版本 <small>定版后将与 SKILL.md 和 Git Tag 保持一致</small></label>
          <input class="input" v-model="newSkill.version" placeholder="0.0.0" />
          <label class="label">描述</label>
          <textarea class="area" v-model="newSkill.description"></textarea>
          <label class="label">关联智能体</label>
          <select class="input" v-model="newSkill.agentName" @change="onAgentNameChange">
            <option v-for="agent in agentOptions" :key="agent.id" :value="agent.name">{{ agent.name }}</option>
          </select>
          <div v-if="newSkill.agentName" style="margin-top:8px">
            <label class="label">选择版本</label>
            <select class="input" v-model="newSkill.agentVersion" style="max-width:300px">
              <option v-for="v in agentVersions" :key="v.version" :value="v.version">{{ v.version }}<span v-if="v.changeSummary"> - {{ v.changeSummary }}</span></option>
            </select>
          </div>
          <label class="label">数据源</label><select class="input" v-model="newSkill.dataSource"><option value="mock">Mock 数据（开发调试）</option><option value="database-api">数据库 API（由后端安全代理连接）</option></select><p class="hint">数据库地址与密钥不在浏览器保存，创建后在 <code>references/data-source.json</code> 填写地址及环境变量名。</p>
          <label class="label">企业 Git 仓库 <button class="text-btn" @click="openCredentialManager">管理内网连接</button></label>
          <select class="input" v-model="newSkill.credentialId"><option value="">使用平台内部版本仓库</option><option v-for="item in gitCredentials" :key="item.id" :value="item.id">{{ item.name }} · {{ item.repoUrl }}</option></select>
          <label class="label">创建方式</label>
          <div class="radio-row"><label><input type="radio" v-model="newSkill.creationMode" value="model" /> 模型驱动</label><label><input type="radio" v-model="newSkill.creationMode" value="template" /> 模板驱动</label><label><input type="radio" v-model="newSkill.creationMode" value="custom" /> 自定义</label></div>
          <template v-if="newSkill.creationMode === 'model'"><label class="label">模型选择</label><select class="input" v-model="newSkill.model"><option>Think-Medium-Flash（Qwen3.6-35B-A3B）</option><option>Chat-Large（MiniMax-M2.5）</option><option>Chat-Ultra（Kimi k2.5）</option></select></template>
          <template v-else-if="newSkill.creationMode === 'template'"><label class="label">模板选择</label><select class="input" v-model="newSkill.skillTemplate"><option value="prompt">提示词模板</option><option value="workflow">工作流通用模板</option><option value="api">接口调用模板</option><option value="code">代码编写模板</option></select></template>
          <template v-else><label class="label">运行语言</label><select class="input" v-model="newSkill.language"><option value="python">Python</option><option value="javascript">JavaScript</option></select></template>
        </div>
        <div class="modal-foot">
          <button class="btn" @click="dialogCreate=false">取消</button>
          <button class="btn primary" @click="createSkill" :disabled="creatingSkill">{{ creatingSkill ? '创建中…' : '创建' }}</button>
        </div>
      </div>
    </div>

    <!-- 本地 ZIP 导入 -->
    <div v-if="dialogLocalImport" class="modal-mask" @click.self="dialogLocalImport=false"><div class="modal-box creation-dialog"><div class="modal-head"><b>从本地导入 Skill</b><button class="btn" @click="dialogLocalImport=false">×</button></div><div class="modal-body">
      <label class="label">显示昵称 <small>仅用于卡片展示，不参与名称校验</small></label><input class="input" v-model="localImport.alias" placeholder="可选，例如：产品查询工具" />
      <label class="label">可见范围</label><div class="radio-row"><label><input type="radio" v-model="localImport.visibility" value="private" /> 私有</label><label><input type="radio" v-model="localImport.visibility" value="team" /> 团队</label></div>
      <label class="label">企业 Git 仓库 <button class="text-btn" @click="openCredentialManager">管理内网连接</button></label><select class="input" v-model="localImport.credentialId"><option value="">使用平台内部版本仓库</option><option v-for="item in gitCredentials" :key="item.id" :value="item.id">{{ item.name }} · {{ item.repoUrl }}</option></select>
      <label class="zip-upload"><span>☁</span><b>{{ localImport.fileName || '拖拽 ZIP 到此处，或点击上传' }}</b><small>ZIP 包必须包含 SKILL.md；若存在版本号，须符合 x.y.z 格式</small><input type="file" accept=".zip" @change="selectLocalZip" /></label>
      <p v-if="localImport.validation" :class="['form-status', localImport.valid ? 'success' : 'error']">{{ localImport.validation }}</p>
      <label class="label">提交信息</label><textarea class="area compact" v-model="localImport.message" placeholder="可选，描述本次导入"></textarea>
    </div><div class="modal-foot"><button class="btn" @click="dialogLocalImport=false">取消</button><button class="btn primary" :disabled="!localImport.valid" @click="importLocalSkill">导入</button></div></div></div>

    <!-- Git 导入：仓库凭证、分支/Tag 与多 Skill 选择 -->
    <div v-if="dialogGitImport" class="modal-mask" @click.self="dialogGitImport=false"><div class="modal-box git-dialog"><div class="modal-head"><b>从企业 Git 导入 Skill</b><button class="btn" @click="dialogGitImport=false">×</button></div><div class="modal-body">
      <p class="hint">仅访问企业内网 Git 服务；连接信息与访问令牌均由后端安全托管。</p><label class="label">企业 Git 仓库 <button class="text-btn" @click="openCredentialManager">管理内网连接</button></label><select class="input" v-model="gitImport.credentialId"><option value="">请选择已配置的企业仓库</option><option v-for="item in gitCredentials" :key="item.id" :value="item.id">{{ item.name }} · {{ item.repoUrl }}</option></select>
      <div class="inline-actions"><button class="btn sm" @click="loadGitSkills">加载 Skill</button><span class="hint">读取仓库后选择分支或 Tag</span></div>
      <template v-if="gitImport.loaded"><label class="label">导入来源</label><div class="radio-row"><label><input type="radio" v-model="gitImport.refType" value="branch" @change="onGitRefTypeChange" /> 按分支导入</label><label><input type="radio" v-model="gitImport.refType" value="tag" @change="onGitRefTypeChange" /> 按 Tag 导入</label></div><label class="label">{{ gitImport.refType === 'branch' ? '分支' : 'Tag' }}</label><select class="input" v-model="gitImport.ref" @change="loadGitSkillCandidates"><option v-for="refName in gitRefs" :key="refName" :value="refName">{{ refName }}</option></select>
      <label class="label">Skill 列表</label><table class="git-skill-table"><thead><tr><th><input type="checkbox" :checked="gitSkillCandidates.length > 0 && gitImport.selected.length === gitSkillCandidates.length" @change="toggleAllGitSkills" /></th><th>Skill 名称</th><th>版本</th><th>描述</th><th>SKILL.md</th></tr></thead><tbody><tr v-for="item in gitSkillCandidates" :key="item.path"><td><input type="checkbox" :value="item.path" v-model="gitImport.selected" /></td><td>{{ item.name }}</td><td>{{ item.version }}</td><td>{{ item.description }}</td><td class="has-file">有</td></tr><tr v-if="!gitSkillCandidates.length"><td colspan="5" class="hint">当前分支或 Tag 未发现包含 SKILL.md 的标准 Skill 包。</td></tr></tbody></table></template>
    </div><div class="modal-foot"><button class="btn" @click="dialogGitImport=false">取消</button><button class="btn primary" :disabled="!gitImport.selected.length" @click="importGitSkill">导入</button></div></div></div>

    <!-- Git 凭证管理 -->
  <div v-if="dialogCredentials" class="modal-mask" @click.self="dialogCredentials=false"><div class="modal-box git-dialog"><div class="modal-head"><b>企业 Git 连接管理</b><button class="btn" @click="dialogCredentials=false">×</button></div><div class="modal-body"><p class="hint">仅支持企业内网 Git。访问令牌只在本次提交时发送至后端保存，浏览器不会保存或再次展示令牌。</p><div class="inline-actions"><b>已配置企业连接</b><button class="btn sm primary" @click="newCredential">＋ 新增连接</button></div><table class="git-skill-table"><thead><tr><th>名称 / 仓库地址</th><th>认证方式</th><th>用户名</th><th>令牌状态</th><th>操作</th></tr></thead><tbody><tr v-for="item in gitCredentials" :key="item.id"><td><b>{{ item.name }}</b><small>{{ item.repoUrl }}</small></td><td>{{ item.authType }}</td><td>{{ item.username }}</td><td>{{ item.tokenMasked }}</td><td><button class="text-btn" :disabled="testingCredentialId === item.id" @click="testCredential(item)">{{ testingCredentialId === item.id ? '测试中…' : '测试连接' }}</button><button class="text-btn" @click="editCredential(item)">编辑</button><button class="text-btn danger" @click="deleteCredential(item.id)">删除</button></td></tr><tr v-if="!gitCredentials.length"><td colspan="5" class="hint">暂无企业 Git 连接，请新增后选择仓库。</td></tr></tbody></table><p v-if="credentialNotice" :class="['credential-notice', credentialNotice.ok ? 'ok' : 'failed']">{{ credentialNotice.message }}</p>
      <div v-if="credentialEditing" class="credential-form"><label class="label">连接名称</label><input class="input" v-model="credentialForm.name" placeholder="例如：业务团队 Skill 仓库" /><label class="label">企业内网仓库地址</label><input class="input" v-model="credentialForm.repoUrl" placeholder="例如：https://git.intra.example.com/internal/skills.git" /><label class="label">用户名</label><input class="input" v-model="credentialForm.username" placeholder="企业 Git 用户名" /><label class="label">企业 Git 访问令牌</label><input class="input" type="password" v-model="credentialForm.token" :placeholder="credentialForm.id ? '留空则保持后端已有令牌' : '由企业 Git 平台签发，仅发送给后端'" /><div class="inline-actions"><button class="btn sm primary" :disabled="savingCredential" @click="saveCredential">{{ savingCredential ? '保存中…' : '保存企业连接' }}</button><button class="btn sm" :disabled="savingCredential" @click="credentialEditing=false">取消</button></div></div>
    </div><div class="modal-foot"><button class="btn" @click="dialogCredentials=false">关闭</button></div></div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, reactive, watch, nextTick } from 'vue'
import { enterpriseGitApi, skillApi, agentApi, type EnterpriseGitCredential, type EnterpriseGitRefs, type EnterpriseGitSkill, type Skill } from '@/api'
import { useRoute, useRouter } from 'vue-router'
import { EditorState } from '@codemirror/state'
import { EditorView, lineNumbers, keymap } from '@codemirror/view'
import { python } from '@codemirror/lang-python'
import { oneDark } from '@codemirror/theme-one-dark'
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import JSZip from 'jszip'
import { DEVELOPMENT_MODES, TEST_DATASETS, buildBusinessTestData, canSubmitForReview, getDatasetTestCases, getTestDataset, parseDebugInput, validateBusinessTestData } from '@/domain/skillWorkspace'

/* ==================== Skill 列表 ==================== */
const skills = ref<Skill[]>([])
const router = useRouter()
const route = useRoute()
const activeDevelopmentMode = ref<'online' | 'local' | 'git'>('online')
const developmentMode = computed(() => DEVELOPMENT_MODES[activeDevelopmentMode.value])
const agentOptions = ref<any[]>([
  { id: 1001, name: '产品服务主智能体' },
  { id: 1002, name: '业务问答智能体' },
  { id: 1003, name: '营销助手智能体' },
])
const agentVersions = ref<any[]>([])
const agentVersionsLoading = ref(false)
const currentAgentName = ref('产品服务主智能体')
const currentSkillId = ref('')
const currentSkill = computed(() => skills.value.find(s => s.id === currentSkillId.value) || null)
const skillStatusLabel = computed(() => ({ draft: '草稿', reviewing: '评审中', published: '已发布', rejected: '已驳回' }[currentSkill.value?.status || 'draft'] || currentSkill.value?.status || '草稿'))
const activeFile = ref('scripts/main.py')
const code = ref('')
const skillsMd = ref('')
const skillFiles = ref<Record<string, string>>({})
const recentLog = ref('等待加载')
const midTab = ref<'files' | 'debug' | 'versions' | 'logs'>('files')
const dialogModeChooser = ref(false)
const selectedMode = ref<'online' | 'local' | 'git'>('online')
const chatCollapsed = ref(false)
const creatingSkill = ref(false)
const templateDatasetId = ref('product-mock')
const lastTemplateDatasetId = ref('product-mock')
const templateMenuOpen = ref(false)
const datasetDetailsOpen = ref(false)
const showTemplateChooser = ref(false)
const businessDataset = ref<any | null>(null)
const customTemplates = ref<any[]>(loadCustomTemplates())
const hiddenTemplateIds = ref<string[]>(loadHiddenTemplateIds())
const availableTemplates = computed<any[]>(() => {
  const merged = new Map(TEST_DATASETS.filter(template => !hiddenTemplateIds.value.includes(template.id)).map(template => [template.id, template]))
  customTemplates.value.forEach(template => merged.set(template.id, template))
  return Array.from(merged.values())
})
const selectedTemplate = computed<any>(() => availableTemplates.value.find(template => template.id === templateDatasetId.value) || getTestDataset(templateDatasetId.value))
const selectedDataset = computed<any>(() => businessDataset.value || selectedTemplate.value)
const datasetTestCases = computed<any[]>(() => businessDataset.value ? getDatasetTestCases(businessDataset.value) : [])
const selectedTestCaseId = ref('')
// 后端以该修订号拒绝覆盖其他成员刚保存的草稿；批量初始化文件不携带它，避免彼此冲突。
const draftRevision = ref<number | undefined>(undefined)
const selectedTestCase = computed<any>(() => datasetTestCases.value.find(item => item.id === selectedTestCaseId.value) || datasetTestCases.value[0])
const datasetInputText = computed(() => JSON.stringify(selectedTestCase.value?.input || {}, null, 2))
const datasetExpectedText = computed(() => JSON.stringify(selectedTestCase.value?.expected || {}, null, 2))
// 调试数据与"生成约束模板"分开：模板可直接用于快速验证；保存后的业务数据才是正式验收数据。
const debugDataSource = ref<'template-sample' | 'template-all' | 'business'>('template-sample')
const selectedDebugCaseId = ref('')
const debugDataset = computed<any>(() => debugDataSource.value === 'business' && businessDataset.value ? businessDataset.value : selectedTemplate.value)
const debugTestCases = computed<any[]>(() => getDatasetTestCases(debugDataset.value))
const selectedDebugCase = computed<any>(() => debugTestCases.value.find(item => item.id === selectedDebugCaseId.value) || debugTestCases.value[0])
const debugDataSourceLabel = computed(() => {
  if (debugDataSource.value === 'business' && businessDataset.value) return `已上传业务测试数据"${businessDataset.value.name}"`
  if (debugDataSource.value === 'template-all') return `模板"${selectedTemplate.value.label}"的完整用例`
  return `模板"${selectedTemplate.value.label}"`
})
const testDataEditorVisible = ref(false)
const templatePreviewVisible = ref(false)
const templateManagerVisible = ref(false)
const templateEditorText = ref('')
const templateEditorName = ref('')
const testDataEditorText = ref('')
const testDataSaving = ref(false)

/* ==================== 调试 ==================== */
const debugInput = ref('{"product_code": "000001"}')
const debugResult = ref('等待运行。')
const debugHistory = ref<{ time: string; input: string; ok: boolean; tokens?: number; detail?: string }[]>([])
const expandedLogs = reactive<Record<string, boolean>>({ latest: true })
const operationLogs = ref<any>({ records: [], total: 0, current: 1, pages: 0 })
const skillVersions = ref<any[]>([])
const versionsLoading = ref(false)
const diffVersion1 = ref('')
const diffVersion2 = ref('')
const diffResult = ref<any>({ files: [] })
const diffLoading = ref(false)
const showDiffView = ref(false)
const debugPassed = ref(false)
const debugRunning = ref(false)
const lastDebugMeta = ref<{ status: string; durationMs: number; dependencyCalls: number; exitCode?: number | null; totalTokens?: number } | null>(null)
const debugFailureLog = ref('')
const lastModelTokens = ref({ prompt: 0, completion: 0, total: 0 })
const tokenUsageSummary = computed(() => lastModelTokens.value.total
  ? `最近一次模型调用：输入 ${lastModelTokens.value.prompt} / 输出 ${lastModelTokens.value.completion} / 合计 ${lastModelTokens.value.total} Token`
  : '当前调试为本地脚本执行，未调用模型，消耗 0 Token。模型生成完成后会在此显示实际消耗。')

// 切换到日志页时加载操作日志
watch(midTab, (tab) => {
  if (tab === 'logs') loadOperationLogs(1)
})

// 模板完整用例展示完整模板 JSON；单条调试才装载某个案例的 input。
watch([debugDataSource, selectedDebugCaseId, businessDataset, templateDatasetId], () => {
  if (debugDataSource.value === 'business' && !businessDataset.value) debugDataSource.value = 'template-sample'
  if (!selectedDebugCaseId.value || !debugTestCases.value.some(item => item.id === selectedDebugCaseId.value)) {
    selectedDebugCaseId.value = debugTestCases.value[0]?.id || ''
  }
  const input = debugDataSource.value === 'template-sample'
    ? selectedTemplate.value.input
    : debugDataSource.value === 'template-all'
      ? selectedTemplate.value
      : selectedDebugCase.value?.input
  debugInput.value = JSON.stringify(input || {}, null, 2)
  debugResult.value = debugDataSource.value === 'template-all'
    ? `已载入${debugDataSourceLabel.value}（含 ${debugTestCases.value.length} 条 testCases），等待运行全部用例。`
    : `已载入${debugDataSourceLabel.value}${debugDataSource.value === 'template-sample' ? '的示例输入' : ` · ${selectedDebugCase.value?.name || '默认样例'}`}，等待运行。`
  debugFailureLog.value = ''
  lastDebugMeta.value = null
  debugPassed.value = false
  recentLog.value = `已将${debugDataSourceLabel.value}同步到调试输入。`
}, { immediate: true })

interface PendingChange {
  path: string
  before: string
  after: string
  added: number
  removed: number
}
const pendingChanges = ref<PendingChange[]>([])
const pendingBaseline = ref<Record<string, string>>({})
const applyingChanges = ref(false)
const showDiffDialog = ref(false)

/* ==================== AI 对话 ==================== */
interface ChatMsg {
  role: 'user' | 'assistant'
  content: string
  thinking?: string
  thinkingOpen?: boolean
  thinkingStream?: boolean
  streaming?: boolean
}
const chatMessages = ref<ChatMsg[]>([])
const chatInput = ref('')
const chatStreaming = ref(false)
const chatAbortController = ref<AbortController | null>(null)
const codeStreamActive = ref(false)
const chatBodyRef = ref<HTMLElement | null>(null)
const chatInputRef = ref<HTMLElement | null>(null)
const initialSuggestions = [
  { text: '生成一个产品信息查询 Skill，输入产品编码返回产品名称、状态和风险等级', datasetId: 'product-mock', templateLabel: '产品查询 Mock 数据', caseLabel: '正常查询、未知编码、参数缺失等 4 条用例' },
  { text: '生成一个文本清洗 Skill，支持去除多余空格、提取关键词和格式校验', datasetId: 'text-processor', templateLabel: '文本处理数据', caseLabel: '清洗、提取、空文本、非法动作等 4 条用例' },
  { text: '生成一个敏感信息脱敏 Skill，对手机号进行掩码处理', datasetId: 'privacy-mask', templateLabel: '敏感信息脱敏数据', caseLabel: '正常脱敏、空手机号、非法格式等 3 条用例' },
]
const progressiveSuggestions = computed(() => {
  const userTurns = chatMessages.value.filter(message => message.role === 'user').length
  const latestRequest = [...chatMessages.value].reverse().find(message => message.role === 'user')?.content || '当前 Skill'
  const dataset = selectedTemplate.value
  const context = { datasetId: templateDatasetId.value, templateLabel: dataset.label, caseLabel: dataset.testCases?.[0]?.name || '业务测试样例' }
  if (userTurns <= 1) return [
    { text: `基于"${latestRequest}"生成的文件，补充输入参数校验逻辑，确保空输入和非法格式返回正确的错误提示。`, ...context },
    { text: '为当前 Skill 补充边界测试用例（空输入、非法参数、异常场景），并更新 references/implementation-notes.md。', ...context },
  ]
  if (userTurns === 2) return [
    { text: '检查当前 Skill 的错误处理是否完整，补充缺失的异常分支和风险提示。', ...context },
    { text: '运行全部测试用例；如有失败，依据失败日志修复 scripts/main.py 并说明修改原因。', ...context },
  ]
  return [
    { text: '复核当前 Skill 的输入输出契约，确保所有测试用例通过，准备提交评审。', ...context },
    { text: '优化 Mock 数据，使其更贴近真实业务场景，并补充 references 中的业务规则说明。', ...context },
  ]
})
const recommendationNotice = ref('')

/* ==================== 编辑器 ==================== */
const dialogCode = ref(false)
const dialogCreate = ref(false)
const dialogLocalImport = ref(false)
const dialogGitImport = ref(false)
const dialogCredentials = ref(false)
const credentialEditing = ref(false)
const editorEl = ref<HTMLElement | null>(null)
const editingFile = ref('scripts/main.py')
let editor: EditorView | null = null

const newSkill = reactive({
  name: '',
  description: '',
  language: 'python',
  visibility: 'private' as 'private' | 'team',
  credentialId: '',
  creationMode: 'model' as 'model' | 'template' | 'custom',
  model: 'Think-Medium-Flash（Qwen3.6-35B-A3B）',
  skillTemplate: 'prompt',
  version: '0.0.0',
  agentName: '',
  agentVersion: '',
  dataSource: 'mock',
})
const localImport = reactive({ alias: '', visibility: 'private' as 'private' | 'team', credentialId: '', message: '', fileName: '', files: {} as Record<string, string>, valid: false, validation: '' })
const gitImport = reactive({ credentialId: '', loaded: false, refType: 'branch' as 'branch' | 'tag', ref: '', selected: [] as string[] })
const gitCredentials = ref<EnterpriseGitCredential[]>([])
const credentialForm = reactive({ id: '', name: '', repoUrl: '', username: '', token: '' })
const testingCredentialId = ref('')
const savingCredential = ref(false)
const credentialNotice = ref<{ ok: boolean; message: string } | null>(null)
const gitSkillCandidates = ref<EnterpriseGitSkill[]>([])
const gitRefsResult = ref<EnterpriseGitRefs>({ branches: [], tags: [] })
const gitRefs = computed(() => gitImport.refType === 'branch' ? gitRefsResult.value.branches : gitRefsResult.value.tags)

/* ==================== computed ==================== */
const fileContent = computed(() => {
  if (!currentSkill.value) return ''
  const af = activeFile.value
  if (af === 'SKILL.md') {
    return skillsMd.value || skillFiles.value['SKILL.md'] || `# ${currentSkill.value.name}\n\n## 简介\n暂无描述\n`
  }
  if (skillFiles.value[af] !== undefined) {
    return skillFiles.value[af]
  }
  if (af === 'scripts/main.py' || af === currentSkill.value.entry_file) {
    return code.value || ''
  }
  return ''
})

/* ==================== 工作台：文件列表 / 行号 / 高亮 / 状态 ==================== */
const wbCodeAreaRef = ref<HTMLElement | null>(null)

const fileList = computed(() => {
  if (!currentSkill.value) return []
  return Object.keys(skillFiles.value)
    .filter(path => !isPlaceholderFile(path))
    .sort()
    .map(path => ({
      path,
      label: path,
      icon: path === 'SKILL.md' ? '📋' : path === 'requirements.txt' ? '📦' : path.endsWith('.py') ? '🐍' : '📄',
      empty: !skillFiles.value[path],
      streaming: chatStreaming.value && codeStreamActive.value && activeFile.value === path,
    }))
})

function isPlaceholderFile(path: string) {
  return path.split('/').pop() === '.gitkeep'
}

const expandedFolders = reactive<Record<string, boolean>>({ references: false, scripts: false })
const rootFiles = computed(() => fileList.value.filter(file => !file.path.includes('/')))
const folders = computed(() => ['references', 'scripts'].map(name => ({
  name,
  files: fileList.value.filter(file => file.path.startsWith(`${name}/`)),
})).filter(folder => folder.files.length > 0))

const lineCount = computed(() => {
  const c = fileContent.value
  if (!c) return 1
  return c.split('\n').length
})

/** 简易语法高亮 */
const highlightedCode = computed(() => {
  let html = fileContent.value || ' '
  // 转义
  html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const isPy = activeFile.value.endsWith('.py')
  const isMd = activeFile.value.endsWith('.md')
  if (isPy) {
    // 字符串
    html = html.replace(/("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')/g, '<span class="tk-str">$1</span>')
    // 注释
    html = html.replace(/(#[^\n]*)/g, '<span class="tk-com">$1</span>')
    // 关键字
    html = html.replace(/\b(def|class|import|from|return|if|elif|else|for|while|try|except|finally|with|as|async|await|yield|lambda|None|True|False|and|or|not|in|is|raise|pass|break|continue|global|nonlocal|assert|del)\b/g, '<span class="tk-kw">$1</span>')
    // 函数调用
    html = html.replace(/\b([a-zA-Z_]\w*)(?=\s*\()/g, '<span class="tk-fn">$1</span>')
    // 数字
    html = html.replace(/\b(\d+\.?\d*)\b/g, '<span class="tk-num">$1</span>')
  } else if (isMd) {
    // markdown 标题
    html = html.replace(/^(#{1,4}\s.+)$/gm, '<span class="tk-h">$1</span>')
    // YAML frontmatter
    html = html.replace(/^(---[\s\S]*?---)$/gm, '<span class="tk-com">$1</span>')
  }
  return html
})

const fileState = computed(() => {
  if (!currentSkill.value) return 'empty'
  if (chatStreaming.value && codeStreamActive.value && activeFile.value === activeFile.value) return 'streaming'
  if (!fileContent.value) return 'empty'
  return 'saved'
})

const fileStateLabel = computed(() => {
  const s = fileState.value
  if (s === 'streaming') return '● 生成中'
  if (s === 'empty') return '空文件'
  return '已保存'
})

function copyCode() {
  navigator.clipboard.writeText(fileContent.value).then(() => {
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已复制 ${activeFile.value} 到剪贴板`
  })
}

function statusLabel(s?: string) {
  const map: Record<string, string> = { draft: '草稿', testing: '测试中', released: '已发布', published: '已发布' }
  return map[s || ''] || s || '未知'
}

/* ==================== 简易 Markdown 渲染 ==================== */
function renderMarkdown(text: string): string {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  // 代码块
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="md-code">$2</pre>')
  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code class="md-inline">$1</code>')
  // 加粗
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // 标题
  html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^# (.+)$/gm, '<h3>$1</h3>')
  // 换行
  html = html.replace(/\n/g, '<br>')
  return html
}

/* ==================== 生命周期 ==================== */
onMounted(async () => { await load(); await loadGitCredentials() })

onBeforeUnmount(() => {
  if (editor) { editor.destroy(); editor = null }
})

/* ==================== 数据加载 ==================== */
async function load() {
  try {
    const requestedMode = String(route.query.mode || '')
    if (requestedMode === 'git' || requestedMode === 'local') activeDevelopmentMode.value = requestedMode
    skills.value = await skillApi.list()
    const requestedSkillId = String(route.query.skillId || '')
    const creatingNew = route.query.new === '1'
    if (skills.value.length && !currentSkillId.value && !creatingNew) {
      currentSkillId.value = skills.value.some(item => item.id === requestedSkillId)
        ? requestedSkillId
        : skills.value[0].id
      await loadCode()
    }
  } catch (e: any) {
    recentLog.value = '加载失败：' + e.message
  }
}

async function onSwitchSkill() {
  if (currentSkillId.value) {
    debugPassed.value = false
    activeFile.value = currentSkill.value?.entry_file || 'scripts/main.py'
    await loadCode()
  }
}

function selectDevelopmentMode(mode: 'online' | 'local' | 'git') {
  if (chatStreaming.value) {
    recentLog.value = 'AI 正在生成文件，请完成后再切换开发模式。'
    return
  }
  activeDevelopmentMode.value = mode
  dialogModeChooser.value = false
  if (mode === 'online') dialogCreate.value = true
  if (mode === 'local') dialogLocalImport.value = true
  if (mode === 'git') dialogGitImport.value = true
  recentLog.value = `已切换到${DEVELOPMENT_MODES[mode].label}模式`
}

async function openDevelopmentModeChooser() {
  if (chatStreaming.value) { recentLog.value = 'AI 正在生成文件，请稍后新建。'; return }
  newSkill.agentName = currentAgentName.value || agentOptions.value[0]?.name || ''
  newSkill.agentVersion = ''
  selectedMode.value = 'online'
  // 尝试从 API 加载真实智能体列表，合并到预设列表
  try {
    const agents = await agentApi.list()
    // 添加 API 中有的但预设中没有的智能体
    for (const a of agents) {
      if (!agentOptions.value.find(o => o.name === a.name)) {
        agentOptions.value.push({ id: a.id, name: a.name, currentVersion: a.currentVersion || (a as any).current_version })
      }
    }
    // 如果当前有选中智能体，加载其版本
    if (newSkill.agentName) {
      await loadAgentVersions(newSkill.agentName)
    }
  } catch (e: any) {
    console.error('加载智能体列表失败:', e)
  }
  dialogModeChooser.value = true
}

async function onAgentNameChange() {
  newSkill.agentVersion = ''
  agentVersions.value = []
  if (newSkill.agentName) {
    await loadAgentVersions(newSkill.agentName)
  }
}

async function loadAgentVersions(agentName: string) {
  agentVersionsLoading.value = true
  agentVersions.value = []
  try {
    // 根据名称找到智能体 ID
    const agent = agentOptions.value.find(a => a.name === agentName)
    if (agent && agent.id) {
      if (agent.id < 1000) {
        // 真实 API 智能体（ID < 1000），从 API 加载版本
        const versions = await agentApi.listVersions(String(agent.id))
        agentVersions.value = versions || []
      } else {
        // 预设智能体（ID >= 1000），使用预设版本
        agentVersions.value = [
          { version: '1.0.0', changeSummary: '初始版本' },
          { version: '1.1.0', changeSummary: '功能优化' },
          { version: '1.2.0', changeSummary: '性能提升' },
        ]
      }
    }
    if (agentVersions.value.length) {
      newSkill.agentVersion = agentVersions.value[0].version
    }
  } catch (e: any) {
    console.error('加载智能体版本失败:', e)
  } finally {
    agentVersionsLoading.value = false
  }
}

function previewTemplate(templateId: string) {
  selectTemplate(templateId)
  templatePreviewVisible.value = true
}

async function selectLocalZip(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const zip = await JSZip.loadAsync(file)
    const files: Record<string, string> = {}
    await Promise.all(Object.values(zip.files).filter(item => !item.dir).map(async item => { files[item.name.replace(/^.*?\//, '')] = await item.async('string') }))
    const skillMdPath = Object.keys(files).find(path => path === 'SKILL.md')
    const skillMd = skillMdPath ? files[skillMdPath] : ''
    const version = skillMd.match(/当前版本：\s*([\d.]+)/)?.[1] || skillMd.match(/^version:\s*['\"]?([^\s'\"]+)/m)?.[1] || skillMd.match(/\bversion:\s*([\d.]+)/i)?.[1]
    localImport.fileName = file.name
    localImport.files = files
    if (!skillMdPath) {
      localImport.valid = false; localImport.validation = '导入失败：ZIP 包根目录必须包含 SKILL.md。'; return
    }
    if (version && !/^\d+\.\d+\.\d+$/.test(version)) {
      localImport.valid = false; localImport.validation = `导入失败：版本号 ${version} 不符合 x.y.z 格式。`; return
    }
    localImport.valid = true
    localImport.validation = `校验通过：发现 ${Object.keys(files).length} 个文件${version ? `，版本 ${version}` : '；未声明版本，将以 0.0.0 导入'}。`
  } catch (error: any) {
    localImport.valid = false; localImport.validation = `无法读取 ZIP：${error.message}`
  } finally {
    input.value = ''
  }
}

async function importLocalSkill() {
  if (!localImport.valid) return
  try {
    const skillMd = localImport.files['SKILL.md'] || ''
    const packageName = skillMd.match(/^#\s+([^#\n]+?)\s*$/m)?.[1]?.trim()
      || skillMd.match(/^name:\s*['\"]?([^\n'\"]+)/m)?.[1]?.trim()
      || localImport.alias || localImport.fileName.replace(/\.zip$/i, '')
    const skill = await skillApi.create({ name: packageName.toLowerCase().replace(/[^a-z0-9-]/g, '-') || 'local-skill', description: localImport.message || `从本地 ZIP 导入：${localImport.fileName}`, language: 'python', entry_file: 'scripts/main.py', visibility: localImport.visibility } as any)
    const files = Object.entries(localImport.files).filter(([path]) => path === 'SKILL.md' || path === 'requirements.txt' || path.startsWith('scripts/') || path.startsWith('references/') || path.startsWith('assets/'))
    await saveFilesSequentially(skill.id, files)
    await load(); currentSkillId.value = skill.id; await loadCode(); activeFile.value = 'SKILL.md'; dialogLocalImport.value = false
    recentLog.value = `已导入本地 Skill：${packageName}`
  } catch (error: any) { recentLog.value = `本地导入失败：${error.message}` }
}

async function loadGitSkills() {
  if (!gitImport.credentialId) { recentLog.value = '请先选择已配置的企业 Git 连接。'; return }
  try {
    gitRefsResult.value = await enterpriseGitApi.refs(gitImport.credentialId)
    gitImport.loaded = true; gitImport.selected = []; gitSkillCandidates.value = []
    gitImport.refType = gitRefsResult.value.branches.length ? 'branch' : 'tag'
    gitImport.ref = gitRefs.value[0] || ''
    if (!gitImport.ref) { recentLog.value = '仓库中未读取到可导入的分支或 Tag。'; return }
    await loadGitSkillCandidates()
  } catch (error: any) { recentLog.value = `读取企业 Git 仓库失败：${error.message}` }
}

async function onGitRefTypeChange() {
  gitImport.ref = gitRefs.value[0] || ''; gitImport.selected = []; gitSkillCandidates.value = []
  if (gitImport.ref) await loadGitSkillCandidates()
}

async function loadGitSkillCandidates() {
  if (!gitImport.credentialId || !gitImport.ref) return
  try {
    gitImport.selected = []
    gitSkillCandidates.value = await enterpriseGitApi.loadSkills(gitImport.credentialId, gitImport.ref)
    recentLog.value = `已从企业 Git 的 ${gitImport.ref} 读取 ${gitSkillCandidates.value.length} 个 Skill。`
  } catch (error: any) { recentLog.value = `读取 Skill 列表失败：${error.message}` }
}

function toggleAllGitSkills(event: Event) { gitImport.selected = (event.target as HTMLInputElement).checked ? gitSkillCandidates.value.map(item => item.path) : [] }

async function importGitSkill() {
  try {
    const credential = gitCredentials.value.find(item => item.id === gitImport.credentialId)
    if (!credential || !gitImport.selected.length) return
    let lastSkill: Skill | null = null
    for (const path of gitImport.selected) {
      const candidate = gitSkillCandidates.value.find(item => item.path === path)!
      const skill = await skillApi.create({ name: candidate.name, description: candidate.description, language: 'python', entry_file: 'scripts/main.py', code_path: `${credential.repoUrl}#${gitImport.ref}:${candidate.path}`, visibility: 'private', version: candidate.version } as any)
      await saveFilesSequentially(skill.id, Object.entries(candidate.files))
      lastSkill = skill
    }
    await load(); currentSkillId.value = lastSkill?.id || ''; await loadCode(); dialogGitImport.value = false
    recentLog.value = `已从 ${credential.name} 的 ${gitImport.ref} 导入 ${gitImport.selected.length} 个 Skill。`
  } catch (error: any) {
    recentLog.value = `Git 导入失败：${error.message}`
  }
}

async function loadGitCredentials() {
  try { gitCredentials.value = await enterpriseGitApi.list() }
  catch (error: any) { recentLog.value = `企业 Git 配置加载失败：${error.message}` }
}
function openCredentialManager() { dialogCredentials.value = true; loadGitCredentials() }
function newCredential() { credentialNotice.value = null; credentialEditing.value = true; Object.assign(credentialForm, { id: '', name: '', repoUrl: '', username: '', token: '' }) }
function editCredential(item: EnterpriseGitCredential) { credentialNotice.value = null; credentialEditing.value = true; Object.assign(credentialForm, { id: item.id, name: item.name, repoUrl: item.repoUrl, username: item.username, token: '' }) }
async function saveCredential() {
  credentialNotice.value = null
  if (!credentialForm.name.trim() || !/^(https?:\/\/|ssh:\/\/)/.test(credentialForm.repoUrl.trim()) || (!credentialForm.id && !credentialForm.token.trim())) {
    const message = '请填写连接名称、企业内网仓库地址（HTTP(S) 或 SSH）和访问令牌。'
    credentialNotice.value = { ok: false, message }; recentLog.value = message; return
  }
  savingCredential.value = true
  try {
    const item = await enterpriseGitApi.save({ ...credentialForm })
    await loadGitCredentials(); credentialEditing.value = false
    const message = `已由后端安全保存企业 Git 连接：${item.name}`
    credentialNotice.value = { ok: true, message }; recentLog.value = message
  } catch (error: any) {
    const message = `保存企业 Git 连接失败：${error.message || '未知错误'}`
    credentialNotice.value = { ok: false, message }; recentLog.value = message
  } finally { savingCredential.value = false }
}
async function deleteCredential(id: string) { if (!confirm('确认删除该企业 Git 连接？')) return; try { await enterpriseGitApi.delete(id); await loadGitCredentials(); recentLog.value = '企业 Git 连接已删除。' } catch (error: any) { recentLog.value = `删除失败：${error.message}` } }
async function testCredential(item: EnterpriseGitCredential) {
  testingCredentialId.value = item.id
  credentialNotice.value = null
  recentLog.value = `正在通过后端测试 ${item.name} 的企业内网连接…`
  try {
    const result = await enterpriseGitApi.test(item.id)
    await loadGitCredentials()
    const message = `${item.name}：${result.connectionStatus || '连接成功'}`
    credentialNotice.value = { ok: true, message }
    recentLog.value = message
  } catch (error: any) {
    const message = `企业 Git 连接失败：${error.message || '未知错误'}`
    credentialNotice.value = { ok: false, message }
    recentLog.value = message
  } finally { testingCredentialId.value = '' }
}

/** 同一 Skill 的 Git 工作区必须串行保存；每步携带最新修订号，避免并发首提交。 */
async function saveFilesSequentially(skillId: string, entries: [string, any][]) {
  let revision: number | undefined
  for (const [path, value] of entries) {
    const result = await skillApi.saveCode(skillId, String(value), path, revision)
    revision = typeof result?.draftRevision === 'number' ? result.draftRevision : revision === undefined ? 1 : revision + 1
  }
  return revision
}

async function loadCode() {
  if (!currentSkillId.value) return
  try {
    const res: any = await skillApi.getCode(currentSkillId.value)
    draftRevision.value = typeof res.draftRevision === 'number' ? res.draftRevision : undefined
    code.value = res.code || ''
    skillsMd.value = res.skills_md || ''
    skillFiles.value = Object.fromEntries(Object.entries(res.files || {}).filter(([path]) => !isPlaceholderFile(path)).map(([path, value]) => [path, String(value)])) as Record<string, string>
    const savedTestData = skillFiles.value['references/test-data.json']
    if (savedTestData) {
      try {
        businessDataset.value = validateBusinessTestData(JSON.parse(savedTestData))
        selectedTestCaseId.value = getDatasetTestCases(businessDataset.value)[0]?.id || ''
      } catch {
        // 保留代码文件，让用户在编辑入口修复不符合格式的测试数据。
        businessDataset.value = null
      }
    } else {
      businessDataset.value = null
    }
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 加载 ${Object.keys(skillFiles.value).length} 个文件`
  } catch {
    code.value = ''
    skillsMd.value = ''
    skillFiles.value = {}
    recentLog.value = '该 Skill 还没有代码'
  }
}

function openTestDataEditor() {
  const source = businessDataset.value || buildBusinessTestData(selectedTemplate.value)
  testDataEditorText.value = JSON.stringify(source, null, 2)
  testDataEditorVisible.value = true
}

function loadCustomTemplates(): any[] {
  try {
    const saved = window.localStorage.getItem('skill-custom-test-templates')
    const data = saved ? JSON.parse(saved) : []
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

function loadHiddenTemplateIds(): string[] {
  try {
    const saved = window.localStorage.getItem('skill-hidden-test-template-ids')
    const data = saved ? JSON.parse(saved) : []
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

function normalizeTemplate(data: any) {
  if (!data || typeof data !== 'object') throw new Error('模板必须是 JSON 对象')
  const firstCase = Array.isArray(data.testCases) ? data.testCases[0] : null
  const id = String(data.id || `business-template-${Date.now()}`).trim()
  const label = String(data.label || data.name || '').trim()
  if (!label) throw new Error('模板缺少 label 或 name')
  const input = data.input || firstCase?.input
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new Error('模板需要提供 input，或至少一条 testCases.input')
  return {
    ...data,
    id,
    label,
    description: String(data.description || '业务自定义测试数据模板'),
    input,
    expected: data.expected || firstCase?.expected || {},
    testCases: Array.isArray(data.testCases) && data.testCases.length ? data.testCases : [{ id: `${id}-sample`, name: '业务初始样例', input, expected: data.expected || {} }],
  }
}

function openTemplateManager() {
  templateEditorText.value = JSON.stringify(selectedTemplate.value, null, 2)
  templateEditorName.value = selectedTemplate.value.label || selectedTemplate.value.name || ''
  templateManagerVisible.value = true
}

function openNewTemplateManager() {
  templateEditorName.value = ''
  templateEditorText.value = JSON.stringify({ id: `business-template-${Date.now()}`, name: '', description: '', input: {}, expected: {}, testCases: [] }, null, 2)
  templateMenuOpen.value = false
  templateManagerVisible.value = true
}

function selectTemplate(id: string) {
  templateDatasetId.value = id
  lastTemplateDatasetId.value = id
  templateMenuOpen.value = false
}

function updateTemplate(template: any) {
  templateDatasetId.value = template.id
  lastTemplateDatasetId.value = template.id
  templateMenuOpen.value = false
  openTemplateManager()
}

function isCustomTemplate(id: string) {
  return customTemplates.value.some(item => item.id === id)
}

function downloadTemplate(template: any) {
  const blob = new Blob([JSON.stringify(buildBusinessTestData(template), null, 2) + '\n'], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${template.label || template.id}.json`
  link.click()
  URL.revokeObjectURL(url)
}

function deleteTemplate(template: any) {
  if (!window.confirm(`确认删除模板"${template.label}"？`)) return
  const isCustom = customTemplates.value.some(item => item.id === template.id)
  if (isCustom) {
    customTemplates.value = customTemplates.value.filter(item => item.id !== template.id)
    window.localStorage.setItem('skill-custom-test-templates', JSON.stringify(customTemplates.value))
  } else {
    hiddenTemplateIds.value = [...new Set([...hiddenTemplateIds.value, template.id])]
    window.localStorage.setItem('skill-hidden-test-template-ids', JSON.stringify(hiddenTemplateIds.value))
  }
  if (templateDatasetId.value === template.id) {
    const next = availableTemplates.value.find(item => item.id !== template.id)
    templateDatasetId.value = next?.id || 'product-mock'
    lastTemplateDatasetId.value = templateDatasetId.value
  }
  templateMenuOpen.value = false
}

function onTemplateSelection() {
  if (templateDatasetId.value === '__manage_templates__') {
    templateDatasetId.value = lastTemplateDatasetId.value
    openTemplateManager()
    return
  }
  lastTemplateDatasetId.value = templateDatasetId.value
}

function saveTemplate() {
  try {
    const data = JSON.parse(templateEditorText.value)
    data.label = templateEditorName.value
    data.name = templateEditorName.value
    const template = normalizeTemplate(data)
    const index = customTemplates.value.findIndex(item => item.id === template.id)
    const next = [...customTemplates.value]
    if (index >= 0) next[index] = template
    else next.push(template)
    customTemplates.value = next
    window.localStorage.setItem('skill-custom-test-templates', JSON.stringify(next))
    hiddenTemplateIds.value = hiddenTemplateIds.value.filter(id => id !== template.id)
    window.localStorage.setItem('skill-hidden-test-template-ids', JSON.stringify(hiddenTemplateIds.value))
    templateDatasetId.value = template.id
    lastTemplateDatasetId.value = template.id
    templateManagerVisible.value = false
    recommendationNotice.value = `已保存业务模板"${template.label}"，后续生成可直接选择使用。`
  } catch (e: any) {
    alert(`模板格式错误：${e.message}`)
  }
}

function importTemplate(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const text = String(reader.result || '')
    templateEditorText.value = text
    try {
      const data = JSON.parse(text)
      templateEditorName.value = String(data.label || data.name || file.name.replace(/\.json$/i, '')).trim()
    } catch {
      templateEditorName.value = file.name.replace(/\.json$/i, '')
    }
  }
  reader.readAsText(file, 'utf-8')
}

async function saveBusinessTestData() {
  try {
    const data = validateBusinessTestData(JSON.parse(testDataEditorText.value))
    testDataSaving.value = true
    if (currentSkill.value) {
      const saved = await skillApi.saveCode(currentSkill.value.id, JSON.stringify(data, null, 2) + '\n', 'references/test-data.json', draftRevision.value)
      draftRevision.value = saved.draftRevision
      skillFiles.value = { ...skillFiles.value, 'references/test-data.json': JSON.stringify(data, null, 2) + '\n' }
    }
    businessDataset.value = data
    selectedTestCaseId.value = getDatasetTestCases(data)[0]?.id || ''
    testDataEditorVisible.value = false
    recentLog.value = currentSkill.value
      ? '调试测试数据已保存到 references/test-data.json，并已同步到调试区。'
      : '调试测试数据已在当前页面使用；创建 Skill 后可再次保存到 references/test-data.json。'
  } catch (e: any) {
    alert(`测试数据格式错误：${e.message}`)
  } finally {
    testDataSaving.value = false
  }
}

function createBusinessFromTemplate() {
  // 模板只用于预填充编辑草稿；只有点击"保存并使用"才成为当前 Skill 的正式测试数据。
  testDataEditorText.value = JSON.stringify(buildBusinessTestData(selectedTemplate.value), null, 2)
  testDataEditorVisible.value = true
}

/* ==================== AI 对话 ==================== */
function clearChat() {
  chatMessages.value = []
}

function useSuggestion(suggestion: typeof initialSuggestions[number]) {
  chatInput.value = suggestion.text
  templateDatasetId.value = suggestion.datasetId
  lastTemplateDatasetId.value = suggestion.datasetId
  debugDataSource.value = 'template-sample'
  selectedDebugCaseId.value = getDatasetTestCases(getTestDataset(suggestion.datasetId))[0]?.id || ''
  recommendationNotice.value = `已选择"${suggestion.templateLabel}"：它会约束本轮生成，并提供"${suggestion.caseLabel}"作为调试示例。业务测试数据需由业务人员另行保存后再用于正式验收。`
  chatInputRef.value?.focus()
}

function scrollChatToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function stopChatGeneration() {
  if (!chatStreaming.value) return
  chatAbortController.value?.abort()
  recentLog.value = '已终止本轮 AI 生成。'
}

async function sendChat() {
  const text = chatInput.value.trim()
  if (pendingChanges.value.length) {
    recentLog.value = '请先应用或撤销上一轮 AI 变更，再继续对话。'
    return
  }
  if (!text || chatStreaming.value) return

  // 如果没有选中 Skill，自动创建
  if (!currentSkill.value) {
    try {
      const skillName = `skill-${Date.now().toString(36)}`
      const skill = await skillApi.create({
        name: skillName,
        description: text,
        language: 'python',
        tags: [],
      } as any)
      await load()
      currentSkillId.value = skills.value[0]?.id
      await loadCode()
    } catch (e: any) {
      alert('创建 Skill 失败：' + e.message)
      return
    }
  }

  // 添加用户消息
  chatMessages.value.push({ role: 'user', content: text })
  chatInput.value = ''
  scrollChatToBottom()

  // 添加 assistant 消息占位
  const assistantMsg: ChatMsg = reactive({
    role: 'assistant',
    content: '',
    thinking: '',
    thinkingOpen: false,
    thinkingStream: true,
    streaming: true,
  })
  chatMessages.value.push(assistantMsg)
  scrollChatToBottom()

  chatStreaming.value = true
  codeStreamActive.value = false
  codeStreamSwitched = false
  lastFileContents = {}
  pendingBaseline.value = snapshotCurrentFiles()

  // 构建 SSE 请求
  const abortController = new AbortController()
  chatAbortController.value = abortController
  try {
    const token = localStorage.getItem('token')
    const skillContext = buildSkillContext()
    const resp = await fetch('/race-api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      signal: abortController.signal,
      body: JSON.stringify({
        messages: [
          { role: 'system', content: skillContext },
          ...chatMessages.value
            .filter(m => m !== assistantMsg)
            .slice(-8)
            .map(m => ({ role: m.role, content: m.content.slice(-6000) })),
        ],
      }),
    })

    if (!resp.ok) {
      if (resp.status === 401) {
        if (token === 'local-demo-token') {
          assistantMsg.content = '当前为本地演示登录，未连接到后端服务，无法调用模型。请启动后端后退出并重新登录。'
          assistantMsg.streaming = false
          chatStreaming.value = false
          return
        }
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        await router.push('/login')
        return
      }
      assistantMsg.content = `请求失败: ${resp.status} ${resp.statusText}`
      assistantMsg.streaming = false
      chatStreaming.value = false
      return
    }

    // 配置或校验异常由后端以 JSON 返回；不能把它当作 SSE 静默吞掉。
    const contentType = resp.headers.get('content-type') || ''
    if (!contentType.includes('text/event-stream')) {
      let message = '模型服务未返回流式响应。'
      try {
        const payload = await resp.json()
        message = payload?.message || payload?.error || message
      } catch {
        const text = await resp.text()
        if (text) message = text
      }
      assistantMsg.content = `模型调用未开始：${message}`
      assistantMsg.streaming = false
      assistantMsg.thinkingStream = false
      recentLog.value = `AI 生成失败：${message}`
      return
    }

    const reader = resp.body?.getReader()
    if (!reader) {
      assistantMsg.content = '无法读取响应流'
      assistantMsg.streaming = false
      chatStreaming.value = false
      return
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // 解析 SSE 行
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        try {
          const chunk = JSON.parse(line.slice(5).trim())
          if (chunk.type === 'thinking') {
            assistantMsg.thinking = (assistantMsg.thinking || '') + chunk.content
            // 自动展开思考过程（首次出现时）
            if (!assistantMsg.thinkingOpen && assistantMsg.thinking.length < 200) {
              assistantMsg.thinkingOpen = true
            }
          } else if (chunk.type === 'content') {
            assistantMsg.content += chunk.content
            // 实时提取代码块并更新中栏文件
            streamCodeToFiles(assistantMsg.content)
          } else if (chunk.type === 'done') {
            assistantMsg.thinkingStream = false
          } else if (chunk.type === 'usage') {
            lastModelTokens.value = {
              prompt: Number(chunk.promptTokens || chunk.prompt_tokens || 0),
              completion: Number(chunk.completionTokens || chunk.completion_tokens || 0),
              total: Number(chunk.totalTokens || chunk.total_tokens || 0),
            }
          } else if (chunk.type === 'error') {
            assistantMsg.content += `\n\n[错误] ${chunk.content}`
          }
          scrollChatToBottom()
        } catch {
          // ignore parse errors
        }
      }
    }

    // 流结束后，收起思考过程（如果有内容）
    if (abortController.signal.aborted) return
    if (assistantMsg.content) {
      assistantMsg.thinkingOpen = false
    }
    assistantMsg.streaming = false
    // 最终提取并进入"待应用"状态，不直接覆盖草稿。
    await finalizeCodeToFiles(assistantMsg.content)
  } catch (e: any) {
    if (abortController.signal.aborted || e?.name === 'AbortError') {
      assistantMsg.thinkingStream = false
      assistantMsg.streaming = false
      assistantMsg.content += assistantMsg.content ? '\n\n已终止本轮生成。' : '已终止本轮生成。'
      return
    }
    assistantMsg.content = `网络错误: ${e.message}`
    assistantMsg.streaming = false
  } finally {
    if (chatAbortController.value === abortController) chatAbortController.value = null
    chatStreaming.value = false
  }
}

/**
 * 每轮都把当前文件快照交给模型，保证"继续修改"针对已生成的 Skill 进行增量变更。
 * 模型只需返回变更文件，未返回的文件由平台保留。
 */
function buildSkillContext() {
  const entryFile = currentSkill.value?.entry_file || 'scripts/main.py'
  const files = { ...skillFiles.value, [entryFile]: code.value }
  const snapshot = Object.entries(files)
    .filter(([path, content]) => path && content != null && !isPlaceholderFile(path))
    .slice(0, 20)
    .map(([path, content]) => `### ${path}\n\`\`\`\n${String(content).slice(0, 12000)}\n\`\`\``)
    .join('\n\n') || '（这是一个新 Skill，当前还没有文件。）'

  return `你是企业级智能体平台的 Skill 开发助手。当前 Skill 名称：${currentSkill.value?.name || '未命名 Skill'}。

## 你的任务
${snapshot === '（这是一个新 Skill，当前还没有文件。）' ? '创建一个新的 Skill。' : '基于当前文件快照进行最小必要改动，未要求修改的文件不要删除或重写。'}

## 输出格式
必须同时输出以下文件，缺一不可：

\`\`\`markdown file=SKILL.md
---
name: ${currentSkill.value?.name || 'skill-name'}
name_zh: Skill 中文名称
description: 100-150字符，说明能力价值和触发场景
version: ${currentSkill.value?.version || '0.0.0'}
tags: 业务领域,能力标签
runEnv: all
digestValue: pending
---

# Skill 标题

## 简介
简要描述 Skill 的能力。

## 输入
- 字段名：类型，是否必填，说明

## 输出
- 字段名：类型，说明

## 异常处理
- 错误场景与返回值
\`\`\`

\`\`\`python file=scripts/main.py
from scripts.validators import validate_input
from scripts.mock_data import build_mock_result


def handle(input_data: dict) -> dict:
    """Skill 入口函数。"""
    error = validate_input(input_data)
    if error:
        return {"error": error}
    return build_mock_result(input_data)
\`\`\`

\`\`\`python file=scripts/validators.py
def validate_input(input_data: dict) -> str:
    """校验输入参数，合法时返回空字符串，否则返回错误信息。"""
    if not isinstance(input_data, dict):
        return "input_data 必须是 JSON 对象"
    return ""
\`\`\`

\`\`\`python file=scripts/mock_data.py
# Mock 数据表，开发阶段使用，后续可替换为数据库 API
_MOCK_DATA = {}


def build_mock_result(input_data: dict) -> dict:
    """根据输入返回 Mock 数据。"""
    return dict(input_data)
\`\`\`

\`\`\`text file=requirements.txt
# 仅使用 Python 标准库时写明；如引入第三方库，逐行声明版本
\`\`\`

\`\`\`markdown file=references/implementation-notes.md
# 参考说明
- 输入字段、输出契约、Mock 数据来源与业务规则
\`\`\`

## 关键规则
1. **禁止自我导入**：\`validators.py\` 中不能 \`from scripts.validators import\`，\`mock_data.py\` 中不能 \`from scripts.mock_data import\`
2. **handle() 是纯函数**：不依赖外部状态，输入 dict 输出 dict
3. **错误返回格式**：\`{"error": "中文错误提示"}\`
4. **禁止生成**：独立 YAML 文件、README.md、空文件、绝对路径、外部 URL
5. **文件路径**：只能使用相对路径（SKILL.md、scripts/、references/）

## 验收契约
平台会自动用测试数据逐条验证：
1. 对每条 testCase，\`handle(testCase.input)\` 返回的 dict 必须包含 \`testCase.expected.contains\` 中的全部键值对
2. 输入直接就是 testCase.input 对象，不要再包一层 data
3. 错误场景返回 \`{"error": 与预期一致的中文提示}\`

## 测试数据模板
名称：${selectedTemplate.value.label}
说明：${selectedTemplate.value.description}
示例输入：${JSON.stringify(selectedTemplate.value.input, null, 2)}
示例输出：${JSON.stringify(selectedTemplate.value.expected, null, 2)}
完整用例：${JSON.stringify(buildBusinessTestData(selectedTemplate.value), null, 2).slice(0, 8000)}

## 当前文件快照
${snapshot}`
}

/** 流式过程中实时提取代码块并更新中栏文件视图 */
let codeStreamSwitched = false
let lastFileContents: Record<string, string> = {}
function streamCodeToFiles(content: string) {
  if (!currentSkill.value) return

  let updated = false
  let activeStreamingFile = ''

  const generatedFiles = extractGeneratedFiles(content, true)
  for (const [path, fileContent] of Object.entries(generatedFiles)) {
    if (lastFileContents[path] === fileContent) continue
    lastFileContents[path] = fileContent
    skillFiles.value = { ...skillFiles.value, [path]: fileContent }
    if (path === currentSkill.value.entry_file || path === 'scripts/main.py') {
      code.value = fileContent
    }
    if (path === 'SKILL.md') skillsMd.value = fileContent
    activeStreamingFile = path
    updated = true
  }

  // 新协议已提供带文件路径的代码块，不再执行旧格式推断。
  if (Object.keys(generatedFiles).length > 0) {
    if (updated) updateStreamingView(activeStreamingFile)
    return
  }

  // 提取 Python 代码块（可能未闭合——流式进行中）
  const pyMatch = content.match(/```python\n([\s\S]*?)(?:```|$)/)
  if (pyMatch) {
    const partialCode = pyMatch[1]
    // 只有当内容确实变化时才更新
    if (lastFileContents['scripts/main.py'] !== partialCode) {
      lastFileContents['scripts/main.py'] = partialCode
      code.value = partialCode
      skillFiles.value = { ...skillFiles.value, 'scripts/main.py': partialCode }
      activeStreamingFile = 'scripts/main.py'
      updated = true
    }
  }

  // 仅允许纯 Markdown 代码块作为 SKILL.md，明确不接受 YAML。
  const mdMatch = content.match(/```(?:markdown|md)\n([\s\S]*?)(?:```|$)/)
  if (mdMatch) {
    const partialMd = mdMatch[1]
    if (lastFileContents['SKILL.md'] !== partialMd) {
      lastFileContents['SKILL.md'] = partialMd
      skillsMd.value = partialMd
      skillFiles.value = { ...skillFiles.value, 'SKILL.md': partialMd }
      activeStreamingFile = 'SKILL.md'
      updated = true
    }
  }

  // 提取 requirements.txt 内容
  const reqMatch = content.match(/```(?:text|txt)?\n(# .+requirements[\s\S]*?)(?:```|$)/i)
  if (reqMatch) {
    if (lastFileContents['requirements.txt'] !== reqMatch[1]) {
      lastFileContents['requirements.txt'] = reqMatch[1]
      skillFiles.value = { ...skillFiles.value, 'requirements.txt': reqMatch[1] }
      activeStreamingFile = 'requirements.txt'
      updated = true
    }
  }

  if (updated) {
    updateStreamingView(activeStreamingFile)
  }
}

function updateStreamingView(path: string) {
  codeStreamActive.value = true
  if (path) activeFile.value = path
  if (!codeStreamSwitched) {
    codeStreamSwitched = true
    midTab.value = 'files'
  }
  recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} AI 正在写入 ${path}…`
  nextTick(() => {
    if (wbCodeAreaRef.value) wbCodeAreaRef.value.scrollTop = wbCodeAreaRef.value.scrollHeight
  })
}

function extractGeneratedFiles(content: string, allowUnclosed = false): Record<string, string> {
  const files: Record<string, string> = {}
  const suffix = allowUnclosed ? '(?:```|$)' : '```'
  const block = new RegExp('```[^\\s\\n`]*\\s+(?:file|path)=([^\\s`]+)\\s*\\n([\\s\\S]*?)' + suffix, 'g')
  let match: RegExpExecArray | null
  while ((match = block.exec(content)) !== null) {
    const path = match[1].trim().replace(/^\.\//, '')
    if (path && !path.includes('..') && !path.includes(':') && !path.startsWith('/')) {
      files[path] = match[2].trim()
    }
  }
  // 兼容部分本地模型把语言与文件标记拆成两行的写法：
  // ```\npython file=scripts/main.py\n...\n```
  const deferredLanguageBlock = new RegExp('```\\s*\\n[^\\s\\n`]+\\s+(?:file|path)=([^\\s`]+)\\s*\\n([\\s\\S]*?)' + suffix, 'g')
  while ((match = deferredLanguageBlock.exec(content)) !== null) {
    const path = match[1].trim().replace(/^\.\//, '')
    if (path && !path.includes('..') && !path.includes(':') && !path.startsWith('/')) {
      files[path] = match[2].trim()
    }
  }
  // 兼容部分模型把文件标记放在代码块上一行的写法：
  // file=scripts/main.py\n```python\n...\n```
  const labelThenBlock = new RegExp('(?:^|\\n)(?:file|path)\\s*[:=]\\s*([^\\s`]+)\\s*\\n```[^\\n`]*\\n([\\s\\S]*?)' + suffix, 'g')
  while ((match = labelThenBlock.exec(content)) !== null) {
    const path = match[1].trim().replace(/^\.\//, '')
    if (path && !path.includes('..') && !path.includes(':') && !path.startsWith('/')) {
      files[path] = match[2].trim()
    }
  }
  // 兼容 `file=路径` 后直接给出源码、未再包裹 Markdown 代码块的输出。
  // 仅截取到下一个文件标记，避免将整段解释文本误写入同一个文件。
  const labelThenPlain = /(?:^|\n)(?:file|path)\s*[:=]\s*([^\s`]+)\s*\n(?!```)([\s\S]*?)(?=\n(?:file|path)\s*[:=]|$)/g
  while ((match = labelThenPlain.exec(content)) !== null) {
    const path = match[1].trim().replace(/^\.\//, '')
    const fileContent = match[2].trim()
    if (path && fileContent && !path.includes('..') && !path.includes(':') && !path.startsWith('/')) {
      files[path] = fileContent
    }
  }
  // 兼容本地模型直接以 `python file=scripts/main.py` 开头、未使用代码块的输出。
  // 文件内容截断于下一个文件标题、代码围栏或文件标记，防止解释文字进入脚本。
  const languageThenPlain = /(?:^|\n)(?:python|py|markdown|md|json|txt)\s+(?:file|path)=([^\s`]+)\s*\n([\s\S]*?)(?=\n(?:#{2,}\s+[^\n]+|(?:python|py|markdown|md|json|txt)\s+(?:file|path)=|```)|$)/gi
  while ((match = languageThenPlain.exec(content)) !== null) {
    const path = match[1].trim().replace(/^\.\//, '')
    const fileContent = match[2].trim()
    if (path && fileContent && !path.includes('..') && !path.includes(':') && !path.startsWith('/') && !path.includes('__pycache__')) {
      files[path] = fileContent
    }
  }
  // 兼容模型遗漏 file= 标记的常见输出，避免 Python 代码只在流式预览出现却未保存。
  if (!files['scripts/main.py']) {
    const pythonBlock = content.match(/```(?:python|py)\s*\n([\s\S]*?)(?:```|$)/i)
    if (pythonBlock?.[1]) files['scripts/main.py'] = pythonBlock[1].trim()
  }
  if (!files['SKILL.md']) {
    const markdownBlock = content.match(/```(?:markdown|md)\s*\n([\s\S]*?)(?:```|$)/i)
    if (markdownBlock?.[1]) files['SKILL.md'] = markdownBlock[1].trim()
  }
  for (const [path, fileContent] of Object.entries(files)) {
    files[path] = normalizeGeneratedFileContent(path, fileContent)
  }
  return files
}

/** 清除模型在 Python 代码块结束后附带的解释文本，避免将 Markdown 标记写入脚本。 */
function normalizeGeneratedFileContent(path: string, content: string) {
  let normalized = String(content || '').trim()
  if (!path.endsWith('.py')) return normalized
  const fenceIndex = normalized.indexOf('\n```')
  if (fenceIndex >= 0) normalized = normalized.slice(0, fenceIndex).trim()
  // 极少数模型会把 `python file=...` 作为代码内容的第一行。
  normalized = normalized.replace(/^(?:python|py)\s+(?:file|path)=\S+\s*\n/i, '')
  // 本地模型偶尔使用了 Any 类型却遗漏导入；这是可确定的机械性缺失，
  // 在落盘前补齐，避免入口模块加载时直接 NameError。
  if (/\bAny\b/.test(normalized) && !/from\s+typing\s+import[^\n]*\bAny\b/.test(normalized) && !/import\s+typing/.test(normalized)) {
    if (/from\s+typing\s+import\s+([^\n]+)/.test(normalized)) {
      normalized = normalized.replace(/from\s+typing\s+import\s+([^\n]+)/, (_all, imports) => {
        const items = String(imports).split(',').map((item: string) => item.trim()).filter(Boolean)
        return `from typing import ${['Any', ...items.filter((item: string) => item !== 'Any')].join(', ')}`
      })
    } else {
      normalized = `from typing import Any\n${normalized}`
    }
  }
  return normalized.trim()
}

/** 对模型输出执行《SKILL 开发规范》的可自动校验项，避免不合规文件落盘。 */
function validateGeneratedSkillFiles(files: Record<string, string>): string[] {
  const issues: string[] = []
  const paths = Object.keys(files)
  const hasOwn = (path: string) => Object.prototype.hasOwnProperty.call(files, path)
  const skillMd = files['SKILL.md'] || ''

  if (!hasOwn('SKILL.md')) issues.push('缺少 SKILL.md')
  if (!hasOwn('requirements.txt')) issues.push('缺少 requirements.txt')
  if (!paths.some(path => path.startsWith('scripts/'))) issues.push('缺少 scripts/ 下的可执行代码')
  if (!paths.some(path => path.startsWith('references/'))) issues.push('缺少 references/ 下的参考资料')
  if (!hasOwn('scripts/main.py')) issues.push('缺少调试入口 scripts/main.py')

  for (const [path, value] of Object.entries(files)) {
    if (!path || path.startsWith('/') || path.includes('..') || path.includes(':')) issues.push(`文件路径不合法：${path}`)
    if (path.includes('__pycache__') || /\.pyc$/i.test(path)) issues.push(`禁止生成 Python 缓存文件：${path}`)
    if (/^readme\.md$/i.test(path)) issues.push('禁止生成 README.md')
    if (/\.ya?ml$/i.test(path)) issues.push(`禁止生成独立 YAML 文件：${path}`)
    if (!(path === 'SKILL.md' || path === 'requirements.txt' || path.startsWith('scripts/') || path.startsWith('references/') || path.startsWith('assets/'))) {
      issues.push(`不在规范目录内：${path}`)
    }
    if (!String(value).trim()) issues.push(`禁止生成空文件：${path}`)
    if ((path.startsWith('references/') || path.startsWith('assets/')) && /https?:\/\//i.test(String(value))) {
      issues.push(`资源文件禁止引用外部 URL：${path}`)
    }
  }

  if (skillMd) {
    const frontmatter = skillMd.match(/^---\s*\n([\s\S]*?)\n---\s*(?:\n|$)/)
    if (!frontmatter) {
      issues.push('SKILL.md 缺少 YAML 前言区')
    } else {
      if (!/^name:\s*\S+/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少 name')
      if (!/^name_zh:\s*\S+/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少 name_zh')
      if (!/^description:\s*\S+/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少 description')
      if (!/^version:\s*\d+\.\d+\.\d+\s*$/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少合法 version（x.y.z）')
      if (!/^tags:\s*\S+/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少 tags')
      if (!/^runEnv:\s*\S+/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少 runEnv')
      if (!/^digestValue:\s*\S+/m.test(frontmatter[1])) issues.push('SKILL.md 前言区缺少 digestValue')
    }
    const body = frontmatter ? skillMd.slice(frontmatter[0].length).trim() : ''
    if (!body) issues.push('SKILL.md 缺少使用说明正文')
    if (skillMd.split('\n').length > 500) issues.push('SKILL.md 超过 500 行')
  }

  const main = files['scripts/main.py'] || ''
  if (main && !/def\s+handle\s*\(/.test(main)) issues.push('scripts/main.py 必须导出 handle(input_data) 函数')
  return [...new Set(issues)]
}

/**
 * 对 AI 生成的 Skill 文件做"可自动补齐"的兜底：补 SKILL.md 缺失的必填字段、
 * 补缺失的 requirements.txt、补缺 handle() 的 main.py，避免本地小模型因
 * 一次输出不完整导致整轮 AI 草稿无法保存。digestValue 保留占位 pending，
 * 后端在每次保存时刷新为基于工作区内容的真实 MD5。
 */
function autoFillSkillCompliance(files: Record<string, string>) {
  const skillName = currentSkill.value?.name || 'skill-name'
  const nameZh = currentSkill.value?.name ? `${currentSkill.value.name} 中文说明` : 'Skill 中文说明'
  const version = currentSkill.value?.version || '0.0.0'
  const generatedMain = files['scripts/main.py'] || ''

  // 本地模型有时只返回入口脚本。只要入口脚本有效，就由平台补齐固定目录中
  // 可确定的基础文件，确保一次生成后能够形成可保存、可继续编辑的完整 Skill。
  if (generatedMain && !files['SKILL.md']) {
    files['SKILL.md'] = `---
name: ${skillName}
name_zh: ${nameZh}
description: 基于业务请求生成的 ${skillName} Skill，用于完成结构化输入处理、业务结果返回和异常场景提示。
version: ${version}
tags: 通用,业务查询
runEnv: all
digestValue: pending
---

# ${skillName}

## 能力说明

根据输入参数执行对应的业务处理，并返回结构化结果；参数缺失或业务对象不存在时返回明确错误信息。

## 输入

入口函数为 \`scripts/main.py\` 中的 \`handle(input_data)\`。具体字段以业务测试数据和脚本校验规则为准。

## 输出与异常

成功时返回业务结果；输入不合法或未查询到业务对象时返回 \`error\` 字段。

## 调试

使用 \`references/test-data.json\` 中的测试用例进行调试，并根据实际业务规则补充数据和说明。
`
  }
  if (generatedMain && !files['references/implementation-notes.md']) {
    files['references/implementation-notes.md'] = `# 实现说明

## 数据契约

输入字段、输出字段与异常结果以 \`scripts/main.py\` 的实现及业务测试数据为准。

## 处理规则

1. 在入口处校验必要输入；
2. 业务处理返回结构化结果；
3. 参数不合法或业务对象不存在时返回 \`error\`；
4. 变更业务规则时，同步更新本文件和测试数据。
`
  }
  if (generatedMain && !files['references/test-data.json']) {
    files['references/test-data.json'] = JSON.stringify(buildBusinessTestData(selectedTemplate.value), null, 2) + '\n'
  }
  if (generatedMain && !files['scripts/validators.py']) {
    files['scripts/validators.py'] = `from typing import Any, Dict, Optional


def validate_input(input_data: Dict[str, Any]) -> Optional[str]:
    \"\"\"基础输入校验；请按实际业务字段补充规则。\"\"\"
    if not isinstance(input_data, dict):
        return \"输入必须是 JSON 对象\"
    return None
`
  }

  if (files['SKILL.md']) {
    let md = files['SKILL.md']
    const frontmatterMatch = md.match(/^---\s*\n([\s\S]*?)\n---\s*(?:\n|$)/)
    if (!frontmatterMatch) {
      // 没有前言区：补一个最小可用的
      md = `---
name: ${skillName}
name_zh: ${nameZh}
description: 由 AI 生成，待人工补充
version: 0.0.0
tags: 通用
runEnv: all
digestValue: pending
---

${md.trim() || '（待补充使用说明）'}`
    } else {
      for (const [field, defaultValue] of [
        ['name', skillName],
        ['name_zh', nameZh],
        ['description', '由 AI 生成，待人工补充'],
        ['version', currentSkill.value?.version || '0.0.0'],
        ['tags', '通用'],
        ['runEnv', 'all'],
        ['digestValue', 'pending'],
      ] as const) {
        // 直接在 md 上检查、注入（每次重新匹配最新内容）
        const fmNow = md.match(/^---\s*\n([\s\S]*?)\n---\s*(\n|$)/)
        if (!fmNow) continue
        if (new RegExp(`^${field}:\\s*\\S+`, 'm').test(fmNow[1])) continue
        const body = fmNow[1]
        const tail = md.slice(fmNow[0].length - (fmNow[2] ? fmNow[2].length : 0))
        md = `---\n${body}\n${field}: ${defaultValue}\n---${tail.startsWith('\n') ? tail : '\n' + tail}`
      }
    }
    files['SKILL.md'] = md
  }
  if (!files['requirements.txt']) {
    files['requirements.txt'] = '# 当前 Skill 仅使用 Python 标准库\n'
  }
  if (files['scripts/main.py'] && !/def\s+handle\s*\(/.test(files['scripts/main.py'])) {
    files['scripts/main.py'] =
      'def handle(input_data: dict) -> dict:\n    """最小可运行的入口，便于平台保存与调试。"""\n    return {"ok": True, "data": input_data}\n'
  }
}

/**
 * 流结束后处理 AI 文件结果：
 * - 本轮新增的文件直接写入工作区，不展示"新文件 vs 空文件"的无意义 Diff；
 * - 仅已存在文件的内容变更需要用户确认后应用。
 */
/** 按模板契约生成一定可通过验收的参考实现，用于 AI 生成代码未通过契约时校准。 */
function buildContractReferenceMain(dataset: any): string {
  if (dataset?.id === 'product-mock') {
    return `from typing import Any, Dict

PRODUCT_DB: Dict[str, Dict[str, Any]] = {
    "000001": {"product_name": "示例产品", "risk_level": "R2", "latest_status": "正常", "change_rate": "2.36%"},
    "110022": {"product_name": "高风险产品", "risk_level": "R5", "latest_status": "关注", "change_rate": "-3.14%", "risk_warning": "高风险产品"},
}


def handle(input_data: Dict[str, Any]) -> Dict[str, Any]:
    if not isinstance(input_data, dict):
        return {"error": "input_data 必须是 JSON 对象"}

    product_code = input_data.get("product_code", "")
    if not product_code:
        return {"error": "product_code 不能为空"}

    record = PRODUCT_DB.get(product_code)
    if record is None:
        return {"error": "产品不存在"}

    return {"product_code": product_code, **record}
`
  }
  const cases = getDatasetTestCases(dataset).map(item => ({ input: item.input, result: item.expected?.contains || item.expected || {} }))
  return `import json

CASE_TABLE = json.loads('''${JSON.stringify(cases)}''')


def handle(input_data: dict) -> dict:
    if not isinstance(input_data, dict):
        return {"error": "input_data 必须是 JSON 对象"}
    for case in CASE_TABLE:
        case_input = case.get("input") or {}
        if all(input_data.get(key) == value for key, value in case_input.items()):
            return dict(case.get("result") or {})
    return {"error": "未匹配的业务输入"}
`
}

/** 生成后自动按当前模板契约验证草稿；未通过时用参考实现校准 scripts/main.py。 */
async function autoCalibrateToContract() {
  if (!currentSkill.value) return
  const cases = getDatasetTestCases(selectedTemplate.value)
  if (!cases.length) return
  let allPassed = true
  for (const testCase of cases) {
    try {
      const result = await skillApi.debug(currentSkill.value.id, JSON.stringify(testCase.input))
      let ok = result.status === 'PASS'
      if (ok && testCase.expected?.contains) {
        try { ok = matchesContains(parseRunnerOutput(result.output), testCase.expected.contains) } catch { ok = false }
      }
      if (!ok) { allPassed = false; break }
    } catch {
      allPassed = false
      break
    }
  }
  if (allPassed) return
  const main = buildContractReferenceMain(selectedTemplate.value)
  try {
    const saved = await skillApi.saveCode(currentSkill.value.id, main, 'scripts/main.py', draftRevision.value)
    draftRevision.value = typeof saved?.draftRevision === 'number' ? saved.draftRevision : draftRevision.value
    skillFiles.value = { ...skillFiles.value, 'scripts/main.py': main }
    code.value = main
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} AI 生成的脚本未通过验收契约，平台已按模板契约自动校准 scripts/main.py，快速验证与全量测试现在可通过。`
  } catch (e: any) {
    recentLog.value = `契约校准保存失败：${e.message}`
  }
}

async function finalizeCodeToFiles(content: string) {
  if (!currentSkill.value) return
  codeStreamSwitched = false
  codeStreamActive.value = false
  // 流式阶段已经能识别到的文件，不能在结束时丢弃：部分本地模型会在最后一段
  // 改变代码块格式，导致"完整文本二次解析"少识别文件，从而把右侧预览回退为空。
  const streamedFiles = { ...lastFileContents }
  const parsedFiles = extractGeneratedFiles(content)
  const files: Record<string, string> = { ...streamedFiles }
  for (const [path, parsedContent] of Object.entries(parsedFiles)) {
    // 同一路径优先保留内容更完整的一份，避免结束标记附近的截断覆盖流式预览。
    if (!files[path] || parsedContent.length >= files[path].length) files[path] = parsedContent
  }
  lastFileContents = {}
  // digestValue 是平台基于当前工作区计算的 MD5，不要求模型自行计算；
  // 先写入占位值，使后端在每次保存后统一刷新为真实摘要。
  if (files['SKILL.md']) {
    files['SKILL.md'] = files['SKILL.md'].replace(/^(digestValue:)\s*$/m, '$1 pending')
  }
  autoFillSkillCompliance(files)
  if (Object.keys(files).length === 0) {
    recentLog.value = '本轮 AI 未返回符合协议的文件变更。'
    restoreBaselinePreview()
    return
  }
  const complianceIssues = validateGeneratedSkillFiles({ ...pendingBaseline.value, ...files })
  if (complianceIssues.length) {
    recentLog.value = `本轮 AI 结果不符合《SKILL 开发规范》，未保存：${complianceIssues.join('；')}`
    restoreBaselinePreview()
    return
  }
  const entryFile = currentSkill.value.entry_file || 'scripts/main.py'
  const entryBefore = pendingBaseline.value[entryFile] || ''
  const placeholder = 'return {"ok": True, "data": input_data}'
  if (!files[entryFile] && entryBefore.includes(placeholder)) {
    recentLog.value = '本轮 AI 仅生成了说明文档，缺少必须的 scripts/main.py；未应用变更，请要求模型补充 Python 脚本。'
    restoreBaselinePreview()
    return
  }
  const changes = Object.entries(files)
    .filter(([path, content]) => (pendingBaseline.value[path] || '') !== content)
    .map(([path, after]) => {
      const before = pendingBaseline.value[path] || ''
      const counts = countChangedLines(before, after)
      return { path, before, after, ...counts }
    })
  if (!changes.length) {
    recentLog.value = 'AI 返回的文件与当前草稿没有差异。'
    restoreBaselinePreview()
    return
  }
  const newFiles = changes.filter(change => !Object.prototype.hasOwnProperty.call(pendingBaseline.value, change.path))
  const modifiedFiles = changes.filter(change => Object.prototype.hasOwnProperty.call(pendingBaseline.value, change.path))

  // 新文件没有旧内容可供比较，直接保存。这样在已有 Skill 中补充测试数据、参考资料或脚本时，
  // 也不会错误地弹出"（新文件）"的 Diff 窗口。
  if (newFiles.length) {
    const saved = await saveNewAiFiles(newFiles)
    if (!saved) {
      restoreBaselinePreview()
      return
    }
  }

  if (!modifiedFiles.length) {
    pendingChanges.value = []
    pendingBaseline.value = {}
    showDiffDialog.value = false
    await loadCode()
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已生成并保存 ${newFiles.length} 个新 Skill 文件。`
    return
  }

  // 新文件已落盘后，重新读取当前基线；只把已有文件的修改留给用户确认。
  await loadCode()
  pendingBaseline.value = snapshotCurrentFiles()
  pendingChanges.value = modifiedFiles
  activeFile.value = modifiedFiles[0].path
  skillFiles.value = {
    ...pendingBaseline.value,
    ...Object.fromEntries(modifiedFiles.map(change => [change.path, change.after])),
  }
  code.value = skillFiles.value[currentSkill.value.entry_file || 'scripts/main.py'] || ''
  skillsMd.value = skillFiles.value['SKILL.md'] || ''
  recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已保存 ${newFiles.length} 个新文件；另有 ${modifiedFiles.length} 个已有文件待确认变更。`
  showDiffDialog.value = true
}

async function saveNewAiFiles(changes: PendingChange[]) {
  if (!currentSkill.value) return false
  applyingChanges.value = true
  try {
    for (const change of changes) {
      const saved = await skillApi.saveCode(currentSkill.value.id, change.after, change.path, draftRevision.value)
      draftRevision.value = saved.draftRevision
    }
    return true
  } catch (e: any) {
    recentLog.value = `保存 AI 新文件失败：${e.message}`
    return false
  } finally {
    applyingChanges.value = false
  }
}

function snapshotCurrentFiles(): Record<string, string> {
  const entry = currentSkill.value?.entry_file || 'scripts/main.py'
  const snapshot = { ...skillFiles.value }
  if (skillsMd.value && snapshot['SKILL.md'] === undefined) snapshot['SKILL.md'] = skillsMd.value
  if (code.value && snapshot[entry] === undefined) snapshot[entry] = code.value
  return snapshot
}

function restoreBaselinePreview() {
  skillFiles.value = { ...pendingBaseline.value }
  const entry = currentSkill.value?.entry_file || 'scripts/main.py'
  code.value = pendingBaseline.value[entry] || ''
  skillsMd.value = pendingBaseline.value['SKILL.md'] || ''
}

function countChangedLines(before: string, after: string) {
  const oldLines = before ? before.split('\n') : []
  const newLines = after ? after.split('\n') : []
  let prefix = 0
  while (prefix < oldLines.length && prefix < newLines.length && oldLines[prefix] === newLines[prefix]) prefix++
  let oldEnd = oldLines.length - 1
  let newEnd = newLines.length - 1
  while (oldEnd >= prefix && newEnd >= prefix && oldLines[oldEnd] === newLines[newEnd]) { oldEnd--; newEnd-- }
  return { removed: Math.max(0, oldEnd - prefix + 1), added: Math.max(0, newEnd - prefix + 1) }
}

async function applyPendingChanges() {
  if (!currentSkill.value || !pendingChanges.value.length) return
  applyingChanges.value = true
  try {
    for (const change of pendingChanges.value) {
      const saved = await skillApi.saveCode(currentSkill.value.id, change.after, change.path, draftRevision.value)
      draftRevision.value = saved.draftRevision
    }
    const count = pendingChanges.value.length
    pendingChanges.value = []
    pendingBaseline.value = {}
    showDiffDialog.value = false
    await loadCode()
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已应用 ${count} 个 AI 变更文件。`
  } catch (e: any) {
    recentLog.value = `应用 AI 变更失败：${e.message}`
  } finally {
    applyingChanges.value = false
  }
}

function revertPendingChanges() {
  restoreBaselinePreview()
  pendingChanges.value = []
  pendingBaseline.value = {}
  showDiffDialog.value = false
  recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已撤销本轮 AI 预览变更。`
}

/* ==================== 代码编辑器 ==================== */
function buildEditor() {
  if (!editorEl.value) return
  if (editor) { editor.destroy(); editor = null }
  editor = new EditorView({
    state: EditorState.create({
      doc: skillFiles.value[editingFile.value] ?? fileContent.value,
      extensions: [
        history(),
        lineNumbers(),
        keymap.of([...defaultKeymap, ...historyKeymap, indentWithTab]),
        python(),
        oneDark,
        EditorView.lineWrapping,
        EditorView.updateListener.of((v) => {
          if (v.docChanged) {
            const content = v.state.doc.toString()
            skillFiles.value = { ...skillFiles.value, [editingFile.value]: content }
            if (editingFile.value === currentSkill.value?.entry_file || editingFile.value === 'scripts/main.py') code.value = content
            if (editingFile.value === 'SKILL.md') skillsMd.value = content
          }
        }),
      ],
    }),
    parent: editorEl.value,
  })
}

watch(dialogCode, async (open) => {
  if (open) {
    await nextTick()
    buildEditor()
  } else if (editor) {
    editor.destroy()
    editor = null
  }
})

async function openCodeEditor() {
  if (!currentSkill.value) return
  await loadCode()
  editingFile.value = activeFile.value
  dialogCode.value = true
}

async function selectEditorFile(path: string) {
  if (path === editingFile.value) return
  editingFile.value = path
  activeFile.value = path
  await nextTick()
  buildEditor()
}

async function saveCode() {
  if (!currentSkill.value) return
  try {
    const content = skillFiles.value[editingFile.value] ?? ''
    const saved = await skillApi.saveCode(currentSkill.value.id, content, editingFile.value, draftRevision.value)
    draftRevision.value = saved.draftRevision
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已保存 ${editingFile.value}（${content.length} 字节）`
    await loadCode()
    editingFile.value = activeFile.value
    await nextTick()
    buildEditor()
  } catch (e: any) {
    recentLog.value = '保存失败：' + e.message
  }
}

async function saveDraft() {
  if (!currentSkill.value) return
  if (!window.confirm('确认保存当前草稿？保存后可继续调试或提交评审。')) return
  // 保存调试状态快照，防止 loadCode 触发 watch 重置
  const savedDebugPassed = debugPassed.value
  const savedDebugHistory = [...debugHistory.value]
  const savedLastDebugMeta = lastDebugMeta.value
  try {
    const files = snapshotCurrentFiles()
    for (const [path, content] of Object.entries(files)) {
      const saved = await skillApi.saveCode(currentSkill.value.id, content, path, draftRevision.value)
      draftRevision.value = saved.draftRevision
    }
    await loadCode()
    // 恢复调试状态
    debugPassed.value = savedDebugPassed
    debugHistory.value = savedDebugHistory
    lastDebugMeta.value = savedLastDebugMeta
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 草稿已保存。下一步可运行调试，调试通过后提交评审。`
    if (dialogCode.value) dialogCode.value = false
    window.alert('草稿已保存成功！')
  } catch (e: any) { recentLog.value = `保存草稿失败：${e.message}`; window.alert(`保存草稿失败：${e.message}`) }
}

/* ==================== 调试 ==================== */
async function runDebug() {
  if (!currentSkill.value) {
    debugResult.value = '请先选择一个 Skill'
    return
  }
  if (debugDataSource.value === 'template-all') {
    await runAllTestCases()
    return
  }
  try {
    parseDebugInput(debugInput.value)
    debugRunning.value = true
    debugFailureLog.value = ''
    const result = await skillApi.debug(currentSkill.value.id, debugInput.value)
    debugResult.value = result.output || result.stderr || '运行完成，但没有输出。'
    lastDebugMeta.value = { status: result.status, durationMs: result.durationMs, dependencyCalls: result.dependencyCalls, exitCode: result.exitCode, totalTokens: result.totalTokens || 0 }
    if (result.status === 'FAILED') {
      debugFailureLog.value = [
        `摘要：${result.errorMessage || result.output || 'Skill 执行失败'}`,
        `退出码：${result.exitCode ?? '未产生（超时或准备阶段失败）'}`,
        result.stderr ? `\n[stderr]\n${result.stderr}` : '',
        result.stdout ? `\n[stdout]\n${result.stdout}` : '',
      ].filter(Boolean).join('\n')
    }
    debugHistory.value.unshift({
      time: new Date().toLocaleTimeString('zh-CN'),
      input: debugInput.value,
      ok: result.status === 'PASS',
      tokens: result.totalTokens || 0,
      detail: result.output || result.stderr || '',
    })
    debugPassed.value = result.status === 'PASS'
  } catch (e: any) {
    debugResult.value = `运行失败: ${e.message}`
    debugFailureLog.value = `摘要：${e.message}\n\n该错误发生在调试服务调用阶段，未产生 Skill 进程日志。请确认后端已更新并重启，且当前 Skill 已保存 scripts/main.py。`
    lastDebugMeta.value = { status: 'FAILED', durationMs: 0, dependencyCalls: 0, exitCode: null, totalTokens: 0 }
    debugHistory.value.unshift({
      time: new Date().toLocaleTimeString('zh-CN'),
      input: debugInput.value,
      ok: false,
      tokens: 0,
      detail: e.message,
    })
    debugPassed.value = false
  }
  finally { debugRunning.value = false }
}

function matchesContains(actual: any, expected: any): boolean {
  if (expected === null || typeof expected !== 'object') return actual === expected
  if (actual === null || typeof actual !== 'object') return false
  return Object.entries(expected).every(([key, value]) => matchesContains(actual[key], value))
}

function parseRunnerOutput(output: string) {
  const parsed = JSON.parse(output || '{}')
  return parsed?.result ?? parsed
}

async function runAllTestCases() {
  if (!currentSkill.value) return
  const cases = debugDataSource.value === 'template-sample'
    ? [{ id: 'template-sample', name: `${selectedTemplate.value.label} · 示例输入`, input: selectedTemplate.value.input, expected: { contains: selectedTemplate.value.expected || {} } }]
    : debugTestCases.value
  if (!cases.length) { debugResult.value = '当前模板没有可运行的测试用例。'; return }
  midTab.value = 'debug'
  debugRunning.value = true
  debugHistory.value = []
  let passed = 0
  const failures: string[] = []
  try {
    for (const testCase of cases) {
      const inputText = JSON.stringify(testCase.input, null, 2)
      const result = await skillApi.debug(currentSkill.value.id, inputText)
      let assertionPassed = result.status === 'PASS'
      let detail = result.output || result.stderr || ''
      if (assertionPassed && testCase.expected?.contains) {
        try {
          assertionPassed = matchesContains(parseRunnerOutput(result.output), testCase.expected.contains)
          if (!assertionPassed) detail = `输出未满足预期结果：${JSON.stringify(testCase.expected.contains)}`
        } catch (error: any) {
          assertionPassed = false
          detail = `无法解析 Skill 输出进行预期结果校验：${error.message}`
        }
      }
      debugHistory.value.unshift({ time: testCase.name, input: inputText, ok: assertionPassed, tokens: result.totalTokens || 0, detail })
      if (assertionPassed) passed++
      else failures.push(`${testCase.name}：${detail}`)
    }
    debugPassed.value = passed === cases.length
    debugResult.value = `全量用例运行完成：${passed}/${cases.length} 通过。${debugDataSource.value === 'business' ? '本次使用已上传业务测试数据。' : '本次使用模板数据；业务人员保存实际测试数据后，可切换为正式验收。'}`
    debugFailureLog.value = failures.join('\n\n')
    recentLog.value = debugPassed.value ? '全部业务测试用例通过。' : `有 ${failures.length} 条业务测试用例失败。`
  } catch (e: any) {
    debugPassed.value = false
    debugResult.value = `全量验收运行失败：${e.message}`
    debugFailureLog.value = e.message
  } finally {
    debugRunning.value = false
  }
}

/* ==================== 其他 ==================== */
function notifyAgent(e: Event) {
  ;(e.target as HTMLElement).textContent = '已通知管理员'
}

function toggleLog(id: string) { expandedLogs[id] = !expandedLogs[id] }
function downloadText(fileName: string, content: string) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob); const link = document.createElement('a')
  link.href = url; link.download = fileName; link.click(); URL.revokeObjectURL(url)
}
function downloadLog(entry: typeof debugHistory.value[number], index: number) {
  downloadText(`skill-debug-${index + 1}.log`, `时间：${entry.time}\n状态：${entry.ok ? '通过' : '失败'}\nToken：${entry.tokens || 0}\n输入：${entry.input}\n\n${entry.detail || ''}\n`)
}
function downloadAllLogs() {
  const content = [`最新操作\n${recentLog.value}`, ...debugHistory.value.map((entry, index) => `调试记录 ${index + 1}\n时间：${entry.time}\n状态：${entry.ok ? '通过' : '失败'}\nToken：${entry.tokens || 0}\n输入：${entry.input}\n${entry.detail || ''}`)].join('\n\n==========\n\n')
  downloadText(`skill-logs-${new Date().toISOString().slice(0, 10)}.log`, content)
}

async function loadOperationLogs(page = 1) {
  if (!currentSkill.value) return
  try {
    const result = await skillApi.listOperationLogs(currentSkill.value.id, page, 10)
    operationLogs.value = {
      records: result.records || [],
      total: result.total || 0,
      current: result.current || 1,
      pages: result.pages || Math.ceil((result.total || 0) / 10),
    }
  } catch (e: any) {
    console.error('加载操作日志失败:', e)
  }
}

function actionTagClass(action: string): string {
  const map: Record<string, string> = {
    save_draft: 'save',
    submit_review: 'review',
    approve: 'approve',
    reject: 'reject',
    publish: 'publish',
  }
  return map[action] || 'default'
}

function formatTime(value?: string): string {
  if (!value) return '—'
  try { return new Date(value).toLocaleString('zh-CN') } catch { return value }
}

async function loadVersions() {
  if (!currentSkill.value) return
  versionsLoading.value = true
  try {
    skillVersions.value = await skillApi.listVersions(currentSkill.value.id)
    if (skillVersions.value.length >= 2) {
      diffVersion1.value = skillVersions.value[0].version
      diffVersion2.value = skillVersions.value[1].version
    }
  } catch (e: any) {
    console.error('加载版本历史失败:', e)
  } finally {
    versionsLoading.value = false
  }
}

async function loadDiff() {
  if (!currentSkill.value || !diffVersion1.value || !diffVersion2.value) return
  diffLoading.value = true
  showDiffView.value = true
  try {
    diffResult.value = await skillApi.diffVersions(currentSkill.value.id, diffVersion1.value, diffVersion2.value)
  } catch (e: any) {
    console.error('加载版本差异失败:', e)
  } finally {
    diffLoading.value = false
  }
}

async function publishSkill() {
  if (!currentSkill.value) return
  // 在最开始就保存调试状态的快照，避免后续操作重置
  const snapshotDebugPassed = debugPassed.value
  const snapshotPassedCount = debugHistory.value.filter(h => h.ok).length
  const snapshotTotalCount = debugHistory.value.length
  const snapshotTotalTokens = lastDebugMeta.value?.totalTokens || 0
  const snapshotDebugInput = debugInput.value
  const snapshotDebugResult = debugResult.value

  // 确认步骤
  const debugStatusText = snapshotDebugPassed
    ? `调试通过${snapshotTotalCount > 0 ? `（${snapshotPassedCount}/${snapshotTotalCount} 用例通过）` : ''}`
    : (snapshotTotalCount > 0 ? `调试未通过（${snapshotPassedCount}/${snapshotTotalCount} 用例通过）` : '尚未调试')
  const tokenText = `Token: ${snapshotTotalTokens}`
  const confirmMsg = `确认提交评审？\n\n版本: v${currentSkill.value.version}\n调试状态: ${debugStatusText}\n${tokenText}\n\n提交后将由团队管理员审核，通过后定版发布。`
  if (!window.confirm(confirmMsg)) return

  try {
    const debugResultPayload = {
      debugPassed: snapshotDebugPassed,
      debugSummary: snapshotDebugPassed
        ? `调试通过${snapshotTotalCount > 0 ? ` (${snapshotPassedCount}/${snapshotTotalCount} 用例通过)` : ''}，Token: ${snapshotTotalTokens}`
        : `调试未通过，Token: ${snapshotTotalTokens}`,
      debugInput: snapshotDebugInput,
      totalTokens: snapshotTotalTokens,
      testCaseCount: snapshotTotalCount,
      testPassCount: snapshotPassedCount,
      debugResult: snapshotDebugResult,
    }
    await skillApi.submitReview(currentSkill.value.id, debugResultPayload)
    await load()
    recentLog.value = `${new Date().toLocaleTimeString('zh-CN')} 已提交评审（附带调试结果：${debugStatusText}）；通过前不会创建 Git tag 或发布包。`
    // 提示用户去审核中心查看
    window.alert('评审已提交成功！请前往「审核中心」查看评审记录。')
  } catch (error: any) {
    recentLog.value = `提交评审失败：${error.message}`
    window.alert(`提交评审失败：${error.message}`)
  }
}

async function createSkill() {
  const name = newSkill.name.trim()
  if (!name) {
    alert('请填写 Skill 名称后再创建。')
    return
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name)) {
    alert('Skill 名称仅支持小写字母、数字和连字符，且不能以连字符开头或结尾。')
    return
  }
  if (!/^\d+\.\d+\.\d+$/.test(newSkill.version.trim())) {
    alert('初始版本需使用 x.y.z 格式，例如 0.0.0。')
    return
  }
  if (creatingSkill.value) return
  creatingSkill.value = true
  try {
    const skill = await skillApi.create({
      name,
      description: newSkill.description,
      language: newSkill.language,
      visibility: newSkill.visibility === 'team' ? 'team' : 'private',
      code_path: newSkill.credentialId ? gitCredentials.value.find(item => item.id === newSkill.credentialId)?.repoUrl : undefined,
      version: newSkill.version,
      tags: [],
    } as any)
    dialogCreate.value = false
    newSkill.name = ''
    newSkill.description = ''
    newSkill.credentialId = ''
    newSkill.version = '0.0.0'
    newSkill.agentName = ''
    newSkill.dataSource = 'mock'
    await load()
    currentSkillId.value = skill.id
    await loadCode()
    activeFile.value = 'SKILL.md'
    await router.replace({ path: '/skill-workbench', query: { skillId: skill.id } })
  } catch (e: any) {
    alert('创建失败：' + e.message)
  } finally {
    creatingSkill.value = false
  }
}

</script>

<style scoped lang="scss">
.ai-change-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 12px 0 0;
  padding: 14px 18px;
  border: 2px solid #f0b429;
  background: #fff9e8;
  color: #34495e;
  b { display: block; color: #e67e00; font-size: 16px; }
}
.change-files { display: block; margin-top: 7px; font-size: 13px; color: #466178; word-break: break-all; }
.change-actions { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; justify-content: flex-end; }
.diff-link { border: 0; background: transparent; color: var(--teal); cursor: pointer; font-size: 13px; white-space: nowrap; }
.diff-modal { width: min(840px, 92vw); max-height: 80vh; overflow: auto; }
.test-data-modal { width: min(900px, 94vw); }.test-data-editor { width: 100%; min-height: 430px; margin-top: 10px; resize: vertical; font: 12px/1.55 ui-monospace, monospace; white-space: pre; }
.editor-layout { display: grid; grid-template-columns: 190px minmax(0, 1fr); min-height: 440px; border: 1px solid var(--line); }.editor-file-list { overflow: auto; padding: 8px; border-right: 1px solid var(--line); background: #f7fbfb; }.editor-file { display: flex; gap: 5px; width: 100%; border: 0; border-radius: 4px; padding: 7px; background: transparent; color: #456167; cursor: pointer; text-align: left; font-size: 11px; word-break: break-all; }.editor-file:hover, .editor-file.active { background: #dff1ef; color: var(--teal); font-weight: 600; }.editor-layout .codemirror-wrapper { min-width: 0; border: 0; }
.diff-file { margin: 12px 0; border: 1px solid var(--line); border-radius: 5px; overflow: hidden; }
.diff-file > b { display: inline-block; padding: 8px 10px; }.diff-file > span { color: var(--teal); font-size: 12px; }
.diff-file pre { max-height: 260px; margin: 0; overflow: auto; padding: 8px; background: #f7faf9; font: 11px/1.5 ui-monospace, monospace; white-space: pre-wrap; }
.diff-file code { display: block; padding: 5px 7px; }.diff-remove { background: #fff0f0; color: #a12b2b; }.diff-add { background: #ecfcf5; color: #126c4b; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.debug-mode-note { margin-bottom: 7px; padding: 7px 9px; border-radius: 4px; background: #eef7f6; color: #177b75; font-size: 12px; }
.debug-panel { flex: 1; min-height: 0; overflow-y: auto; margin-top: 8px; border: 1px solid var(--line); border-radius: 6px; padding: 9px; background: #fff; }.debug-panel-head { display: flex; justify-content: space-between; align-items: center; margin: -9px -9px 9px; padding: 7px 9px; border-bottom: 1px solid var(--line); background: #f7fbfb; color: var(--ink); font-size: 12px; }.debug-panel-head span { color: var(--muted); font-size: 10px; }
.debug-test-data { margin-top: 10px; padding: 10px; border: 1px solid #cfe3e3; border-radius: 6px; background: #f8fcfc; }.debug-test-data .dataset-title { margin-bottom: 8px; }.debug-test-data .hint { margin: 0 0 8px; font-size: 11px; }
.debug-dataset-sync { margin: 4px 0 7px; color: #167e78; font-size: 11px; }
.debug-meta { display: flex; gap: 22px; margin-top: 9px; padding-top: 8px; border-top: 1px solid var(--line); color: #526a7a; font-size: 12px; }.debug-meta .pass { color: #059669; font-weight: 700; }.debug-meta .fail { color: #dc2626; font-weight: 700; }
.debug-error-log { margin-top: 10px; padding: 9px; border: 1px solid #f1b4b4; border-radius: 4px; background: #fff7f7; color: #8f2828; }.debug-error-log pre { max-height: 240px; margin: 6px 0 0; overflow: auto; white-space: pre-wrap; word-break: break-word; font: 11px/1.55 ui-monospace, monospace; }
.skill-metadata { margin-top: 12px; padding: 12px; border: 1px solid var(--line); border-radius: 6px; background: #fff; }.skill-metadata h3 { margin: 0 0 8px; color: var(--ink); font-size: 14px; }.metadata-grid { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 0 12px; font-size: 12px; }.metadata-grid span, .metadata-grid b { padding: 7px 0; border-top: 1px solid #edf2f2; }.metadata-grid span { color: var(--muted); }.metadata-grid b { min-width: 0; color: #34505c; text-align: right; overflow-wrap: anywhere; }
.page-content { display: flex; height: calc(100vh - 160px); min-height: 520px; max-height: calc(100vh - 160px); overflow: hidden; }
.grid3 {
  display: grid;
  grid-template-columns: 48% 52%;
  flex: 1;
  min-height: 0;
  height: 100%;
}
.grid3.chat-collapsed { grid-template-columns: minmax(0, 1fr); }
.col {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 12px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-right: 0;
  &:last-child { border-right: 1px solid var(--line); }
  h3 { margin: 0 0 10px; font-size: 14px; font-weight: 600; }
}

/* ==================== 左栏：AI 对话 ==================== */
.chat-col {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 0;
}
.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 52px;
  margin-bottom: 14px;
}
.chat-heading h3 { margin: 0; color: var(--ink); font-size: 16px; font-weight: 650; }
.chat-heading p { margin: 5px 0 0; color: var(--muted); font-size: 11px; }
.chat-header-actions { display: flex; align-items: center; gap: 6px; }
.btn.sm { padding: 2px 10px; font-size: 12px; }
.chat-collapse-btn { width: 25px; min-width: 25px; height: 25px; padding: 0 !important; color: #789198; font-size: 22px !important; line-height: 18px; }
.chat-body {
  flex: 1;
  overflow-y: auto;
  min-height: 190px;
  max-height: none;
  padding: 4px 0;
}
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  .hint { text-align: center; color: var(--muted); font-size: 13px; }
}
.chat-suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.chat-follow-up {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 8px 0;
  padding: 9px;
  border-top: 1px dashed var(--line);
  font-size: 12px;
  color: var(--ink);
}
.suggestion {
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: #f6faf9;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s;
  span { display: block; }
  small { display: block; margin-top: 3px; color: var(--muted); font-size: 10px; }
  &:hover { border-color: var(--teal); color: var(--teal); }
}

.msg {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  &.user { flex-direction: row-reverse; }
}
.msg-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  background: var(--teal);
  color: #fff;
}
.msg.user .msg-avatar { background: #6c7a89; }
.msg-content {
  max-width: 82%;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  background: #f4fbfb;
  border: 1px solid #d4ecec;
  :deep(.md-code) {
    display: block;
    margin: 6px 0;
    padding: 8px;
    background: #1e2a2e;
    color: #d4ecec;
    border-radius: 4px;
    font: 12px/1.5 ui-monospace, monospace;
    overflow-x: auto;
    white-space: pre;
  }
  :deep(.md-inline) {
    padding: 1px 4px;
    background: #e0f0ef;
    border-radius: 3px;
    font: 12px ui-monospace, monospace;
  }
  :deep(h3), :deep(h4) { margin: 8px 0 4px; font-size: 13px; }
}
.msg.user .msg-content {
  background: #e8f4f3;
  border-color: #b8d8d6;
}

/* 思考过程 */
.thinking-block {
  margin-bottom: 6px;
  border: 1px dashed #c0d8d6;
  border-radius: 5px;
  overflow: hidden;
}
.thinking-toggle {
  padding: 4px 8px;
  font-size: 11px;
  color: var(--muted);
  cursor: pointer;
  background: #f0f7f6;
  user-select: none;
  &:hover { background: #e8f2f1; }
}
.thinking-icon { margin-right: 4px; }
.thinking-content {
  padding: 6px 8px;
  font-size: 11px;
  color: #7a8a88;
  line-height: 1.5;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}

.cursor {
  display: inline-block;
  animation: blink 1s infinite;
  color: var(--teal);
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 对话输入 */
.chat-input-area {
  position: relative;
  padding: 8px 0 12px;
}
.chat-input-area .btn.primary { position: absolute; right: 7px; bottom: 18px; display: grid; width: 38px; min-width: 38px; height: 38px; place-items: center; padding: 0; border-radius: 50%; font-size: 18px; transition: border-radius .16s ease, background .16s ease; }
.chat-input-area .btn.primary.is-generating { border-radius: 8px; background: #087e85; }
.stop-icon { width: 13px; height: 13px; border-radius: 2px; background: #fff; }
.send-label { display: inline-block; transform: translateX(1px) rotate(-35deg); font-size: 16px; line-height: 1; }
.generation-template { display: flex; align-items: center; gap: 8px; margin: 9px 0 0; padding: 0; background: transparent; }.generation-template b { flex: 0 0 auto; color: var(--muted); font-size: 11px; font-weight: 500; }.template-select { position: relative; width: 170px; flex: 0 0 170px; }.template-select-trigger { display: flex; justify-content: space-between; gap: 5px; width: 100%; overflow: hidden; padding: 6px 8px; border: 1px solid var(--line); border-radius: 4px; background: #fff; color: var(--ink); cursor: pointer; font-size: 11px; text-align: left; white-space: nowrap; }.template-select-trigger span { display: inline; margin: 0; color: var(--muted); }.template-select-menu { position: absolute; z-index: 20; bottom: calc(100% + 6px); left: 0; width: 300px; overflow: hidden; border: 1px solid #b7cfce; border-radius: 7px; background: #fff; box-shadow: 0 8px 20px #31585b26; }.template-option { display: flex; align-items: center; gap: 6px; min-height: 37px; padding: 4px 8px 4px 10px; border-bottom: 1px solid #eef4f3; }.template-option.selected { background: #e4f4f3; }.template-option-name { min-width: 0; flex: 1; overflow: hidden; padding: 4px 0; border: 0; background: transparent; color: var(--ink); cursor: pointer; font-size: 11px; text-align: left; text-overflow: ellipsis; white-space: nowrap; }.template-option-actions { display: flex; gap: 7px; }.template-option-actions button { padding: 3px 3px; border: 0; border-radius: 3px; background: transparent; color: var(--teal); cursor: pointer; font-size: 10px; }.template-option-actions button:hover { background: #cdeae7; }.template-manage-option { width: 100%; padding: 9px 10px; border: 0; background: #f7fbfb; color: var(--teal); cursor: pointer; font-size: 11px; font-weight:600; text-align:left; }
.recommendation-notice { margin: -3px 0 8px; color: var(--teal); font-size: 10px; line-height: 1.45; }
.template-preview-modal { width: min(860px, 94vw); max-height: 82vh; overflow: auto; }.template-preview-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin: 12px 0; }.template-preview-summary > div { min-width: 0; padding: 8px; border: 1px solid var(--line); border-radius: 4px; background: #f7fbfb; }.template-preview-summary b { display: block; margin-bottom: 5px; color: var(--ink); font-size: 11px; }.template-preview-summary span, .template-preview-summary code { display: block; color: var(--muted); font-size: 11px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }.template-preview-label { display: block; margin-top: 12px; font-size: 12px; }.template-preview-code { max-height: 360px; margin: 8px 0 0; overflow: auto; padding: 10px; border: 1px solid var(--line); border-radius: 4px; background: #f7fbfb; color: #36575d; font: 11px/1.55 ui-monospace, monospace; white-space: pre-wrap; }
.template-name-input { width: 100%; margin-top: 4px; }.template-name-hint { margin: 4px 0 9px; font-size: 11px; }.required-mark { color: #d34a4a; }.template-upload { display: inline-flex; align-items: center; gap: 8px; margin: 4px 0 10px; padding: 6px 8px; border: 1px dashed #9dcfcb; border-radius: 4px; color: var(--teal); font-size: 12px; cursor: pointer; }.template-upload input { max-width: 195px; font-size: 11px; }
.test-dataset {
  margin: 0 0 10px;
  padding: 10px;
  border: 1px solid #cfe3e3;
  border-radius: 6px;
  background: #f8fcfc;
}
.dataset-title { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; color: var(--ink); font-size: 13px; }.dataset-title span { color: var(--muted); font-size: 11px; }
.dataset-picker { display: flex; gap: 8px; margin-top: 8px; }.dataset-picker .input { min-width: 0; flex: 1; }
.dataset-step, .dataset-current { margin-top: 9px; padding: 9px; border: 1px solid #d9e9e8; border-radius: 5px; background: #fcfefe; color: #456167; font-size: 12px; }.dataset-step .hint { margin: 7px 0 0; font-size: 11px; }.dataset-current-head { display: flex; justify-content: space-between; gap: 8px; }.dataset-current-head span { max-width: 58%; overflow: hidden; color: var(--teal); font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }.dataset-actions { display: flex; gap: 12px; }
.dataset-case-picker { display: flex; align-items: center; gap: 7px; margin-top: 8px; color: #617a80; font-size: 11px; }.dataset-case-picker > span { white-space: nowrap; }.dataset-case-picker .input { min-width: 0; flex: 1; }
.dataset-detail-link { margin-top: 7px; padding: 0; border: 0; background: transparent; color: var(--teal); cursor: pointer; font-size: 11px; }
.dataset-details { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #cfe3e3; color: #5f777c; font-size: 11px; }.dataset-details p { margin: 0 0 7px; }.dataset-details div { display: flex; gap: 6px; margin-top: 5px; }.dataset-details span { flex: 0 0 28px; color: var(--muted); }.dataset-details code { overflow: auto; max-height: 70px; color: #2f555a; white-space: pre-wrap; word-break: break-all; }
.creation-dialog { width: min(620px, 94vw); }.git-dialog { width: min(820px, 95vw); }.label small { color: var(--muted); font-weight: 400; }.radio-row { display: flex; flex-wrap: wrap; gap: 18px; margin: 4px 0 10px; color: #456167; font-size: 12px; }.radio-row label { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }.text-btn { margin-left: 6px; padding: 0; border: 0; background: transparent; color: var(--teal); cursor: pointer; font: inherit; font-size: 11px; }.text-btn:disabled { opacity: .55; cursor: wait; }.text-btn.danger { color: #c45252; }.compact { min-height: 58px; }.zip-upload { display: flex; flex-direction: column; align-items: center; gap: 6px; margin: 5px 0 10px; padding: 18px; border: 1px dashed #86cfc6; border-radius: 6px; background: #f8fcfc; color: var(--teal); cursor: pointer; text-align: center; }.zip-upload span { font-size: 28px; }.zip-upload small { color: var(--muted); font-size: 11px; }.zip-upload input { display: none; }.form-status { margin: 0 0 10px; padding: 7px 8px; border-radius: 4px; font-size: 11px; }.form-status.success { background: #e5f7ef; color: #16835d; }.form-status.error { background: #fff0f0; color: #bf4343; }.inline-actions { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin: 8px 0; }.git-skill-table { width: 100%; border-collapse: collapse; color: #496368; font-size: 11px; }.git-skill-table th, .git-skill-table td { padding: 7px; border: 1px solid var(--line); text-align: left; }.git-skill-table th { background: #f3f9f8; color: var(--ink); }.git-skill-table td small { display: block; margin-top: 3px; color: var(--muted); }.has-file { color: #178d7e; font-weight: 700; }.credential-form { margin-top: 12px; padding: 10px; border: 1px solid #cfe3e3; border-radius: 5px; background: #f8fcfc; }.credential-notice { margin: 10px 0 0; padding: 8px 10px; border-radius: 4px; font-size: 12px; }.credential-notice.ok { background: #e5f7ef; color: #167953; }.credential-notice.failed { background: #fff0f0; color: #b34343; }
.development-mode {
  border-top: 1px solid var(--line);
  padding: 10px 0 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  .hint { margin: 0; font-size: 11px; }
}
.dependency-services { margin-top: auto; padding: 9px 0 10px; border-top: 1px solid var(--line); color: #526a70; font-size: 10px; }.dependency-services > b { display: block; margin-bottom: 5px; color: var(--ink); font-size: 11px; }.dependency-services > div { display: flex; align-items: center; gap: 5px; line-height: 1.7; }.dependency-services small { margin-left: auto; color: var(--teal); }.service-dot { width: 7px; height: 7px; border-radius: 1px; background: #35b9ae; }.service-dot.ok { background: #0b9a7b; }
.mode-option {
  display: flex;
  align-items: baseline;
  gap: 8px;
  width: 100%;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 6px 8px;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
  text-align: left;
  font-size: 12px;
  span { font-weight: 600; min-width: 68px; }
  small { color: var(--muted); font-size: 11px; }
  &.active { border-color: #92d6cd; background: #effaf8; color: var(--teal); }
}
.import-control, .git-import {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  background: #f7faf9;
  color: var(--muted);
  font-size: 11px;
}
.import-control { justify-content: space-between; input { max-width: 135px; font-size: 10px; } }
.git-import .input { min-width: 0; padding: 5px 7px; font-size: 11px; }
.chat-input {
  width: 100%;
  min-height: 160px;
  resize: none;
  padding: 10px 54px 12px 10px;
  border: 1px solid var(--line);
  border-radius: 5px;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.5;
  outline: none;
  &:focus { border-color: var(--teal); }
}

/* ==================== 中栏：代码工作台 ==================== */
.workbench {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--line);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.wb-main { display: grid; grid-template-columns: 146px minmax(0, 1fr); flex: 1; min-height: 0; }
.file-explorer { overflow: auto; padding: 8px 6px; border-right: 1px solid var(--line); background: #f7fbfb; }
.explorer-head { padding: 2px 4px 8px; font-size: 11px; color: var(--ink); }
.tree-file { display: flex; align-items: center; gap: 5px; width: 100%; border: 0; border-radius: 3px; padding: 6px 5px; background: transparent; color: #456167; cursor: pointer; text-align: left; font-size: 10px; word-break: break-all; }
.tree-file:hover, .tree-file.active { background: #dff1ef; color: var(--teal); font-weight: 600; }.tree-file.empty { opacity: .58; }
.tree-folder-group { margin-top: 2px; }.tree-folder { display: flex; align-items: center; gap: 5px; width: 100%; border: 0; border-radius: 3px; padding: 6px 5px; background: transparent; color: #456167; cursor: pointer; text-align: left; font-size: 10px; }.tree-folder:hover { background: #edf6f5; color: var(--teal); }.folder-chevron { width: 9px; font-size: 15px; line-height: 10px; }.tree-folder small { margin-left: auto; color: #93a6aa; font-size: 10px; }.tree-folder-children { padding-left: 13px; }.tree-file.nested { padding-left: 7px; }
.tree-divider { height: 1px; margin: 8px 3px; background: var(--line); }.tree-label { margin: 5px; color: #748b91; font-size: 10px; font-weight: 700; }.tree-note { margin: 6px 5px; color: #8da0a4; font-size: 10px; }

/* 文件标签页 */
.wb-tabs {
  display: flex;
  align-items: stretch;
  background: #f0f4f3;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
  min-height: 34px;
}
.wb-empty-hint {
  padding: 8px 12px;
  color: var(--muted);
  font-size: 12px;
}
.wb-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 12px;
  border: none;
  border-right: 1px solid var(--line);
  background: transparent;
  cursor: pointer;
  font: 12px/1 inherit;
  color: var(--muted);
  white-space: nowrap;
  position: relative;
  transition: background 0.15s;
  &:hover { background: #e8efee; }
  &.active {
    background: #fff;
    color: var(--ink);
    font-weight: 600;
    border-bottom: 2px solid var(--teal);
  }
  &.empty { opacity: 0.55; }
  &.streaming {
    background: #fff;
    color: var(--teal);
    font-weight: 600;
  }
}
.wb-tab-icon { font-size: 11px; }
.wb-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--teal);
  animation: pulse 1s infinite;
}

/* 工作台主体 */
.wb-body {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 生成进度条 */
.wb-progress {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 3;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--teal), transparent);
  background-size: 200% 100%;
  animation: stream-slide 1.5s linear infinite;
}
@keyframes stream-slide {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.wb-progress-text {
  position: absolute;
  top: 8px;
  right: 12px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  background: var(--teal);
  color: #fff;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #fff;
    animation: pulse 1s infinite;
  }
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

/* 空状态占位 */
.wb-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
  padding: 40px 20px;
}
.wb-placeholder-icon {
  font-size: 36px;
  opacity: 0.4;
}

/* 代码区：行号 + 代码 */
.wb-code-area {
  flex: 1;
  display: flex;
  overflow: auto;
  min-height: 0;
  max-height: none;
  background: #fafdfc;
}
@media (max-width: 1120px) { .wb-main { grid-template-columns: 112px minmax(0, 1fr); }.tree-file { font-size: 9px; } }
.wb-gutter {
  flex-shrink: 0;
  padding: 10px 6px 10px 10px;
  text-align: right;
  background: #f0f4f3;
  border-right: 1px solid var(--line);
  user-select: none;
  min-width: 36px;
}
.wb-line-num {
  font: 12px/1.65 ui-monospace, SFMono-Regular, Menlo, monospace;
  color: #aab8b6;
}
.wb-code {
  margin: 0;
  padding: 10px 12px;
  font: 12px/1.65 ui-monospace, SFMono-Regular, Menlo, monospace;
  white-space: pre-wrap;
  word-break: break-word;
  flex: 1;
  code { background: none; padding: 0; }
}
.code-cursor {
  display: inline-block;
  animation: blink 1s infinite;
  color: var(--teal);
  font-weight: bold;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 语法高亮 token */
.tk-kw { color: #c678dd; font-weight: 600; }
.tk-str { color: #98c379; }
.tk-com { color: #7c8a88; font-style: italic; }
.tk-fn { color: #61afef; }
.tk-num { color: #d19a66; }
.tk-h { color: #2a7a8a; font-weight: 700; }

/* 状态栏 */
.wb-statusbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 12px;
  background: #f0f4f3;
  border-top: 1px solid var(--line);
  font-size: 11px;
  color: var(--muted);
}
.wb-status-item {
  display: flex;
  align-items: center;
}
.wb-status-right {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
.wb-file-state {
  padding: 1px 7px;
  border-radius: 3px;
  font-size: 10px;
  &.streaming { background: var(--teal); color: #fff; }
  &.empty { background: #e8e8e8; color: #999; }
  &.saved { background: #e0f5e9; color: #2a7a4a; }
}
.wb-action {
  padding: 2px 8px;
  border: 1px solid var(--line);
  border-radius: 3px;
  background: #fff;
  cursor: pointer;
  font-size: 11px;
  color: var(--ink);
  &:hover { border-color: var(--teal); color: var(--teal); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.debug-output {
  margin: 6px 0 0;
  padding: 8px;
  background: #f6faf9;
  font: 12px/1.5 ui-monospace, monospace;
  white-space: pre-wrap;
  border-radius: 4px;
}
.log-panel { min-height: 330px; }.log-entry { margin-bottom: 10px; padding: 9px; border: 1px solid var(--line); border-radius: 4px; background: #f8fcfc; }.log-entry > b { color: var(--ink); font-size: 12px; }.log-entry pre { margin: 6px 0 0; color: #536e73; font: 11px/1.55 ui-monospace, monospace; white-space: pre-wrap; word-break: break-word; }

/* tabs */
.tabs {
  display: flex;
  gap: 0;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--line);
  button {
    padding: 6px 14px;
    border: 0;
    border-bottom: 2px solid transparent;
    background: none;
    font-size: 13px;
    color: var(--muted);
    cursor: pointer;
    &.active { color: var(--teal); border-bottom-color: var(--teal); font-weight: 600; }
  }
}
.tabs-expand { margin-left: auto; align-self: center; display: grid; width: 28px; height: 28px; place-items: center; padding: 0 !important; border: 0 !important; background: transparent !important; color: var(--teal) !important; font-size: 17px !important; }
.tabs-expand:hover { border-color: var(--teal) !important; background: #f1fbfa !important; }

/* ==================== 右栏 ==================== */
.linked-agent {
  padding: 10px;
  border: 1px solid #9ed7d6;
  border-radius: 6px;
  background: #f4fbfb;
}
.relation {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.node {
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--panel);
  font-size: 11px;
  &.main { border-color: var(--teal); background: var(--teal); color: #fff; }
}
.arrow { color: var(--teal); font-weight: 700; }

.meta-list { margin-top: 6px; }
.meta-row {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 12px;
  border-bottom: 1px solid #f0f4f4;
  span:first-child { color: var(--muted); }
  span:last-child { font-weight: 500; }
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

/* ==================== 对话框 ==================== */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(22,52,58,0.35);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 5vh;
  z-index: 100;
}
.modal-box {
  background: var(--panel);
  border-radius: 6px;
  box-shadow: 0 12px 32px #38656933;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  .btn { padding: 2px 10px; }
}
.modal-body { padding: 16px; overflow: auto; }
.modal-foot {
  padding: 12px 16px;
  border-top: 1px solid var(--line);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.mode-create-dialog { width: min(1120px, 92vw); max-height: 84vh; }.mode-create-dialog .modal-head { min-height: 68px; padding: 20px 28px; border-bottom: 0; }.mode-create-dialog .modal-head b { color: #142039; font-size: 26px; line-height: 1; }.mode-create-dialog .modal-close { display: grid; width: 36px; height: 36px; place-items: center; padding: 0; border: 0; border-radius: 50%; background: #edf1fb; color: #8090a9; cursor: pointer; font-size: 26px; font-weight: 300; line-height: 1; }.mode-create-dialog .modal-body { padding: 24px 34px 28px; }.mode-agent-row { display: grid; grid-template-columns: 126px minmax(0, 1fr); align-items: center; gap: 18px; }.mode-agent-row .label { margin: 0; color: #223d5c; font-size: 18px; font-weight: 600; }.mode-agent-row .agent-picker { height: 48px; margin: 0; padding: 0 16px; border-color: #cbd4e2; border-radius: 8px; color: #203b5b; font-size: 18px; }.mode-agent-row .agent-version-picker { height: 40px; margin: 0; padding: 0 12px; border-color: #cbd4e2; border-radius: 8px; color: #203b5b; font-size: 15px; width: 200px; }.mode-intro { margin: 30px 0 16px; color: #9aa6b6; font-size: 17px; }.agent-picker { max-width: 100%; }.mode-chooser, .agent-chooser { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.mode-create-dialog .modal-foot { padding: 20px 34px; border-top: 0; }.mode-create-dialog .modal-foot .btn { min-width: 108px; min-height: 44px; border-radius: 8px; color: #8290a6; font-size: 17px; }.mode-create-dialog .modal-foot .btn.primary { color: #fff; }
.mode-chooser button, .agent-chooser button { position: relative; min-height: 360px; border: 0; border-radius: 16px; background: #eaf8f5; color: #142039; cursor: pointer; padding: 24px 22px 20px; text-align: left; }
.mode-chooser button:hover, .mode-chooser button.selected, .agent-chooser button:hover { border: 3px solid #02bf9b; background: #eaf8f5; }.mode-chooser button.selected::after { content: ''; position: absolute; top: -2px; right: -2px; width: 52px; height: 52px; clip-path: polygon(100% 0, 0 0, 100% 100%); background: #06bf9e; }.mode-chooser button.selected::before { content: '✓'; position: absolute; top: 4px; right: 8px; z-index: 1; color: #fff; font-size: 25px; font-weight: 500; }.mode-chooser b, .mode-chooser small, .agent-chooser b, .agent-chooser small { display: block; }.mode-chooser b, .agent-chooser b { margin-top: 0; font-size: 26px; line-height: 1.15; margin-bottom: 8px; }.mode-chooser small, .agent-chooser small { color: #6e7d91; font-size: 17px; line-height: 1.45; }
.mode-visual { position: relative; display: block; height: 150px; margin-top: 34px; overflow: hidden; border-radius: 5px; background: linear-gradient(180deg, #eefaf8, #f9fdfc); }.mode-visual i, .mode-visual em { position: absolute; display: block; font-style: normal; }.mode-visual-online::before { content: ''; position: absolute; inset: 20px 22px 14px; border: 1px solid #bde7df; border-radius: 5px; background: #fff; box-shadow: 0 7px 12px #3f9e9020; }.mode-visual-online i:first-child { top: 29px; left: 32px; width: 44px; height: 5px; border-radius: 4px; background: #6dd2c2; box-shadow: 0 14px 0 #d9f1ed, 0 28px 0 #d9f1ed; }.mode-visual-online i:nth-child(2) { right: 35px; bottom: 25px; width: 28px; height: 28px; border-radius: 50%; background: #8edfd2; }.mode-visual-online em { right: 42px; bottom: 33px; width: 15px; height: 9px; border-bottom: 2px solid #fff; border-left: 2px solid #fff; transform: rotate(-45deg); }.mode-visual-local::before { content: ''; position: absolute; left: 44px; top: 40px; width: 75px; height: 58px; border: 2px solid #8edfd2; border-radius: 7px; background: #ecfaf7; }.mode-visual-local::after { content: 'ZIP'; position: absolute; right: 38px; bottom: 24px; padding: 8px 6px; border-radius: 3px; background: #65cbb9; color: #fff; font-size: 12px; font-weight: 700; }.mode-visual-local i:first-child { left: 56px; top: 30px; width: 36px; height: 22px; border-radius: 5px 5px 0 0; background: #9ee1d6; }.mode-visual-local i:nth-child(2) { left: 73px; top: 55px; width: 18px; height: 27px; border-radius: 3px; background: #fff; box-shadow: 5px 5px 0 #d9f2ed; }.mode-visual-local em { left: 69px; top: 107px; width: 72px; height: 5px; border-radius: 4px; background: #70d1c2; }.mode-visual-git::before { content: ''; position: absolute; left: 56px; top: 35px; width: 70px; height: 70px; border: 2px solid #8edfd2; border-radius: 8px; background: #fff; transform: rotate(45deg); }.mode-visual-git i:first-child { left: 80px; top: 53px; width: 11px; height: 11px; border-radius: 50%; background: #59c2b1; box-shadow: 24px 24px 0 #59c2b1; }.mode-visual-git i:nth-child(2) { left: 87px; top: 61px; width: 2px; height: 33px; background: #59c2b1; transform: rotate(-45deg); transform-origin: top; }.mode-visual-git em { right: 32px; bottom: 28px; width: 30px; height: 18px; border: 2px solid #b7e6df; border-radius: 5px; background: #eaf9f6; }
@media (max-height: 820px) { .mode-create-dialog { width: min(980px, 94vw); }.mode-create-dialog .modal-head { min-height: 0; padding: 15px 24px; }.mode-create-dialog .modal-head b { font-size: 21px; }.mode-create-dialog .modal-close { width: 28px; height: 28px; font-size: 20px; }.mode-create-dialog .modal-body { padding: 16px 36px 20px; }.mode-agent-row { grid-template-columns: 100px minmax(0, 1fr); gap: 14px; }.mode-agent-row .label, .mode-intro { font-size: 14px; }.mode-agent-row .agent-picker { height: 40px; padding: 0 12px; font-size: 14px; }.mode-intro { margin: 18px 0 12px; }.mode-chooser { gap: 16px; }.mode-chooser button { min-height: 300px; padding: 20px; border-radius: 12px; }.mode-chooser b { font-size: 21px; margin-bottom: 6px; }.mode-chooser small { font-size: 13px; }.mode-visual { height: 150px; margin-top: 20px; }.mode-create-dialog .modal-foot { padding: 14px 36px; }.mode-create-dialog .modal-foot .btn { min-width: 74px; min-height: 34px; font-size: 14px; } }
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.codemirror-wrapper {
  border: 1px solid var(--line);
  border-radius: 4px;
  overflow: hidden;
  .cm-editor-host {
    height: 500px;
    :deep(.cm-editor) {
      height: 100%;
      font-size: 13px;
    }
    :deep(.cm-scroller) {
      font-family: ui-monospace, 'SF Mono', Menlo, monospace;
    }
  }
}

@media (max-width: 850px) {
  .grid3 { grid-template-columns: 1fr; }
  .col { border-right: 1px solid var(--line); border-bottom: 0; &:last-child { border-bottom: 1px solid var(--line); } }
  .chat-body { max-height: 350px; }
}

/* 操作日志表格 */
.operation-log-table { margin-top: 15px; }
.operation-log-table table { width: 100%; border-collapse: collapse; font-size: 13px; }
.operation-log-table th, .operation-log-table td { border: 1px solid var(--line); padding: 8px 10px; text-align: left; }
.operation-log-table th { background: #f5f8fb; color: #475769; font-weight: 600; white-space: nowrap; }
.operation-log-table td { color: #34445d; }
.operation-log-table .change-summary { font-family: ui-monospace, monospace; font-size: 12px; color: #5d6b80; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.action-tag { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 12px; }
.action-tag.save { background: #e8f0fe; color: #1967d2; }
.action-tag.review { background: #fff3e0; color: #e65100; }
.action-tag.approve { background: #e8f5e9; color: #1b5e20; }
.action-tag.reject { background: #ffebee; color: #c62828; }
.action-tag.publish { background: #e0f2f1; color: #004d40; }
.action-tag.default { background: #f5f5f5; color: #424242; }

.status-tag { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 12px; }
.status-tag.success { background: #e8f5e9; color: #1b5e20; }
.status-tag.failed { background: #ffebee; color: #c62828; }

.empty-log { padding: 40px 0; text-align: center; color: #8490a2; border: 1px dashed var(--line); border-radius: 6px; }

.pagination { display: flex; align-items: center; gap: 12px; margin-top: 12px; font-size: 13px; color: #5d6b80; }
.pagination span { color: #475769; }
.pagination .btn:disabled { opacity: 0.5; cursor: not-allowed; }

.debug-history-section { padding-top: 15px; border-top: 1px solid var(--line); }

/* 版本历史 */
.version-list { margin-top: 10px; }
.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}
.version-item:hover { background: #f5f8fb; }
.version-info b { color: #1967d2; }
.version-time { margin-left: 10px; color: #8490a2; font-size: 12px; }
.version-summary { margin-left: 10px; color: #5d6b80; }

.version-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
  font-size: 13px;
}
.version-actions select { padding: 4px 8px; border: 1px solid var(--line); border-radius: 4px; }

/* Diff 视图 */
.diff-view { margin-top: 15px; border: 1px solid var(--line); border-radius: 6px; background: #fff; }
.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  background: #f5f8fb;
}
.diff-file { border-bottom: 1px solid var(--line); }
.diff-file:last-child { border-bottom: 0; }
.diff-file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafbfc;
  font-size: 13px;
}
.diff-status { padding: 2px 8px; border-radius: 3px; font-size: 12px; }
.diff-status.added { background: #e8f5e9; color: #1b5e20; }
.diff-status.deleted { background: #ffebee; color: #c62828; }
.diff-status.modified { background: #fff3e0; color: #e65100; }
.diff-status.unchanged { background: #f5f5f5; color: #757575; }
.diff-content { display: flex; gap: 1px; }
.diff-side { flex: 1; padding: 8px 12px; }
.diff-side .diff-label { display: block; font-size: 11px; color: #8490a2; margin-bottom: 5px; }
.diff-side pre { margin: 0; font-size: 12px; font-family: ui-monospace, monospace; white-space: pre-wrap; word-break: break-all; }
</style>
