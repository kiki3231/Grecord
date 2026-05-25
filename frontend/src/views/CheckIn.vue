<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { useGameStore, type Game } from '@/stores/game'
import { useRecordStore } from '@/stores/record'
import CheckInRail from '@/components/checkin/CheckInRail.vue'
import {
  buildQuickPicks,
  buildGameHistory,
  formatPlayMinutes
} from '@/utils/checkinRecords'

const gameStore = useGameStore()
const recordStore = useRecordStore()

const railLoading = ref(true)
const railError = ref(false)

const keyword = ref('')
const selectedGame = ref<Game | null>(null)
const showResults = ref(false)
const hours = ref(0)
const minutes = ref(30)
const rating = ref(8)
const status = ref(1)
const notes = ref('')
const showSuccess = ref(false)
const submitError = ref('')

/** 快捷选游戏 / 下拉点选时写入 keyword，不触发搜索请求 */
const suppressKeywordSearch = ref(false)

onMounted(async () => {
  railLoading.value = true
  railError.value = false
  const ok = await recordStore.fetchRecentRecords()
  railError.value = !ok
  railLoading.value = false
})

const quickPicks = computed(() => buildQuickPicks(recordStore.recentRecords, 5))

const gameHistory = computed(() => {
  if (!selectedGame.value) return null
  return buildGameHistory(recordStore.recentRecords, selectedGame.value.id)
})

const previewMinutesLabel = computed(() => {
  const total = Math.max(0, (hours.value || 0) * 60 + (minutes.value || 0))
  return formatPlayMinutes(total)
})

const railProps = computed(() => ({
  quickPicks: quickPicks.value,
  selectedGame: selectedGame.value,
  previewMinutesLabel: previewMinutesLabel.value,
  previewRating: rating.value,
  previewRatingLabel: ratingLabel.value,
  previewStatus: status.value,
  previewNotes: notes.value,
  gameHistory: gameHistory.value,
  loading: railLoading.value,
  railError: railError.value,
  disabled: recordStore.loading
}))

function pickQuickGame(game: Game) {
  if (recordStore.loading) return
  selectGame(game)
  submitError.value = ''
}

let searchTimer: ReturnType<typeof setTimeout> | null = null

function cancelPendingSearch() {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
}

watch(keyword, (val) => {
  if (suppressKeywordSearch.value) {
    suppressKeywordSearch.value = false
    return
  }
  if (selectedGame.value && val.trim() !== selectedGame.value.name) {
    selectedGame.value = null
  }
  cancelPendingSearch()
  if (!val.trim()) {
    showResults.value = false
    gameStore.searchResults = []
    return
  }
  searchTimer = setTimeout(() => {
    gameStore.searchGames(val)
    showResults.value = true
  }, 300)
})

function selectGame(game: Game) {
  cancelPendingSearch()
  suppressKeywordSearch.value = true
  selectedGame.value = game
  keyword.value = game.name
  showResults.value = false
  gameStore.searchResults = []
}

function onSearchFocus() {
  if (selectedGame.value) return
  if (keyword.value.trim()) showResults.value = true
}

function clearSelection() {
  cancelPendingSearch()
  selectedGame.value = null
  keyword.value = ''
  showResults.value = false
  gameStore.searchResults = []
}

async function submit() {
  if (!selectedGame.value) { submitError.value = '请先选择一个游戏'; return }
  const totalMinutes = hours.value * 60 + minutes.value
  if (totalMinutes <= 0) { submitError.value = '游戏时长需要大于 0'; return }
  submitError.value = ''

  const res = await recordStore.createRecord({
    gameId: selectedGame.value.id,
    playTime: totalMinutes,
    rating: rating.value,
    status: status.value,
    notes: notes.value || undefined
  })

  if (res && res.code === 200) {
    showSuccess.value = true
    await recordStore.fetchRecentRecords()
    setTimeout(() => {
      showSuccess.value = false
      selectedGame.value = null
      keyword.value = ''
      hours.value = 0
      minutes.value = 30
      rating.value = 8
      status.value = 1
      notes.value = ''
    }, 2400)
  } else {
    submitError.value = res?.msg || '打卡失败，请重试'
  }
}

const statusOptions = [
  { value: 1, label: '在玩',  icon: '🎮' },
  { value: 2, label: '通关',  icon: '🏆' },
  { value: 3, label: '搁置',  icon: '⏸️' },
  { value: 4, label: '白金',  icon: '💎' }
]

