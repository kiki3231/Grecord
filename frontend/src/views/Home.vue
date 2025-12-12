<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfoApi } from '../api/auth'

const navOpen = ref(true)
const showDropdown = ref(false)
const router = useRouter()
const userInfo = ref({
  username: 'User',
  nickname: '',
  avatar: ''
})
const loading = ref(false)

// 获取用户信息
async function getUserInfo() {
  loading.value = true
  try {
    const res = await getUserInfoApi()
    if (res.code === 200) {
      userInfo.value = res.data
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  } finally {
    loading.value = false
  }
}

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

// 组件挂载时获取用户信息
onMounted(() => {
  getUserInfo()
})
</script>

<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <button class="toggle-btn" @click="navOpen = !navOpen">☰</button>
        <div class="logo">
          <span class="logo-icon">G</span>
          <span class="logo-text">GRecord</span>
        </div>
        <nav class="main-nav">
          <a href="#" class="nav-item active">首页</a>
          <a href="#" class="nav-item">发现</a>
          <a href="#" class="nav-item">社区</a>
          <a href="#" class="nav-item">帮助</a>
        </nav>
      </div>
      
      <div class="header-right">
        <div class="search-box">
          <input type="text" placeholder="搜索游戏或用户...">
          <button class="search-btn">🔍</button>
        </div>
        
        <div class="user-actions">
          <button class="action-btn notification-btn">🔔</button>
          <button class="action-btn message-btn">💬</button>
          
          <div class="user-dropdown">
            <div class="user-info" @click="showDropdown = !showDropdown">
              <div class="avatar">
                <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="头像" />
                <span v-else class="avatar-placeholder">{{ userInfo.username.charAt(0) }}</span>
              </div>
              <span class="username">{{ userInfo.nickname || userInfo.username }}</span>
              <span class="arrow">▼</span>
            </div>
            <div v-if="showDropdown" class="dropdown-menu">
              <router-link to="/profile" class="dropdown-item">个人中心</router-link>
              <a href="#" class="dropdown-item">我的收藏</a>
              <a href="#" class="dropdown-item">设置</a>
              <button class="dropdown-item logout" @click="logout">退出登录</button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <div class="main-content">
      <!-- 左侧侧边栏 -->
      <aside :class="['sidebar', { 'sidebar-open': navOpen }]">
        <div class="sidebar-nav">
          <a href="#" class="sidebar-item active">
            <span class="sidebar-icon">📊</span>
            <span class="sidebar-text">仪表盘</span>
          </a>
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">🎮</span>
            <span class="sidebar-text">我的游戏</span>
          </a>
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">✅</span>
            <span class="sidebar-text">打卡记录</span>
          </a>
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">📈</span>
            <span class="sidebar-text">统计报表</span>
          </a>
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">🏆</span>
            <span class="sidebar-text">成就系统</span>
          </a>
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">⚙️</span>
            <span class="sidebar-text">设置</span>
          </a>
        </div>
        
        <div class="sidebar-footer">
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">❓</span>
            <span class="sidebar-text">帮助中心</span>
          </a>
          <a href="#" class="sidebar-item">
            <span class="sidebar-icon">📝</span>
            <span class="sidebar-text">反馈建议</span>
          </a>
        </div>
      </aside>

      <!-- 主内容区域 -->
      <main class="content">
        <div class="content-header">
          <h1>欢迎回来，{{ userInfo.nickname || userInfo.username }}！</h1>
          <p>记录你的游戏历程，追踪你的游戏成就</p>
        </div>

        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon stat-icon-primary">📅</div>
            <div class="stat-content">
              <h3>今日打卡</h3>
              <p class="stat-value">还未打卡</p>
              <button class="primary-btn">立即打卡</button>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-icon stat-icon-success">⏱️</div>
            <div class="stat-content">
              <h3>本周时长</h3>
              <p class="stat-value">0 分钟</p>
              <p class="stat-desc">继续加油，保持记录！</p>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-icon stat-icon-info">🎮</div>
            <div class="stat-content">
              <h3>进行中的游戏</h3>
              <ul class="game-list">
                <li>示例：Elden Ring</li>
                <li>示例：Hades</li>
              </ul>
              <a href="#" class="link">查看全部</a>
            </div>
          </div>
          
          <div class="stat-card">
            <div class="stat-icon stat-icon-warning">🏆</div>
            <div class="stat-content">
              <h3>本周成就</h3>
              <p class="stat-value">0 个新成就</p>
              <a href="#" class="link">探索更多</a>
            </div>
          </div>
        </div>

        <div class="content-sections">
          <div class="section-card">
            <div class="section-header">
              <h2>近期游戏记录</h2>
              <a href="#" class="link">查看全部</a>
            </div>
            <div class="record-list">
              <div class="record-item">
                <div class="record-game">
                  <span class="game-cover">🎮</span>
                  <div class="game-info">
                    <h4>Elden Ring</h4>
                    <p>开放世界 RPG</p>
                  </div>
                </div>
                <div class="record-time">2024-01-15</div>
                <div class="record-duration">2 小时 30 分钟</div>
                <div class="record-status completed">已完成</div>
              </div>
              <div class="record-item">
                <div class="record-game">
                  <span class="game-cover">🎮</span>
                  <div class="game-info">
                    <h4>Hades</h4>
                    <p>动作 Roguelike</p>
                  </div>
                </div>
                <div class="record-time">2024-01-14</div>
                <div class="record-duration">1 小时 45 分钟</div>
                <div class="record-status in-progress">进行中</div>
              </div>
            </div>
          </div>

          <div class="section-card">
            <div class="section-header">
              <h2>游戏时长统计</h2>
              <div class="section-actions">
                <button class="tab-btn active">本周</button>
                <button class="tab-btn">本月</button>
                <button class="tab-btn">本年</button>
              </div>
            </div>
            <div class="chart-placeholder">
              <div class="chart-skeleton"></div>
              <p>图表加载中...</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* CSS变量定义 */
:root {
  --primary-color: #409EFF;
  --primary-dark: #337ECC;
  --success-color: #67C23A;
  --warning-color: #E6A23C;
  --danger-color: #F56C6C;
  --info-color: #909399;
  --text-color: #303133;
  --text-color-secondary: #606266;
  --border-color: #E4E7ED;
  --border-radius: 8px;
  --shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 4px 20px 0 rgba(0, 0, 0, 0.12);
  --transition: all 0.3s ease;
  --sidebar-width: 240px;
  --sidebar-width-collapsed: 72px;
  --header-height: 64px;
  --content-padding: 24px;
}

/* 全局布局 */
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #F5F7FA;
}

