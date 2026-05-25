<script setup lang="ts">
import type { Game } from '@/stores/game'
import type { GameHistorySummary, QuickPickItem } from '@/utils/checkinRecords'
import { formatPlayMinutes, STATUS_LABELS } from '@/utils/checkinRecords'

defineProps<{
  quickPicks: QuickPickItem[]
  selectedGame: Game | null
  previewMinutesLabel: string
  previewRating: number
  previewRatingLabel: string
  previewStatus: number
  previewNotes: string
  gameHistory: GameHistorySummary | null
  loading?: boolean
  railError?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  pick: [game: Game]
}>()
</script>

<template>
  <div class="checkin-rail-inner">
    <!-- 快捷续打 -->
    <section class="rail-card">
      <div class="rail-head">
        <span class="rail-eyebrow">✦ QUICK PLAY</span>
        <h2 class="rail-title">快捷续打</h2>
      </div>

      <p v-if="railError && !loading" class="rail-hint warn">
        辅助信息加载失败，不影响打卡提交
      </p>
      <p v-else-if="loading" class="rail-hint">正在加载最近记录…</p>
      <p v-else-if="quickPicks.length === 0" class="rail-hint">
        暂无历史记录，搜索游戏开始第一次打卡吧
      </p>

      <ul v-else class="quick-list" role="list">
        <li v-for="item in quickPicks" :key="item.game.id">
          <button
            type="button"
            class="quick-item"
            :class="{ active: selectedGame?.id === item.game.id }"
            :disabled="disabled"
            @click="emit('pick', item.game)"
          >
            <div class="qi-thumb">
              <img
                v-if="item.game.icon"
                :src="item.game.icon"
                :alt="item.game.name"
                loading="lazy"
                decoding="async"
              />
              <span v-else>🎮</span>
            </div>
            <div class="qi-body">
              <span class="qi-name">{{ item.game.name }}</span>
              <span class="qi-meta">{{ item.lastDate }} · {{ formatPlayMinutes(item.lastPlayTime) }}</span>
            </div>
          </button>
        </li>
      </ul>
    </section>

    <!-- 实时预览 -->
    <section class="rail-card preview-card">
      <div class="rail-head">
        <span class="rail-eyebrow">✦ PREVIEW</span>
        <h2 class="rail-title">打卡预览</h2>
      </div>

      <div v-if="!selectedGame" class="preview-empty">
        <span class="pe-icon">✧</span>
        <p>选择游戏并填写详情后，这里会显示提交后的样子</p>
      </div>

      <article v-else class="preview-tl">
        <div class="pv-thumb">
          <img
            v-if="selectedGame.icon"
            :src="selectedGame.icon"
            :alt="selectedGame.name"
            loading="lazy"
            decoding="async"
          />
          <span v-else>🎮</span>
        </div>
        <div class="pv-main">
          <div class="pv-name">{{ selectedGame.name }}</div>
          <div class="pv-row">
            <span class="pv-chip time">{{ previewMinutesLabel }}</span>
            <span class="pv-chip rating">{{ previewRating }} · {{ previewRatingLabel }}</span>
          </div>
          <div class="pv-row">
            <span class="pv-chip status">
              {{ STATUS_LABELS[previewStatus] ?? '在玩' }}
            </span>
          </div>
          <p v-if="previewNotes.trim()" class="pv-notes">{{ previewNotes.trim() }}</p>
          <p v-else class="pv-notes muted">（未填写感想）</p>
        </div>
      </article>
    </section>

    <!-- 选中游戏历史 -->
    <section v-if="selectedGame" class="rail-card history-card">
      <div class="rail-head">
        <span class="rail-eyebrow">✦ HISTORY</span>
        <h2 class="rail-title">本游戏记录</h2>
      </div>

      <div v-if="!gameHistory" class="rail-hint">
        这是你的第一次打卡，提交后将出现在热力图中 ✨
      </div>

      <dl v-else class="history-grid">
        <div class="hist-item">
          <dt>上次打卡</dt>
          <dd>{{ gameHistory.lastDate }}</dd>
        </div>
        <div class="hist-item">
          <dt>上次时长</dt>
          <dd>{{ formatPlayMinutes(gameHistory.lastPlayTime) }}</dd>
        </div>
        <div class="hist-item">
          <dt>累计时长</dt>
          <dd>{{ formatPlayMinutes(gameHistory.totalMinutes) }}</dd>
        </div>
        <div class="hist-item">
          <dt>打卡次数</dt>
          <dd>{{ gameHistory.recordCount }} 次</dd>
        </div>
        <div v-if="gameHistory.lastRating != null" class="hist-item">
          <dt>上次评分</dt>
          <dd>{{ gameHistory.lastRating }} / 10</dd>
        </div>
        <div v-if="gameHistory.lastStatus != null" class="hist-item">
          <dt>上次状态</dt>
          <dd>{{ STATUS_LABELS[gameHistory.lastStatus] ?? '—' }}</dd>
        </div>
      </dl>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.checkin-rail-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rail-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 22px;
  padding: 16px 16px 14px;
  transition: border-color var(--transition-fast);

  &:hover {
    border-color: rgba(255, 110, 181, 0.2);
  }
}

