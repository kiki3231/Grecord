<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { loginApi, registerApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
// 登录相关状态
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const loginType = ref('password') // 'password' 或 'code'

// 注册相关状态
const registerUsername = ref('')
const registerPassword = ref('')
const registerConfirm = ref('')
const registerNickname = ref('')
const registerEmail = ref('')
const registerPhone = ref('')
const registerLoading = ref(false)
const registerError = ref('')

// 当前激活的表单类型（login 或 register）
const activeForm = ref('login')

function validate() {
    if (!username.value.trim()) {
        error.value = '请输入用户名'
        return false
    }
    if (loginType.value === 'password') {
        if (!password.value.trim()) {
            error.value = '请输入密码'
            return false
        }
        if (password.value.length < 6) {
            error.value = '密码长度至少 6 位'
            return false
        }
    } else {
        if (!password.value.trim() || password.value.length !== 6) {
            error.value = '请输入6位验证码'
            return false
        }
    }
    return true
}

async function onSubmit() {
    error.value = ''
    if (!validate()) return
    loading.value = true
    try {
        // 根据登录类型调用不同的API
        const res = await loginApi({ 
            username: username.value.trim(), 
            password: password.value
        })
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

// 注册表单验证
function validateRegister() {
    registerError.value = ''
    
    // 用户名验证
    if (!registerUsername.value.trim()) {
        registerError.value = '请输入用户名'
        return false
    }
    if (registerUsername.value.length < 3 || registerUsername.value.length > 20) {
        registerError.value = '用户名长度为3-20个字符'
        return false
    }
    
    // 昵称验证
    if (registerNickname.value.trim() && (registerNickname.value.length < 2 || registerNickname.value.length > 20)) {
        registerError.value = '昵称长度为2-20个字符'
        return false
    }
    
    // 邮箱验证
    if (registerEmail.value.trim()) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(registerEmail.value)) {
            registerError.value = '请输入有效的邮箱地址'
            return false
        }
    }
    
    // 手机号码验证
    if (registerPhone.value.trim()) {
        const phoneRegex = /^1[3-9]\d{9}$/
        if (!phoneRegex.test(registerPhone.value)) {
            registerError.value = '请输入有效的手机号码'
            return false
        }
    }
    
    // 密码验证
    if (!registerPassword.value) {
        registerError.value = '请输入密码'
        return false
    }
    if (registerPassword.value.length < 6) {
        registerError.value = '密码长度至少6位'
        return false
    }
    
    // 确认密码验证
    if (registerPassword.value !== registerConfirm.value) {
        registerError.value = '两次输入的密码不一致'
        return false
    }
    
    return true
}

// 注册表单提交
async function onRegisterSubmit() {
    registerError.value = ''
    if (!validateRegister()) return
    registerLoading.value = true
    try {
        const res = await registerApi({ 
            username: registerUsername.value.trim(), 
            password: registerPassword.value,
            confirmPassword: registerConfirm.value,
            nickname: registerNickname.value.trim() || undefined,
            email: registerEmail.value.trim() || undefined,
            phone: registerPhone.value.trim() || undefined
        })
        if (res.code === 200) {
            // 注册成功后，自动切换到登录表单
            registerError.value = '注册成功，请登录'
            activeForm.value = 'login'
            // 可以自动填充用户名
            username.value = registerUsername.value
        } else {
            registerError.value = res.msg || '注册失败'
        }
    } catch (e: any) {
        registerError.value = e?.response?.data?.message || e?.message || '注册失败'
    } finally {
        registerLoading.value = false
    }
}

function getVerificationCode() {
    // 获取验证码逻辑
    if (!username.value.trim()) {
        error.value = '请先输入用户名'
        return
    }
    // 这里可以添加获取验证码的API调用
    console.log('获取验证码:', username.value)
    error.value = '验证码已发送（模拟）'
}
</script>

<template>
    <div class="login-page">
        <!-- 背景图片层 -->
        <div class="background-image"></div>
        
        <!-- 登录卡片 -->
        <div class="login-card">
            <!-- 品牌标题 -->
            <div class="brand-title">
                <h1>GRecord</h1>
                <p class="slogan">记录游戏精彩瞬间，分享游戏快乐体验</p>
            </div>
            
            <!-- 登录卡片内容 -->
            <div class="login-card-content">
                <!-- 左侧二维码登录 -->
                <div class="qrcode-login">
                    <!-- G动画状态 -->
                    <div class="g-animation">
                        <div class="g-icon">G</div>
                        <div class="g-animation-text">扫码登录</div>
                    </div>
                    
                    <!-- 二维码登录状态 -->
                    <div class="qrcode-content">
                        <h3>扫码登录</h3>
                        <p class="qrcode-desc">打开GRecord App，使用「扫一扫」功能扫描下方二维码</p>
                        <div class="qrcode-container">
                            <div class="qrcode">
                                <div class="qrcode-placeholder">二维码</div>
                            </div>
                        </div>
                        <p class="qrcode-hint">其他扫码方式：微信</p>
                        <div class="app-download">
                            <a href="#" class="download-link">下载APP</a>
                            <a href="#" class="no-app">无App扫码</a>
                        </div>
                    </div>
                </div>
                
                <!-- 分隔线 -->
                <div class="divider-vertical">
                    <span>或</span>
                </div>
                
                <!-- 右侧表单登录 -->
                <div class="form-login">
                    <!-- 登录表单 -->
                    <div v-if="activeForm === 'login'" class="form-content">
                        <div class="login-header">
                            <h2>欢迎回来</h2>
                            <p>登录你的账号，继续游戏记录之旅</p>
                        </div>
                        
                        <div class="login-tabs">
                            <div 
                                class="tab-item"
                                :class="{ active: loginType === 'password' }"
                                @click="loginType = 'password'"
                            >
                                密码登录
                            </div>
                            <div 
                                class="tab-item"
                                :class="{ active: loginType === 'code' }"
                                @click="loginType = 'code'"
                            >
                                验证码登录
                            </div>
                        </div>
                        
                        <form @submit.prevent="onLoginSubmit" class="login-form">
                            <div class="form-group">
                                <div class="input-wrapper">
                                    <input 
                                        v-model="loginUsername" 
                                        class="form-control"
                                        placeholder="用户名/手机号"
                                        autocomplete="username"
                                    />
                                </div>
                            </div>

                            <div class="form-group">
                                <div class="input-wrapper">
                                    <input 
                                        v-model="loginPassword" 
                                        :type="loginType === 'password' ? 'password' : 'text'"
                                        class="form-control"
                                        :placeholder="loginType === 'password' ? '请输入密码' : '请输入6位验证码'"
                                        :autocomplete="loginType === 'password' ? 'current-password' : 'one-time-code'"
                                    />
                                    <button 
                                        v-if="loginType === 'code'"
                                        type="button"
                                        class="code-btn"
                                        @click="getVerificationCode"
                                        :disabled="loginLoading"
                                    >
                                        获取验证码
                                    </button>
                                </div>
                            </div>

                        <div class="button-group">
                            <button 
                                type="submit" 
                                class="login-btn"
                                :disabled="loading"
                            >
                                {{ loading ? '登录中...' : '登录' }}
                            </button>
                            
                            <button 
                                type="button" 
                                class="register-btn"
                                @click="activeForm = 'register'"
                            >
                                注册
                            </button>
                        </div>

                        <div v-if="error" class="error-message">{{ error }}</div>
                    </form>

                    <div class="other-login">
                        <div class="divider">
                            <span>其他登录方式</span>
                        </div>
                        <div class="login-icons">
                            <div class="icon-item wechat">微信</div>
                            <div class="icon-item qq">QQ</div>
                            <div class="icon-item weibo">微博</div>
                        </div>
                    </div>

                    <p class="agreement">
                        登录即表示同意 <a href="#">用户协议</a> 和 <a href="#">隐私政策</a>
                    </p>
                </div>

                <!-- 注册表单 -->
                <div v-if="activeForm === 'register'" class="form-content">
                    <div class="login-header">
                        <h2>创建新账号</h2>
                        <p>注册GRecord，开始记录你的游戏精彩瞬间</p>
                    </div>
                    
                    <form @submit.prevent="onRegisterSubmit" class="login-form">
                        <div class="form-group">
                            <div class="input-wrapper">
                                <input 
                                    v-model="registerUsername" 
                                    class="form-control"
                                    placeholder="设置用户名"
                                    autocomplete="username"
                                />
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="input-wrapper">
                                <input 
                                    v-model="registerNickname" 
                                    class="form-control"
                                    placeholder="设置昵称（选填）"
                                    autocomplete="nickname"
                                />
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="input-wrapper">
                                <input 
                                    v-model="registerEmail" 
                                    type="email"
                                    class="form-control"
                                    placeholder="设置邮箱（选填）"
                                    autocomplete="email"
                                />
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="input-wrapper">
                                <input 
                                    v-model="registerPhone" 
                                    type="tel"
                                    class="form-control"
                                    placeholder="设置手机号码（选填）"
                                    autocomplete="tel"
                                />
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="input-wrapper">
                                <input 
                                    v-model="registerPassword" 
                                    type="password"
                                    class="form-control"
                                    placeholder="设置密码"
                                    autocomplete="new-password"
                                />
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="input-wrapper">
                                <input 
                                    v-model="registerConfirm" 
                                    type="password"
                                    class="form-control"
                                    placeholder="确认密码"
                                    autocomplete="new-password"
                                />
                            </div>
                        </div>

                        <div class="button-group">
                            <button 
                                type="submit" 
                                class="login-btn"
                                :disabled="registerLoading"
                            >
                                {{ registerLoading ? '注册中...' : '注册' }}
                            </button>
                            
                            <button 
                                type="button" 
                                class="register-btn"
                                @click="activeForm = 'login'"
                            >
                                返回登录
                            </button>
                        </div>
                        
                        <div class="error-message" v-if="registerError">
                            {{ registerError }}
                        </div>

                        <p class="agreement">
                            注册即表示同意 <a href="#">用户协议</a> 和 <a href="#">隐私政策</a>
                        </p>
                    </form>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>

<style lang="scss">
/* 全局样式重置 */
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>

<style lang="scss" scoped>
.login-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  display: flex;
  align-items: center;
  justify-content: center;

  /* 背景图片 */
  .background-image {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: url('@/assets/image/loginBac.png');
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    opacity: 0.8;
    z-index: 1;
  }

  /* 登录卡片 */
  .login-card {
    position: relative;
    z-index: 10;
    width: 100%;
    max-width: 800px;
    background-color: #fff;
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    overflow: hidden;
    opacity: 0.9;

    /* 品牌标题 */
    .brand-title {
      text-align: center;
      padding: 20px 0;
      border-bottom: 1px solid #f0f0f0;

      h1 {
        font-size: 28px;
        margin: 0 0 8px;
        color: #333;
      }

      .slogan {
        font-size: 14px;
        color: #666;
        margin: 0;
      }
    }

    /* 登录卡片内容 */
    .login-card-content {
      display: flex;
      height: 500px;

      /* 左侧二维码登录 */
      .qrcode-login {
        flex: 1;
        padding: 40px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        text-align: center;
        position: relative;
        overflow: hidden;
        transition: all 0.5s ease;

        /* G动画状态 */
        .g-animation {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          transition: all 0.5s ease;
          opacity: 1;
          z-index: 10;

          .g-icon {
            width: 120px;
            height: 120px;
            background: linear-gradient(135deg, #1890ff, #40a9ff);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 60px;
            font-weight: bold;
            color: #fff;
            box-shadow: 0 8px 24px rgba(24, 144, 255, 0.4);
            animation: gRotate 3s ease-in-out infinite;
            margin-bottom: 20px;
          }

          .g-animation-text {
            font-size: 18px;
            color: #333;
            font-weight: 500;
          }
        }

        /* 二维码登录状态 */
        .qrcode-content {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(200%, -50%);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          opacity: 0;
          transition: all 0.5s ease;
          width: 100%;
          z-index: 5;

          h3 {
            font-size: 20px;
            margin: 0 0 10px;
            color: #333;
          }

          .qrcode-desc {
            font-size: 14px;
            color: #666;
            margin: 0 0 25px;
            line-height: 1.5;
          }

          .qrcode-container {
            margin-bottom: 20px;

            .qrcode {
              width: 180px;
              height: 180px;
              background-color: #f5f5f5;
              border: 1px solid #e8e8e8;
              display: flex;
              align-items: center;
              justify-content: center;
              margin: 0 auto;
              transition: all 0.3s ease;
            }

            .qrcode-placeholder {
              color: #999;
              font-size: 14px;
            }
          }

          .qrcode-hint {
            font-size: 14px;
            color: #666;
            margin: 0 0 30px;
          }

          .app-download {
            display: flex;
            gap: 15px;

            .download-link, .no-app {
              padding: 8px 20px;
              border-radius: 4px;
              font-size: 14px;
              text-decoration: none;
              transition: all 0.3s;

              &:hover {
                text-decoration: underline;
              }
            }

            .download-link {
              background-color: #1890ff;
              color: #fff;
              border: 1px solid #1890ff;

              &:hover {
                background-color: #40a9ff;
              }
            }

            .no-app {
              background-color: #fff;
              color: #1890ff;
              border: 1px solid #1890ff;

              &:hover {
                background-color: #e6f7ff;
              }
            }
          }
        }

        /* 鼠标悬停效果 */
        &:hover {
          .g-animation {
            transform: translate(-200%, -50%);
            opacity: 0;
          }

          .qrcode-content {
            transform: translate(-50%, -50%);
            opacity: 1;
          }
        }
      }

      /* 垂直分隔线 */
      .divider-vertical {
        position: relative;
        width: 1px;
        background-color: #e8e8e8;

        span {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          background-color: #fff;
          color: #999;
          padding: 0 10px;
          font-size: 14px;
        }
      }

      /* 右侧表单登录 */
      .form-login {
        flex: 1;
        padding: 40px;
        display: flex;
        flex-direction: column;
        justify-content: center;

        .login-header {
          text-align: center;
          margin-bottom: 30px;

          h2 {
            font-size: 24px;
            margin: 0 0 10px;
            color: #333;
          }

          p {
            font-size: 14px;
            color: #666;
            margin: 0;
          }
        }

        /* 登录/注册切换标签 */
        .auth-tabs {
          display: flex;
          margin-bottom: 25px;
          border-bottom: 1px solid #e8e8e8;

          .tab-item {
            flex: 1;
            padding: 10px 0;
            text-align: center;
            cursor: pointer;
            font-size: 16px;
            font-weight: 500;
            color: #666;
            transition: all 0.3s;

            &.active {
              color: #1890ff;
              border-bottom: 2px solid #1890ff;
            }

            &:hover {
              color: #1890ff;
            }
          }
        }

        /* 登录方式切换标签 */
        .login-tabs {
          display: flex;
          margin-bottom: 25px;
          border-bottom: 1px solid #e8e8e8;

          .tab-item {
            flex: 1;
            padding: 10px 0;
            text-align: center;
            cursor: pointer;
            font-size: 15px;
            color: #666;
            transition: all 0.3s;

            &.active {
              color: #1890ff;
              border-bottom: 2px solid #1890ff;
            }

            &:hover {
              color: #1890ff;
            }
          }
        }

        .login-form {
          margin-bottom: 20px;

          .form-group {
            margin-bottom: 15px;

            .input-wrapper {
              position: relative;
            }

            .form-control {
              width: 100%;
              height: 40px;
              padding: 0 10px;
              border: 1px solid #d9d9d9;
              border-radius: 4px;
              font-size: 14px;
              transition: border-color 0.3s;

              &:focus {
                outline: none;
                border-color: #1890ff;
                box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
              }
            }

            .code-btn {
              position: absolute;
              right: 1px;
              top: 1px;
              bottom: 1px;
              padding: 0 15px;
              background-color: #f5f5f5;
              border: none;
              color: #1890ff;
              cursor: pointer;
              font-size: 14px;
              transition: all 0.3s;

              &:hover {
                background-color: #e6f7ff;
              }

              &:disabled {
                background-color: #f5f5f5;
                color: #d9d9d9;
                cursor: not-allowed;
              }
            }
          }

          .button-group {
            display: flex;
            gap: 10px;
          }

          .login-btn, .register-btn {
            flex: 1;
            height: 40px;
            border: none;
            border-radius: 4px;
            color: #fff;
            font-size: 16px;
            cursor: pointer;
            transition: background-color 0.3s;
          }

          .login-btn {
            background-color: #1890ff;

            &:hover {
              background-color: #40a9ff;
            }

            &:disabled {
              background-color: #d9d9d9;
              cursor: not-allowed;
            }
          }

          .register-btn {
            background-color: #52c41a;

            &:hover {
              background-color: #73d13d;
            }
          }

          .error-message {
            color: #f5222d;
            font-size: 12px;
            margin-top: 10px;
            text-align: center;
          }
        }

        .other-login {
          margin-bottom: 20px;

          .divider {
            position: relative;
            text-align: center;
            margin-bottom: 15px;

            &::before, &::after {
              content: '';
              position: absolute;
              top: 50%;
              width: 40%;
              height: 1px;
              background-color: #e8e8e8;
            }

            &::before {
              left: 0;
            }

            &::after {
              right: 0;
            }

            span {
              display: inline-block;
              padding: 0 10px;
              background-color: #fff;
              color: #999;
              font-size: 12px;
            }
          }

          .login-icons {
            display: flex;
            justify-content: center;
            gap: 20px;

            .icon-item {
              width: 40px;
              height: 40px;
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
              cursor: pointer;
              font-size: 14px;
              transition: all 0.3s;

              &.wechat {
                background-color: #07c160;
                color: #fff;
              }

              &.qq {
                background-color: #12b7f5;
                color: #fff;
              }

              &.weibo {
                background-color: #e6162d;
                color: #fff;
              }

              &:hover {
                transform: translateY(-3px);
                box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
              }
            }
          }
        }

        .agreement {
          font-size: 12px;
          color: #999;
          text-align: center;
          margin: 0;

          a {
            color: #1890ff;
            text-decoration: none;

            &:hover {
              text-decoration: underline;
            }
          }
        }
      }
    }
  }
}

/* G图标旋转动画 */
@keyframes gRotate {
  0% {
    transform: rotate(0deg) scale(1);
  }
  25% {
    transform: rotate(90deg) scale(1.1);
  }
  50% {
    transform: rotate(180deg) scale(1);
  }
  75% {
    transform: rotate(270deg) scale(0.9);
  }
  100% {
    transform: rotate(360deg) scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-page {
    padding: 20px;

    .login-card {
      max-width: 100%;

      .login-card-content {
        flex-direction: column;
        height: auto;

        .qrcode-login {
          padding: 30px 20px;
          height: 350px;
        }

        .divider-vertical {
          width: 100%;
          height: 1px;
          margin: 0;

          span {
            top: 50%;
            left: 50%;
          }
        }

        .form-login {
          padding: 30px 20px;
        }
      }
    }
  }
}
</style>

<style lang="scss">
.login-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>