/* 顶部导航栏 */
.top-header {
  height: var(--header-height);
  background-color: #fff;
  border-bottom: 1px solid var(--border-color);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
}

.toggle-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--text-color);
  padding: 8px;
  margin-right: 16px;
  border-radius: 4px;
  transition: var(--transition);
}

.toggle-btn:hover {
  background-color: #F5F7FA;
}

.logo {
  display: flex;
  align-items: center;
  margin-right: 32px;
}

.logo-icon {
  font-size: 24px;
  font-weight: bold;
  color: var(--primary-color);
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: rgba(64, 158, 255, 0.1);
  border-radius: 8px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

/* 主导航菜单 */
.main-nav {
  display: flex;
  gap: 16px;
}

.nav-item {
  color: var(--text-color-secondary);
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  transition: var(--transition);
  font-weight: 500;
}

.nav-item:hover {
  color: var(--primary-color);
  background-color: rgba(64, 158, 255, 0.1);
}

.nav-item.active {
  color: var(--primary-color);
  background-color: rgba(64, 158, 255, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 搜索框 */
.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-box input {
  padding: 8px 12px 8px 36px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  outline: none;
  transition: var(--transition);
  width: 240px;
}

.search-box input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  width: 300px;
}

.search-btn {
  position: absolute;
  left: 12px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-color-secondary);
}

/* 用户操作区 */
.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--text-color-secondary);
  padding: 8px;
  border-radius: 4px;
  transition: var(--transition);
  position: relative;
}

.action-btn:hover {
  color: var(--primary-color);
  background-color: rgba(64, 158, 255, 0.1);
}

/* 用户下拉菜单 */
.user-dropdown {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: var(--transition);
}

.user-info:hover {
  background-color: #F5F7FA;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  background-color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 500;
  font-size: 16px;
}

.avatar-placeholder {
  font-size: 16px;
  font-weight: bold;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  margin-right: 8px;
  font-weight: 500;
  color: var(--text-color);
}

.arrow {
  font-size: 12px;
  color: var(--text-color-secondary);
  transition: var(--transition);
}

.user-dropdown:hover .arrow {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: #fff;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  min-width: 180px;
  margin-top: 4px;
  z-index: 1000;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 12px 16px;
  text-align: left;
  border: none;
  background: none;
  color: var(--text-color);
  text-decoration: none;
  cursor: pointer;
  transition: var(--transition);
}

.dropdown-item:hover {
  background-color: #F5F7FA;
  color: var(--primary-color);
}

.dropdown-item.logout {
  color: var(--danger-color);
}

.dropdown-item.logout:hover {
  background-color: rgba(245, 108, 108, 0.1);
}

/* 主内容区域 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: var(--sidebar-width);
  background-color: #fff;
  border-right: 1px solid var(--border-color);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  transition: var(--transition);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar.sidebar-open {
  width: var(--sidebar-width);
}

.sidebar:not(.sidebar-open) {
  width: var(--sidebar-width-collapsed);
}

/* 侧边栏导航 */
.sidebar-nav {
  padding: 16px 0;
}

.sidebar-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: var(--text-color-secondary);
  text-decoration: none;
  transition: var(--transition);
  border-left: 3px solid transparent;
}

