<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useGameStore } from '@/stores/game'
import type { Game } from '@/stores/game'
import { useBacklogStore } from '@/stores/backlog'
import type { SortKey } from '@/api/game'
import { displayGameName, localizeGameType, localizePlatform } from '@/utils/gameDisplay'

const gameStore = useGameStore()
const backlogStore = useBacklogStore()
const keyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 爱心点击后的 toast 类型：
 *   add    - 从"无记录" 新增 → 想玩
 *   switch - 已有记录但 status!=0 → 改为想玩
 *   remove - 已是想玩 → 取消想玩，从清单移除
 */
type WishToastType = 'add' | 'switch' | 'remove'
const statusLabels: Record<number, string> = { 0: '想玩', 1: '在玩', 2: '已完成', 3: '已搁置' }

const wishToast = ref<{ visible: boolean; type: WishToastType; name: string; prevStatus?: number }>({
  visible: false, type: 'add', name: '', prevStatus: undefined
})
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showToast(type: WishToastType, name: string, prevStatus?: number) {
  if (toastTimer) clearTimeout(toastTimer)
  wishToast.value = { visible: true, type, name, prevStatus }
  toastTimer = setTimeout(() => { wishToast.value.visible = false }, 1800)
}

async function onToggleWish(game: Game, ev: MouseEvent) {
  ev.stopPropagation()
  if (backlogStore.wishPending.has(game.id)) return
  // 记录点击前的状态，便于事后判断本次属于哪种切换
  const prevStatus = backlogStore.getBacklogStatus(game.id)
  const res = await backlogStore.toggleWish(game.id, {
    id: game.id,
    name: displayGameName(game),
    icon: game.icon,
    platforms: game.platforms,
    gameTypes: game.gameTypes,
    rating: game.rating,
  })
  if (!res) return
  if (res.code === 200) {
    let toastType: WishToastType
    if (prevStatus === 0) {
      toastType = 'remove'
    } else if (prevStatus === null) {
      toastType = 'add'
    } else {
      toastType = 'switch'
    }
    showToast(toastType, displayGameName(game), prevStatus ?? undefined)
  } else if (res.code === 409) {
    // 后端已存在(可能跨标签同步) → 只刷新 wish 映射，避免动 Backlog 页 tab 下的 items
    await backlogStore.refreshWishMap()
  } else {
    console.warn('[toggleWish] 失败：', res)
  }
}

/** 爱心按钮的 hover 提示文案，针对不同 status 给出更精准的语义 */
function getWishTitle(gameId: number): string {
  const s = backlogStore.getBacklogStatus(gameId)
  if (s === 0) return '取消想玩（从清单移除）'
  if (s === 1) return `当前为「${statusLabels[1]}」，点击改为「想玩」`
  if (s === 2) return `当前为「${statusLabels[2]}」，点击改为「想玩」`
  if (s === 3) return `当前为「${statusLabels[3]}」，点击改为「想玩」`
  return '加入想玩清单'
}

/** Toast 中"原状态"的中文文案，避免在模板里用 ?? 操作符引发解析器报错 */
function getPrevStatusLabel(s: number | undefined): string {
  if (s === undefined || s === null) return '其他'
  return statusLabels[s] || '其他'
}

onMounted(() => {
  gameStore.fetchFilters()
  // 只拉 wishlist 映射，不替换 backlog.items（避免影响 Backlog 页面 tab 过滤状态）
  backlogStore.refreshWishMap()
  if (gameStore.libraryGames.length === 0) {
    gameStore.fetchLibrary({ reset: true })
  }
})

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    gameStore.fetchLibrary({ keyword: keyword.value, reset: true })
  }, 320)
}

function clearKeyword() {
  keyword.value = ''
  gameStore.fetchLibrary({ keyword: '', reset: true })
}

function togglePlatform(name: string) {
  const next = gameStore.libraryPlatform === name ? '' : name
  gameStore.fetchLibrary({ platform: next, reset: true })
}
function toggleType(name: string) {
  const next = gameStore.libraryType === name ? '' : name
  gameStore.fetchLibrary({ type: next, reset: true })
}
function setSort(key: SortKey) {
  if (gameStore.librarySort === key) return
  gameStore.fetchLibrary({ sortBy: key, reset: true })
}

function resetAll() {
  keyword.value = ''
  gameStore.resetLibraryFilters()
}

