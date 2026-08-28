<template>
  <div>
    <section class="page-head">
      <strong class="page-title">审核中心</strong>
      <span class="chip">Skill 评审</span>
      <div class="actions">
        <select v-model="statusFilter" @change="load" class="status-filter">
          <option value="pending">待处理</option>
          <option value="approved">已通过</option>
          <option value="rejected">已驳回</option>
          <option value="">全部</option>
        </select>
        <button class="btn" @click="load">刷新</button>
      </div>
    </section>
    <section class="page-content">
      <div class="notice">提交评审不会定版。只有"通过"后，平台才创建 Git tag、发布包并将 Skill 标记为已发布。提交评审时会附带调试结果作为审核依据。</div>
      <section class="panel">
        <h3>{{ statusLabel }}评审</h3>
        <div v-if="loading" class="pad hint">正在加载评审记录…</div>
        <div v-else-if="!reviews.length" class="pad hint">暂无{{ statusLabel }}的 Skill 评审。</div>
        <div v-else class="table-wrap"><table>
          <thead><tr><th>Skill</th><th>版本</th><th>申请人</th><th>调试结果</th><th>评审状态</th><th>申请时间</th><th>操作</th></tr></thead>
          <tbody><tr v-for="item in reviews" :key="item.id">
            <td>{{ item.skillName }}</td>
            <td>{{ item.version || '—' }}</td>
            <td>{{ item.applicantName || '#' + item.applicantId }}</td>
            <td>
              <span v-if="item.debugPassed === true" class="debug-tag pass">✓ 通过</span>
              <span v-else-if="item.debugPassed === false" class="debug-tag fail">✗ 失败</span>
              <span v-else class="debug-tag none">未调试</span>
              <small v-if="item.debugSummary" class="debug-summary">{{ item.debugSummary }}</small>
            </td>
            <td>
              <span v-if="item.status === 'approved'" class="debug-tag pass">已通过</span>
              <span v-else-if="item.status === 'rejected'" class="debug-tag fail">已驳回</span>
              <span v-else class="debug-tag none">待处理</span>
            </td>
            <td>{{ formatTime(item.createTime) }}</td>
            <td class="actions-cell">
              <button class="btn" @click="openReviewDetail(item)">{{ item.status === 'pending' ? '审核' : '查看' }}</button>
            </td>
          </tr></tbody>
        </table></div>
        <div v-if="message" class="pad hint" :class="messageClass">{{ message }}</div>
      </section>
    </section>

    <!-- 评审详情弹窗 -->
    <div v-if="showReviewDetail" class="modal-mask" @click.self="showReviewDetail = false">
      <div class="modal-box review-detail-modal">
        <div class="modal-head">
          <b>评审详情：{{ selectedReview?.skillName }}</b>
          <button class="close-btn" @click="showReviewDetail = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h4>Skill 信息</h4>
            <p><b>版本：</b>{{ selectedReview?.version || '—' }}</p>
            <p><b>申请人：</b>{{ selectedReview?.applicantName || '#' + selectedReview?.applicantId }}</p>
            <p><b>申请时间：</b>{{ formatTime(selectedReview?.createTime) }}</p>
          </div>
          <div class="detail-section">
            <h4>调试结果</h4>
            <div class="debug-result">
              <p><b>调试状态：</b>
                <span v-if="selectedReview?.debugPassed === true" class="debug-tag pass">✓ 通过</span>
                <span v-else-if="selectedReview?.debugPassed === false" class="debug-tag fail">✗ 失败</span>
                <span v-else class="debug-tag none">未提供</span>
              </p>
              <p v-if="selectedReview?.debugSummary"><b>调试摘要：</b>{{ selectedReview.debugSummary }}</p>
              <p v-if="selectedReview?.testPassCount != null"><b>测试用例：</b>{{ selectedReview.testPassCount }}/{{ selectedReview.testCaseCount }} 通过</p>
              <p v-if="selectedReview?.totalTokens"><b>Token 消耗：</b>{{ selectedReview.totalTokens }}</p>
            </div>
          </div>
          <div class="detail-section" v-if="selectedReview?.debugInput">
            <h4>调试输入</h4>
            <pre class="debug-input">{{ selectedReview.debugInput }}</pre>
          </div>
          <div class="detail-section">
            <h4>评审意见</h4>
            <textarea v-if="selectedReview?.status === 'pending'" v-model="reviewComment" class="input textarea" rows="3" placeholder="请输入评审意见..."></textarea>
            <p v-else class="review-comment-display">{{ selectedReview?.comment || '无' }}</p>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" @click="showReviewDetail = false">关闭</button>
          <template v-if="selectedReview?.status === 'pending'">
            <button class="btn reject" @click="decide(selectedReview!, 'rejected')">驳回</button>
            <button class="btn primary" @click="decide(selectedReview!, 'approved')">通过并定版</button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { skillApi, type SkillReview } from '@/api'

