<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTheme } from '@/composables/useTheme'
import BrandLogo from '@/components/brand/BrandLogo.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { isDark, init: initTheme, toggle: toggleTheme } = useTheme()

const sidebarOpen = ref(true)
const showDropdown = ref(false)

onMounted(() => {
  initTheme()
  userStore.fetchUserInfo()
  if (window.innerWidth < 900) sidebarOpen.value = false
})

function logout() {
  userStore.logout()
  router.push('/login')
}

interface NavItem {
  path: string
  label: string
  iconPath: string
  color: string
}

const navItems: NavItem[] = [
  {
    path: '/',
    label: '仪表盘',
    iconPath: 'M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25',
    color: '#FF6EB5'
  },
  {
    path: '/library',
    label: '游戏库',
    iconPath: 'M20 7H4a2 2 0 00-2 2v6a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2zM16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16',
    color: '#7DD3FC'
  },
  {
    path: '/checkin',
    label: '游戏打卡',
    iconPath: 'M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
    color: '#A78BFA'
  },
  {
    path: '/backlog',
    label: '游戏清单',
    iconPath: 'M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z',
    color: '#6EE7B7'
  },
  {
    path: '/profile',
    label: '个人中心',
    iconPath: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z',
    color: '#FDE68A'
  }
]

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const userInitial = computed(() =>
  (userStore.user.nickname || userStore.user.username || 'G').charAt(0).toUpperCase()
)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '深夜了'
  if (h < 12) return '早上好 ✨'
  if (h < 18) return '下午好 ☀️'
  return '晚上好 🌙'
})

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/': '仪表盘',
    '/library': '游戏库',
    '/checkin': '游戏打卡',
    '/backlog': '游戏清单',
    '/profile': '个人中心'
  }
  return titles[route.path] || 'GRecord'
})
</script>

<template>
  <div class="layout">
    <!-- ======= SIDEBAR ======= -->
    <aside :class="['sidebar', { collapsed: !sidebarOpen }]">
      <!-- Polka-dot kawaii background texture -->
      <div class="sb-dots" aria-hidden="true" />
      <!-- Rainbow shimmer top line -->
      <div class="sb-shimmer" aria-hidden="true" />

      <!-- Logo -->
      <div class="sb-header">
        <router-link to="/" class="logo" :title="!sidebarOpen ? 'GRecord' : undefined">
          <BrandLogo size="md" class="logo-mark" />
          <div v-show="sidebarOpen" class="logo-text-wrap">
            <span class="logo-text">GRecord</span>
            <span class="logo-sub">ゲーム記録 ♪</span>
          </div>
        </router-link>
      </div>

      <!-- Navigation -->
      <nav class="sb-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', { active: isActive(item.path) }]"
          :title="!sidebarOpen ? item.label : undefined"
        >
          <!-- Colored icon bubble -->
          <div class="nav-bubble" :style="{ '--item-color': item.color }">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path :d="item.iconPath" />
            </svg>
          </div>
          <span v-show="sidebarOpen" class="nav-label">{{ item.label }}</span>
          <!-- Active sparkle dot -->
          <span v-if="isActive(item.path) && sidebarOpen" class="nav-star">✦</span>
        </router-link>
      </nav>

      <!-- Footer -->
      <div class="sb-footer">
        <button class="collapse-btn" @click="sidebarOpen = !sidebarOpen">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path :d="sidebarOpen ? 'M15 19l-7-7 7-7' : 'M9 5l7 7-7 7'" />
          </svg>
          <span v-show="sidebarOpen" class="collapse-label">收起侧栏</span>
        </button>
      </div>
    </aside>

    <!-- ======= MAIN WRAPPER ======= -->
    <div class="main-wrap">
      <!-- Topbar -->
      <header class="topbar">
        <div class="topbar-left">
          <button v-if="!sidebarOpen" class="menu-btn" @click="sidebarOpen = true" title="展开侧栏">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          <div class="page-title-area">
            <span class="page-sparkle">✦</span>
            <span class="page-title">{{ pageTitle }}</span>
          </div>
        </div>

        <div class="topbar-right">
          <!-- Day / Night toggle -->
          <button
            class="theme-btn"
            @click="toggleTheme"
            :title="isDark ? '切换亮色模式' : '切换暗色模式'"
            :aria-label="isDark ? '切换亮色模式' : '切换暗色模式'"
          >
            <span class="theme-icon">
              <!-- Sun in dark mode -->
              <svg v-if="isDark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
              <!-- Moon in light mode -->
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
            </span>
          </button>

          <!-- Quick check-in: kawaii pill button -->
          <router-link to="/checkin" class="btn-checkin">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <path d="M12 5v14M5 12h14" />
            </svg>
            今日打卡
          </router-link>

          <!-- User area -->
          <div class="user-trigger" @click="showDropdown = !showDropdown">
            <div class="avatar-orb">
              <img
                v-if="userStore.avatarSrc"
                :key="userStore.avatarSrc"
                :src="userStore.avatarSrc"
                alt=""
                @error="userStore.onAvatarError"
              />
              <span v-else class="avatar-letter">{{ userInitial }}</span>
            </div>
            <div class="user-info">
              <span class="user-name">{{ userStore.user.nickname || userStore.user.username }}</span>
              <span class="user-greeting">{{ greeting }}</span>
            </div>
            <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M6 9l6 6 6-6" />
            </svg>
          </div>

          <!-- Dropdown -->
          <Transition name="menu-fade">
            <div v-if="showDropdown" class="user-menu" @mouseleave="showDropdown = false">
              <router-link to="/profile" class="menu-item" @click="showDropdown = false">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <path d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
                个人中心
              </router-link>
              <div class="menu-divider" />
              <button class="menu-item danger" @click="logout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <path d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                </svg>
                退出登录
              </button>
            </div>
          </Transition>
        </div>
      </header>

      <!-- Main content -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* ======= Root layout ======= */
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-primary);
}

