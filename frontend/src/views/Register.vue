<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi } from '../api/auth'

const router = useRouter()
const username = ref('')
const nickname = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const confirm = ref('')
const loading = ref(false)
const error = ref('')

function validate() {
    error.value = ''
    if (!username.value.trim()) { error.value = '请输入用户名'; return false }
    if (password.value.length < 6) { error.value = '密码至少 6 位'; return false }
    if (password.value !== confirm.value) { error.value = '两次输入的密码不一致'; return false }
    // 邮箱格式验证（可选）
    if (email.value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
        error.value = '邮箱格式不正确'; return false
    }
    // 手机号格式验证（可选）
    if (phone.value && !/^1[3-9]\d{9}$/.test(phone.value)) {
        error.value = '手机号格式不正确'; return false
    }
    return true
}

// 实时手机号校验
function validatePhone() {
    // 只有当手机号输入框有内容时才进行验证
    if (phone.value) {
        // 标准中国手机号正则：1开头，第二位3-9，后面9位数字，共11位
        const phoneRegex = /^1[3-9]\d{9}$/
        if (!phoneRegex.test(phone.value)) {
            error.value = '请输入正确的手机号格式'
        } else {
            // 如果当前错误是手机号格式错误且现在输入正确了，则清除错误
            if (error.value === '请输入正确的手机号格式' || error.value === '手机号格式不正确') {
                error.value = ''
            }
        }
    } else {
        // 如果手机号输入框为空，清除可能的手机号格式错误
        if (error.value === '请输入正确的手机号格式' || error.value === '手机号格式不正确') {
            error.value = ''
        }
    }
}

async function onSubmit() {
    if (!validate()) return
    loading.value = true
    try {
        const res = await registerApi({
            username: username.value.trim(),
            password: password.value,
            confirmPassword: confirm.value,
            nickname: nickname.value.trim() || undefined,
            email: email.value.trim() || undefined,
            phone: phone.value.trim() || undefined
        })
        if (res.code === 200) {
            // 注册成功，直接跳转到登录页面
            router.push('/login')
        } else {
            error.value = res.msg || '注册失败'
        }
    } catch (e: any) {
        error.value = e?.response?.data?.message || e?.message || '注册失败'
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="login-page">
        <div class="card">
            <div class="brand-container">
                <div class="brand-icon">G</div>
                <h1>GRecord</h1>
            </div>
            <p class="subtitle">游戏记录打卡 · 注册</p>

            <form @submit.prevent="onSubmit">
                <div class="form-group">
                    <label class="form-label">用户名</label>
                    <input 
                        v-model="username" 
                        class="form-control"
                        placeholder="请输入用户名" 
                        autocomplete="username"
                    />
                </div>

                <div class="form-group">
                    <label class="form-label">昵称（可选）</label>
                    <input 
                        v-model="nickname" 
                        class="form-control"
                        placeholder="请输入昵称" 
                    />
                </div>

                <div class="form-group">
                    <label class="form-label">邮箱（可选）</label>
                    <input 
                        v-model="email" 
                        type="email" 
                        class="form-control"
                        placeholder="请输入邮箱" 
                        autocomplete="email"
                    />
                </div>

                <div class="form-group">
                    <label class="form-label">手机号（可选）</label>
                    <input 
                        v-model="phone" 
                        type="tel" 
                        class="form-control"
                        placeholder="请输入手机号" 
                        @input="validatePhone"
                        autocomplete="tel"
                    />
                </div>

                <div class="form-group">
                    <label class="form-label">密码</label>
                    <input 
                        v-model="password" 
                        type="password" 
                        class="form-control"
                        placeholder="至少 6 位" 
                        autocomplete="new-password"
                    />
                </div>

                <div class="form-group">
                    <label class="form-label">确认密码</label>
                    <input 
                        v-model="confirm" 
                        type="password" 
                        class="form-control"
                        placeholder="再次输入密码" 
                        autocomplete="new-password"
                    />
                </div>

                <button 
                    type="submit" 
                    class="login-btn"
                    :disabled="loading"
                >
                    {{ loading ? '注册中...' : '注册' }}
                </button>

                <div v-if="error" class="error-message">{{ error }}</div>
            </form>

            <p class="tip">
                已有账号？
                <router-link to="/login">返回登录</router-link>
            </p>
        </div>
    </div>
</template>

<style scoped>
:root {
  --primary-color: #646cff;
  --primary-hover: #535bf2;
  --secondary-color: #f5f5f7;
  --text-primary: #1d1d1f;
  --text-secondary: #6e6e73;
  --text-tertiary: #86868b;
  --border-color: #e5e5ea;
  --success-color: #34c759;
  --error-color: #ff3b30;
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.1);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-full: 50%;
  --transition-fast: 0.2s ease;
  --transition-normal: 0.3s ease;
}

.login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 1.5rem;
    position: relative;
    overflow: hidden;
}

