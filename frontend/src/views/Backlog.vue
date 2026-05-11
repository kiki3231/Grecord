<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useBacklogStore, type BacklogItem } from '@/stores/backlog'
import draggable from 'vuedraggable'

const backlogStore = useBacklogStore()
const activeTab = ref<number | undefined>(undefined)
const showCheckinModal = ref(false)
const checkinTarget = ref<BacklogItem | null>(null)
const checkinPlayTime = ref(60)
const checkinNotes = ref('')

onMounted(() => {
  backlogStore.fetchBacklog()
})

const tabs = [
  { value: undefined, label: '全部' },
  { value: 0, label: '想玩' },
  { value: 1, label: '在玩' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已搁置' }
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

async function onStatusChange(item: BacklogItem, newStatus: number) {
  await backlogStore.updateStatus(item.id, newStatus)
}

async function removeItem(item: BacklogItem) {
  if (confirm(`确定要从清单中移除「${item.game?.name}」吗？`)) {
    await backlogStore.removeItem(item.id)
  }
}

function openCheckin(item: BacklogItem) {
  checkinTarget.value = item
  checkinPlayTime.value = 60
  checkinNotes.value = ''
  showCheckinModal.value = true
}

async function submitCheckin() {
  if (!checkinTarget.value) return
  await backlogStore.checkin(checkinTarget.value.id, checkinPlayTime.value, checkinNotes.value)
  showCheckinModal.value = false
  checkinTarget.value = null
}

async function onDragEnd() {
  const sortList = backlogStore.items.map((item, idx) => ({
    id: item.id,
    sortOrder: idx
  }))
  await backlogStore.batchSort(sortList)
}

const statusLabels: Record<number, string> = { 0: '想玩', 1: '在玩', 2: '已完成', 3: '已搁置' }
const statusColors: Record<number, string> = { 0: 'var(--neon-blue)', 1: 'var(--neon-cyan)', 2: 'var(--neon-green)', 3: 'var(--text-muted)' }

function nextStatus(current: number): number | null {
  const map: Record<number, number> = { 0: 1, 1: 2, 3: 1 }
  return map[current] ?? null
}

function nextStatusLabel(current: number): string {
  const map: Record<number, string> = { 0: '开始玩', 1: '通关', 3: '继续玩' }
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
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="switchTab(tab.value)"
      >
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
              <span class="status-tag" :style="{ color: statusColors[element.status] }">{{ statusLabels[element.status] }}</span>
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

    <div v-if="showCheckinModal" class="modal-overlay" @click.self="showCheckinModal = false">
      <div class="modal-card">
        <h3>快速打卡</h3>
        <p class="modal-game-name">{{ checkinTarget?.game?.name }}</p>
        <div class="modal-form">
          <label>游戏时长（分钟）</label>
          <input v-model.number="checkinPlayTime" type="number" min="1" class="form-input" />
          <label>备注</label>
          <textarea v-model="checkinNotes" class="form-textarea" rows="2" placeholder="写点什么..."></textarea>
        </div>
        <div class="modal-actions">
          <button class="submit-btn" @click="submitCheckin">确认打卡</button>
          <button class="cancel-btn" @click="showCheckinModal = false">取消</button>
        </div>
      </div>
    </div>
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

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(10, 10, 15, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  padding: 28px;
  width: 400px;
  max-width: 90vw;
  h3 { font-size: 18px; margin-bottom: 8px; color: var(--text-primary); }
}

.modal-game-name { color: var(--neon-cyan); font-size: 14px; margin-bottom: 16px; }

.modal-form {
  label {
    display: block;
    font-size: 13px;
    color: var(--text-secondary);
    margin-bottom: 6px;
    margin-top: 12px;
    font-weight: 500;
  }
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  &:focus { border-color: var(--neon-cyan); }
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 14px;
  resize: vertical;
  outline: none;
  &:focus { border-color: var(--neon-cyan); }
  &::placeholder { color: var(--text-muted); }
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.submit-btn {
  flex: 1;
  padding: 10px;
  background: linear-gradient(135deg, var(--neon-cyan), var(--neon-purple));
  border: none;
  border-radius: var(--border-radius);
  color: var(--bg-primary);
  font-weight: 700;
  font-size: 14px;
  &:hover { box-shadow: 0 4px 20px rgba(0, 240, 255, 0.3); }
}

.cancel-btn {
  flex: 1;
  padding: 10px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  font-size: 14px;
  &:hover { border-color: var(--text-secondary); }
}
</style>