/* ======= Sidebar ======= */
.sidebar {
  width: var(--sidebar-width);
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: relative;
  transition: width 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  z-index: 50;
  /* Soft purple glow on the right edge */
  box-shadow: 2px 0 30px rgba(167, 139, 250, 0.1), inset -1px 0 0 rgba(255, 110, 181, 0.12);

  &.collapsed {
    width: var(--sidebar-width-collapsed);

    .nav-item {
      justify-content: center;
      padding: 11px;
    }
  }
}

/* Kawaii polka-dot texture */
.sb-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 110, 181, 0.07) 1.5px, transparent 1.5px);
  background-size: 20px 20px;
  pointer-events: none;
  z-index: 0;
}

/* Rainbow shimmer accent at top */
.sb-shimmer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(
    90deg,
    #FF6EB5 0%,
    #A78BFA 30%,
    #7DD3FC 55%,
    #6EE7B7 75%,
    #FDE68A 100%
  );
  background-size: 300% 100%;
  animation: shimmer 6s linear infinite;
  z-index: 2;
  border-radius: 0 0 3px 3px;
}

/* Logo */
.sb-header {
  padding: 20px 16px 16px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  text-decoration: none;
}

.logo-mark {
  transition: var(--transition-bounce);

  .logo:hover & :deep(.brand-logo__orb) {
    transform: scale(1.1) rotate(10deg);
    box-shadow:
      0 0 28px rgba(255, 110, 181, 0.85),
      0 0 60px rgba(167, 139, 250, 0.35);
  }
}

.logo-text-wrap {
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.logo-text {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.logo-sub {
  font-size: 10px;
  color: var(--text-secondary);
  margin-top: 3px;
  font-family: var(--font-display);
  letter-spacing: 0.06em;
}

/* Navigation */
.sb-nav {
  flex: 1;
  padding: 6px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 14px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition-fast);
  position: relative;
  white-space: nowrap;
  font-size: 13.5px;
  font-weight: 700;

  &:hover {
    background: rgba(255, 110, 181, 0.08);
    color: var(--text-primary);
    transform: translateX(2px);

    .nav-bubble {
      transform: scale(1.12) rotate(-5deg);
    }
  }

  &.active {
    /* Soft pink-lavender gradient background */
    background: linear-gradient(
      135deg,
      rgba(255, 110, 181, 0.15) 0%,
      rgba(167, 139, 250, 0.1) 100%
    );
    color: var(--accent);

    /* Pill left indicator */
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 20%;
      bottom: 20%;
      width: 4px;
      border-radius: 0 4px 4px 0;
      background: linear-gradient(to bottom, var(--accent), var(--cyan));
      box-shadow: 0 0 10px var(--accent);
    }
  }
}