.sidebar-item:hover {
  background-color: rgba(64, 158, 255, 0.05);
  color: var(--primary-color);
}

.sidebar-item.active {
  background-color: rgba(64, 158, 255, 0.1);
  color: var(--primary-color);
  border-left-color: var(--primary-color);
}

.sidebar-icon {
  font-size: 18px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

.sidebar-text {
  font-weight: 500;
}

.sidebar:not(.sidebar-open) .sidebar-text {
  display: none;
}

.sidebar:not(.sidebar-open) .sidebar-item {
  justify-content: center;
  padding: 12px 0;
}

/* 侧边栏底部 */
.sidebar-footer {
  margin-top: auto;
  padding: 16px 0;
  border-top: 1px solid var(--border-color);
}

/* 主内容 */
.content {
  flex: 1;
  padding: var(--content-padding);
  overflow-y: auto;
}

.content-header {
  margin-bottom: 24px;
}

.content-header h1 {
  margin: 0 0 8px 0;
  color: var(--text-color);
  font-size: 28px;
  font-weight: 600;
}

.content-header p {
  margin: 0;
  color: var(--text-color-secondary);
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: #fff;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 20px;
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon-primary {
  background-color: rgba(64, 158, 255, 0.1);
  color: var(--primary-color);
}

.stat-icon-success {
  background-color: rgba(103, 194, 58, 0.1);
  color: var(--success-color);
}

.stat-icon-info {
  background-color: rgba(144, 147, 153, 0.1);
  color: var(--info-color);
}

.stat-icon-warning {
  background-color: rgba(230, 162, 60, 0.1);
  color: var(--warning-color);
}

.stat-content {
  flex: 1;
}

.stat-content h3 {
  margin: 0 0 4px 0;
  color: var(--text-color-secondary);
  font-size: 14px;
  font-weight: 500;
}

.stat-value {
  margin: 0 0 8px 0;
  color: var(--text-color);
  font-size: 20px;
  font-weight: 600;
}

.stat-desc {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 13px;
}

/* 按钮样式 */
.primary-btn {
  padding: 6px 12px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition);
}

.primary-btn:hover {
  background-color: var(--primary-dark);
}

/* 游戏列表 */
.game-list {
  margin: 8px 0;
  padding-left: 20px;
}

.game-list li {
  margin-bottom: 4px;
  color: var(--text-color);
  font-size: 14px;
}

/* 链接样式 */
.link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition);
}

.link:hover {
  text-decoration: underline;
}

/* 内容区块 */
.content-sections {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 24px;
}

.section-card {
  background-color: #fff;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  color: var(--text-color);
  font-size: 18px;
  font-weight: 600;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 6px 12px;
  background-color: #F5F7FA;
  color: var(--text-color-secondary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: var(--transition);
}

.tab-btn:hover {
  background-color: #E4E7ED;
  color: var(--text-color);
}

.tab-btn.active {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

/* 记录列表 */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background-color: #F5F7FA;
  border-radius: 4px;
  transition: var(--transition);
}

.record-item:hover {
  background-color: #E4E7ED;
}

.record-game {
  display: flex;
  align-items: center;
  gap: 12px;
}

.game-cover {
  width: 40px;
  height: 40px;
  background-color: var(--primary-color);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.game-info h4 {
  margin: 0 0 4px 0;
  color: var(--text-color);
  font-size: 14px;
  font-weight: 600;
}

.game-info p {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 12px;
}

.record-time, .record-duration {
  color: var(--text-color-secondary);
  font-size: 14px;
}

.record-status {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.record-status.completed {
  background-color: rgba(103, 194, 58, 0.1);
  color: var(--success-color);
}

.record-status.in-progress {
  background-color: rgba(64, 158, 255, 0.1);
  color: var(--primary-color);
}

/* 图表占位符 */
.chart-placeholder {
  height: 300px;
  background-color: #F5F7FA;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-color-secondary);
  border: 2px dashed var(--border-color);
}

.chart-skeleton {
  width: 80%;
  height: 160px;
  background: linear-gradient(90deg, #E4E7ED 25%, #F5F7FA 50%, #E4E7ED 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 16px;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .content-sections {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .top-header {
    padding: 0 16px;
  }
  
  .main-nav {
    display: none;
  }
  
  .search-box input {
    width: 180px;
  }
  
  .search-box input:focus {
    width: 220px;
  }
  
  .sidebar {
    position: fixed;
    left: 0;
    top: var(--header-height);
    height: calc(100vh - var(--header-height));
    z-index: 99;
    transform: translateX(-100%);
  }
  
  .sidebar.sidebar-open {
    transform: translateX(0);
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .content {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .header-right {
    gap: 8px;
  }
  
  .search-box {
    display: none;
  }
  
  .content-header h1 {
    font-size: 24px;
  }
  
  .stat-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>