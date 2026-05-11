import http from './http'

export async function addBacklogApi(payload: Record<string, unknown>) {
  const { data } = await http.post('/backlog', payload)
  return data
}

export async function getBacklogListApi(status?: number) {
  const params: Record<string, unknown> = {}
  if (status !== undefined && status !== null) params.status = status
  const { data } = await http.get('/backlog/list', { params })
  return data
}

export async function updateBacklogApi(id: number, payload: Record<string, unknown>) {
  const { data } = await http.put(`/backlog/${id}`, payload)
  return data
}

export async function updateBacklogStatusApi(id: number, status: number) {
  const { data } = await http.put(`/backlog/${id}/status`, { status })
  return data
}

export async function batchSortApi(sortList: { id: number; sortOrder: number }[]) {
  const { data } = await http.put('/backlog/sort', sortList)
  return data
}

export async function removeBacklogApi(id: number) {
  const { data } = await http.delete(`/backlog/${id}`)
  return data
}

export async function checkinFromBacklogApi(backlogId: number, playTime: number, notes?: string) {
  const { data } = await http.post(`/backlog/${backlogId}/checkin`, { playTime, notes })
  return data
}
