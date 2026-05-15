<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRecordStore } from '@/stores/record'
import { updateUserInfoApi, uploadAvatarApi } from '@/api/auth'

const userStore = useUserStore()
const recordStore = useRecordStore()

const isEditing = ref(false)
const form = ref({ nickname: '', email: '', phone: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')
const previewAvatar = ref<string | null>(null)

onMounted(async () => {
  await Promise.all([
    userStore.fetchUserInfo(),
    recordStore.fetchStats()
  ])
  form.value = {
    nickname: userStore.user.nickname,
    email: userStore.user.email,
    phone: userStore.user.phone
  }
})

const displayName = computed(() =>
  userStore.user.nickname || userStore.user.username || 'Player'
)

const totalHours = computed(() =>
  (Number(recordStore.stats.totalMinutes) / 60 || 0).toFixed(1)
)
const weekHours = computed(() =>
  (Number(recordStore.stats.weekMinutes) / 60 || 0).toFixed(1)
)
const gameCount = computed(() => Number(recordStore.stats.gameCount) || 0)
const recordCount = computed(() => Number(recordStore.stats.recordCount) || 0)

const avatarInitial = computed(() =>
  (displayName.value).charAt(0).toUpperCase()
)

/* Player level — based on total hours */
const playerLevel = computed(() => {
  const h = Number(totalHours.value)
  if (h >= 500) return { lv: 'S', label: 'Legend', color: '#FF6EB5' }
  if (h >= 200) return { lv: 'A', label: 'Veteran', color: '#C084FC' }
  if (h >= 80)  return { lv: 'B', label: 'Regular', color: '#A78BFA' }
  if (h >= 20)  return { lv: 'C', label: 'Rookie', color: '#6EE7B7' }
  return { lv: 'D', label: 'Newbie', color: '#FDE68A' }
})

/* Tick positions for avatar orbit */
function getOrbitStyle(n: number) {
  const angle = (n * 45 - 90) * (Math.PI / 180)
  const r = 64
  return {
    top: `calc(50% + ${Math.sin(angle) * r}px - 3px)`,
    left: `calc(50% + ${Math.cos(angle) * r}px - 3px)`
  }
}

function startEdit() {
  isEditing.value = true
  form.value = {
    nickname: userStore.user.nickname,
    email: userStore.user.email,
    phone: userStore.user.phone
  }
}

async function saveProfile() {
  loading.value = true
  error.value = ''
  try {
    const res = await updateUserInfoApi({ ...form.value })
    if (res.code === 200) {
      await userStore.fetchUserInfo()
      isEditing.value = false
      success.value = '保存成功 ✨'
      setTimeout(() => (success.value = ''), 2500)
    } else {
      error.value = res.msg || '保存失败'
    }
  } catch {
    error.value = '保存失败'
  } finally {
    loading.value = false
  }
}

function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.[0]) return
  const file = input.files[0]
  if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) {
    error.value = '请选择小于10MB的图片文件'
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    previewAvatar.value = String(reader.result)
    uploadAvatar(file)
  }
  reader.readAsDataURL(file)
}

