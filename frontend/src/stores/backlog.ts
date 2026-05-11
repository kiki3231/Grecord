import { acceptHMRUpdate, defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getBacklogListApi, updateBacklogStatusApi, batchSortApi, removeBacklogApi, addBacklogApi, checkinFromBacklogApi } from '@/api/backlog'

export interface BacklogItem {
  id: number
  userId: number
  gameId: number
  status: number
  priority: number
  sortOrder: number
  notes: string
  startedDate: string
  completedDate: string
  game: {
    id: number
    name: string
    icon: string
    platforms: string
    gameTypes: string
    rating: number
  }
}

export const useBacklogStore = defineStore('backlog', () => {
  const items = ref<BacklogItem[]>([])
  const statusCounts = ref<Record<number, number>>({})
  const loading = ref(false)
  const loadedOnce = ref(false)

  /** gameId -> backlogId 的反查映射，方便卡片爱心按钮 toggle */
  const gameIdToBacklogId = computed(() => {
    const m = new Map<number, number>()
    for (const it of items.value) {
      if (it.gameId) m.set(it.gameId, it.id)
    }
    return m
  })

  /** 操作中的 gameId 集合，避免重复点击 */
  const wishPending = ref<Set<number>>(new Set())

  function isInBacklog(gameId: number) {
    return gameIdToBacklogId.value.has(gameId)
  }

  async function fetchBacklog(status?: number) {
    loading.value = true
    try {
      const res = await getBacklogListApi(status)
      if (res.code === 200) {
        items.value = res.data.items || []
        const counts: Record<number, number> = {}
        const rawCounts = res.data.statusCounts || []
        for (const c of rawCounts) {
          counts[c.status] = c.count
        }
        statusCounts.value = counts
        loadedOnce.value = true
      }
    } catch (err) {
      console.error('[fetchBacklog] 请求失败：', err)
    } finally {
      loading.value = false
    }
  }

  async function ensureBacklogLoaded() {
    if (loadedOnce.value) return
    await fetchBacklog()
  }

  async function updateStatus(id: number, newStatus: number) {
    const res = await updateBacklogStatusApi(id, newStatus)
    if (res.code === 200) {
      await fetchBacklog()
    }
    return res
  }

  async function batchSort(sortList: { id: number; sortOrder: number }[]) {
    await batchSortApi(sortList)
  }

  async function removeItem(id: number) {
    await removeBacklogApi(id)
    await fetchBacklog()
  }

  async function addItem(gameId: number, notes?: string) {
    const res = await addBacklogApi({ gameId, notes })
    if (res.code === 200) {
      await fetchBacklog()
    }
    return res
  }

  /**
   * 在游戏库卡片上"加入想玩 / 取消想玩"的统一入口。
   * 乐观更新：先在本地翻状态，失败再回滚。
   */
  async function toggleWish(gameId: number, gameMeta?: BacklogItem['game']) {
    if (wishPending.value.has(gameId)) return { code: 0, msg: '处理中…' } as const
    wishPending.value.add(gameId)

    const existedId = gameIdToBacklogId.value.get(gameId)
    try {
      if (existedId != null) {
        // 已收藏 → 取消（本地先删，失败再补回）
        const snapshot = items.value.find(i => i.id === existedId)
        items.value = items.value.filter(i => i.id !== existedId)
        try {
          const res = await removeBacklogApi(existedId)
          if (res.code !== 200 && snapshot) {
            items.value = [...items.value, snapshot]
          }
          return res
        } catch (err) {
          if (snapshot) items.value = [...items.value, snapshot]
          throw err
        }
      } else {
        // 未收藏 → 加入（本地占位，等接口回来用真实数据替换）
        const tempId = -Math.floor(Math.random() * 1_000_000)
        const placeholder: BacklogItem = {
          id: tempId,
          userId: 0,
          gameId,
          status: 0,
          priority: 1,
          sortOrder: 0,
          notes: '',
          startedDate: '',
          completedDate: '',
          game: gameMeta || ({ id: gameId, name: '', icon: '', platforms: '', gameTypes: '', rating: 0 } as BacklogItem['game']),
        }
        items.value = [...items.value, placeholder]
        try {
          const res = await addBacklogApi({ gameId })
          if (res.code === 200 && res.data?.id) {
            items.value = items.value.map(i => i.id === tempId ? { ...placeholder, id: res.data.id } : i)
          } else {
            // 失败 → 撤销
            items.value = items.value.filter(i => i.id !== tempId)
          }
          return res
        } catch (err) {
          items.value = items.value.filter(i => i.id !== tempId)
          throw err
        }
      }
    } finally {
      wishPending.value.delete(gameId)
    }
  }

  async function checkin(backlogId: number, playTime: number, notes?: string) {
    const res = await checkinFromBacklogApi(backlogId, playTime, notes)
    if (res.code === 200) {
      await fetchBacklog()
    }
    return res
  }

  return {
    items, statusCounts, loading, loadedOnce,
    gameIdToBacklogId, wishPending,
    fetchBacklog, ensureBacklogLoaded, isInBacklog,
    updateStatus, batchSort, removeItem, addItem, toggleWish, checkin,
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useBacklogStore, import.meta.hot))
}
