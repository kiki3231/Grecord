<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useBacklogStore, type BacklogItem } from '@/stores/backlog'
import draggable from 'vuedraggable'

const backlogStore = useBacklogStore()
const activeTab = ref<number | undefined>(undefined)
const showCheckinModal = ref(false)
const checkinTarget = ref<BacklogItem | null>(null)
const checkinHours = ref(0)
const checkinMinutes = ref(30)
const checkinRating = ref(8)
/** 与今日打卡页一致：1在玩 2通关 3搁置 4白金 */
const checkinRecordStatus = ref(1)
const checkinNotes = ref('')
const checkinSubmitting = ref(false)
const submitError = ref('')

const checkinStatusOptions = [
  { value: 1, label: '在玩', icon: '🎮' },
  { value: 2, label: '通关', icon: '🏆' },
  { value: 3, label: '搁置', icon: '⏸️' },
  { value: 4, label: '白金', icon: '💎' },
]

const checkinRatingLabel = computed(() => {
  const r = checkinRating.value
  if (r >= 9.5) return '神作 ✦'
  if (r >= 8.5) return '极佳 ★'
  if (r >= 7) return '好玩 ♪'
  if (r >= 5) return '一般 ～'
  return '略差 ▽'
})

onMounted(() => {
  backlogStore.fetchBacklog()
})

