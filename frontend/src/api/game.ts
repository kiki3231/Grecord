import http from './http'

export type SortKey = 'rating_desc' | 'rating_asc' | 'name_asc' | 'newest'

export interface SearchGamesParams {
  keyword?: string
  platform?: string
  type?: string
  sortBy?: SortKey
  page?: number
  size?: number
}

export async function searchGamesApi(params: SearchGamesParams = {}) {
  const cleaned: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') cleaned[k] = v
  }
  const { data } = await http.get('/game/search', { params: cleaned })
  return data
}

export async function getGameDetailApi(id: number) {
  const { data } = await http.get(`/game/${id}`)
  return data
}

export async function createGameApi(payload: Record<string, unknown>) {
  const { data } = await http.post('/game', payload)
  return data
}

export async function getHotGamesApi(page = 1, size = 20) {
  const { data } = await http.get('/game/hot', { params: { page, size } })
  return data
}

export interface FilterOption {
  name: string
  count: number
}

export async function getGameFiltersApi() {
  const { data } = await http.get('/game/filters')
  return data as { code: number; msg: string; data: { platforms: FilterOption[]; types: FilterOption[] } }
}
