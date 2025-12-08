import http from './http'

export async function loginApi(payload: { username: string; password: string }) {
  const { data } = await http.post('/auth/login', payload)
  return data
}

export async function registerApi(payload: { 
  username: string; 
  password: string;
  nickname?: string;
  email?: string;
  phone?: string;
  confirmPassword: string;
}) {
  const { data } = await http.post('/auth/register', payload)
  return data
}

// 获取当前用户信息
export async function getUserInfoApi() {
  const { data } = await http.get('/user/info')
  return data
}

// 更新用户信息
export async function updateUserInfoApi(payload: { 
  nickname?: string;
  email?: string;
  phone?: string;
}) {
  const { data } = await http.put('/user/info', payload)
  return data
}