/* 添加装饰性元素 */
.login-page::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
    animation: float 20s ease-in-out infinite;
}

@keyframes float {
    0%, 100% { transform: translate(0, 0) rotate(0deg); }
    25% { transform: translate(20%, -20%) rotate(90deg); }
    50% { transform: translate(0, -40%) rotate(180deg); }
    75% { transform: translate(-20%, -20%) rotate(270deg); }
}

/* 登录卡片 */
.card {
    width: 100%;
    max-width: 420px;
    background: white;
    padding: 2.5rem;
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    position: relative;
    z-index: 1;
    border: 1px solid rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 品牌部分 */
.brand-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.75rem;
    margin-bottom: 0.5rem;
}

.brand-icon {
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: 800;
    font-size: 1.5rem;
    box-shadow: var(--shadow-md);
}

h1 {
    margin: 0;
    font-size: 2rem;
    font-weight: 800;
    color: var(--text-primary);
    letter-spacing: -0.02em;
}

.subtitle {
    margin: 0 0 2.5rem 0;
    text-align: center;
    color: var(--text-secondary);
    font-size: 0.9375rem;
    font-weight: 400;
}

/* 表单样式 */
form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.form-label {
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.01em;
}

.form-control {
    width: 100%;
    padding: 1rem 1.125rem;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    font-size: 0.9375rem;
    color: var(--text-primary);
    background-color: white;
    transition: all var(--transition-fast);
    box-shadow: var(--shadow-sm);
}

.form-control:focus {
    outline: none;
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(100, 108, 255, 0.1);
    transform: translateY(-1px);
}

.form-control::placeholder {
    color: var(--text-tertiary);
}

/* 登录按钮 */
.login-btn {
    width: 100%;
    padding: 1rem 1.125rem;
    background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
    color: white;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    font-size: 0.9375rem;
    font-weight: 600;
    transition: all var(--transition-normal);
    box-shadow: var(--shadow-md);
    margin-top: 0.5rem;
    position: relative;
    overflow: hidden;
}

.login-btn::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    border-radius: var(--radius-full);
    background: rgba(255, 255, 255, 0.2);
    transform: translate(-50%, -50%);
    transition: width var(--transition-normal), height var(--transition-normal);
}

.login-btn:hover::before {
    width: 300px;
    height: 300px;
}

.login-btn:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
}

.login-btn:active {
    transform: translateY(0);
}

.login-btn:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
}

.login-btn:disabled:hover {
    box-shadow: var(--shadow-md);
}

/* 错误信息 */
.error-message {
    color: var(--error-color);
    background-color: rgba(255, 59, 48, 0.1);
    border: 1px solid rgba(255, 59, 48, 0.2);
    padding: 0.875rem 1rem;
    border-radius: var(--radius-md);
    font-size: 0.875rem;
    font-weight: 500;
    text-align: center;
    animation: shake 0.5s ease-in-out;
}

@keyframes shake {
    0%, 100% { transform: translateX(0); }
    25% { transform: translateX(-5px); }
    75% { transform: translateX(5px); }
}

/* 提示信息 */
.tip {
    margin-top: 1.75rem;
    text-align: center;
    color: var(--text-secondary);
    font-size: 0.875rem;
}

.tip a {
    color: var(--primary-color);
    text-decoration: none;
    font-weight: 500;
    transition: color var(--transition-fast);
}

.tip a:hover {
    color: var(--primary-hover);
    text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 480px) {
    .login-page {
        padding: 1rem;
    }
    
    .card {
        padding: 2rem 1.5rem;
        border-radius: var(--radius-md);
    }
    
    h1 {
        font-size: 1.75rem;
    }
    
    .brand-icon {
        width: 40px;
        height: 40px;
        font-size: 1.25rem;
    }
}
</style>