const reviews = ref<SkillReview[]>([])
const loading = ref(false)
const message = ref('')
const messageClass = ref('')
const showReviewDetail = ref(false)
const selectedReview = ref<SkillReview | null>(null)
const reviewComment = ref('')
const statusFilter = ref('pending')

const statusLabel = computed(() => {
  const map: Record<string, string> = { pending: '待处理', approved: '已通过', rejected: '已驳回', '': '全部' }
  return map[statusFilter.value] || '待处理'
})

const formatTime = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '—'

async function load() {
  loading.value = true
  message.value = ''
  try { reviews.value = await skillApi.listReviews(statusFilter.value || undefined) } catch (error: any) { message.value = `加载失败：${error.message}` } finally { loading.value = false }
}

function openReviewDetail(item: SkillReview) {
  selectedReview.value = item
  reviewComment.value = ''
  showReviewDetail.value = true
}

async function decide(item: SkillReview, decision: 'approved' | 'rejected') {
  if (decision === 'rejected' && !reviewComment.value?.trim()) {
    alert('请填写驳回原因')
    return
  }
  try {
    await skillApi.decideReview(String(item.id), decision, reviewComment.value || undefined)
    message.value = decision === 'approved' ? `${item.skillName} 已通过评审并定版发布。` : `${item.skillName} 已驳回并退回草稿。`
    messageClass.value = decision === 'approved' ? 'success' : 'error'
    showReviewDetail.value = false
    await load()
  } catch (error: any) { message.value = `处理失败：${error.message}`; messageClass.value = 'error' }
}

onMounted(load)
</script>

<style scoped>
.actions-cell { display:flex; gap:8px; align-items:center; }

.debug-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  margin-right: 5px;
}
.debug-tag.pass { background: #e8f5e9; color: #1b5e20; }
.debug-tag.fail { background: #ffebee; color: #c62828; }
.debug-tag.none { background: #f5f5f5; color: #757575; }
.debug-summary { display: block; font-size: 11px; color: #8490a2; margin-top: 2px; }

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
  width: min(600px, 100%);
  max-height: 80vh;
  overflow-y: auto;
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
.modal-body { display: grid; gap: 16px; padding: 20px; }
.modal-foot { justify-content: flex-end; gap: 8px; border-top: 1px solid #e6ebf2; }
.close-btn {
  border: 0;
  background: none;
  color: #7d899a;
  cursor: pointer;
  font-size: 24px;
}
.detail-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #253650;
  border-bottom: 1px solid #edf1f5;
  padding-bottom: 6px;
}
.detail-section p {
  margin: 4px 0;
  color: #526176;
  font-size: 13px;
}
.debug-result { padding: 10px; background: #f5f8fb; border-radius: 4px; }
.debug-input {
  padding: 10px;
  background: #f5f8fb;
  border-radius: 4px;
  font-size: 12px;
  max-height: 150px;
  overflow: auto;
}
.textarea { resize: vertical; width: 100%; }
.btn.reject { border-color: #c62828; color: #c62828; }
.pad.hint.success { color: #1b5e20; background: #e8f5e9; border-radius: 4px; padding: 10px; }
.pad.hint.error { color: #c62828; background: #ffebee; border-radius: 4px; padding: 10px; }
.status-filter { padding: 5px 10px; border: 1px solid #cfd9e6; border-radius: 4px; font-size: 13px; }
.review-comment-display { padding: 10px; background: #f5f8fb; border-radius: 4px; color: #526176; font-size: 13px; }
</style>