function loadMore() {
  gameStore.loadMoreLibrary()
}

const hasAnyFilter = computed(() =>
  !!gameStore.libraryKeyword ||
  !!gameStore.libraryPlatform ||
  !!gameStore.libraryType ||
  gameStore.librarySort !== 'rating_desc'
)

const hasMore = computed(
  () => gameStore.libraryPage < gameStore.libraryTotalPages
)

const sortOptions: { key: SortKey; label: string; hint: string }[] = [
  { key: 'rating_desc', label: '高分', hint: '评分降序' },
  { key: 'rating_asc',  label: '低分', hint: '评分升序' },
  { key: 'newest',      label: '最新', hint: '最近收录' },
  { key: 'name_asc',    label: 'A→Z', hint: '名称升序' },
]

const currentSortHint = computed(() => {
  const found = sortOptions.find(s => s.key === gameStore.librarySort)
  return found ? found.hint : ''
})

function formatRating(r: number | null) {
  if (!r) return null
  return Number(r).toFixed(1)
}
function getPlatforms(p: string) {
  if (!p) return []
  return p.split(',').map(s => localizePlatform(s.trim())).filter(Boolean).slice(0, 3)
}
function getTypes(t: string) {
  if (!t) return []
  return t.split(',').map(s => localizeGameType(s.trim())).filter(Boolean).slice(0, 2)
}
function getInitial(game: Game) {
  const label = displayGameName(game)
  return label ? label.charAt(0).toUpperCase() : '?'
}

const gridRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

function initObserver() {
  if (typeof IntersectionObserver === 'undefined') return
  observer?.disconnect()
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
      }
    })
  }, { threshold: 0.06 })
  nextTick(() => {
    gridRef.value?.querySelectorAll('.game-card').forEach(el => observer!.observe(el))
  })
}

onMounted(() => initObserver())
onUnmounted(() => observer?.disconnect())
watch(() => gameStore.libraryGames.length, () => initObserver())
</script>