const tabs = [
  { value: undefined, label: '全部' },
  { value: 0, label: '想玩' },
  { value: 1, label: '在玩' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已搁置' },
  { value: 4, label: '已白金' }
]

function switchTab(status: number | undefined) {
  activeTab.value = status
  backlogStore.fetchBacklog(status)
}

function getCount(status: number) {
  return backlogStore.statusCounts[status] || 0
}

const totalCount = computed(() => {
  return Object.values(backlogStore.statusCounts).reduce((a: number, b) => a + (b as number), 0)
})

async function onStatusChange(item: BacklogItem, newStatus: number | null) {
  await backlogStore.updateStatus(item.id, newStatus as number)
}

async function removeItem(item: BacklogItem) {
  const name = item.game?.name || '该游戏'
  if (!confirm(`确定要从清单中移除「${name}」吗？`)) return
  try {
    const res = await backlogStore.removeItem(item.id)
    if (!res || res.code !== 200) {
      alert(res?.msg || '移除失败，请稍后重试')
    }
  } catch (err) {
    console.error('[Backlog.removeItem] 网络/服务异常：', err)
    alert('移除失败，请检查网络或稍后重试')
  }
}

function openCheckin(item: BacklogItem) {
  checkinTarget.value = item
  checkinHours.value = 0
  checkinMinutes.value = 30
  checkinRating.value = 8
  submitError.value = ''
  const s = item.status
  if (s === 0 || s === null || s === undefined) checkinRecordStatus.value = 1
  else if (s === 1) checkinRecordStatus.value = 1
  else if (s === 2) checkinRecordStatus.value = 2
  else if (s === 3) checkinRecordStatus.value = 3
  else if (s === 4) checkinRecordStatus.value = 4
  else checkinRecordStatus.value = 1
  checkinNotes.value = ''
  showCheckinModal.value = true
}

async function submitCheckin() {
  if (!checkinTarget.value) return
  const totalMinutes = checkinHours.value * 60 + checkinMinutes.value
  if (totalMinutes <= 0) {
    submitError.value = '游戏时长需要大于 0'
    return
  }
  submitError.value = ''
  checkinSubmitting.value = true
  try {
    const res = await backlogStore.checkin(
      checkinTarget.value.id,
      {
        playTime: totalMinutes,
        rating: checkinRating.value,
        status: checkinRecordStatus.value,
        notes: checkinNotes.value || undefined,
      },
      activeTab.value,
    )
    if (res?.code === 200) {
      showCheckinModal.value = false
      checkinTarget.value = null
    } else {
      submitError.value = res?.msg || '打卡失败，请稍后重试'
    }
  } catch {
    submitError.value = '网络异常，请稍后重试'
  } finally {
    checkinSubmitting.value = false
  }
}

async function onDragEnd() {
  const sortList = backlogStore.items.map((item, idx) => ({
    id: item.id,
    sortOrder: idx
  }))
  await backlogStore.batchSort(sortList)
}

const statusLabels: Record<number, string> = { 0: '想玩', 1: '在玩', 2: '已完成', 3: '已搁置', 4: '已白金' }
const statusColors: Record<number, string> = { 0: 'var(--neon-blue)', 1: 'var(--neon-cyan)', 2: 'var(--neon-green)', 3: 'var(--text-muted)', 4: 'var(--neon-purple)' }

function getStatusLabel(status: number | null): string {
  if (status === null || status === undefined) return '未分类'
  return statusLabels[status] || '未知'
}
function getStatusColor(status: number | null): string {
  if (status === null || status === undefined) return 'var(--text-muted)'
  return statusColors[status] || 'var(--text-secondary)'
}

function nextStatus(current: number | null): number | null {
  if (current === null || current === undefined) return null
  const map: Record<number, number> = { 1: 2, 3: 1 }
  return map[current] ?? null
}

function nextStatusLabel(current: number | null): string {
  if (current === null || current === undefined) return ''
  const map: Record<number, string> = { 1: '通关', 3: '继续玩' }
  return map[current] ?? ''
}
</script>

<template>
  <div class="backlog-page">
    <h1 class="page-title">游戏清单</h1>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="String(tab.value)"
        :class="['tab-btn', { active: activeTab === tab.value, platinum: tab.value === 4 }]"
        @click="switchTab(tab.value)"
      >
        <span v-if="tab.value === 4">💎</span>
        {{ tab.label }}
        <span class="badge">{{ tab.value === undefined ? totalCount : getCount(tab.value) }}</span>
      </button>
    </div>

    <draggable
      v-model="backlogStore.items"
      item-key="id"
      class="backlog-list"
      ghost-class="ghost"
      @end="onDragEnd"
    >
      <template #item="{ element }">
        <div class="backlog-item">
          <div class="drag-handle">⠿</div>
          <img v-if="element.game?.icon" :src="element.game.icon" class="game-thumb" alt="" />
          <div class="placeholder-thumb" v-else>🎮</div>
          <div class="item-info">
            <div class="item-name">{{ element.game?.name || '未知游戏' }}</div>
            <div class="item-meta">
              <span v-if="element.game?.platforms" class="platform-tag">{{ element.game.platforms }}</span>
              <span class="status-tag" :style="{ color: getStatusColor(element.status) }">{{ getStatusLabel(element.status) }}</span>
            </div>
          </div>
          <div class="item-actions">
            <button v-if="nextStatus(element.status) !== null" class="action-btn flow" @click="onStatusChange(element, nextStatus(element.status) ?? 0)">
              {{ nextStatusLabel(element.status) }}
            </button>
            <button class="action-btn checkin" @click="openCheckin(element)">打卡</button>
            <button class="action-btn remove" @click="removeItem(element)">移除</button>
          </div>
        </div>
      </template>
    </draggable>

    <p v-if="!backlogStore.loading && backlogStore.items.length === 0" class="empty">
      清单为空，去搜索游戏并添加到清单吧
    </p>

    <Teleport to="body">
      <div
        v-if="showCheckinModal"
        class="chk-modal-backdrop"
        aria-hidden="false"
        @click.self="showCheckinModal = false"
      >
        <div
          class="chk-modal-panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="chk-modal-title"
          @click.stop
        >
        <div class="chk-modal-head">
          <p class="chk-modal-eyebrow">清单内打卡</p>
          <h3 id="chk-modal-title" class="chk-modal-title">填写本次游玩</h3>
          <p class="chk-modal-game">{{ checkinTarget?.game?.name || '未知游戏' }}</p>
          <p v-if="checkinTarget" class="chk-modal-meta">
            当前清单状态：<span class="chk-meta-badge">{{ getStatusLabel(checkinTarget.status) }}</span>
          </p>
        </div>

        <div class="chk-form">
          <div class="chk-group">
            <label class="chk-label">游戏时长</label>
            <div class="chk-time-row">
              <div class="chk-time-cell">
                <input v-model.number="checkinHours" type="number" min="0" max="999" class="chk-num" />
                <span class="chk-unit">小时</span>
              </div>
              <span class="chk-colon">:</span>
              <div class="chk-time-cell">
                <input v-model.number="checkinMinutes" type="number" min="0" max="59" class="chk-num" />
                <span class="chk-unit">分钟</span>
              </div>
            </div>
          </div>

          <div class="chk-group">
            <label class="chk-label">评分 <span class="chk-rating-hint">{{ checkinRating }} / 10 · {{ checkinRatingLabel }}</span></label>
            <input
              v-model.number="checkinRating"
              type="range"
              min="1"
              max="10"
              step="0.5"
              class="chk-range"
              :style="{ '--pct': ((checkinRating - 1) / 9 * 100) + '%' }"
            />
            <div class="chk-ticks">
              <span v-for="n in [1, 3, 5, 7, 9, 10]" :key="n" class="chk-tick">{{ n }}</span>
            </div>
          </div>

          <div class="chk-group chk-full">
            <label class="chk-label">游戏状态（打卡后清单将同步为此状态）</label>
            <div class="chk-pills">
              <button
                v-for="opt in checkinStatusOptions"
                :key="opt.value"
                type="button"
                :class="['chk-status-btn', { active: checkinRecordStatus === opt.value }]"
                @click="checkinRecordStatus = opt.value"
              >
                <span>{{ opt.icon }}</span> {{ opt.label }}
              </button>
            </div>
          </div>

          <div class="chk-group chk-full">
            <label class="chk-label">感想备注</label>
            <textarea
              v-model="checkinNotes"
              class="chk-notes"
              rows="3"
              placeholder="写下今天的游戏感想…"
            />
          </div>

          <p v-if="submitError" class="chk-err">{{ submitError }}</p>
        </div>

        <div class="chk-actions">
          <button type="button" class="chk-submit" :disabled="checkinSubmitting" @click="submitCheckin">
            {{ checkinSubmitting ? '提交中…' : '提交打卡 ✨' }}
          </button>
          <button type="button" class="chk-cancel" :disabled="checkinSubmitting" @click="showCheckinModal = false">取消</button>
        </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.backlog-page { max-width: 900px; }

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 8px 18px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: var(--transition-fast);

  &:hover { border-color: var(--neon-cyan); color: var(--text-primary); }
  &.active {
    border-color: var(--neon-cyan);
    background: rgba(0, 240, 255, 0.1);
    color: var(--neon-cyan);
  }

  &.platinum {
    &:hover { border-color: var(--neon-purple); }
    &.active {
      border-color: var(--neon-purple);
      background: rgba(167, 139, 250, 0.12);
      color: var(--neon-purple);
      box-shadow: 0 0 12px rgba(167, 139, 250, 0.2);
    }
  }
}

