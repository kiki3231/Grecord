<template>
  <div class="page-root">
    <!-- Top navbar -->
    <header class="topbar">
      <div class="top-left">
        <button class="menu-btn" @click="toggleCollapse">☰</button>
        <div class="logo">GRecord</div>
        <nav class="top-links">
          <a href="/">首页</a>
          <a href="/discover">发现</a>
          <a href="/community">社区</a>
        </nav>
      </div>

      <div class="top-right">
        <div class="search">
          <input placeholder="搜索游戏或用户..." v-model="search" />
        </div>
        <div class="user-mini">
          <img :src="user.avatar || defaultAvatar" alt="avatar" class="mini-avatar" />
          <span class="username">{{ user.nickname || user.username }}</span>
        </div>
      </div>
    </header>

    <div class="content-wrap">
      <!-- Left sidebar -->
      <aside class="sidebar" :class="{ collapsed }">
        <ul class="side-menu">
          <li class="side-item active">📊 仪表盘</li>
          <li class="side-item">🎮 我的游戏</li>
          <li class="side-item">✅ 打卡记录</li>
          <li class="side-item">📈 统计报表</li>
          <li class="side-item">🏆 成就系统</li>
          <li class="side-item">⚙️ 设置</li>
        </ul>
      </aside>

      <!-- Main -->
      <main class="main-area">
        <!-- Welcome row -->
        <section class="welcome-card">
          <div class="welcome-left">
            <h1>欢迎回来，{{ user.nickname || user.username }}！</h1>
            <p class="muted">记录你的游戏历程，追踪你的游戏成就</p>
          </div>
          <div class="welcome-right">
            <div class="stat">
              <small>今日打卡</small>
              <div class="stat-value">还未打卡</div>
            </div>
            <div class="stat">
              <small>本周时长</small>
              <div class="stat-value">0 分钟</div>
            </div>
            <div class="stat">
              <small>本周成就</small>
              <div class="stat-value">0 个</div>
            </div>
          </div>
        </section>

        <!-- Grid: left column narrow for profile card + right column wider for records -->
        <section class="grid-area">
          <div class="left-column">
            <!-- profile card -->
            <div class="card profile-card">
              <div class="avatar-area">
                <label class="avatar-wrapper" for="avatarInput">
                  <div class="avatar-placeholder" v-if="!previewAvatar && !user.avatar">{{ user.username?.charAt(0) || 'U' }}</div>
                  <img v-if="previewAvatar" :src="previewAvatar" alt="avatar preview" class="avatar-img" />
                  <img v-else-if="user.avatar" :src="user.avatar" alt="user avatar" class="avatar-img" />
                  <div class="avatar-mask">更换头像</div>
                </label>
                <input id="avatarInput" ref="avatarInput" type="file" accept="image/*" @change="onAvatarChange" />
              </div>

              <div class="profile-info">
                <h3>{{ user.nickname || user.username }}</h3>
                <p class="muted">{{ user.email || '未设置邮箱' }}</p>
              </div>

              <div class="profile-actions">
                <button class="btn" @click="startEdit">编辑资料</button>
                <button class="btn ghost" @click="logout">退出登录</button>
              </div>
            </div>

            <!-- basic info form -->
            <div class="card edit-card" v-if="isEditing">
              <h4>编辑基本信息</h4>
              <form @submit.prevent="saveProfile">
                <label>昵称</label>
                <input v-model="form.nickname" />

                <label>邮箱</label>
                <input v-model="form.email" type="email" />

                <label>手机号</label>
                <input v-model="form.phone" type="tel" />

                <div class="form-actions">
                  <button class="btn primary" type="submit" :disabled="loading">{{ loading ? '保存中...' : '保存' }}</button>
                  <button class="btn ghost" type="button" @click="cancelEdit">取消</button>
                </div>
              </form>
            </div>
          </div>

          <div class="right-column">
            <!-- recent game records -->
            <div class="card records-card">
              <div class="card-header">
                <h4>近期游戏记录</h4>
                <a class="link-muted" href="/records">查看全部</a>
              </div>

              <ul class="records-list">
                <li v-for="(r, idx) in recentRecords" :key="idx" class="record-item">
                  <div class="record-left">
                    <div class="game-icon">🎮</div>
                    <div>
                      <div class="game-name">{{ r.title }}</div>
                      <div class="game-sub muted">{{ r.category }}</div>
                    </div>
                  </div>
                  <div class="record-right">
                    <div class="record-date">{{ r.date }}</div>
                    <div class="record-duration muted">{{ r.duration }}</div>
                  </div>
                </li>
                <li v-if="recentRecords.length === 0" class="empty muted">暂无记录</li>
              </ul>
            </div>

            <!-- placeholder: other dashboard area -->
            <div class="card placeholder-card">
              <h4>统计 & 成就</h4>
              <p class="muted">这里可以放更详细的统计图、成就墙等。</p>
            </div>
          </div>
        </section>

        <!-- alerts -->
        <div v-if="error" class="alert error">{{ error }}</div>
        <div v-if="success" class="alert success">{{ success }}</div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfoApi, updateUserInfoApi, uploadAvatarApi } from '../api/auth'