.rail-head {
  margin-bottom: 12px;
}

.rail-eyebrow {
  display: block;
  font-family: var(--font-display);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: var(--accent);
  opacity: 0.75;
  margin-bottom: 4px;
}

.rail-title {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
}

.rail-hint {
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  line-height: 1.5;
  margin: 0;

  &.warn {
    color: var(--rose);
  }
}

.quick-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 14px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-input);
  cursor: pointer;
  text-align: left;
  transition: var(--transition-bounce);

  &:hover:not(:disabled) {
    border-color: var(--border-accent);
    transform: translateX(3px);
  }

  &.active {
    border-color: var(--accent);
    background: linear-gradient(135deg, rgba(255, 110, 181, 0.1), rgba(167, 139, 250, 0.06));
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.qi-thumb {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span { font-size: 18px; }
}

.qi-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.qi-name {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qi-meta {
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--text-secondary);
}

.preview-empty {
  text-align: center;
  padding: 20px 8px;

  .pe-icon {
    display: block;
    font-size: 28px;
    color: var(--accent);
    opacity: 0.5;
    margin-bottom: 8px;
  }

  p {
    margin: 0;
    font-family: var(--font-display);
    font-size: 12px;
    font-weight: 600;
    color: var(--text-muted);
    line-height: 1.45;
  }
}

.preview-tl {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 110, 181, 0.06), rgba(167, 139, 250, 0.04));
  border: 1px solid rgba(255, 110, 181, 0.18);
}

.pv-thumb {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-accent);
  background: var(--bg-input);
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span { font-size: 24px; }
}

.pv-main {
  flex: 1;
  min-width: 0;
}

.pv-name {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.25;
}

.pv-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.pv-chip {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid var(--border-color);

  &.time {
    color: var(--accent);
    background: var(--accent-dim);
    border-color: rgba(255, 110, 181, 0.25);
  }

  &.rating {
    color: var(--amber);
    background: rgba(253, 230, 138, 0.12);
    border-color: rgba(253, 230, 138, 0.25);
  }

  &.status {
    color: var(--cyan);
    background: rgba(167, 139, 250, 0.12);
    border-color: rgba(167, 139, 250, 0.25);
  }
}

.pv-notes {
  margin: 4px 0 0;
  font-size: 12px;
  font-family: var(--font-display);
  font-weight: 500;
  color: var(--text-secondary);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;

  &.muted {
    color: var(--text-muted);
    font-style: italic;
  }
}

.history-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
  margin: 0;
}

.hist-item {
  dt {
    font-family: var(--font-display);
    font-size: 10px;
    font-weight: 800;
    letter-spacing: 0.06em;
    color: var(--text-muted);
    margin-bottom: 3px;
  }

  dd {
    margin: 0;
    font-family: var(--font-hero);
    font-size: 15px;
    font-weight: 900;
    color: var(--text-primary);
  }
}

@media (prefers-reduced-motion: reduce) {
  .quick-item:hover:not(:disabled) {
    transform: none;
  }
}
</style>