async function uploadAvatar(file: File) {
  loading.value = true
  error.value = ''
  try {
    const fd = new FormData()
    fd.append('avatar', file)
    const res = await uploadAvatarApi(fd)
    if (res.code === 200 && typeof res.data === 'string') {
      const avatarUrl = res.data + '?t=' + Date.now()
      previewAvatar.value = avatarUrl
      await userStore.fetchUserInfo()
      success.value = '头像上传成功 ✨'
      setTimeout(() => (success.value = ''), 2500)
    }
  } catch {
    error.value = '头像上传失败'
    previewAvatar.value = null
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="profile-page">

    <!-- ══════════ HERO BANNER ══════════ -->
    <section class="hero-banner">
      <!-- Background decoration -->
      <div class="hero-bg-orb orb1" />
      <div class="hero-bg-orb orb2" />
      <div class="hero-bg-orb orb3" />
      <!-- Polka dot texture -->
      <div class="hero-dots" aria-hidden="true" />

      <!-- Sparkles -->
      <span class="sp sp1" aria-hidden="true">✦</span>
      <span class="sp sp2" aria-hidden="true">✧</span>
      <span class="sp sp3" aria-hidden="true">★</span>
      <span class="sp sp4" aria-hidden="true">✦</span>
      <span class="sp sp5" aria-hidden="true">✧</span>

      <!-- Avatar column -->
      <div class="hero-avatar-col">
        <label class="avatar-ring-wrap" for="avatarFileInput" title="更换头像">
          <!-- Orbit dots -->
          <div
            v-for="n in 8"
            :key="n"
            class="av-orbit-dot"
            :class="n % 2 === 0 ? 'big' : ''"
            :style="getOrbitStyle(n)"
          />
          <!-- Spinning rings -->
          <div class="av-ring r1" />
          <div class="av-ring r2" />
          <!-- Avatar -->
          <div class="avatar-inner">
            <div v-if="!previewAvatar && !userStore.user.avatar" class="avatar-initial">
              {{ avatarInitial }}
            </div>
            <img
              v-else
              :src="previewAvatar || userStore.user.avatar"
              alt="avatar"
              class="avatar-img"
            />
            <div class="avatar-hover-mask">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                <circle cx="12" cy="13" r="4" />
              </svg>
              <span>更换头像</span>
            </div>
          </div>
        </label>
        <input id="avatarFileInput" type="file" accept="image/*" class="hidden-file-input" @change="onAvatarChange" />

        <!-- Level badge below avatar -->
        <div class="level-badge" :style="{ '--lv-color': playerLevel.color }">
          <span class="lv-rank">{{ playerLevel.lv }}</span>
          <span class="lv-label">{{ playerLevel.label }}</span>
        </div>
      </div>

      <!-- Info column -->
      <div class="hero-info-col">
        <div class="hero-eyebrow">
          <span class="hero-tag">✦ PLAYER</span>
          <span class="hero-id">ID · {{ userStore.user.id || '—' }}</span>
        </div>

        <h1 class="hero-name">{{ displayName }}</h1>

        <p class="hero-username">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" width="13" height="13">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
          </svg>
          {{ userStore.user.username }}
        </p>

        <!-- Quick stat pills -->
        <div class="hero-stats-row">
          <div class="hs-pill pink">
            <span class="hs-val">{{ totalHours }}</span>
            <span class="hs-lbl">小时</span>
          </div>
          <div class="hs-sep" aria-hidden="true" />
          <div class="hs-pill purple">
            <span class="hs-val">{{ gameCount }}</span>
            <span class="hs-lbl">游戏</span>
          </div>
          <div class="hs-sep" aria-hidden="true" />
          <div class="hs-pill mint">
            <span class="hs-val">{{ recordCount }}</span>
            <span class="hs-lbl">打卡</span>
          </div>
          <div class="hs-sep" aria-hidden="true" />
          <div class="hs-pill gold">
            <span class="hs-val">{{ weekHours }}</span>
            <span class="hs-lbl">本周时</span>
          </div>
        </div>
      </div>

      <!-- Action buttons top-right -->
      <div class="hero-actions">
        <button v-if="!isEditing" class="edit-hero-btn" @click="startEdit">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
          编辑资料
        </button>
      </div>
    </section>

    <!-- ══════════ MAIN GRID ══════════ -->
    <div class="main-grid">

      <!-- Account Info card -->
      <div class="kawaii-card info-card">
        <div class="kc-header">
          <div class="kc-dots">
            <i class="d-pink"/><i class="d-purple"/><i class="d-mint"/>
          </div>
          <span class="kc-title">✦ 账号信息</span>
          <span class="kc-badge">ACCOUNT</span>
        </div>

        <div class="info-list">
          <div class="info-row">
            <div class="info-icon pink">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <div class="info-content">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ userStore.user.username || '—' }}</span>
            </div>
          </div>

          <div class="info-row">
            <div class="info-icon purple">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <div class="info-content">
              <span class="info-label">昵称</span>
              <span class="info-value">{{ userStore.user.nickname || '未设置' }}</span>
            </div>
          </div>

          <div class="info-row">
            <div class="info-icon mint">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
            </div>
            <div class="info-content">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ userStore.user.email || '未设置' }}</span>
            </div>
          </div>

          <div class="info-row">
            <div class="info-icon gold">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.14 13.5a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.05 2.7h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 21 16.92z" />
              </svg>
            </div>
            <div class="info-content">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ userStore.user.phone || '未设置' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Stats cards column -->
      <div class="stats-col">
        <div class="stat-card stat-pink">
          <div class="sc-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
            </svg>
          </div>
          <div class="sc-body">
            <span class="sc-val">{{ totalHours }}<em>h</em></span>
            <span class="sc-lbl">总游玩时长</span>
          </div>
          <span class="sc-deco">⏱</span>
        </div>

        <div class="stat-card stat-purple">
          <div class="sc-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <rect x="2" y="6" width="20" height="12" rx="2" /><path d="M6 12h4m6-2v4" />
            </svg>
          </div>
          <div class="sc-body">
            <span class="sc-val">{{ gameCount }}</span>
            <span class="sc-lbl">收录游戏</span>
          </div>
          <span class="sc-deco">🎮</span>
        </div>

        <div class="stat-card stat-mint">
          <div class="sc-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <path d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="sc-body">
            <span class="sc-val">{{ recordCount }}</span>
            <span class="sc-lbl">打卡次数</span>
          </div>
          <span class="sc-deco">✅</span>
        </div>

        <div class="stat-card stat-gold">
          <div class="sc-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <path d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
          </div>
          <div class="sc-body">
            <span class="sc-val">{{ weekHours }}<em>h</em></span>
            <span class="sc-lbl">本周时长</span>
          </div>
          <span class="sc-deco">📈</span>
        </div>
      </div>

    </div>

    <!-- ══════════ EDIT PANEL ══════════ -->
    <Transition name="edit-panel">
      <div v-if="isEditing" class="edit-overlay" @click.self="isEditing = false">
        <div class="edit-panel">
          <!-- Panel decoration -->
          <div class="ep-bg-orb" />
          <span class="ep-sp1" aria-hidden="true">✦</span>
          <span class="ep-sp2" aria-hidden="true">✧</span>

          <div class="ep-header">
            <div class="ep-title-group">
              <span class="ep-eyebrow">✦ EDIT</span>
              <h2 class="ep-title">编辑个人资料</h2>
            </div>
            <button class="ep-close" @click="isEditing = false" aria-label="关闭">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <form @submit.prevent="saveProfile" class="ep-form">
            <div class="ep-field">
              <label class="ep-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
                </svg>
                昵称
              </label>
              <input
                v-model="form.nickname"
                class="ep-input"
                placeholder="设置你的昵称"
                autocomplete="off"
              />
            </div>

            <div class="ep-field">
              <label class="ep-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
                邮箱
              </label>
              <input
                v-model="form.email"
                type="email"
                class="ep-input"
                placeholder="your@email.com"
                autocomplete="off"
              />
            </div>

            <div class="ep-field">
              <label class="ep-label">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="13" height="13">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.14 13.5a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.05 2.7h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 21 16.92z" />
                </svg>
                手机号
              </label>
              <input
                v-model="form.phone"
                type="tel"
                class="ep-input"
                placeholder="138 0000 0000"
                autocomplete="off"
              />
            </div>

            <div class="ep-actions">
              <button type="submit" class="ep-save" :disabled="loading">
                <svg v-if="loading" class="spin-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="15" height="15">
                  <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48 2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48 2.83-2.83" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                {{ loading ? '保存中…' : '保存更改' }}
              </button>
              <button type="button" class="ep-cancel" @click="isEditing = false">取消</button>
            </div>
          </form>
        </div>
      </div>
    </Transition>

    <!-- Toast notifications -->
    <Transition name="toast">
      <div v-if="error" class="toast toast-error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="14" height="14">
          <circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" />
        </svg>
        {{ error }}
      </div>
    </Transition>
    <Transition name="toast">
      <div v-if="success" class="toast toast-success">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" width="14" height="14">
          <circle cx="12" cy="12" r="10" /><polyline points="9 12 11 14 15 10" />
        </svg>
        {{ success }}
      </div>
    </Transition>

  </div>
