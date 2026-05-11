import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfoApi } from '@/api/auth'

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

  async function fetchUserInfo() {
    const res = await getUserInfoApi()
    if (res.code === 200) {
      user.value = res.data
    }
  }

  function setToken(token: string) {
    localStorage.setItem('token', token)
    isLoggedIn.value = true
  }

  function logout() {
    localStorage.removeItem('token')
    isLoggedIn.value = false
    user.value = { id: 0, username: '', nickname: '', email: '', phone: '', avatar: '' }
  }

  return { user, isLoggedIn, fetchUserInfo, setToken, logout }
})