const ratingLabel = computed(() => {
  const r = rating.value
  if (r >= 9.5) return '神作 ✦'
  if (r >= 8.5) return '极佳 ★'
  if (r >= 7)   return '好玩 ♪'
  if (r >= 5)   return '一般 ～'
  return '略差 ▽'
})
</script>

<template>
  <div class="checkin-shell">

    <!-- ── Success overlay ── -->
    <Transition name="success-fade">
      <div v-if="showSuccess" class="success-overlay">
        <div class="success-card">
          <div class="success-sparkles">
            <span class="ss sp1">✦</span>
            <span class="ss sp2">★</span>
            <span class="ss sp3">✧</span>
          </div>
          <div class="success-icon">🎉</div>
          <div class="success-title">打卡成功！</div>
          <div class="success-sub">游玩记录已保存 ✨</div>
        </div>
      </div>
    </Transition>

    <div class="checkin-main">
    <!-- ── Page header ── -->
    <div class="page-header">
      <span class="page-eyebrow">✦ CHECK IN</span>
      <h1 class="page-title">今日打卡</h1>
      <p class="page-sub">记录你今天的游戏旅程</p>
    </div>

    <!-- 窄屏：辅助栏在表单前 -->
    <div class="checkin-rail-inline">
      <CheckInRail v-bind="railProps" @pick="pickQuickGame" />
    </div>

    <!-- ── Form card ── -->
    <div class="checkin-card">

      <!-- ── Step 1: Select game ── -->
      <div class="step">
        <div class="step-label">
          <div class="step-num">1</div>
          <span class="step-text">选择游戏</span>
        </div>

        <div class="search-area">
          <label class="search-field">
            <svg class="search-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
            </svg>
            <input
              v-model="keyword"
              class="search-input"
              placeholder="搜索游戏名称…"
              @focus="onSearchFocus"
              autocomplete="off"
            />
            <button v-if="selectedGame || keyword" class="clear-btn" type="button" @click="clearSelection">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="13" height="13">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </label>

          <!-- Dropdown -->
          <Transition name="dropdown-fade">
            <div
              v-if="showResults && (gameStore.searchResults.length > 0 || gameStore.searchLoading)"
              class="search-dropdown"
            >
              <div v-if="gameStore.searchLoading" class="search-loading">
                <span class="loading-dot" /><span class="loading-dot" /><span class="loading-dot" />
              </div>
              <div
                v-for="game in gameStore.searchResults"
                :key="game.id"
                class="search-item"
                @click="selectGame(game)"
              >
                <div class="si-thumb">
                  <img v-if="game.icon" :src="game.icon" :alt="game.name" />
                  <span v-else class="si-fb">🎮</span>
                </div>
                <div class="si-info">
                  <div class="si-name">{{ game.name }}</div>
                  <div class="si-meta">
                    <span v-if="game.platforms">{{ game.platforms.split(',')[0] }}</span>
                    <span v-if="game.gameTypes"> · {{ game.gameTypes.split(',')[0] }}</span>
                  </div>
                </div>
                <div v-if="game.rating" class="si-rating">
                  <svg viewBox="0 0 20 20" fill="currentColor" width="9" height="9"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
                  {{ Number(game.rating).toFixed(1) }}
                </div>
              </div>
            </div>
          </Transition>

          <div
            v-if="showResults && keyword.trim() && gameStore.searchResults.length === 0 && !gameStore.searchLoading"
            class="search-empty"
          >
            <span>🔍</span> 没有找到相关游戏
          </div>
        </div>

        <!-- Selected game preview -->
        <Transition name="selected-slide">
          <div v-if="selectedGame" class="selected-game">
            <div class="sg-thumb">
              <img v-if="selectedGame.icon" :src="selectedGame.icon" :alt="selectedGame.name" />
              <span v-else>🎮</span>
            </div>
            <div class="sg-info">
              <div class="sg-name">{{ selectedGame.name }}</div>
              <div class="sg-meta">
                <span v-if="selectedGame.platforms">{{ selectedGame.platforms.split(',')[0] }}</span>
                <span v-if="selectedGame.rating"> · ⭐ {{ Number(selectedGame.rating).toFixed(1) }}</span>
              </div>
            </div>
            <div class="sg-check">✓</div>
          </div>
        </Transition>
      </div>

      <!-- ── Step 2: Details ── -->
      <div class="step">
        <div class="step-label">
          <div class="step-num">2</div>
          <span class="step-text">填写详情</span>
        </div>

        <div class="form-grid">

          <!-- Play time -->
          <div class="form-group">
            <label class="form-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" width="13" height="13"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              游戏时长
            </label>
            <div class="time-inputs">
              <div class="time-field">
                <input v-model.number="hours" type="number" min="0" max="24" class="num-input" />
                <span class="time-unit">小时</span>
              </div>
              <div class="time-sep">:</div>
              <div class="time-field">
                <input v-model.number="minutes" type="number" min="0" max="59" class="num-input" />
                <span class="time-unit">分钟</span>
              </div>
            </div>
          </div>

          <!-- Rating -->
          <div class="form-group">
            <label class="form-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" width="13" height="13"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
              评分 <span class="rating-display">{{ rating }} / 10 · {{ ratingLabel }}</span>
            </label>
            <div class="range-wrap">
              <input
                v-model.number="rating"
                type="range"
                min="1"
                max="10"
                step="0.5"
                class="range-input"
                :style="{ '--pct': ((rating - 1) / 9 * 100) + '%' }"
              />
              <div class="range-ticks">
                <span v-for="n in [1,3,5,7,9,10]" :key="n" class="tick">{{ n }}</span>
              </div>
            </div>
          </div>

          <!-- Status -->
          <div class="form-group full">
            <label class="form-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" width="13" height="13"><path d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
              游戏状态
            </label>
            <div class="status-pills">
              <button
                v-for="opt in statusOptions"
                :key="opt.value"
                type="button"
                :class="['status-pill', { active: status === opt.value }]"
                @click="status = opt.value"
              >
                <span class="pill-icon">{{ opt.icon }}</span>
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- Notes -->
          <div class="form-group full">
            <label class="form-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" width="13" height="13"><path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
              感想备注
            </label>
            <textarea
              v-model="notes"
              class="notes-input"
              rows="3"
              placeholder="写下今天的游戏感想…"
            />
          </div>

        </div>
      </div>

      <!-- Error -->
      <Transition name="err-fade">
        <div v-if="submitError" class="error-msg">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" width="14" height="14"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
          {{ submitError }}
        </div>
      </Transition>

      <!-- Submit -->
      <button class="submit-btn" :disabled="recordStore.loading" @click="submit">
        <svg v-if="!recordStore.loading" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="18" height="18">
          <path d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <span class="spin-dots" v-else>
          <span /><span /><span />
        </span>
        {{ recordStore.loading ? '提交中…' : '提交打卡 ✨' }}
      </button>

    </div>
    </div><!-- /.checkin-main -->

    <aside class="checkin-rail" aria-label="打卡辅助信息">
      <CheckInRail v-bind="railProps" @pick="pickQuickGame" />
    </aside>
  </div><!-- /.checkin-shell -->