.badge {
  font-size: 11px;
  font-family: var(--font-mono);
  background: var(--bg-secondary);
  padding: 1px 6px;
  border-radius: 10px;
}

.backlog-list { display: flex; flex-direction: column; gap: 8px; }

.backlog-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  transition: var(--transition-fast);

  &:hover { border-color: var(--border-glow); background: var(--bg-card-hover); }
}

.ghost { opacity: 0.4; }

.drag-handle {
  cursor: grab;
  color: var(--text-muted);
  font-size: 18px;
  line-height: 1;
  user-select: none;
  &:active { cursor: grabbing; }
}

.game-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background: var(--bg-secondary);
  flex-shrink: 0;
}

.placeholder-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.item-info { flex: 1; min-width: 0; }
.item-name { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-meta { display: flex; gap: 8px; align-items: center; margin-top: 4px; }
.platform-tag { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.status-tag { font-size: 12px; font-weight: 600; }

.item-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn {
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-input);
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  transition: var(--transition-fast);
  white-space: nowrap;

  &:hover { border-color: var(--neon-cyan); color: var(--text-primary); }
  &.flow { color: var(--neon-cyan); border-color: rgba(0, 240, 255, 0.3); }
  &.checkin { color: var(--neon-green); border-color: rgba(57, 255, 20, 0.3); }
  &.remove { color: var(--neon-pink); border-color: rgba(255, 45, 107, 0.3); &:hover { background: rgba(255, 45, 107, 0.1); } }
}

.empty { text-align: center; color: var(--text-muted); padding: 40px; font-size: 14px; }

/* 挂到 body，避免侧栏/主内容区的 transform、overflow 影响叠层与裁切 */
.chk-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 10050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(16px, env(safe-area-inset-bottom));
  background: rgba(6, 4, 14, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* 不用 --bg-card：设计里为极低透明度 rgba，弹窗必须用实底 */
.chk-modal-panel {
  width: 100%;
  max-width: 520px;
  max-height: min(90vh, 640px);
  overflow-x: hidden;
  overflow-y: auto;
  padding: 22px 24px 20px;
  border-radius: var(--border-radius-lg);
  border: 1px solid rgba(200, 160, 255, 0.22);
  background: var(--bg-secondary);
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.35),
    0 28px 80px rgba(0, 0, 0, 0.65),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  color: var(--text-primary);
}

.chk-modal-head {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.chk-modal-eyebrow {
  margin: 0;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.chk-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.35;
}

.chk-modal-game {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--neon-cyan);
  line-height: 1.45;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.chk-modal-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.chk-meta-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(0, 240, 255, 0.12);
  border: 1px solid rgba(0, 240, 255, 0.35);
  color: var(--neon-cyan);
  font-weight: 700;
}

.chk-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chk-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chk-group.chk-full {
  width: 100%;
}

.chk-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.03em;
}