</template>

<style lang="scss" scoped>
/* ======================================================
   Profile Page — Kawaii Magical Girl Theme
   ====================================================== */
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
  animation: fadeSlideIn 0.5s ease both;
}

/* ══════════ HERO BANNER ══════════ */
.hero-banner {
  position: relative;
  border-radius: var(--border-radius-xl);
  border: 1px solid var(--border-accent);
  padding: 36px 40px;
  overflow: hidden;
  min-height: 200px;
  display: flex;
  align-items: center;
  gap: 36px;

  background:
    radial-gradient(ellipse at 8% 60%, rgba(255, 110, 181, 0.12) 0%, transparent 52%),
    radial-gradient(ellipse at 80% 20%, rgba(167, 139, 250, 0.09) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 90%, rgba(192, 132, 252, 0.06) 0%, transparent 45%),
    var(--bg-card);

  @media (max-width: 680px) {
    flex-direction: column;
    align-items: flex-start;
    padding: 28px 24px;
    gap: 20px;
  }
}

/* BG orbs */
.hero-bg-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;

  &.orb1 {
    width: 320px; height: 320px;
    top: -80px; right: -60px;
    background: radial-gradient(circle, rgba(255,110,181,0.07) 0%, transparent 70%);
    animation: orb-drift 8s ease-in-out infinite;
  }
  &.orb2 {
    width: 200px; height: 200px;
    bottom: -60px; left: 30%;
    background: radial-gradient(circle, rgba(167,139,250,0.08) 0%, transparent 70%);
    animation: orb-drift 11s ease-in-out infinite reverse;
  }
  &.orb3 {
    width: 140px; height: 140px;
    top: -20px; left: 55%;
    background: radial-gradient(circle, rgba(192,132,252,0.06) 0%, transparent 70%);
    animation: orb-drift 14s ease-in-out infinite 3s;
  }
}