<template>
  <div class="library">

    <!-- ── Header ── -->
    <div class="lib-header">
      <div class="lib-header-left">
        <span class="lib-eyebrow">✦ GAME DATABASE</span>
        <h1 class="lib-title">游戏库</h1>
        <div class="lib-meta">
          <span class="lib-total">{{ gameStore.libraryTotal.toLocaleString() }}</span>
          <span class="lib-total-label">款匹配 / 共 {{ (gameStore.platformOptions.length ? gameStore.libraryTotal : '—') }} 收录</span>
        </div>
      </div>

      <div class="lib-search-wrap">
        <label class="lib-search">
          <svg class="search-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
          </svg>
          <input
            v-model="keyword"
            @input="onSearchInput"
            placeholder="搜索游戏名、平台、类型、开发商…"
            class="lib-input"
            autocomplete="off"
          />
          <button v-if="keyword" class="lib-clear" @click="clearKeyword" type="button">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </label>
      </div>
    </div>

    <!-- ── Console: sort + filter chips ── -->
    <section class="console">
      <!-- Sort segments -->
      <div class="console-row top">
        <div class="seg-wrap">
          <span class="seg-label">SORT</span>
          <div class="seg-group" role="tablist">
            <button
              v-for="opt in sortOptions"
              :key="opt.key"
              :class="['seg', { active: gameStore.librarySort === opt.key }]"
              :title="opt.hint"
              @click="setSort(opt.key)"
              type="button"
            >
              <span class="seg-dot" />
              {{ opt.label }}
            </button>
          </div>
        </div>

        <button
          v-if="hasAnyFilter"
          class="reset-btn"
          @click="resetAll"
          type="button"
        >
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <path d="M3 12a9 9 0 1 0 3-6.7" />
            <path d="M3 4v5h5" />
          </svg>
          清除全部
        </button>
      </div>

      <!-- Platforms row -->
      <div class="console-row chip-row">
        <div class="row-label">
          <span class="row-eyebrow">PLATFORM</span>
          <span class="row-sparkle">✦</span>
        </div>
        <div class="chip-track-wrap">
          <div class="chip-track">
            <button
              v-for="p in gameStore.platformOptions"
              :key="'pf-' + p.name"
              :class="['chip', 'chip-cyan', { active: gameStore.libraryPlatform === p.name }]"
              @click="togglePlatform(p.name)"
              type="button"
            >
              <span class="chip-name">{{ p.name }}</span>
              <span class="chip-count">{{ p.count }}</span>
            </button>
            <div v-if="gameStore.platformOptions.length === 0" class="chip-empty">加载中…</div>
          </div>
        </div>
      </div>

      <!-- Types row -->
      <div class="console-row chip-row">
        <div class="row-label">
          <span class="row-eyebrow">GENRE</span>
          <span class="row-sparkle">✦</span>
        </div>
        <div class="chip-track-wrap">
          <div class="chip-track">
            <button
              v-for="t in gameStore.typeOptions"
              :key="'tp-' + t.name"
              :class="['chip', 'chip-pink', { active: gameStore.libraryType === t.name }]"
              @click="toggleType(t.name)"
              type="button"
            >
              <span class="chip-name">{{ t.name }}</span>
              <span class="chip-count">{{ t.count }}</span>
            </button>
            <div v-if="gameStore.typeOptions.length === 0" class="chip-empty">加载中…</div>
          </div>
        </div>
      </div>

      <!-- Active filter pills (only show when any) -->
      <div v-if="hasAnyFilter" class="active-strip">
        <span class="active-label">激活</span>
        <span v-if="gameStore.libraryKeyword" class="pill">
          <span class="pill-key">关键词</span>
          <span class="pill-val">{{ gameStore.libraryKeyword }}</span>
          <button class="pill-x" @click="clearKeyword" type="button" aria-label="移除关键词">×</button>
        </span>
        <span v-if="gameStore.libraryPlatform" class="pill">
          <span class="pill-key">平台</span>
          <span class="pill-val">{{ gameStore.libraryPlatform }}</span>
          <button class="pill-x" @click="togglePlatform(gameStore.libraryPlatform)" type="button" aria-label="移除平台">×</button>
        </span>
        <span v-if="gameStore.libraryType" class="pill">
          <span class="pill-key">类型</span>
          <span class="pill-val">{{ gameStore.libraryType }}</span>
          <button class="pill-x" @click="toggleType(gameStore.libraryType)" type="button" aria-label="移除类型">×</button>
        </span>
        <span v-if="gameStore.librarySort !== 'rating_desc'" class="pill">
          <span class="pill-key">排序</span>
          <span class="pill-val">{{ currentSortHint }}</span>
          <button class="pill-x" @click="setSort('rating_desc')" type="button" aria-label="恢复排序">×</button>
        </span>
      </div>
    </section>

    <!-- ── Skeleton：游戏加载中 或 wishmap 尚未就绪（确保首次显示时心形已有正确状态）── -->
    <div v-if="(gameStore.libraryLoading && gameStore.libraryGames.length === 0) || !backlogStore.wishMapLoaded" class="game-grid">
      <div v-for="i in 24" :key="i" class="sk-card">
        <div class="sk-cover" />
        <div class="sk-body">
          <div class="sk-line" :style="{ width: (50 + (i * 13) % 40) + '%' }" />
          <div class="sk-line short" :style="{ width: (30 + (i * 7) % 30) + '%' }" />
        </div>
      </div>
    </div>

    <!-- ── Game Grid（wishmap 已就绪时才展示，心形状态与卡片同步出现）── -->
    <div v-else ref="gridRef" class="game-grid">
      <div
        v-for="game in gameStore.libraryGames"
        :key="game.id"
        class="game-card"
      >
        <div class="card-cover">
          <img
            v-if="game.icon"
            :src="game.icon"
            :alt="displayGameName(game)"
            loading="lazy"
            class="cover-img"
          />
          <div v-else class="cover-fb">
            <span class="fb-initial">{{ getInitial(game) }}</span>
          </div>

          <!-- ♥ Wishlist toggle (top-left)。红心仅当 status===0 时亮起 -->
          <button
            type="button"
            :class="[
              'wish-btn',
              backlogStore.isWanted(game.id) ? 'is-on' : '',
              backlogStore.wishPending.has(game.id) ? 'is-busy' : '',
            ]"
            :title="getWishTitle(game.id)"
            :aria-pressed="backlogStore.isWanted(game.id)"
            @click.stop="onToggleWish(game, $event)"
          >
            <svg class="heart-ico" viewBox="0 0 24 24" width="14" height="14">
              <path d="M12 21s-7.5-4.5-9.5-9.2C1.1 8.4 3 5 6.3 5c2 0 3.4 1.1 4.2 2.4h.9C12.3 6.1 13.7 5 15.7 5 19 5 20.9 8.4 19.5 11.8 17.5 16.5 12 21 12 21z"
                    fill="currentColor" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
            </svg>
            <span class="wish-burst" aria-hidden="true">
              <span /><span /><span /><span /><span /><span />
            </span>
          </button>

          <div v-if="game.rating" class="rating-badge">
            <svg viewBox="0 0 20 20" fill="currentColor" width="10" height="10">
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
            </svg>
            {{ formatRating(game.rating) }}
          </div>

          <div class="card-overlay">
            <div class="overlay-content">
              <p v-if="game.description" class="ov-desc">{{ game.description }}</p>
              <div class="ov-tags">
                <span
                  v-for="t in getTypes(game.gameTypes)"
                  :key="t"
                  class="tag type-tag"
                >{{ t }}</span>
              </div>
              <div v-if="game.developer" class="ov-dev">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" width="12" height="12">
                  <path d="M20 7H4a2 2 0 00-2 2v6a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2z"/><path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/>
                </svg>
                {{ game.developer }}
              </div>
              <div v-if="game.avgPlayTime" class="ov-time">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" width="12" height="12">
                  <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
                </svg>
                均 {{ Math.round(game.avgPlayTime / 60) }}h 通关
              </div>
            </div>
          </div>
        </div>

        <div class="card-body">
          <div class="card-name" :title="displayGameName(game)">{{ displayGameName(game) }}</div>
          <div class="card-platforms">
            <span
              v-for="p in getPlatforms(game.platforms)"
              :key="p"
              class="tag platform-tag"
            >{{ p }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="!gameStore.libraryLoading && gameStore.libraryGames.length === 0" class="empty-state">
      <div class="empty-icon">🎮</div>
      <p class="empty-text">当前筛选下没有符合条件的游戏</p>
      <button class="empty-reset" @click="resetAll">清除全部筛选</button>
    </div>

    <!-- Load more / loading -->
    <div class="footer-area">
      <div v-if="gameStore.libraryLoading && gameStore.libraryGames.length > 0" class="loading-dots">
        <span /><span /><span />
      </div>
      <button v-else-if="hasMore" class="load-more-btn" @click="loadMore">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="16" height="16">
          <path d="M12 5v14M5 12l7 7 7-7"/>
        </svg>
        加载更多
        <span class="lm-count">{{ gameStore.libraryGames.length }} / {{ gameStore.libraryTotal }}</span>
      </button>
      <div v-else-if="gameStore.libraryGames.length > 0" class="all-loaded">
        ✦ 已加载全部 {{ gameStore.libraryTotal }} 款游戏 ✦
      </div>
    </div>

    <!-- Wishlist toast -->
    <Transition name="toast-fade">
      <div v-if="wishToast.visible" :class="['wish-toast', wishToast.type]">
        <svg v-if="wishToast.type !== 'remove'" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
          <path d="M12 21s-7.5-4.5-9.5-9.2C1.1 8.4 3 5 6.3 5c2 0 3.4 1.1 4.2 2.4h.9C12.3 6.1 13.7 5 15.7 5 19 5 20.9 8.4 19.5 11.8 17.5 16.5 12 21 12 21z"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <path d="M18 6L6 18M6 6l12 12"/>
        </svg>
        <span class="toast-msg">
          <strong>
            <template v-if="wishToast.type === 'add'">已加入想玩</template>
            <template v-else-if="wishToast.type === 'switch'">
              已从「{{ getPrevStatusLabel(wishToast.prevStatus) }}」改为「想玩」
            </template>
            <template v-else>已从清单移除</template>
          </strong>
          <span class="toast-name">{{ wishToast.name }}</span>
        </span>
      </div>
    </Transition>

    <p class="rawg-attribution">
      游戏数据由
      <a href="https://rawg.io" target="_blank" rel="noopener noreferrer">RAWG</a>
      提供
    </p>

  </div>
</template>

<style lang="scss" scoped>
/* ===== Page root ===== */
.library {
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: lib-in 0.45s ease both;
}

@keyframes lib-in {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ===== Header ===== */
.lib-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.lib-header-left { display: flex; flex-direction: column; gap: 4px; }

.lib-eyebrow {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.22em;
  color: var(--accent);
  opacity: 0.7;
}

.lib-title {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.lib-meta { display: flex; align-items: baseline; gap: 6px; }
.lib-total {
  font-family: var(--font-hero);
  font-size: 18px;
  font-weight: 900;
  color: var(--accent);
}
.lib-total-label {
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
}

.lib-search-wrap {
  flex-shrink: 0;
  width: 360px;
  max-width: 100%;
  @media (max-width: 640px) { width: 100%; }
}
.lib-search {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 16px;
  background: var(--bg-card);
  border: 1.5px solid var(--border-color);
  border-radius: 999px;
  transition: var(--transition-fast);
  &:focus-within {
    border-color: var(--border-accent);
    box-shadow: 0 0 0 3px rgba(255, 110, 181, 0.12), var(--accent-glow);
  }
  .search-ico {
    width: 16px; height: 16px;
    color: var(--text-secondary);
    flex-shrink: 0;
    transition: color var(--transition-fast);
  }
  &:focus-within .search-ico { color: var(--accent); }
}
.lib-input {
  flex: 1;
  background: none; border: none; outline: none;
  font-family: var(--font-display);
  font-size: 13px; font-weight: 600;
  color: var(--text-primary);
  min-width: 0;
  &::placeholder { color: var(--text-muted); font-weight: 500; }
}
.lib-clear {
  background: none; border: none;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex; align-items: center;
  padding: 2px;
  transition: var(--transition-fast);
  flex-shrink: 0;
  svg { width: 13px; height: 13px; }
  &:hover { color: var(--accent); }
}

/* ===== Console panel ===== */
.console {
  position: relative;
  padding: 18px 20px;
  border-radius: 18px;
  background:
    radial-gradient(120% 240% at 0% 0%, rgba(255, 110, 181, 0.08), transparent 55%),
    radial-gradient(120% 240% at 100% 100%, rgba(167, 139, 250, 0.07), transparent 55%),
    var(--bg-card);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: -1px;
    border-radius: inherit;
    padding: 1px;
    background: linear-gradient(135deg,
      rgba(255, 110, 181, 0.45),
      rgba(167, 139, 250, 0.15) 40%,
      transparent 60%,
      rgba(167, 139, 250, 0.3));
    -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
    -webkit-mask-composite: xor;
            mask-composite: exclude;
    pointer-events: none;
    opacity: 0.45;
  }
}

.console-row {
  display: flex;
  align-items: center;
  gap: 14px;

  &.top {
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 10px;
  }
}

/* Sort segment control */
.seg-wrap { display: flex; align-items: center; gap: 10px; }
.seg-label {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.24em;
  color: var(--text-muted);
}
.seg-group {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
}
.seg {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  border-radius: 999px;
  cursor: pointer;
  transition: var(--transition-fast);

  .seg-dot {
    width: 5px; height: 5px;
    border-radius: 50%;
    background: var(--text-muted);
    transition: var(--transition-fast);
  }

  &:hover:not(.active) {
    color: var(--text-primary);
    .seg-dot { background: var(--cyan); }
  }
  &.active {
    color: #fff;
    background: linear-gradient(135deg, var(--accent), var(--violet));
    box-shadow: 0 4px 14px rgba(255, 110, 181, 0.35);
    .seg-dot { background: #fff; box-shadow: 0 0 8px rgba(255, 255, 255, 0.8); }
  }
}

.reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1.5px dashed var(--border-accent);
  background: transparent;
  color: var(--accent);
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: var(--transition-bounce);

  svg { transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }

  &:hover {
    background: var(--accent-dim);
    border-style: solid;
    transform: translateY(-1px);
    svg { transform: rotate(-180deg); }
  }
}

/* Chip rows */
.chip-row { gap: 14px; }

.row-label {
  flex-shrink: 0;
  width: 84px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding-top: 4px;
}
.row-eyebrow {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.22em;
  color: var(--text-secondary);
}
.row-sparkle {
  font-size: 11px;
  color: var(--accent);
  opacity: 0.6;
}

.chip-track-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
  /* edge fade */
  -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 20px, #000 calc(100% - 28px), transparent 100%);
          mask-image: linear-gradient(90deg, transparent 0, #000 20px, #000 calc(100% - 28px), transparent 100%);
}

.chip-track {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 24px 8px 16px;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: var(--border-color) transparent;

  &::-webkit-scrollbar { height: 6px; }
  &::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 999px;
  }
  &::-webkit-scrollbar-thumb:hover { background: var(--border-accent); }
}

.chip-empty {
  padding: 6px 10px;
  font-family: var(--font-display);
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 700;
  letter-spacing: 0.1em;
}

.chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px 6px 13px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--bg-input);
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
  transition: var(--transition-fast);
  position: relative;

  .chip-name { letter-spacing: 0.02em; }
  .chip-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 16px;
    padding: 0 5px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.04);
    font-family: var(--font-mono);
    font-size: 9.5px;
    font-weight: 700;
    color: var(--text-muted);
  }

  &:hover:not(.active) {
    color: var(--text-primary);
    border-color: var(--border-glow);
    transform: translateY(-1px);
    .chip-count { color: var(--text-secondary); }
  }
}

