<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  todayCheckedIn: boolean
  todayMinutesLabel: string
  streak: number
  weekHours: string
  recordCount: number
  loading?: boolean
  partialError?: boolean
}>()

const isNewUser = computed(() => props.recordCount === 0 && !props.loading)

const missionTitle = computed(() => {
  if (props.loading) return '加载中…'
  if (isNewUser.value) return '开启你的第一条记录'
  if (props.todayCheckedIn) return '今日已打卡，太棒了！'
  return '今日魔力值未收集 ✦'
})

const missionSubtitle = computed(() => {
  if (props.loading) return '正在同步游玩数据'
  if (props.partialError) return '部分数据未加载，仍可前往打卡'
  if (isNewUser.value) return '从打卡开始，养成游玩记录习惯'
  if (props.todayCheckedIn) {
    return props.todayMinutesLabel
      ? `今日已玩 ${props.todayMinutesLabel}，热力图已点亮`
      : '今日打卡已记录，热力图已点亮'
  }
  return '记录一局，点亮今日热力格'
})

const ctaLabel = computed(() => {
  if (isNewUser.value) return '开始第一次打卡 ✨'
  if (props.todayCheckedIn) return '再记一局'
  return '今日打卡 ✨'
})
</script>

<template>
  <section
    class="today-mission kawaii-card"
    :class="{ done: todayCheckedIn && !loading, 'is-new': isNewUser && !loading }"
    aria-labelledby="daily-quest-title"
  >
    <div class="kawaii-header">
      <div class="kh-dots">
        <i class="kd-pink" /><i class="kd-purple" /><i class="kd-mint" />
      </div>
      <span id="daily-quest-title" class="kh-title">✦ 今日任务 · DAILY QUEST</span>
    </div>

    <div class="mission-body">
      <div
        class="mission-ring"
        :class="{ complete: todayCheckedIn && !loading, pending: !todayCheckedIn && !loading }"
        aria-hidden="true"
      >
        <span v-if="loading" class="ring-icon">···</span>
        <span v-else-if="todayCheckedIn" class="ring-icon">✓</span>
        <span v-else class="ring-icon">✧</span>
      </div>

      <div class="mission-copy">
        <h2 class="mission-title">{{ missionTitle }}</h2>
        <p class="mission-sub">{{ missionSubtitle }}</p>
      </div>

      <div v-if="!loading" class="mission-meta">
        <span v-if="streak > 0" class="meta-chip">
          <span class="meta-ico">🔥</span>
          {{ streak }} 天连续
        </span>
        <span v-else-if="!isNewUser && !todayCheckedIn" class="meta-chip muted">
          连续打卡从 1 天开始
        </span>
        <span class="meta-chip">
          <span class="meta-ico">📈</span>
          本周 {{ weekHours }}h
        </span>
      </div>

      <router-link
        to="/checkin"
        class="mission-cta"
        :aria-label="ctaLabel"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <path d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ ctaLabel }}
      </router-link>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.today-mission {
  border-radius: 22px;
  overflow: hidden;

  &.done {
    background:
      radial-gradient(ellipse at 100% 0%, rgba(255, 110, 181, 0.12) 0%, transparent 55%),
      var(--bg-card);
  }

  .kawaii-header {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 11px 16px;
    background: rgba(255, 110, 181, 0.05);
    border-bottom: 1px solid var(--border-color);
  }
}

.kh-dots {
  display: flex;
  gap: 5px;
  flex-shrink: 0;

  i {
    display: block;
    width: 9px;
    height: 9px;
    border-radius: 50%;

    &.kd-pink { background: var(--accent); }
    &.kd-purple { background: var(--cyan); }
    &.kd-mint { background: var(--emerald); }
  }
}

.kh-title {
  flex: 1;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
}

.mission-body {
  padding: 18px 16px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  text-align: center;
}

.mission-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.pending {
    border: 2px dashed rgba(255, 110, 181, 0.45);
    background: var(--accent-dim);
  }

  &.complete {
    border: 2px solid transparent;
    background:
      linear-gradient(var(--bg-card), var(--bg-card)) padding-box,
      linear-gradient(135deg, var(--accent), var(--cyan)) border-box;
    box-shadow: 0 0 20px rgba(255, 110, 181, 0.25);
  }

  .ring-icon {
    font-family: var(--font-display);
    font-size: 28px;
    font-weight: 900;
    line-height: 1;
    color: var(--accent);
  }
}

.mission-copy {
  width: 100%;
}

.mission-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 6px;
  line-height: 1.3;
}

.mission-sub {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
  line-height: 1.45;
}

.mission-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  width: 100%;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 999px;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  background: var(--bg-input);
  border: 1px solid var(--border-color);

  &.muted {
    opacity: 0.85;
  }
}

.meta-ico {
  font-size: 12px;
  line-height: 1;
}

.mission-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  padding: 11px 20px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  font-family: var(--font-display);
  text-decoration: none;
  transition: var(--transition-bounce);
  box-shadow: 0 4px 20px rgba(255, 110, 181, 0.4);

  svg {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(255, 110, 181, 0.55);
    color: #fff;
  }

  &:active {
    transform: scale(0.98);
  }
}

@media (prefers-reduced-motion: reduce) {
  .mission-cta:hover {
    transform: none;
  }
}
</style>