@keyframes orb-drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(12px, -16px) scale(1.05); }
}

/* Polka dot texture */
.hero-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255,110,181,0.045) 1px, transparent 1px);
  background-size: 26px 26px;
  pointer-events: none;
  border-radius: inherit;
}

/* Sparkles */
.sp {
  position: absolute;
  pointer-events: none;
  z-index: 0;

  &.sp1 { top: 12%; right: 18%; color: var(--accent); font-size: 16px; animation: sparkle-float 3.2s ease-in-out infinite; }
  &.sp2 { top: 68%; right: 10%; color: var(--cyan); font-size: 10px; animation: sparkle-float 2.8s ease-in-out infinite 1.1s; }
  &.sp3 { top: 32%; right: 26%; color: var(--amber); font-size: 11px; animation: sparkle-float 3.6s ease-in-out infinite 2.2s; }
  &.sp4 { bottom: 20%; left: 42%; color: var(--rose); font-size: 8px; animation: sparkle-float 2.5s ease-in-out infinite 0.6s; }
  &.sp5 { top: 50%; right: 38%; color: var(--violet); font-size: 9px; animation: sparkle-float 4s ease-in-out infinite 1.8s; }
}

@keyframes sparkle-float {
  0%, 100% { transform: translateY(0) rotate(0deg) scale(1); opacity: 0.4; }
  50% { transform: translateY(-10px) rotate(22deg) scale(1.18); opacity: 0.85; }
}

/* ─── Avatar ─── */
.hero-avatar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.avatar-ring-wrap {
  position: relative;
  width: 148px;
  height: 148px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

/* Orbit dots */
.av-orbit-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255, 110, 181, 0.4);
  pointer-events: none;

  &.big {
    width: 7px;
    height: 7px;
    background: var(--accent);
    box-shadow: 0 0 7px var(--accent);
  }
}

/* Spinning rings around avatar */
.av-ring {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;

  &.r1 {
    width: 148px; height: 148px;
    border: 1.5px solid rgba(255, 110, 181, 0.22);
    border-top-color: var(--accent);
    border-right-color: var(--rose);
    animation: spin-ring 12s linear infinite;
  }
  &.r2 {
    width: 112px; height: 112px;
    border: 1.5px solid rgba(167, 139, 250, 0.18);
    border-left-color: var(--cyan);
    border-bottom-color: var(--violet);
    animation: spin-ring 8s linear infinite reverse;
  }
}

