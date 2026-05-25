import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfoApi } from '@/api/auth'
import { resolveAvatarUrl, stripAvatarQuery } from '@/utils/avatarUrl'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserInfo>({
    id: 0,
    username: '',
    nickname: '',
    email: '',
    phone: '',
    avatar: ''
  })
  const isLoggedIn = ref(!!localStorage.getItem('token'))
  /** 头像缓存版本，上传或拉取用户信息后递增 */
  const avatarVersion = ref(0)
  const avatarLoadFailed = ref(false)

  const avatarSrc = computed(() => {
    if (avatarLoadFailed.value) return ''
    return resolveAvatarUrl(user.value.avatar, avatarVersion.value)
  })

  async function fetchUserInfo(): Promise<boolean> {
    try {
      const res = await getUserInfoApi()
      if (res.code === 200 && res.data) {
        avatarLoadFailed.value = false
        avatarVersion.value = Date.now()
        const data = res.data as UserInfo
        user.value = {
          ...data,
          avatar: data.avatar ? stripAvatarQuery(String(data.avatar)) : ''
        }
        return true
      }
      return false
    } catch {
      return false
    }
  }

  /** 上传成功后立即更新头像（无需整页刷新） */
  function applyAvatar(url: string) {
    if (!url) return
    avatarLoadFailed.value = false
    avatarVersion.value = Date.now()
    user.value = {
      ...user.value,
      avatar: stripAvatarQuery(url)
    }
  }

  function onAvatarError() {
    avatarLoadFailed.value = true
  }

  function setToken(token: string) {
    localStorage.setItem('token', token)
    isLoggedIn.value = true
  }

  function logout() {
    localStorage.removeItem('token')
    isLoggedIn.value = false
    avatarVersion.value = 0
    avatarLoadFailed.value = false
    user.value = { id: 0, username: '', nickname: '', email: '', phone: '', avatar: '' }
  }

  return {
    user,
    isLoggedIn,
    avatarSrc,
    avatarLoadFailed,
    fetchUserInfo,
    applyAvatar,
    onAvatarError,
    setToken,
    logout
  }
})