const router = useRouter()

// 状态管理
const user = ref({
  username: 'User',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})
const defaultAvatar = '/default-avatar.png'
const previewAvatar = ref<string | null>(null)
const collapsed = ref(false)
const search = ref('')

// 表单和UI状态
const isEditing = ref(false)
const form = ref({ nickname: '', email: '', phone: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')

// 最近游戏记录
const recentRecords = ref([
  { title: 'Elden Ring', category: '开放世界 RPG', date: '2024-01-15', duration: '2 小时 30 分钟' },
  { title: 'Hades', category: '动作 Roguelike', date: '2024-01-14', duration: '1 小时 45 分钟' }
])

// 获取用户信息
async function getUserInfo() {
  loading.value = true
  error.value = ''
  try {
    const res = await getUserInfoApi()
    if (res.code === 200) {
      const newUserInfo = res.data
      // 保留带时间戳的头像URL
      if (user.value.avatar?.includes('?t=')) {
        newUserInfo.avatar = user.value.avatar
      } else if (newUserInfo.avatar?.length < 5) {
        newUserInfo.avatar = ''
      }
      
      user.value = newUserInfo
      form.value = { nickname: newUserInfo.nickname, email: newUserInfo.email, phone: newUserInfo.phone }
      
      if (!previewAvatar.value) {
        previewAvatar.value = newUserInfo.avatar || null
      }
    }
  } catch (e: any) {
    error.value = '获取用户信息失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getUserInfo()
})

// 切换侧边栏
function toggleCollapse() {
  collapsed.value = !collapsed.value
}

// 头像上传处理
const avatarInput = ref<HTMLInputElement | null>(null)

function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.[0]) return
  
  const file = input.files[0]
  // 限制类型和大小
  if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) {
    error.value = '请选择小于10MB的图片文件'
    resetAvatarInput()
    return
  }

  // 预加载图片
  const reader = new FileReader()
  reader.onload = () => {
    previewAvatar.value = String(reader.result)
    uploadAvatar(file)
  }
  reader.readAsDataURL(file)
}

// 上传头像
async function uploadAvatar(file: File) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const fd = new FormData()
    fd.append('avatar', file)
    const res = await uploadAvatarApi(fd)
    
    if (res.code === 200 && typeof res.data === 'string') {
      // 添加时间戳避免缓存
      const avatarUrl = res.data + '?t=' + Date.now()
      previewAvatar.value = avatarUrl
      user.value.avatar = avatarUrl
      success.value = '头像上传成功'
    }
  } catch (e: any) {
    error.value = '头像上传失败'
    previewAvatar.value = user.value.avatar || null
  } finally {
    loading.value = false
    resetAvatarInput()
  }
}

// 重置文件输入
function resetAvatarInput() {
  avatarInput.value?.value && (avatarInput.value.value = '')
}

// 编辑资料
function startEdit() {
  isEditing.value = true
  form.value = { ...user.value }
}

function cancelEdit() {
  isEditing.value = false
  error.value = ''
}

async function saveProfile() {
  loading.value = true
  error.value = ''
  try {
    const res = await updateUserInfoApi({ ...form.value })
    if (res.code === 200) {
      user.value = { ...user.value, ...form.value }
      isEditing.value = false
      success.value = '保存成功'
      setTimeout(() => (success.value = ''), 2500)
    }
  } catch (e: any) {
    error.value = '保存失败'
  } finally {
    loading.value = false
  }
}

// 退出登录
function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
/* 基本布局 */
.page-root {
  min-height: 100vh;
  background: #f6f8fa;
  color: #2b2b2b;
  font-family: "Segoe UI", Roboto, "Helvetica Neue", Arial;
}

/* 顶部导航栏 */
.topbar {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #eef1f4;
  box-shadow: 0 1px 6px rgba(32, 41, 54, 0.04);
}

.top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-weight: 700;
  font-size: 18px;
  margin-left: 6px;
}

.top-links a {
  margin: 0 10px;
  color: #34495e;
  text-decoration: none;
  transition: color 0.2s;
}

.top-links a:hover {
  color: #1e6fff;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.search input {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #e6eaf0;
  min-width: 220px;
  transition: border-color 0.2s;
}

.search input:focus {
  outline: none;
  border-color: #1e6fff;
}

.user-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #f0f6ff;
}

/* 布局 */
.content-wrap {
  display: flex;
  gap: 24px;
  width: calc(100% - 48px);
  max-width: 1200px;
  margin: 26px auto;
  align-items: flex-start;
}

/* 侧边栏 */
.sidebar {
  width: 240px;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
  min-height: 600px;
  flex-shrink: 0;
  transition: width 0.2s;
}