@keyframes spin-ring {
  to { transform: rotate(360deg); }
}

.avatar-inner {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(255, 110, 181, 0.45);
  box-shadow: 0 0 28px rgba(255, 110, 181, 0.4), 0 0 8px rgba(167, 139, 250, 0.3);
  z-index: 1;
  transition: var(--transition-bounce);

  .avatar-ring-wrap:hover & {
    transform: scale(1.05);
    box-shadow: 0 0 38px rgba(255, 110, 181, 0.65), 0 0 14px rgba(167, 139, 250, 0.4);
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initial {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 900;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0,0,0,0.3);
}

.avatar-hover-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(19, 8, 32, 0.72);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.22s;
  color: #fff;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;

  svg { width: 20px; height: 20px; stroke: var(--accent); }

  .avatar-ring-wrap:hover & { opacity: 1; }
}

.hidden-file-input {
  position: absolute;
  width: 0; height: 0;
  overflow: hidden; opacity: 0;
}

/* Level badge */
.level-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px 5px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--lv-color) 14%, transparent);
  border: 1px solid color-mix(in srgb, var(--lv-color) 38%, transparent);
  transition: var(--transition-bounce);

  &:hover { transform: scale(1.05); }
}

.lv-rank {
  font-family: var(--font-hero);
  font-size: 17px;
  font-weight: 900;
  color: var(--lv-color);
  filter: drop-shadow(0 0 8px var(--lv-color));
  line-height: 1;
}

.lv-label {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.05em;
}

/* ─── Hero Info ─── */
.hero-info-col {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.hero-tag {
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 800;
  color: var(--accent);
  letter-spacing: 0.12em;
  background: var(--accent-dim);
  padding: 3px 11px;
  border-radius: 999px;
  border: 1px solid rgba(255, 110, 181, 0.28);
}

.hero-id {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.08em;
}

.hero-name {
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 900;
  line-height: 1.04;
  letter-spacing: 0.01em;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 2px 12px rgba(255,110,181,0.25));

  @media (max-width: 640px) { font-size: 30px; }
}

.hero-username {
  display: flex;
  align-items: center;
  gap: 5px;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 20px;

  svg { opacity: 0.6; flex-shrink: 0; }
}

/* Quick stats pills row */
.hero-stats-row {
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
  background: rgba(255,110,181,0.04);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  padding: 8px 16px;
  width: fit-content;
}

.hs-pill {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2px 16px;

  &.pink  .hs-val { color: var(--accent); }
  &.purple .hs-val { color: var(--cyan); }
  &.mint  .hs-val { color: var(--emerald); }
  &.gold  .hs-val { color: var(--amber); }
}

.hs-val {
  font-family: var(--font-hero);
  font-size: 22px;
  font-weight: 900;
  line-height: 1;
}

.hs-lbl {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.05em;
  margin-top: 2px;
}

.hs-sep {
  width: 1px;
  height: 30px;
  background: var(--border-color);
  flex-shrink: 0;
}

/* Edit button */
.hero-actions {
  position: absolute;
  top: 24px;
  right: 28px;
  z-index: 2;

  @media (max-width: 680px) {
    position: static;
    align-self: flex-end;
  }
}

.edit-hero-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 20px;
  border-radius: 999px;
  background: rgba(255, 110, 181, 0.1);
  border: 1px solid rgba(255, 110, 181, 0.3);
  color: var(--accent);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  transition: var(--transition-bounce);
  white-space: nowrap;

  svg { width: 14px; height: 14px; }

  &:hover {
    background: rgba(255, 110, 181, 0.18);
    border-color: rgba(255, 110, 181, 0.6);
    transform: translateY(-2px) scale(1.04);
    box-shadow: 0 6px 20px rgba(255, 110, 181, 0.25);
  }

  &:active { transform: scale(0.97); }
}

/* ══════════ MAIN GRID ══════════ */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 20px;
  align-items: start;

  @media (max-width: 860px) {
    grid-template-columns: 1fr;
  }
}

/* ── Account Info Card ── */
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

.kc-header {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 18px;
  background: rgba(255, 110, 181, 0.045);
  border-bottom: 1px solid var(--border-color);
}