</template>

<style lang="scss" scoped>
/* ===== Shell：主表单 + 粘性侧栏 ===== */
.checkin-shell {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  width: 100%;
  animation: ci-in 0.4s ease both;
}

.checkin-main {
  flex: 1;
  min-width: 0;
  max-width: 720px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.checkin-rail {
  width: 300px;
  min-width: 280px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  overscroll-behavior: contain;
}

.checkin-rail-inline {
  display: none;
}

@media (max-width: 1099px) {
  .checkin-shell {
    flex-direction: column;
  }

  .checkin-rail {
    display: none;
  }

  .checkin-rail-inline {
    display: block;
  }

  .checkin-main {
    max-width: none;
  }
}

@keyframes ci-in {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ===== Page header ===== */
.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-eyebrow {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.22em;
  color: var(--accent);
  opacity: 0.7;
}

.page-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.1;
}

.page-sub {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

/* ===== Card ===== */
.checkin-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-xl);
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 28px;

  &:hover {
    border-color: rgba(255, 110, 181, 0.18);
  }

  @media (max-width: 640px) {
    padding: 20px 16px;
  }
}

/* ===== Steps ===== */
.step {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-label {
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-hero);
  font-size: 13px;
  font-weight: 900;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 0 14px rgba(255, 110, 181, 0.4);
}

.step-text {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
}

/* ===== Search ===== */
.search-area {
  position: relative;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--bg-input);
  border: 1.5px solid var(--border-color);
  border-radius: var(--border-radius);
  transition: var(--transition-fast);
  cursor: text;

  &:focus-within {
    border-color: var(--border-accent);
    box-shadow: 0 0 0 3px rgba(255, 110, 181, 0.1);
  }

  .search-ico {
    width: 16px;
    height: 16px;
    color: var(--text-secondary);
    flex-shrink: 0;
    transition: color var(--transition-fast);
  }

  &:focus-within .search-ico {
    color: var(--accent);
  }
}