.chk-rating-hint {
  font-weight: 800;
  color: var(--amber);
  margin-left: 4px;
}

.chk-time-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chk-time-cell {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.chk-colon {
  font-weight: 900;
  color: var(--text-muted);
  font-size: 18px;
  flex-shrink: 0;
}

.chk-num {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 800;
  text-align: center;
  outline: none;
  &:focus {
    border-color: var(--neon-cyan);
  }
}

.chk-unit {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.chk-range {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  border-radius: 999px;
  outline: none;
  background: linear-gradient(
    to right,
    var(--accent) 0%,
    var(--neon-cyan) var(--pct, 50%),
    var(--bg-input) var(--pct, 50%),
    var(--bg-input) 100%
  );
  border: 1px solid var(--border-color);
  cursor: pointer;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--accent), var(--neon-cyan));
    box-shadow: 0 0 10px rgba(255, 110, 181, 0.5);
    cursor: pointer;
  }
}

.chk-ticks {
  display: flex;
  justify-content: space-between;
  padding: 0 2px;
}

.chk-tick {
  font-size: 9px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.chk-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chk-status-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-input);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition-fast);

  &:hover {
    border-color: var(--neon-cyan);
    color: var(--text-primary);
  }

  &.active {
    border-color: var(--accent);
    color: var(--accent);
    background: rgba(255, 110, 181, 0.1);
    box-shadow: 0 0 12px rgba(255, 110, 181, 0.2);
  }
}

.chk-notes {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 14px;
  resize: vertical;
  outline: none;
  line-height: 1.5;
  &:focus {
    border-color: var(--neon-cyan);
  }
  &::placeholder {
    color: var(--text-muted);
  }
}

.chk-err {
  margin: 0;
  font-size: 13px;
  color: var(--rose);
  font-weight: 600;
}

.chk-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.chk-submit {
  flex: 2;
  padding: 12px 16px;
  border: none;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, var(--neon-cyan), var(--neon-purple));
  color: var(--bg-primary);
  font-weight: 800;
  font-size: 14px;
  cursor: pointer;
  &:hover:not(:disabled) {
    box-shadow: 0 4px 20px rgba(0, 240, 255, 0.35);
  }
  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.chk-cancel {
  flex: 1;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background: var(--bg-input);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  &:hover:not(:disabled) {
    border-color: var(--text-secondary);
  }
  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}
</style>