.kc-dots {
  display: flex;
  gap: 5px;

  i {
    display: block;
    width: 9px; height: 9px;
    border-radius: 50%;

    &.d-pink   { background: var(--accent); box-shadow: 0 0 6px rgba(255,110,181,0.6); }
    &.d-purple { background: var(--cyan); box-shadow: 0 0 6px rgba(167,139,250,0.5); }
    &.d-mint   { background: var(--emerald); box-shadow: 0 0 6px rgba(110,231,183,0.5); }
  }
}

.kc-title {
  flex: 1;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.05em;
}

.kc-badge {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--accent);
  background: var(--accent-dim);
  border: 1px solid rgba(255, 110, 181, 0.28);
  border-radius: 999px;
  padding: 2px 11px;
}

.info-list {
  padding: 8px 0;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  transition: var(--transition-fast);
  border-bottom: 1px solid rgba(200, 120, 255, 0.05);

  &:last-child { border-bottom: none; }

  &:hover {
    background: rgba(255, 110, 181, 0.04);
  }
}

.info-icon {
  width: 36px; height: 36px;
  border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: var(--transition-bounce);

  svg { width: 16px; height: 16px; }

  .info-row:hover & { transform: scale(1.12) rotate(-6deg); }

  &.pink   { background: rgba(255,110,181,0.14); color: var(--accent); }
  &.purple { background: rgba(167,139,250,0.14); color: var(--cyan); }
  &.mint   { background: rgba(110,231,183,0.14); color: var(--emerald); }
  &.gold   { background: rgba(253,230,138,0.14); color: var(--amber); }
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.info-label {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.info-value {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Stats Column ── */
.stats-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  border-radius: 18px;
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
  }

  &::after {
    content: '';
    position: absolute;
    bottom: 0; left: 0; right: 0;
    height: 3px;
    border-radius: 0 0 18px 18px;
  }

  &.stat-pink {
    .sc-icon-wrap { background: rgba(255,110,181,0.14); color: var(--accent); }
    .sc-val { color: var(--accent); }
    &::after { background: linear-gradient(90deg, var(--accent), transparent); }
  }
  &.stat-purple {
    .sc-icon-wrap { background: rgba(167,139,250,0.14); color: var(--cyan); }
    .sc-val { color: var(--cyan); }
    &::after { background: linear-gradient(90deg, var(--cyan), transparent); }
  }
  &.stat-mint {
    .sc-icon-wrap { background: rgba(110,231,183,0.14); color: var(--emerald); }
    .sc-val { color: var(--emerald); }
    &::after { background: linear-gradient(90deg, var(--emerald), transparent); }
  }
  &.stat-gold {
    .sc-icon-wrap { background: rgba(253,230,138,0.14); color: var(--amber); }
    .sc-val { color: var(--amber); }
    &::after { background: linear-gradient(90deg, var(--amber), transparent); }
  }
}

.sc-icon-wrap {
  width: 38px; height: 38px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: transform var(--transition-bounce);

  .stat-card:hover & { transform: scale(1.12) rotate(-6deg); }

  svg { width: 18px; height: 18px; }
}

.sc-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sc-val {
  font-family: var(--font-hero);
  font-size: 28px;
  font-weight: 900;
  line-height: 1;

  em {
    font-size: 13px;
    font-weight: 700;
    font-style: normal;
    color: var(--text-secondary);
    margin-left: 2px;
    font-family: var(--font-display);
  }
}

.sc-lbl {
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.06em;
  margin-top: 4px;
}

.sc-deco {
  position: absolute;
  top: 10px; right: 12px;
  font-size: 18px;
  opacity: 0.28;
  pointer-events: none;
  transition: var(--transition-bounce);

  .stat-card:hover & { opacity: 0.6; transform: scale(1.22) rotate(9deg); }
}

/* ══════════ EDIT PANEL ══════════ */
.edit-overlay {
  position: fixed;
  inset: 0;
  background: rgba(19, 8, 32, 0.72);
  backdrop-filter: blur(6px);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.edit-panel {
  position: relative;
  width: 420px;
  max-width: 100%;
  background: var(--bg-secondary);
  border: 1px solid var(--border-accent);
  border-radius: var(--border-radius-xl);
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(255, 110, 181, 0.18), 0 8px 32px rgba(167, 139, 250, 0.15);

  /* Decoration */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: radial-gradient(circle, rgba(255,110,181,0.04) 1px, transparent 1px);
    background-size: 22px 22px;
    pointer-events: none;
  }
}

.ep-bg-orb {
  position: absolute;
  width: 260px; height: 260px;
  top: -80px; right: -60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,110,181,0.1) 0%, transparent 70%);
  pointer-events: none;
}

