<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRecordStore } from '@/stores/record'
import { useUserStore } from '@/stores/user'
import { useTheme } from '@/composables/useTheme'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { HeatmapChart } from 'echarts/charts'
import { CalendarComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([HeatmapChart, CalendarComponent, TooltipComponent, VisualMapComponent, CanvasRenderer])

const recordStore = useRecordStore()
const userStore = useUserStore()
const { isDark } = useTheme()
const currentYear = new Date().getFullYear()

onMounted(async () => {
  await Promise.all([
    recordStore.fetchHeatmap(currentYear),
    recordStore.fetchStats(),
    recordStore.fetchRecentRecords()
  ])
})

const todayStr = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y} / ${m} / ${day}`
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '深夜也在玩游戏呢～'
  if (h < 12) return '早上好！今天也要加油哦'
  if (h < 18) return '下午好！游戏时间到啦'
  return '晚上好！今天玩了什么呢？'
})

const streak = computed(() => {
  const dateSet = new Set(
    recordStore.heatmapData.filter(d => d.count > 0).map(d => d.date)
  )
  let count = 0
  const d = new Date()
  while (true) {
    const s = d.toISOString().slice(0, 10)
    if (dateSet.has(s)) { count++; d.setDate(d.getDate() - 1) }
    else break
  }
  return count
})

const totalHours = computed(() => (Number(recordStore.stats.totalMinutes) / 60 || 0).toFixed(1))
const weekHours  = computed(() => (Number(recordStore.stats.weekMinutes)  / 60 || 0).toFixed(1))

/* Tick mark positions for the magical circle (JS-computed for accuracy) */
function getTickStyle(n: number) {
  const angle = (n * 30 - 90) * (Math.PI / 180)
  const radius = 72
  return {
    top:  `calc(50% + ${Math.sin(angle) * radius}px - 2px)`,
    left: `calc(50% + ${Math.cos(angle) * radius}px - 2px)`
  }
}

/* ECharts heatmap — kawaii pink colour scale */
const heatmapOption = computed(() => {
  const dark = isDark.value
  const emptyCell  = dark ? '#250D3C' : '#FFE4F3'
  const cellBorder = dark ? '#130820' : '#FFF0F8'
  const textColor  = dark ? '#8868A8' : '#9B6BAA'
  const tooltipBg  = dark ? '#1C0F2E' : '#FFE4F3'
  const tooltipTxt = dark ? '#F5EEFF' : '#2B1040'

  return {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: tooltipBg,
      borderColor: 'rgba(255, 110, 181, 0.35)',
      textStyle: { color: tooltipTxt, fontSize: 12, fontFamily: 'Nunito, sans-serif' },
      formatter(params: { value: [string, number] }) {
        const mins = params.value[1]
        const h = Math.floor(mins / 60)
        const m = mins % 60
        const t = h > 0 ? `${h}h ${m}m` : `${m}m`
        return `<b>${params.value[0]}</b><br/>游玩 <span style="color:#FF6EB5;font-weight:800">${t}</span>`
      }
    },
    visualMap: {
      min: 0, max: 300,
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      bottom: 4,
      pieces: [
        { lte: 0,         color: emptyCell,                              label: '无'   },
        { gt: 0,  lte: 30,  color: dark ? '#4A1560' : '#FFC2DF',        label: '≤30m' },
        { gt: 30, lte: 60,  color: dark ? '#7B2D8B' : '#FF98C8',        label: '≤1h'  },
        { gt: 60, lte: 120, color: dark ? '#BE4BAA' : '#FF72B0',        label: '≤2h'  },
        { gt: 120,          color: '#FF6EB5',                            label: '2h+'  }
      ],
      textStyle: { color: textColor, fontSize: 11 },
      itemWidth: 14, itemHeight: 14, itemGap: 8
    },
    calendar: {
      top: 28, left: 36, right: 36, bottom: 52,
      cellSize: ['auto', 14],
      range: String(currentYear),
      itemStyle: { borderWidth: 3, borderColor: cellBorder, color: emptyCell },
      yearLabel: { show: false },
      dayLabel: { color: textColor, fontSize: 10, firstDay: 1, nameMap: ['日','一','二','三','四','五','六'] },
      monthLabel: { color: textColor, fontSize: 11 },
      splitLine: { show: false }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: recordStore.heatmapData.map(item => [item.date, item.totalMinutes || 0]),
      emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(255, 110, 181, 0.7)' } }
    }]
  }
})

/* Top games */
const topGamesList = computed(() => {
  const games = recordStore.topGames.map(g => ({
    icon: String(g.icon || ''),
    name: String(g.name || ''),
    totalMinutes: Number(g.totalMinutes || 0)
  }))
  const maxMins = games[0]?.totalMinutes || 1
  return games.map(g => ({ ...g, pct: Math.round((g.totalMinutes / maxMins) * 100) }))
})

/* Recent records */
const recentList = computed(() =>
  recordStore.recentRecords.slice(0, 7).map(r => {
    const game = r.game as Record<string, unknown> | undefined
    return {
      gameIcon: game ? String(game.icon || '') : '',
      gameName: game ? String(game.name || '') : '未知游戏',
      date: String(r.recordDate || '').slice(0, 10),
      playTime: Number(r.playTime || 0)
    }
  })
)

function formatMinutes(mins: number) {
  if (!mins) return '0m'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0 && m > 0) return `${h}h ${m}m`
  if (h > 0) return `${h}h`
  return `${m}m`
}

/* Rank medal colours */
const RANK_COLORS = ['#FF6EB5', '#C0C0C0', '#E8A45A', '#8868A8', '#8868A8']
/* Rank medal symbols */
const RANK_MEDALS = ['♛', '♜', '♝', '♞', '♟']
</script>

<template>
  <div class="dashboard">

    <!-- ========= TOP ROW: Hero + Stats ========= -->
    <div class="top-row">

      <!-- ── Hero Card ── -->
      <section class="hero-card">
        <!-- Floating sparkle decorations -->
        <span class="sparkle sp1" aria-hidden="true">✦</span>
        <span class="sparkle sp2" aria-hidden="true">✧</span>
        <span class="sparkle sp3" aria-hidden="true">★</span>
        <span class="sparkle sp4" aria-hidden="true">✦</span>

        <div class="hero-body">
          <div class="hero-meta-row">
            <span class="hero-tag">✦ PLAYER</span>
            <span class="hero-date">{{ todayStr }}</span>
          </div>

          <h1 class="hero-name">
            {{ userStore.user.nickname || userStore.user.username }}
          </h1>

          <p class="hero-greeting">{{ greeting }}</p>

          <div class="hero-actions">
            <div v-if="streak > 0" class="streak-pill">
              <span class="streak-fire">🔥</span>
              <span class="streak-num">{{ streak }}</span>
              <span class="streak-lbl">天连续打卡</span>
            </div>

            <router-link to="/checkin" class="hero-cta">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <path d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              今日打卡 ✨
            </router-link>
          </div>
        </div>

        <!-- Magical circle dial (total hours) -->
        <div class="magic-circle" aria-hidden="true">
          <!-- Orbit dots positioned via JS -->
          <div
            v-for="n in 12"
            :key="n"
            class="orbit-dot"
            :class="n % 3 === 0 ? 'big' : ''"
            :style="getTickStyle(n)"
          />
          <!-- Spinning rings -->
          <div class="ring r1" />
          <div class="ring r2" />
          <div class="ring r3" />
          <!-- Center readout -->
          <div class="dial-center">
            <span class="dial-val">{{ totalHours }}</span>
            <span class="dial-unit">小时</span>
          </div>
        </div>
      </section>

      <!-- ── Stats Column ── -->
      <div class="stats-col">

        <div class="stat-card pink">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
            </svg>
          </div>
          <div class="stat-body">
            <span class="stat-label">总游玩时长</span>
            <span class="stat-value">{{ totalHours }}<em>h</em></span>
          </div>
          <div class="stat-deco">⏱</div>
        </div>

        <div class="stat-card purple">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <rect x="2" y="6" width="20" height="12" rx="2" /><path d="M6 12h4m6-2v4" />
            </svg>
          </div>
          <div class="stat-body">
            <span class="stat-label">收录游戏</span>
            <span class="stat-value">{{ recordStore.stats.gameCount || 0 }}</span>
          </div>
          <div class="stat-deco">🎮</div>
        </div>

        <div class="stat-card mint">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <path d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="stat-body">
            <span class="stat-label">打卡次数</span>
            <span class="stat-value">{{ recordStore.stats.recordCount || 0 }}</span>
          </div>
          <div class="stat-deco">✅</div>
        </div>

        <div class="stat-card gold">
          <div class="stat-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <path d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
          </div>
          <div class="stat-body">
            <span class="stat-label">本周时长</span>
            <span class="stat-value">{{ weekHours }}<em>h</em></span>
          </div>
          <div class="stat-deco">📈</div>
        </div>

      </div>
    </div>

    <!-- ========= ACTIVITY LOG (Heatmap) ========= -->
    <div class="kawaii-card log-card">
      <div class="kawaii-header">
        <div class="kh-dots">
          <i class="kd-pink" /><i class="kd-purple" /><i class="kd-mint" />
        </div>
        <span class="kh-title">✦ 活跃记录 · ACTIVITY LOG · {{ currentYear }}</span>
        <span class="kh-badge">热力图</span>
      </div>
      <div class="log-body">
        <v-chart :option="heatmapOption" autoresize style="height: 200px;" />
      </div>
    </div>

    <!-- ========= BOTTOM ROW ========= -->
    <div class="bottom-row">

      <!-- Top Games -->
      <div class="kawaii-card">
        <div class="kawaii-header simple">
          <div>
            <div class="kh-eyebrow">✦ RANKING</div>
            <h2 class="kh-panel-title">最常玩游戏</h2>
          </div>
          <span class="kh-badge">TOP 5</span>
        </div>

        <div class="games-list">
          <div v-for="(g, idx) in topGamesList" :key="idx" class="game-row">
            <span class="rank-medal" :style="{ color: RANK_COLORS[idx] }">{{ RANK_MEDALS[idx] }}</span>
            <div class="game-img">
              <img v-if="g.icon" :src="g.icon" alt="" />
              <span v-else class="img-fb">🎮</span>
            </div>
            <div class="game-info">
              <div class="game-name">{{ g.name }}</div>
              <div class="bar-row">
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: g.pct + '%' }" />
                </div>
                <span class="game-dur">{{ formatMinutes(g.totalMinutes) }}</span>
              </div>
            </div>
          </div>
          <div v-if="topGamesList.length === 0" class="empty-state">
            <span class="empty-icon">🌸</span>
            <span>还没有游玩记录，去打卡吧！</span>
          </div>
        </div>
      </div>

      <!-- Recent Records -->
      <div class="kawaii-card">
        <div class="kawaii-header simple">
          <div>
            <div class="kh-eyebrow">✦ LOG</div>
            <h2 class="kh-panel-title">最近打卡</h2>
          </div>
          <router-link to="/checkin" class="kh-link">全部 →</router-link>
        </div>

        <div class="timeline">
          <div v-for="(r, idx) in recentList" :key="idx" class="tl-item">
            <div class="tl-spine">
              <div class="tl-dot" />
              <div v-if="idx < recentList.length - 1" class="tl-line" />
            </div>
            <div class="tl-card">
              <div class="tl-thumb">
                <img v-if="r.gameIcon" :src="r.gameIcon" alt="" />
                <span v-else>🎮</span>
              </div>
              <div class="tl-info">
                <div class="tl-name">{{ r.gameName }}</div>
                <div class="tl-date">{{ r.date }}</div>
              </div>
              <div class="tl-dur">{{ formatMinutes(r.playTime) }}</div>
            </div>
          </div>
          <div v-if="recentList.length === 0" class="empty-state">
            <span class="empty-icon">🌸</span>
            <span>还没有打卡记录，去打卡吧！</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style lang="scss" scoped>
/* ======= Dashboard root ======= */
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1400px;
  animation: fadeSlideIn 0.5s ease both;
}

/* ======= Top Row ======= */
.top-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 264px;
  gap: 20px;
  align-items: stretch;

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
  }
}

/* ======= Hero Card ======= */
.hero-card {
  position: relative;
  border-radius: var(--border-radius-xl);
  padding: 32px 36px;
  overflow: hidden;
  border: 1px solid var(--border-accent);
  /* Gradient background — soft magical */
  background:
    radial-gradient(ellipse at 10% 50%, rgba(255, 110, 181, 0.1) 0%, transparent 55%),
    radial-gradient(ellipse at 85% 20%, rgba(167, 139, 250, 0.08) 0%, transparent 50%),
    var(--bg-card);
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 218px;

  /* Soft polka dot overlay */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: radial-gradient(circle, rgba(255, 110, 181, 0.05) 1px, transparent 1px);
    background-size: 28px 28px;
    pointer-events: none;
    border-radius: inherit;
  }
}

/* Floating sparkle decorations */
.sparkle {
  position: absolute;
  font-size: 14px;
  pointer-events: none;
  z-index: 0;

  &.sp1 { top: 14%; right: 22%; color: var(--accent); font-size: 16px; animation: float-sparkle 3.2s ease-in-out infinite; }
  &.sp2 { top: 70%; right: 14%; color: var(--cyan); font-size: 10px; animation: float-sparkle 2.8s ease-in-out infinite 1.1s; }
  &.sp3 { top: 35%; right: 30%; color: var(--amber); font-size: 11px; animation: float-sparkle 3.6s ease-in-out infinite 2.2s; }
  &.sp4 { top: 55%; right: 44%; color: var(--rose); font-size: 8px; animation: float-sparkle 2.5s ease-in-out infinite 0.6s; }
}

@keyframes float-sparkle {
  0%, 100% { transform: translateY(0) rotate(0deg) scale(1); opacity: 0.4; }
  50%       { transform: translateY(-10px) rotate(20deg) scale(1.15); opacity: 0.85; }
}

.hero-body {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.hero-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.hero-tag {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 800;
  color: var(--accent);
  letter-spacing: 0.12em;
  background: var(--accent-dim);
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 110, 181, 0.28);
}

.hero-date {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-secondary);
}

/* Gradient name text */
.hero-name {
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: 0.01em;
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  /* Pink → lavender gradient text */
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;

  @media (max-width: 640px) {
    font-size: 30px;
  }
}

.hero-greeting {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 22px;
  font-family: var(--font-display);
  font-weight: 600;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* Streak pill */
.streak-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 999px;
  background: var(--accent-dim);
  border: 1px solid rgba(255, 110, 181, 0.28);
}

.streak-fire { font-size: 15px; }

.streak-num {
  font-family: var(--font-hero);
  font-size: 20px;
  font-weight: 900;
  color: var(--accent);
  line-height: 1;
}

.streak-lbl {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
}

/* Kawaii pill CTA button */
.hero-cta {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 24px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  font-family: var(--font-display);
  letter-spacing: 0.02em;
  text-decoration: none;
  transition: var(--transition-bounce);
  box-shadow: 0 4px 20px rgba(255, 110, 181, 0.45), 0 2px 8px rgba(167, 139, 250, 0.2);
  white-space: nowrap;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 55%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.28), transparent);
    transition: left 0.55s ease;
  }

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover {
    transform: translateY(-3px) scale(1.05);
    box-shadow: 0 8px 30px rgba(255, 110, 181, 0.65), 0 4px 14px rgba(167, 139, 250, 0.35);
    color: #fff;

    &::before { left: 140%; }
  }

  &:active {
    transform: scale(0.96);
  }
}

/* ======= Magical Circle (dial) ======= */
.magic-circle {
  position: relative;
  width: 168px;
  height: 168px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;

  @media (max-width: 680px) {
    display: none;
  }
}

/* Orbit dots */
.orbit-dot {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 110, 181, 0.45);

  &.big {
    width: 6px;
    height: 6px;
    background: var(--accent);
    box-shadow: 0 0 6px var(--accent);
  }
}

/* Spinning rings */
.ring {
  position: absolute;
  border-radius: 50%;

  &.r1 {
    width: 168px;
    height: 168px;
    border: 1.5px solid rgba(255, 110, 181, 0.2);
    border-top-color: var(--accent);
    border-right-color: var(--rose);
    animation: magic-spin 14s linear infinite;
  }

  &.r2 {
    width: 120px;
    height: 120px;
    border: 1.5px solid rgba(167, 139, 250, 0.18);
    border-left-color: var(--cyan);
    border-bottom-color: var(--violet);
    animation: magic-spin 9s linear infinite reverse;
  }

  &.r3 {
    width: 76px;
    height: 76px;
    border: 1px dashed rgba(253, 230, 138, 0.3);
    animation: magic-spin 20s linear infinite;
  }
}

@keyframes magic-spin {
  to { transform: rotate(360deg); }
}

.dial-center {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.dial-val {
  font-family: var(--font-hero);
  font-size: 36px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 8px rgba(255, 110, 181, 0.5));
}

.dial-unit {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.08em;
  margin-top: 3px;
}

/* ======= Stats Column ======= */
.stats-col {
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (max-width: 960px) {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Individual stat card */
.stat-card {
  flex: 1;
  border-radius: 20px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--border-color);
  position: relative;
  overflow: hidden;
  transition: var(--transition-bounce);
  background: var(--bg-card);

  &:hover {
    transform: translateX(4px) scale(1.01);
    border-color: var(--border-accent);

    @media (max-width: 960px) {
      transform: translateY(-3px) scale(1.02);
    }
  }

  /* Bottom glow bar */
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 3px;
    border-radius: 0 0 20px 20px;
  }

  &.pink {
    .stat-icon-wrap { background: rgba(255, 110, 181, 0.14); color: var(--accent); }
    .stat-value { color: var(--accent); }
    &::after { background: linear-gradient(90deg, var(--accent), transparent); }
  }

  &.purple {
    .stat-icon-wrap { background: rgba(167, 139, 250, 0.14); color: var(--cyan); }
    .stat-value { color: var(--cyan); }
    &::after { background: linear-gradient(90deg, var(--cyan), transparent); }
  }

  &.mint {
    .stat-icon-wrap { background: rgba(110, 231, 183, 0.14); color: var(--emerald); }
    .stat-value { color: var(--emerald); }
    &::after { background: linear-gradient(90deg, var(--emerald), transparent); }
  }

  &.gold {
    .stat-icon-wrap { background: rgba(253, 230, 138, 0.14); color: var(--amber); }
    .stat-value { color: var(--amber); }
    &::after { background: linear-gradient(90deg, var(--amber), transparent); }
  }
}

.stat-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform var(--transition-bounce);

  .stat-card:hover & {
    transform: scale(1.12) rotate(-6deg);
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.stat-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.06em;
  margin-bottom: 4px;
}

.stat-value {
  font-family: var(--font-hero);
  font-size: 30px;
  font-weight: 900;
  line-height: 1;

  em {
    font-size: 14px;
    font-weight: 700;
    font-style: normal;
    color: var(--text-secondary);
    margin-left: 2px;
    font-family: var(--font-display);
  }
}

/* Decorative emoji in top-right corner of each stat */
.stat-deco {
  position: absolute;
  top: 10px;
  right: 12px;
  font-size: 18px;
  opacity: 0.3;
  pointer-events: none;
  transition: var(--transition-bounce);

  .stat-card:hover & {
    opacity: 0.6;
    transform: scale(1.2) rotate(8deg);
  }
}

/* ======= Kawaii card wrapper ======= */
.kawaii-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 22px;
  overflow: hidden;
  transition: var(--transition-fast);

  &:hover {
    border-color: rgba(255, 110, 181, 0.22);
    box-shadow: 0 8px 32px rgba(167, 139, 250, 0.08);
  }
}

/* Kawaii card header — macOS-terminal style */
.kawaii-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  background: rgba(255, 110, 181, 0.05);
  border-bottom: 1px solid var(--border-color);

  &.simple {
    background: transparent;
    border-bottom-color: var(--border-color);
    padding: 18px 20px 10px;
    flex-direction: column;
    align-items: flex-start;
    gap: 0;

    &.simple {
      flex-direction: row;
      align-items: flex-end;
      gap: 0;
      justify-content: space-between;
    }
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

    &.kd-pink   { background: var(--accent); box-shadow: 0 0 6px rgba(255,110,181,0.6); }
    &.kd-purple { background: var(--cyan); box-shadow: 0 0 6px rgba(167,139,250,0.5); }
    &.kd-mint   { background: var(--emerald); box-shadow: 0 0 6px rgba(110,231,183,0.5); }
  }
}

.kh-title {
  flex: 1;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kh-badge {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--accent);
  background: var(--accent-dim);
  border: 1px solid rgba(255, 110, 181, 0.28);
  border-radius: 999px;
  padding: 2px 11px;
  flex-shrink: 0;
}

.kh-eyebrow {
  font-family: var(--font-display);
  font-size: 9px;
  font-weight: 800;
  color: var(--accent);
  letter-spacing: 0.22em;
  margin-bottom: 4px;
  opacity: 0.7;
}

.kh-panel-title {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
}

.kh-link {
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition-fast);

  &:hover { color: var(--accent); }
}

.log-body {
  padding: 10px 8px 4px;
}

/* ======= Bottom Row ======= */
.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  .kawaii-card {
    padding: 18px 20px 16px;
    border-radius: 22px;
  }
}

/* ======= Games List ======= */
.games-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
  margin-top: 14px;
}

.game-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rank-medal {
  font-size: 16px;
  width: 22px;
  text-align: center;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 3px currentColor);
}

.game-img {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-input);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid var(--border-color);

  img { width: 100%; height: 100%; object-fit: cover; }
}

.img-fb { font-size: 17px; }

.game-info { flex: 1; min-width: 0; }

.game-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-display);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.bar-row { display: flex; align-items: center; gap: 8px; }

.bar-track {
  flex: 1;
  height: 4px;
  background: var(--bar-track-bg);
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), var(--cyan));
  transition: width 0.65s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.game-dur {
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--text-secondary);
  white-space: nowrap;
  min-width: 40px;
  text-align: right;
}

/* ======= Timeline ======= */
.timeline {
  display: flex;
  flex-direction: column;
  margin-top: 14px;
}

.tl-item { display: flex; gap: 10px; }

.tl-spine {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 14px;
}

.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  box-shadow: 0 0 10px rgba(255, 110, 181, 0.6);
  flex-shrink: 0;
  margin-top: 13px;
}

.tl-line {
  width: 2px;
  flex: 1;
  background: linear-gradient(to bottom, rgba(255, 110, 181, 0.25), rgba(167, 139, 250, 0.15));
  margin: 4px 0;
  min-height: 12px;
  border-radius: 999px;
}

.tl-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border-radius: 12px;
  margin-bottom: 4px;
  transition: var(--transition-fast);

  &:hover {
    background: rgba(255, 110, 181, 0.06);
  }
}

.tl-thumb {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--bg-input);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid var(--border-color);

  img { width: 100%; height: 100%; object-fit: cover; }
  span { font-size: 15px; }
}

.tl-info { flex: 1; min-width: 0; }

.tl-name {
  font-size: 13px;
  font-weight: 700;
  font-family: var(--font-display);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tl-date {
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--text-secondary);
  margin-top: 2px;
}

.tl-dur {
  font-size: 12px;
  font-family: var(--font-display);
  font-weight: 800;
  color: var(--accent);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ======= Empty State ======= */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  padding: 30px 16px;
  color: var(--text-muted);
  font-size: 13px;
  font-family: var(--font-display);
  font-weight: 600;
}

.empty-icon {
  font-size: 30px;
  animation: float-sparkle 3s ease-in-out infinite;
}
</style>