.chip-cyan.active {
  background: linear-gradient(135deg, rgba(167, 139, 250, 0.95), rgba(192, 132, 252, 0.9));
  border-color: rgba(167, 139, 250, 0.6);
  color: #1a0b2e;
  box-shadow:
    0 0 0 3px rgba(167, 139, 250, 0.18),
    var(--cyan-glow);
  .chip-count {
    background: rgba(26, 11, 46, 0.25);
    color: #1a0b2e;
  }
}

.chip-pink.active {
  background: linear-gradient(135deg, rgba(255, 110, 181, 0.95), rgba(244, 114, 182, 0.9));
  border-color: rgba(255, 110, 181, 0.6);
  color: #1a0b2e;
  box-shadow:
    0 0 0 3px rgba(255, 110, 181, 0.2),
    var(--accent-glow);
  .chip-count {
    background: rgba(26, 11, 46, 0.25);
    color: #1a0b2e;
  }
}

[data-theme='light'] {
  .chip-cyan.active, .chip-pink.active { color: #fff; }
  .chip-cyan.active .chip-count, .chip-pink.active .chip-count { color: #fff; }
}

/* Active filter pills strip */
.active-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
}

.active-label {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.24em;
  color: var(--text-muted);
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 4px 4px 10px;
  border-radius: 999px;
  background: var(--accent-dim);
  border: 1px solid var(--border-accent);
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-primary);
  animation: pill-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes pill-in {
  from { opacity: 0; transform: scale(0.8); }
  to   { opacity: 1; transform: scale(1); }
}
.pill-key {
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.1em;
  color: var(--accent);
  opacity: 0.85;
  text-transform: uppercase;
}
.pill-val {
  color: var(--text-primary);
  letter-spacing: 0.02em;
}
.pill-x {
  width: 18px; height: 18px;
  border-radius: 50%;
  background: rgba(255, 110, 181, 0.18);
  border: none;
  color: var(--accent);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  line-height: 1;
  font-weight: 900;
  transition: var(--transition-fast);
  &:hover {
    background: var(--accent);
    color: #fff;
    transform: rotate(90deg);
  }
}

/* ===== Game Grid ===== */
.game-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 18px;
  @media (min-width: 1280px) {
    grid-template-columns: repeat(auto-fill, minmax(172px, 1fr));
  }
  @media (max-width: 640px) {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
  }
}

