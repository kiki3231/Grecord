<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { loginApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

function validate() {
    if (!username.value.trim() || !password.value.trim()) {
        error.value = '请输入用户名和密码'
        return false
    }
    if (password.value.length < 6) {
        error.value = '密码长度至少 6 位'
        return false
    }
    return true
}

async function onSubmit() {
    error.value = ''
    if (!validate()) return
    loading.value = true
    try {
        const res = await loginApi({ username: username.value.trim(), password: password.value })
        if (res.code === 200 && res.data?.token) {
            localStorage.setItem('token', res.data.token)
            const redirect = (route.query.redirect as string) || '/'
            router.push(redirect)
        } else {
            error.value = res.msg || '登录失败'
        }
    } catch (e: any) {
        error.value = e?.response?.data?.message || e?.message || '登录失败'
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="login-page">
        <div class="card">
            <h1>GRecord</h1>
            <p class="subtitle">游戏记录打卡 · 登录</p>

            <form @submit.prevent="onSubmit">
                <label>用户名</label>
                <input v-model="username" placeholder="请输入用户名" autocomplete="username" />

                <label>密码</label>
                <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" />

                <button type="submit" :disabled="loading">
                    {{ loading ? '登录中...' : '登录' }}
                </button>
                <p v-if="error" class="err">{{ error }}</p>
            </form>

            <p class="tip">
                还没有账号？
                <router-link to="/register">去注册</router-link>
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
    background: #646cff;
    color: #fff;
    border: 0;
    border-radius: 8px;
    cursor: pointer;
}

button:disabled {
    opacity: .6;
    cursor: not-allowed;
}

.tip {
    margin-top: 12px;
    text-align: center;
    color: #999;
    font-size: 12px;
}

.err {
    color: #e11d48;
    font-size: 12px;
    margin-top: 8px;
}
</style>