.search-input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 0;

  &::placeholder { color: var(--text-muted); font-weight: 500; }
}

.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  padding: 2px;
  transition: var(--transition-fast);
  flex-shrink: 0;

  &:hover { color: var(--accent); }
}

/* Dropdown */
.search-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  max-height: 288px;
  overflow-y: auto;
  z-index: 60;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 110, 181, 0.08);
}

.search-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px;
}

.loading-dot {
  display: block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent);
  animation: dot-bounce 1.2s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.15s; background: var(--cyan); }
  &:nth-child(3) { animation-delay: 0.3s;  background: var(--emerald); }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.4; }
  40%           { transform: scale(1);   opacity: 1; }
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  cursor: pointer;
  transition: var(--transition-fast);

  &:hover {
    background: rgba(255, 110, 181, 0.07);
  }

  &:not(:last-child) {
    border-bottom: 1px solid var(--border-color);
  }
}

.si-thumb {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-input);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid var(--border-color);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.si-fb { font-size: 20px; }

.si-info { flex: 1; min-width: 0; }

.si-name {
  font-family: var(--font-display);
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.si-meta {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-family: var(--font-display);
}

.si-rating {
  display: flex;
  align-items: center;
  gap: 3px;
  font-family: var(--font-hero);
  font-size: 12px;
  font-weight: 800;
  color: var(--amber);
  flex-shrink: 0;

  svg { color: var(--amber); }
}

.search-empty {
  padding: 18px;
  text-align: center;
  color: var(--text-muted);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* Selected game */
.selected-game {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 13px 16px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 110, 181, 0.07), rgba(167, 139, 250, 0.05));
  border: 1px solid rgba(255, 110, 181, 0.25);
}

.sg-thumb {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-input);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid var(--border-accent);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span { font-size: 22px; }
}

.sg-info { flex: 1; min-width: 0; }

.sg-name {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 800;
  color: var(--accent);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sg-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-family: var(--font-display);
}

.sg-check {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 900;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(255, 110, 181, 0.5);
}

/* ===== Form ===== */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
}

.form-group { display: flex; flex-direction: column; gap: 10px; }
.form-group.full { grid-column: 1 / -1; }

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 800;
  color: var(--text-secondary);
  letter-spacing: 0.04em;

  svg { color: var(--accent); flex-shrink: 0; }
}

/* Time inputs */
.time-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-sep {
  font-family: var(--font-hero);
  font-size: 20px;
  font-weight: 900;
  color: var(--text-muted);
  flex-shrink: 0;
}

.time-field {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.time-unit {
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  white-space: nowrap;
  flex-shrink: 0;
}

.num-input {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  background: var(--bg-input);
  border: 1.5px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-family: var(--font-hero);
  font-size: 18px;
  font-weight: 900;
  text-align: center;
  outline: none;
  transition: var(--transition-fast);

  &:focus { border-color: var(--border-accent); box-shadow: 0 0 0 3px rgba(255, 110, 181, 0.1); }
  &::-webkit-inner-spin-button { -webkit-appearance: none; }
}

/* Rating */
.rating-display {
  font-family: var(--font-hero);
  font-size: 12px;
  font-weight: 900;
  color: var(--amber);
  margin-left: 4px;
}

.range-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.range-input {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  border-radius: 999px;
  outline: none;
  background: linear-gradient(
    to right,
    var(--accent) 0%,
    var(--cyan) var(--pct, 50%),
    var(--bg-input) var(--pct, 50%),
    var(--bg-input) 100%
  );
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: background 0.15s;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--accent), var(--cyan));
    box-shadow: 0 0 10px rgba(255, 110, 181, 0.6), 0 2px 6px rgba(0,0,0,0.3);
    cursor: pointer;
    transition: transform var(--transition-bounce);
  }

  &:hover::-webkit-slider-thumb {
    transform: scale(1.25);
  }
}

.range-ticks {
  display: flex;
  justify-content: space-between;
  padding: 0 2px;
}

.tick {
  font-family: var(--font-mono);
  font-size: 9px;
  color: var(--text-muted);
}