.game-card {
  border-radius: 16px;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  cursor: pointer;
  opacity: 0;
  transform: translateY(16px) scale(0.96);
  transition:
    opacity 0.4s ease,
    transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
    border-color 0.2s ease,
    box-shadow 0.2s ease;
  &.visible { opacity: 1; transform: translateY(0) scale(1); }
  &:hover {
    border-color: var(--border-accent);
    box-shadow: 0 12px 40px rgba(255, 110, 181, 0.18), 0 4px 16px rgba(167, 139, 250, 0.12);
    transform: translateY(-5px) scale(1.02) !important;
    .card-overlay { opacity: 1; }
    .cover-img { transform: scale(1.06); }
  }
}

.card-cover {
  position: relative;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: var(--bg-input);
}
.cover-img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: block;
}
.cover-fb {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--bg-input) 0%, rgba(167, 139, 250, 0.08) 100%);
}
.fb-initial {
  font-family: var(--font-display);
  font-size: 48px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  opacity: 0.5;
}

.rating-badge {
  position: absolute;
  top: 8px; right: 8px;
  display: flex; align-items: center; gap: 3px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(10, 6, 20, 0.72);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 110, 181, 0.3);
  font-family: var(--font-hero);
  font-size: 11px;
  font-weight: 700;
  color: var(--amber);
  z-index: 2;
  svg { color: var(--amber); flex-shrink: 0; }
}

