<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfoApi, updateUserInfoApi } from '../api/auth'

const router = useRouter()
const userInfo = ref({
  username: 'User',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})
const loading = ref(false)
const error = ref('')
const success = ref('')
const editing = ref(false)
const formData = ref({ ...userInfo.value })

// 从后端获取用户信息
async function getUserInfo() {
  loading.value = true
  error.value = ''
  try {
    const res = await getUserInfoApi()
    if (res.code === 200) {
      userInfo.value = res.data
      formData.value = { ...res.data }
    } else {
      error.value = res.msg || '获取用户信息失败'
    }
  } catch (e: any) {
    error.value = e?.response?.data?.msg || e?.message || '获取用户信息失败'
  } finally {
    loading.value = false
  }
}

function handleEdit() {
  editing.value = true
  formData.value = { ...userInfo.value }
}

function handleCancel() {
  editing.value = false
  formData.value = { ...userInfo.value }
}

async function handleSave() {
  loading.value = true
  error.value = ''
  success.value = ''
  
  try {
    // 调用真实API更新用户信息
    const res = await updateUserInfoApi({
      nickname: formData.value.nickname,
      email: formData.value.email,
      phone: formData.value.phone
    })
    
    if (res.code === 200) {
      userInfo.value = { ...formData.value }
      editing.value = false
      success.value = '信息更新成功'
      
      // 3秒后清除成功提示
      setTimeout(() => {
        success.value = ''
      }, 3000)
    } else {
      error.value = res.msg || '更新失败'
    }
  } catch (e: any) {
    error.value = e?.response?.data?.msg || e?.message || '更新失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getUserInfo()
})
</script>

<template>
  <div class="profile-container">
    <div class="profile-header">
      <h2>个人中心</h2>
      <div v-if="!editing" class="header-actions">
        <button class="edit-btn" @click="handleEdit">编辑资料</button>
      </div>
      <div v-else class="header-actions">
        <button class="cancel-btn" @click="handleCancel">取消</button>
        <button class="save-btn" @click="handleSave" :disabled="loading">
          {{ loading ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <div class="profile-content">
      <div class="profile-section">
        <div class="avatar-container">
          <div class="avatar">
            <span>{{ userInfo.username.charAt(0) }}</span>
          </div>
          <div class="avatar-actions">
            <button class="avatar-btn">更换头像</button>
          </div>
        </div>
      </div>

      <div class="profile-section">
        <h3>基本信息</h3>
        <div class="form-group">
          <label>用户名</label>
          <input 
            v-model="formData.username" 
            :disabled="!editing" 
            placeholder="请输入用户名"
          />
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input 
            v-model="formData.nickname" 
            :disabled="!editing" 
            placeholder="请输入昵称"
          />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input 
            v-model="formData.email" 
            :disabled="!editing" 
            type="email" 
            placeholder="请输入邮箱"
          />
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input 
            v-model="formData.phone" 
            :disabled="!editing" 
            type="tel" 
            placeholder="请输入手机号"
          />
        </div>
      </div>

      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="success" class="success-message">{{ success }}</div>
    </div>
  </div>
</template>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 0 1rem;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.profile-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #111827;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.edit-btn, .save-btn, .cancel-btn, .avatar-btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.edit-btn, .save-btn {
  background-color: #646cff;
  color: white;
}

.edit-btn:hover, .save-btn:hover {
  background-color: #535bf2;
}

.cancel-btn {
  background-color: #f3f4f6;
  color: #374151;
}

.cancel-btn:hover {
  background-color: #e5e7eb;
}

.profile-content {
  background-color: white;
  border-radius: 0.75rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
  padding: 1.5rem;
}

.profile-section {
  margin-bottom: 2rem;
}

.profile-section h3 {
  margin: 0 0 1rem 0;
  font-size: 1.125rem;
  color: #374151;
}

.avatar-container {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: #646cff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bold;
}

.avatar-btn {
  background-color: #f3f4f6;
  color: #374151;
}

.avatar-btn:hover {
  background-color: #e5e7eb;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.form-group input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #646cff;
}

.form-group input:disabled {
  background-color: #f9fafb;
  cursor: not-allowed;
}

.error-message {
  color: #dc2626;
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  padding: 0.75rem;
  border-radius: 0.375rem;
  margin-top: 1rem;
}

.success-message {
  color: #059669;
  background-color: #f0fdf4;
  border: 1px solid #d1fae5;
  padding: 0.75rem;
  border-radius: 0.375rem;
  margin-top: 1rem;
}

@media (max-width: 640px) {
  .profile-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .avatar-container {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>