.ep-sp1 {
  position: absolute;
  top: 14%; right: 20%;
  color: var(--accent); font-size: 14px;
  animation: sparkle-float 3.2s ease-in-out infinite;
  pointer-events: none;
}
.ep-sp2 {
  position: absolute;
  bottom: 22%; left: 12%;
  color: var(--cyan); font-size: 10px;
  animation: sparkle-float 2.8s ease-in-out infinite 1.4s;
  pointer-events: none;
}

.ep-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 28px 28px 0;
  position: relative;
  z-index: 1;
}

.ep-title-group { display: flex; flex-direction: column; gap: 4px; }

.ep-eyebrow {
  font-family: var(--font-display);
  font-size: 9px;
  font-weight: 800;
  color: var(--accent);
  letter-spacing: 0.24em;
  opacity: 0.75;
}

.ep-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ep-close {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: rgba(255, 110, 181, 0.08);
  border: 1px solid rgba(255, 110, 181, 0.2);
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary);
  transition: var(--transition-fast);
  flex-shrink: 0;

  svg { width: 14px; height: 14px; }

  &:hover {
    background: rgba(255, 110, 181, 0.18);
    color: var(--accent);
    border-color: var(--accent);
    transform: rotate(90deg);
  }
}

.ep-form {
  padding: 24px 28px 28px;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ep-field { display: flex; flex-direction: column; gap: 7px; }

.ep-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.05em;

  svg { opacity: 0.6; }
}

.ep-input {
  width: 100%;
  padding: 11px 14px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: 14px;
  outline: none;
  transition: var(--transition-fast);

  &::placeholder { color: var(--text-muted); }

  &:focus {
    border-color: var(--accent);
    box-shadow: 0 0 0 3px rgba(255,110,181,0.14);
    background: rgba(37, 13, 60, 0.8);
  }
}

.ep-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.ep-save {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 12px;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  border: none;
  border-radius: var(--border-radius);
  color: #fff;
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 14px;
  letter-spacing: 0.03em;
  transition: var(--transition-bounce);
  box-shadow: 0 4px 18px rgba(255, 110, 181, 0.4);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 28px rgba(255, 110, 181, 0.6);
  }

  &:active { transform: scale(0.97); }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
    transform: none;
  }
}

.ep-cancel {
  flex: 1;
  padding: 12px;
  background: rgba(167, 139, 250, 0.08);
  border: 1px solid rgba(167, 139, 250, 0.22);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 700;
  transition: var(--transition-fast);

  &:hover {
    background: rgba(167, 139, 250, 0.16);
    border-color: var(--cyan);
    color: var(--cyan);
  }
}

.spin-icon {
  animation: spin-icon 0.8s linear infinite;
}

@keyframes spin-icon {
  to { transform: rotate(360deg); }
}

/* ══════════ PANEL TRANSITIONS ══════════ */
.edit-panel-enter-active,
.edit-panel-leave-active {
  transition: opacity 0.28s ease, transform 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.edit-panel-enter-from,
.edit-panel-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}

/* ══════════ TOAST NOTIFICATIONS ══════════ */
.toast {
  position: fixed;
  bottom: 28px;
  right: 28px;
  z-index: 200;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border-radius: var(--border-radius);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);

  &.toast-error {
    background: rgba(244, 114, 182, 0.15);
    border: 1px solid rgba(244, 114, 182, 0.35);
    color: var(--neon-pink);
  }
  &.toast-success {
    background: rgba(110, 231, 183, 0.12);
    border: 1px solid rgba(110, 231, 183, 0.3);
    color: var(--emerald);
  }
}

.toast-enter-active {
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(60px) scale(0.88);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(60px);
}
</style>