/* ===== Wishlist heart button ===== */
.wish-btn {
  position: absolute;
  top: 8px; left: 8px;
  width: 30px; height: 30px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(10, 6, 20, 0.6);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: rgba(245, 238, 255, 0.78);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 4;
  transition:
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
    background 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.25s ease;

  /* idle heart = outline-ish via inner stroke trick */
  .heart-ico {
    transition: transform 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);
    filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.4));
  }

  &:hover {
    background: rgba(20, 8, 36, 0.78);
    border-color: rgba(255, 110, 181, 0.55);
    color: var(--accent);
    transform: scale(1.08);
    .heart-ico { transform: scale(1.12); }
  }

  &:active {
    transform: scale(0.92);
  }

  &.is-on {
    background: linear-gradient(135deg, rgba(255, 110, 181, 0.95), rgba(244, 114, 182, 0.95));
    border-color: rgba(255, 255, 255, 0.4);
    color: #fff;
    box-shadow:
      0 0 0 3px rgba(255, 110, 181, 0.18),
      0 6px 18px rgba(255, 110, 181, 0.45),
      var(--accent-glow);
    .heart-ico { animation: heart-pop 0.42s cubic-bezier(0.34, 1.56, 0.64, 1); }
    .wish-burst { animation: burst-fire 0.6s ease-out; }
  }

  &.is-busy {
    pointer-events: none;
    opacity: 0.7;
    .heart-ico { animation: heart-pulse 0.9s ease-in-out infinite; }
  }
}