/* 隐藏文件输入框 */
input[type="file"] {
  position: absolute;
  width: 0;
  height: 0;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.sidebar.collapsed {
  width: 72px;
}

.side-menu {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-item {
  padding: 12px 14px;
  border-radius: 8px;
  color: #34495e;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-item:hover {
  background: #f8fafc;
}

.side-item.active {
  background: #f0f6ff;
  color: #1e6fff;
  font-weight: 600;
}

/* 主内容区 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 600px;
}

/* 欢迎卡片 */
.welcome-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
  flex-wrap: wrap;
  gap: 16px;
}

.welcome-card h1 {
  margin: 0;
  font-size: 20px;
}

.welcome-right {
  display: flex;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}

.stat {
  text-align: center;
  padding: 8px 14px;
  border-radius: 8px;
  background: #fbfdff;
  min-width: 120px;
  transition: transform 0.2s;
}

.stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.stat small {
  color: #7b8794;
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
}

.stat .stat-value {
  font-weight: 700;
  font-size: 16px;
  color: #2b2b2b;
}

/* 网格区域 */
.grid-area {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 18px;
  align-items: start;
}

/* 通用卡片 */
.card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.2s;
}

.card:hover {
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

/* 个人资料卡片 */
.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
  padding: 24px 18px;
}

.avatar-wrapper {
  display: block;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 6px solid #f0f6ff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.07);
  flex-shrink: 0;
}

.avatar-img, .avatar-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.avatar-placeholder {
  font-size: 36px;
  background: linear-gradient(135deg, #f0f4ff, #e8f1ff);
  color: #2b5ac9;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(0, 0, 0, 0.36);
  opacity: 0;
  transition: opacity 0.18s;
  font-size: 14px;
  backdrop-filter: blur(2px);
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.profile-info h3 {
  margin: 0;
  font-size: 18px;
  color: #2b2b2b;
}

/* 操作按钮 */
.profile-actions {
  display: flex;
  gap: 10px;
  width: 100%;
  justify-content: center;
  margin-top: 8px;
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: #f3f5f7;
  color: #2b2b2b;
  font-size: 14px;
  transition: all 0.2s;
}

.btn:hover {
  background: #e9ecef;
}

.btn:active {
  transform: scale(0.98);
}

.btn.ghost {
  background: transparent;
  border: 1px solid #e6eaf0;
  color: #34495e;
}

.btn.ghost:hover {
  background: #f8fafc;
}

.btn.primary {
  background: #1e6fff;
  color: white;
}

.btn.primary:hover {
  background: #0d5bdc;
}

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

/* 编辑表单 */
.edit-card label {
  display: block;
  margin-top: 16px;
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}

.edit-card label:first-child {
  margin-top: 0;
}

.edit-card input {
  width: 100%;
  padding: 10px 12px;
  margin-top: 6px;
  border-radius: 8px;
  border: 1px solid #e6eaf0;
  font-size: 14px;
  transition: border-color 0.2s;
}

.edit-card input:focus {
  outline: none;
  border-color: #1e6fff;
}

/* 游戏记录 */
.records-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.records-card h4 {
  margin: 0;
  font-size: 16px;
  color: #2b2b2b;
}

.records-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #fbfdff;
  margin-bottom: 10px;
  transition: background 0.2s;
}

.record-item:hover {
  background: #f0f6ff;
}

.record-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.game-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff9e6, #fff2d1);
  color: #d97706;
  font-size: 20px;
}

.game-name {
  font-weight: 600;
  font-size: 14px;
  color: #2b2b2b;
}

.record-right {
  text-align: right;
}

.record-date {
  font-size: 14px;
  color: #2b2b2b;
  font-weight: 500;
}

/* 通用样式 */
.muted, .game-sub, .record-duration, .stat small {
  color: #7b8794;
  font-size: 12px;
}

/* 提示信息 */
.alert {
  padding: 12px 14px;
  border-radius: 8px;
  margin-top: 10px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.alert.error {
  background: #fff0f0;
  color: #c81e1e;
  border-left: 3px solid #c81e1e;
}

.alert.success {
  background: #ecf9f1;
  color: #169e48;
  border-left: 3px solid #169e48;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .content-wrap {
    flex-direction: column;
    max-width: 940px;
    margin: 18px auto;
    gap: 16px;
  }

  .sidebar, .main-area {
    width: 100%;
    min-height: auto;
  }

  .grid-area {
    grid-template-columns: 1fr;
  }

  .welcome-right {
    justify-content: center;
    width: 100%;
  }
}

@media (max-width: 576px) {
  .topbar {
    padding: 0 12px;
  }

  .top-links {
    display: none;
  }

  .search input {
    min-width: 150px;
  }

  .stat {
    min-width: 100px;
    padding: 6px 8px;
  }

  .content-wrap {
    width: calc(100% - 24px);
  }
}
</style>