/* Colored icon bubble */
.nav-bubble {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(var(--item-color), 0.12);
  /* Use the per-item color via custom property */
  background: color-mix(in srgb, var(--item-color, #FF6EB5) 14%, transparent);
  color: var(--item-color, var(--accent));
  transition: var(--transition-bounce);

  .nav-item.active & {
    background: color-mix(in srgb, var(--item-color, #FF6EB5) 22%, transparent);
    box-shadow: 0 0 12px color-mix(in srgb, var(--item-color, #FF6EB5) 50%, transparent);
  }

  svg {
    width: 17px;
    height: 17px;
  }
}

.nav-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: var(--font-display);
}

/* Active sparkle indicator */
.nav-star {
  font-size: 12px;
  color: var(--accent);
  flex-shrink: 0;
  animation: sparkle-pulse 1.8s ease-in-out infinite;
}

@keyframes sparkle-pulse {
  0%, 100% { opacity: 1; transform: scale(1) rotate(0deg); }
  50% { opacity: 0.6; transform: scale(0.85) rotate(15deg); }
}

/* Sidebar footer */
.sb-footer {
  padding: 10px 10px 18px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.collapse-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 8px 12px;
  color: var(--text-secondary);
  transition: var(--transition-fast);
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;

  svg {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
  }

  &:hover {
    background: rgba(255, 110, 181, 0.08);
    color: var(--accent);
    border-color: var(--border-accent);
  }
}

.collapse-label {
  white-space: nowrap;
}

/* ======= Main Wrapper ======= */
.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* Topbar */
.topbar {
  height: var(--header-height);
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  position: relative;
  z-index: 40;

  /* Soft pink-purple gradient underline */
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent 5%, var(--accent) 40%, var(--cyan) 60%, transparent 95%);
    opacity: 0.25;
  }
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-btn {
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: var(--transition-fast);

  svg {
    width: 17px;
    height: 17px;
  }

  &:hover {
    background: var(--accent-dim);
    color: var(--accent);
    border-color: var(--border-accent);
  }
}

.page-title-area {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-sparkle {
  font-size: 13px;
  color: var(--accent);
  animation: sparkle-pulse 2.5s ease-in-out infinite;
}

.page-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

/* ── Theme Toggle ── */
.theme-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: none;
  border: 1.5px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: var(--transition-bounce);
  overflow: hidden;
  position: relative;

  .theme-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  svg {
    width: 17px;
    height: 17px;
  }

  &:hover {
    background: linear-gradient(135deg, var(--accent-dim), var(--cyan-dim));
    color: var(--accent);
    border-color: var(--border-accent);
    box-shadow: 0 0 16px rgba(255, 110, 181, 0.3), 0 0 30px rgba(167, 139, 250, 0.15);

    .theme-icon {
      transform: rotate(25deg) scale(1.15);
    }
  }
}

/* ── Kawaii pill check-in button ── */
.btn-checkin {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  text-decoration: none;
  transition: var(--transition-bounce);
  box-shadow: 0 4px 18px rgba(255, 110, 181, 0.4), 0 2px 8px rgba(167, 139, 250, 0.2);
  white-space: nowrap;
  font-family: var(--font-display);
  letter-spacing: 0.03em;
  position: relative;
  overflow: hidden;

  /* Inner shimmer on hover */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 60%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.25), transparent);
    transition: left 0.55s ease;
  }

  svg {
    width: 14px;
    height: 14px;
  }

  &:hover {
    transform: translateY(-3px) scale(1.04);
    box-shadow: 0 8px 28px rgba(255, 110, 181, 0.6), 0 4px 14px rgba(167, 139, 250, 0.35);
    color: #fff;

    &::before {
      left: 140%;
    }
  }

  &:active {
    transform: scale(0.96);
  }
}

/* User trigger */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 10px;
  border-radius: 14px;
  cursor: pointer;
  transition: var(--transition-fast);
  user-select: none;

  &:hover {
    background: rgba(255, 110, 181, 0.08);
  }
}

/* Avatar orb with rainbow ring */
.avatar-orb {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--cyan));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  /* 强制 GPU 合成层，确保 border-radius + overflow:hidden 裁剪从首帧生效 */
  transform: translateZ(0);
  /* Double ring effect */
  box-shadow:
    0 0 0 2px var(--bg-secondary),
    0 0 0 4px var(--accent),
    0 0 14px rgba(255, 110, 181, 0.4);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-letter {
  font-weight: 900;
  font-size: 15px;
  color: #fff;
  font-family: var(--font-display);
}

.user-info {
  display: flex;
  flex-direction: column;

  @media (max-width: 640px) {
    display: none;
  }
}

.user-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  font-family: var(--font-display);
}

.user-greeting {
  font-size: 11px;
  color: var(--text-secondary);
}

.chevron {
  width: 13px;
  height: 13px;
  color: var(--text-secondary);
}

/* Dropdown */
.user-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  overflow: hidden;
  min-width: 182px;
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 110, 181, 0.08),
    0 0 30px rgba(167, 139, 250, 0.08);
  z-index: 200;
  animation: slide-up 0.18s ease;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 16px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  text-decoration: none;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  transition: var(--transition-fast);
  font-family: var(--font-display);

  svg {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    color: var(--text-secondary);
  }

  &:hover {
    background: rgba(255, 110, 181, 0.08);
  }

  &.danger {
    color: var(--rose);

    svg { color: var(--rose); }

    &:hover {
      background: var(--rose-dim);
    }
  }
}

.menu-divider {
  height: 1px;
  background: var(--border-color);
  margin: 3px 0;
}

/* Main content */
.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 26px;
}

/* ======= Transitions ======= */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.97);
}

/* ======= Responsive ======= */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 100;

    &.collapsed {
      transform: translateX(-100%);
      width: var(--sidebar-width);
    }
  }

  .main-content {
    padding: 16px;
  }
}
</style>