/* Status pills */
.status-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 999px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-input);
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition-bounce);

  .pill-icon { font-size: 14px; }

  &:hover {
    border-color: var(--border-accent);
    color: var(--text-primary);
    transform: translateY(-2px) scale(1.04);
  }

  &.active {
    background: linear-gradient(135deg, rgba(255, 110, 181, 0.15), rgba(167, 139, 250, 0.1));
    border-color: var(--accent);
    color: var(--accent);
    box-shadow: 0 0 16px rgba(255, 110, 181, 0.2);
    transform: translateY(-1px) scale(1.02);
  }
}

/* Notes */
.notes-input {
  width: 100%;
  padding: 12px 14px;
  background: var(--bg-input);
  border: 1.5px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 13.5px;
  font-weight: 500;
  resize: vertical;
  outline: none;
  transition: var(--transition-fast);
  line-height: 1.6;

  &:focus { border-color: var(--border-accent); box-shadow: 0 0 0 3px rgba(255, 110, 181, 0.08); }
  &::placeholder { color: var(--text-muted); }
}

/* Error */
.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 15px;
  border-radius: var(--border-radius);
  background: rgba(244, 114, 182, 0.1);
  border: 1px solid rgba(244, 114, 182, 0.28);
  color: var(--rose);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
}

/* Submit button */
.submit-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 16px 24px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  color: #fff;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: var(--transition-bounce);
  box-shadow: 0 6px 24px rgba(255, 110, 181, 0.45), 0 3px 10px rgba(167, 139, 250, 0.2);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 60%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.22), transparent);
    transition: left 0.55s ease;
  }

  &:hover:not(:disabled) {
    transform: translateY(-3px) scale(1.02);
    box-shadow: 0 10px 36px rgba(255, 110, 181, 0.6), 0 4px 16px rgba(167, 139, 250, 0.35);

    &::before { left: 140%; }
  }

  &:active:not(:disabled) {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

.spin-dots {
  display: flex;
  gap: 5px;

  span {
    display: block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #fff;
    opacity: 0.6;
    animation: dot-bounce 1.2s ease-in-out infinite;

    &:nth-child(2) { animation-delay: 0.15s; }
    &:nth-child(3) { animation-delay: 0.3s; }
  }
}

/* ===== Success overlay ===== */
.success-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(10, 4, 22, 0.88);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-card {
  position: relative;
  background: var(--bg-secondary);
  border: 1px solid var(--border-accent);
  border-radius: var(--border-radius-xl);
  padding: 52px 72px;
  text-align: center;
  box-shadow:
    0 0 60px rgba(255, 110, 181, 0.25),
    0 0 120px rgba(167, 139, 250, 0.12),
    0 24px 80px rgba(0, 0, 0, 0.6);
}

.success-sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ss {
  position: absolute;
  font-size: 16px;
  color: var(--accent);
  animation: float-sparkle 2.4s ease-in-out infinite;

  &.sp1 { top: 18%; left: 14%;  animation-delay: 0s; color: var(--accent); }
  &.sp2 { top: 22%; right: 12%; animation-delay: 0.7s; color: var(--amber); font-size: 14px; }
  &.sp3 { bottom: 22%; left: 20%; animation-delay: 1.3s; color: var(--cyan); font-size: 12px; }
}

@keyframes float-sparkle {
  0%, 100% { transform: translateY(0) rotate(0deg) scale(1); opacity: 0.5; }
  50%       { transform: translateY(-10px) rotate(20deg) scale(1.2); opacity: 1; }
}

.success-icon {
  font-size: 56px;
  margin-bottom: 16px;
  animation: success-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes success-pop {
  from { transform: scale(0.5); opacity: 0; }
  to   { transform: scale(1);   opacity: 1; }
}

.success-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.success-sub {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
}

/* ===== Transitions ===== */
.success-fade-enter-active,
.success-fade-leave-active {
  transition: opacity 0.3s ease;
}
.success-fade-enter-from,
.success-fade-leave-to {
  opacity: 0;
}

.dropdown-fade-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.dropdown-fade-leave-active { transition: opacity 0.1s ease; }
.dropdown-fade-enter-from   { opacity: 0; transform: translateY(-6px); }
.dropdown-fade-leave-to     { opacity: 0; }

.selected-slide-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.selected-slide-leave-active { transition: all 0.18s ease; }
.selected-slide-enter-from   { opacity: 0; transform: translateY(-8px) scale(0.97); }
.selected-slide-leave-to     { opacity: 0; transform: translateY(-4px) scale(0.98); }

.err-fade-enter-active,
.err-fade-leave-active { transition: all 0.2s ease; }
.err-fade-enter-from,
.err-fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