@keyframes heart-pop {
  0%   { transform: scale(1); }
  35%  { transform: scale(1.45) rotate(-8deg); }
  60%  { transform: scale(0.9); }
  100% { transform: scale(1); }
}
@keyframes heart-pulse {
  0%, 100% { transform: scale(1);    opacity: 0.7; }
  50%      { transform: scale(1.18); opacity: 1;   }
}

/* Burst particles */
.wish-burst {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0;

  span {
    position: absolute;
    top: 50%; left: 50%;
    width: 4px; height: 4px;
    border-radius: 50%;
    background: var(--accent);
    transform: translate(-50%, -50%);
  }
  span:nth-child(1) { background: var(--accent);  --tx:  18px; --ty: -14px; }
  span:nth-child(2) { background: var(--cyan);    --tx: -18px; --ty: -14px; }
  span:nth-child(3) { background: var(--rose);    --tx:   0px; --ty: -22px; }
  span:nth-child(4) { background: var(--amber);   --tx:  20px; --ty:  14px; }
  span:nth-child(5) { background: var(--violet);  --tx: -20px; --ty:  14px; }
  span:nth-child(6) { background: var(--emerald); --tx:   0px; --ty:  22px; }
}
@keyframes burst-fire {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0;
  }
}
.wish-btn.is-on .wish-burst span {
  animation: burst-particle 0.55s ease-out forwards;
}
@keyframes burst-particle {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(calc(-50% + var(--tx)), calc(-50% + var(--ty))) scale(0.4);
    opacity: 0;
  }
}

