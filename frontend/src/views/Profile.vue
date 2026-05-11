<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfoApi, uploadAvatarApi } from '@/api/auth'

const userStore = useUserStore()
const isEditing = ref(false)
const form = ref({ nickname: '', email: '', phone: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')
const previewAvatar = ref<string | null>(null)

onMounted(async () => {
  await userStore.fetchUserInfo()
  form.value = {
    nickname: userStore.user.nickname,
    email: userStore.user.email,
    phone: userStore.user.phone
  }
})

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
      success.value = '保存成功'
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
      success.value = '头像上传成功'
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
    <h1 class="page-title">个人中心</h1>

    <div class="profile-grid">
      <div class="card profile-card">
        <label class="avatar-wrapper" for="avatarFile">
          <div class="avatar-placeholder" v-if="!previewAvatar && !userStore.user.avatar">
            {{ (userStore.user.nickname || userStore.user.username || 'U').charAt(0) }}
          </div>
          <img v-else :src="previewAvatar || userStore.user.avatar" alt="" class="avatar-img" />
          <div class="avatar-overlay">更换头像</div>
        </label>
        <input id="avatarFile" type="file" accept="image/*" class="hidden-input" @change="onAvatarChange" />

        <h2 class="profile-name">{{ userStore.user.nickname || userStore.user.username }}</h2>
        <p class="profile-email">{{ userStore.user.email || '未设置邮箱' }}</p>

        <button v-if="!isEditing" class="edit-btn" @click="startEdit">编辑资料</button>
      </div>

      <div class="card edit-card" v-if="isEditing">
        <h3>编辑基本信息</h3>
        <form @submit.prevent="saveProfile">
          <div class="form-group">
            <label>昵称</label>
            <input v-model="form.nickname" class="form-input" />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="form.email" type="email" class="form-input" />
          </div>
          <div class="form-group">
            <label>手机号</label>
            <input v-model="form.phone" type="tel" class="form-input" />
          </div>
          <div class="form-actions">
            <button type="submit" class="save-btn" :disabled="loading">{{ loading ? '保存中...' : '保存' }}</button>
            <button type="button" class="cancel-btn" @click="isEditing = false">取消</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="success" class="alert success">{{ success }}</div>
  </div>
</template>

<style lang="scss" scoped>
.profile-page { max-width: 600px; }

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.profile-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-lg);
  padding: 24px;
}

.profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-wrapper {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 3px solid var(--border-color);
  margin-bottom: 16px;
  display: block;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  color: var(--neon-cyan);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay { opacity: 1; }

.hidden-input {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
  opacity: 0;
}

.profile-name { font-size: 18px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
.profile-email { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; }

.edit-btn {
  padding: 8px 24px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--neon-cyan);
  font-size: 13px;
  transition: var(--transition-fast);
  &:hover { border-color: var(--neon-cyan); box-shadow: var(--glow-cyan); }
}

.edit-card {
  h3 { font-size: 16px; margin-bottom: 16px; color: var(--text-primary); }
}

.form-group {
  margin-bottom: 16px;
  label {
    display: block;
    font-size: 13px;
    color: var(--text-secondary);
    margin-bottom: 6px;
    font-weight: 500;
  }
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  &:focus { border-color: var(--neon-cyan); }
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.save-btn {
  flex: 1;
  padding: 10px;
  background: linear-gradient(135deg, var(--neon-cyan), var(--neon-purple));
  border: none;
  border-radius: var(--border-radius);
  color: var(--bg-primary);
  font-weight: 700;
  font-size: 14px;
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.cancel-btn {
  flex: 1;
  padding: 10px;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  font-size: 14px;
}

.alert {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: var(--border-radius);
  font-size: 13px;
  animation: fadeIn 0.3s;
  &.error { background: rgba(255, 45, 107, 0.1); color: var(--neon-pink); border: 1px solid rgba(255, 45, 107, 0.2); }
  &.success { background: rgba(57, 255, 20, 0.1); color: var(--neon-green); border: 1px solid rgba(57, 255, 20, 0.2); }
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>
