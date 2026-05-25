import http from './http'

export async function createRecordApi(payload: Record<string, unknown>) {
  const { data } = await http.post('/record', payload)
  return data
}

export async function getRecordListApi(page = 1, size = 20) {
  const { data } = await http.get('/record/list', { params: { page, size } })
  return data
}

export async function getRecordsWithGameApi() {
  const { data } = await http.get('/record/listWithGame')
  return data
}

export async function getHeatmapApi(year?: number) {
  const { data } = await http.get('/record/heatmap', { params: { year } })
  return data
}

export async function getStatsApi() {
  const { data } = await http.get('/record/stats')
  return data
}

export async function updateRecordApi(id: number, payload: Record<string, unknown>) {
  const { data } = await http.put(`/record/${id}`, payload)
  return data
}

export async function deleteRecordApi(id: number) {
  const { data } = await http.delete(`/record/${id}`)
  return data
}