/* ===== Wishlist toast ===== */
.wish-toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 18px 11px 14px;
  border-radius: 999px;
  background: rgba(20, 10, 36, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-accent);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(255, 110, 181, 0.08);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  z-index: 999;
  pointer-events: none;

  svg { color: var(--accent); flex-shrink: 0; }
  &.remove svg { color: var(--text-secondary); }

  .toast-msg { display: inline-flex; align-items: baseline; gap: 6px; }
  .toast-name {
    font-weight: 600;
    color: var(--text-secondary);
    max-width: 240px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.toast-fade-enter-active { transition: all 0.32s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-fade-leave-active { transition: all 0.22s ease; }
.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(12px) scale(0.92);
}
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(6px) scale(0.96);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top,
    rgba(13, 5, 25, 0.97) 0%,
    rgba(13, 5, 25, 0.85) 50%,
    rgba(13, 5, 25, 0.4) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 14px;
  z-index: 3;
}
[data-theme='light'] .card-overlay {
  background: linear-gradient(to top,
    rgba(20, 5, 35, 0.95) 0%,
    rgba(20, 5, 35, 0.8) 50%,
    rgba(20, 5, 35, 0.3) 100%);
}
.overlay-content { display: flex; flex-direction: column; gap: 6px; }
.ov-desc {
  font-size: 11px; line-height: 1.5;
  color: rgba(245, 238, 255, 0.85);
  font-family: var(--font-display);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ov-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.ov-dev, .ov-time {
  display: flex; align-items: center; gap: 5px;
  font-size: 10px;
  color: rgba(245, 238, 255, 0.55);
  font-family: var(--font-display);
  font-weight: 600;
}

.card-body {
  padding: 10px 12px 12px;
  display: flex; flex-direction: column; gap: 7px;
}
.card-name {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 800;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}
.card-platforms {
  display: flex; flex-wrap: wrap; gap: 4px;
  min-height: 20px;
}

.tag {
  font-family: var(--font-display);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.04em;
  padding: 2px 7px;
  border-radius: 999px;
  white-space: nowrap;
  line-height: 1.5;
}
.platform-tag {
  background: rgba(167, 139, 250, 0.14);
  color: var(--cyan);
  border: 1px solid rgba(167, 139, 250, 0.22);
}
.type-tag {
  background: rgba(255, 110, 181, 0.14);
  color: var(--accent);
  border: 1px solid rgba(255, 110, 181, 0.22);
}

/* ===== Skeleton ===== */
.sk-card {
  border-radius: 16px;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}
.sk-cover {
  aspect-ratio: 3 / 4;
  background: linear-gradient(90deg,
    var(--bg-input) 25%, rgba(167, 139, 250, 0.06) 50%, var(--bg-input) 75%);
  background-size: 200% 100%;
  animation: shimmer-sk 1.5s ease infinite;
}
.sk-body { padding: 10px 12px 12px; display: flex; flex-direction: column; gap: 7px; }
.sk-line {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg,
    var(--bg-input) 25%, rgba(167, 139, 250, 0.06) 50%, var(--bg-input) 75%);
  background-size: 200% 100%;
  animation: shimmer-sk 1.5s ease infinite;
  &.short { height: 9px; }
}
@keyframes shimmer-sk {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== Empty state ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 80px 0;
}
.empty-icon { font-size: 52px; animation: float-sparkle 3s ease-in-out infinite; }
@keyframes float-sparkle {
  0%, 100% { transform: translateY(0); }
  50%       { transform: translateY(-10px); }
}
.empty-text {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: var(--text-secondary);
}
.empty-reset {
  padding: 9px 24px;
  border-radius: 999px;
  border: 1.5px solid var(--border-accent);
  background: var(--accent-dim);
  color: var(--accent);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: var(--transition-bounce);
  &:hover {
    background: var(--accent);
    color: #fff;
    transform: translateY(-2px) scale(1.04);
    box-shadow: 0 6px 20px rgba(255, 110, 181, 0.45);
  }
}

/* ===== Footer / Load more ===== */
.footer-area {
  display: flex; justify-content: center;
  padding: 12px 0 8px;
}
.loading-dots {
  display: flex; gap: 7px; align-items: center;
  span {
    display: block;
    width: 8px; height: 8px;
    border-radius: 50%;
    background: var(--accent);
    animation: dot-bounce 1.2s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.15s; background: var(--cyan); }
    &:nth-child(3) { animation-delay: 0.3s;  background: var(--emerald); }
  }
}
@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.4; }
  40%           { transform: scale(1);   opacity: 1; }
}
.load-more-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 32px;
  border-radius: 999px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: var(--transition-bounce);
  .lm-count {
    font-size: 11px;
    font-family: var(--font-mono);
    color: var(--text-muted);
    margin-left: 2px;
  }
  &:hover {
    border-color: var(--border-accent);
    background: linear-gradient(135deg, var(--accent-dim), var(--cyan-dim));
    color: var(--accent);
    transform: translateY(-3px) scale(1.03);
    box-shadow: 0 8px 24px rgba(255, 110, 181, 0.18);
    .lm-count { color: var(--text-secondary); }
  }
}
.all-loaded {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  opacity: 0.7;
}

.rawg-attribution {
  margin: 2rem 0 1rem;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  a {
    color: var(--cyan);
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
}
</style>
