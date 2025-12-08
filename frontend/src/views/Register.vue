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
            <h1>注册账号</h1>
            <p class="subtitle">创建你的 GRecord 账户</p>

            <form @submit.prevent="onSubmit">
                <label>用户名</label>
                <input v-model="username" placeholder="请输入用户名" />

                <label>昵称（可选）</label>
                <input v-model="nickname" placeholder="请输入昵称" />

                <label>邮箱（可选）</label>
                <input v-model="email" type="email" placeholder="请输入邮箱" />

                <label>手机号（可选）</label>
                <input v-model="phone" type="tel" placeholder="请输入手机号" @input="validatePhone" />

                <label>密码</label>
                <input v-model="password" type="password" placeholder="至少 6 位" />

                <label>确认密码</label>
                <input v-model="confirm" type="password" placeholder="再次输入密码" />

                <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '注册' }}</button>
                <p v-if="error" class="err">{{ error }}</p>
            </form>

            <p class="tip">
                已有账号？
                <router-link to="/login">返回登录</router-link>
            </p>
        </div>
    </div>
</template>

<style scoped>
.login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f6f7fb;
}

.card {
    width: 360px;
    background: #fff;
    padding: 28px;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, .08);
}

h1 {
    margin: 0 0 6px;
    text-align: center;
}

.subtitle {
    margin: 0 0 18px;
    text-align: center;
    color: #666;
}

label {
    display: block;
    font-size: 12px;
    color: #666;
    margin-top: 12px;
}

input {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    margin-top: 6px;
}

button {
    width: 100%;
    margin-top: 16px;
    padding: 10px 12px;
    background: #10b981;
    color: #fff;
    border: 0;
    border-radius: 8px;
    cursor: pointer;
}

.err {
    color: #e11d48;
    font-size: 12px;
    margin-top: 8px;
}

.ok {
    color: #059669;
    font-size: 12px;
    margin-top: 8px;
}

.tip {
    margin-top: 12px;
    text-align: center;
    color: #999;
    font-size: 12px;
}
</style>