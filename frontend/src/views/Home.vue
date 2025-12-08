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
  <div class="layout">
    <aside :class="{ open: navOpen }">
      <div class="brand">GRecord</div>
      <nav>
        <a class="active">仪表盘</a>
        <a>我的游戏</a>
        <a>打卡记录</a>
        <a>统计报表</a>
        <a>设置</a>
      </nav>
    </aside>

    <section class="main">
      <header>
        <button class="toggle" @click="navOpen = !navOpen">☰</button>
        <div class="spacer"></div>
        <div class="user-dropdown">
          <div class="user-info" @click="showDropdown = !showDropdown">
            <div class="avatar">
              <span>{{ userInfo.username.charAt(0) }}</span>
            </div>
            <span class="username">{{ userInfo.nickname || userInfo.username }}</span>
            <span class="arrow">▼</span>
          </div>
          <div v-if="showDropdown" class="dropdown-menu">
            <router-link to="/profile" class="dropdown-item">个人中心</router-link>
            <button class="dropdown-item logout" @click="logout">退出</button>
          </div>
        </div>
      </header>


      <div class="content">
        <div class="cards">
          <div class="card">
            <h3>今日打卡</h3>
            <p>还未打卡，去记录一下吧～</p>
            <button>新建打卡</button>
          </div>
          <div class="card">
            <h3>本周时长</h3>
            <p>合计 0 分钟</p>
          </div>
          <div class="card">
            <h3>进行中的游戏</h3>
            <ul>
              <li>示例：Elden Ring</li>
              <li>示例：Hades</li>
            </ul>
          </div>
        </div>

        <div class="panel">
          <h3>近期趋势（占位）</h3>
          <div class="placeholder">图表区</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f6f7fb;
}
aside {
  width: 220px;
  background: #111827;
  color: #e5e7eb;
  padding: 16px;
  transition: width 0.2s;
}
aside:not(.open) { width: 64px; }
.brand { font-weight: 700; margin-bottom: 12px; }
nav a {
  display: block;
  padding: 10px 12px;
  border-radius: 8px;
  color: #e5e7eb;
  text-decoration: none;
}
nav a.active, nav a:hover { background: #1f2937; }
.main { flex: 1; display: flex; flex-direction: column; }
header {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 16px;
  box-shadow: 0 1px 0 rgba(0,0,0,.06);
}
.toggle { background: transparent; border: 0; font-size: 18px; cursor: pointer; }
.spacer { flex: 1; }
.user-dropdown {
  position: relative;
  font-size: 14px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.user-info:hover {
  background-color: #f3f4f6;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #646cff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
}
.username {
  color: #374151;
}
.arrow {
  font-size: 10px;
  color: #6b7280;
  transition: transform 0.2s;
}
.user-dropdown:hover .arrow {
  transform: rotate(180deg);
}
.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 140px;
  z-index: 100;
}
.dropdown-item {
  display: block;
  width: 100%;
  padding: 8px 16px;
  text-align: left;
  border: none;
  background: none;
  cursor: pointer;
  color: #374151;
  text-decoration: none;
  font-size: 14px;
}
.dropdown-item:hover {
  background-color: #f3f4f6;
}
.dropdown-item.logout {
  color: #dc2626;
}
.dropdown-item.logout:hover {
  background-color: #fef2f2;
}
.content { padding: 16px; }
.cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(0,0,0,.04);
}
.panel {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(0,0,0,.04);
}
.placeholder {
  height: 200px;
  background: repeating-linear-gradient(45deg,#f3f4f6 0,#f3f4f6 10px,#eef2ff 10px,#eef2ff 20px);
  border-radius: 8px;
}
@media (max-width: 960px) { .cards { grid-template-columns: 1fr; } }
</style>