import { acceptHMRUpdate, defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getBacklogListApi,
  updateBacklogStatusApi,
  batchSortApi,
  removeBacklogApi,
  addBacklogApi,
  checkinFromBacklogApi,
} from '@/api/backlog'

export interface BacklogItem {
  id: number
  userId: number
  gameId: number
  status: number | null
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

/** gameId -> 当前激活 backlog 记录的真实 id 与 status；不存在条目即视为"无 status / 灰心" */
interface BacklogEntry {
  id: number
  status: number | null
}

export const useBacklogStore = defineStore('backlog', () => {
  // Backlog 页面渲染用的列表；可能被 tab 过滤为部分状态
  const items = ref<BacklogItem[]>([])
  const statusCounts = ref<Record<number, number>>({})
  const loading = ref(false)
  const loadedOnce = ref(false)
  /** Library 页面爱心专用：wishmap 是否已完成首次加载，用于协调心形图标与游戏列表的显示时序 */
  const wishMapLoaded = ref(false)

  /**
   * 已有 backlog 记录的权威映射：gameId -> { backlogId, status }。
   * Library 卡片爱心是否亮（红心 = 想玩）完全以此为准，避免：
   *  1) items 被 tab 过滤后丢状态；
   *  2) fetchBacklog 与 toggleWish 之间的 race 窗口；
   *  3) HMR 后旧闭包持有过期 items。
   *
   * 语义：
   *   map 无该 gameId → 数据库无记录（或 is_delete=1），灰心
   *   map.status === 0 → "想玩"，红心
   *   map.status !== 0 → 在玩/已完成/已搁置，灰心
   */
  const backlogMap = ref<Map<number, BacklogEntry>>(new Map())

  /** 操作中的 gameId 集合，避免重复点击触发并发请求 */
  const wishPending = ref<Set<number>>(new Set())

  function rebuildBacklogMap(list: BacklogItem[]) {
    const m = new Map<number, BacklogEntry>()
    for (const it of list) {
      if (it.gameId && it.id > 0) m.set(it.gameId, { id: it.id, status: it.status })
    }
    backlogMap.value = m
  }

  /** 该游戏在 backlog 表中是否有活动记录（无视 status） */
  function isInBacklog(gameId: number) {
    return backlogMap.value.has(gameId)
  }

  /** 该游戏是否被标记为"想玩"（status === 0），红心专用判断 */
  function isWanted(gameId: number) {
    const entry = backlogMap.value.get(gameId)
    return entry !== undefined && entry.status === 0
  }

  /** 读取游戏当前在 backlog 中的 status，无记录返回 null */
  function getBacklogStatus(gameId: number): number | null {
    const entry = backlogMap.value.get(gameId)
    return entry ? entry.status : null
  }

  async function fetchBacklog(status?: number) {
    loading.value = true
    try {
      const res = await getBacklogListApi(status)
      if (res.code === 200) {
        const list: BacklogItem[] = res.data.items || []
        items.value = list

        if (status === undefined || status === null) {
          // 全量查询：用结果整体重建 map
          rebuildBacklogMap(list)
        } else {
          // 按 status 过滤：只刷新本次结果中出现的 gameId 条目，
          // 不删除 map 中其他 status 的条目，避免误清状态
          const m = new Map(backlogMap.value)
          for (const it of list) {
            if (it.gameId && it.id > 0) m.set(it.gameId, { id: it.id, status: it.status })
          }
          backlogMap.value = m
        }

        const counts: Record<number, number> = {}
        const rawCounts = res.data.statusCounts || []
        for (const c of rawCounts) {
          counts[c.status] = c.count
        }
        statusCounts.value = counts
        loadedOnce.value = true
      } else {
        console.warn('[fetchBacklog] 后端返回非 200：', res)
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

  /**
   * 仅刷新 backlogMap（不替换 items），用于 Library 页面进入时
   * 拉取"我对哪些游戏有 backlog 记录、当前 status 各是多少"，
   * 不干扰 Backlog 页面正在使用的 items（可能被 tab 过滤过）
   */
  async function refreshWishMap() {
    try {
      const res = await getBacklogListApi()
      if (res.code === 200) {
        const list: BacklogItem[] = res.data.items || []
        rebuildBacklogMap(list)
      }
    } catch (err) {
      console.error('[refreshWishMap] 失败：', err)
    } finally {
      wishMapLoaded.value = true
    }
  }

  async function updateStatus(id: number, newStatus: number) {
    const res = await updateBacklogStatusApi(id, newStatus)
    if (res.code === 200) {
      // 局部同步 map：根据 backlogId 反查 gameId，立刻把 status 改掉
      for (const [gameId, entry] of backlogMap.value.entries()) {
        if (entry.id === id) {
          backlogMap.value.set(gameId, { id, status: newStatus })
          break
        }
      }
      await fetchBacklog()
    }
    return res
  }

  async function batchSort(sortList: { id: number; sortOrder: number }[]) {
    await batchSortApi(sortList)
  }

  async function removeItem(id: number) {
    if (id < 0) {
      items.value = items.value.filter(i => i.id !== id)
      return { code: 200, msg: 'OK', data: null } as const
    }
    const snapshot = [...items.value]
    const target = items.value.find(i => i.id === id)
    items.value = items.value.filter(i => i.id !== id)

    let prevEntry: BacklogEntry | undefined
    if (target?.gameId) {
      prevEntry = backlogMap.value.get(target.gameId)
      backlogMap.value.delete(target.gameId)
    }

    try {
      const res = await removeBacklogApi(id)
      if (res?.code === 200) {
        await fetchBacklog()
        return res
      }
      // 失败回滚
      items.value = snapshot
      if (target?.gameId && prevEntry) backlogMap.value.set(target.gameId, prevEntry)
      console.warn('[backlog.removeItem] 后端返回非 200：', res)
      return res
    } catch (err) {
      items.value = snapshot
      if (target?.gameId && prevEntry) backlogMap.value.set(target.gameId, prevEntry)
      console.error('[backlog.removeItem] 请求异常：', err)
      throw err
    }
  }

  async function addItem(gameId: number, notes?: string) {
    const res = await addBacklogApi(
      notes != null && notes !== '' ? { gameId, status: 0, notes } : { gameId, status: 0 },
    )
    if (res.code === 200 && res.data?.id) {
      backlogMap.value.set(gameId, { id: res.data.id, status: 0 })
      await fetchBacklog()
    }
    return res
  }

  /**
   * Library 卡片爱心统一入口。语义严格按 status 判断：
   *
   *   1) 当前 status === 0（红心）  →  从清单移除该条（与清单页「移除」一致，is_delete=1），灰心
   *   2) 当前有记录但 status !== 0（灰心）  →  把 status 改成 0  →  红心
   *   3) 当前无记录（灰心）  →  新增记录，status=0  →  红心
   *
   * 关键约定：爱心红/灰只看 backlogMap 中 status===0 是否成立，不看 items。
   */
  async function toggleWish(_gameId: number, _gameMeta?: BacklogItem['game']) {
    void _gameMeta
    const gameId = _gameId
    if (wishPending.value.has(gameId)) {
      return { code: 0, msg: '处理中…' } as const
    }
    wishPending.value.add(gameId)
    try {
      const existing = backlogMap.value.get(gameId)

      // ── Case 1：当前已是想玩(status=0) → 取消想玩 = 从清单移除（后端软删）──
      if (existing && existing.status === 0) {
        const backlogId = existing.id
        const snapshotItems = [...items.value]
        items.value = items.value.filter(i => i.id !== backlogId)
        backlogMap.value.delete(gameId)
        try {
          const res = await removeBacklogApi(backlogId)
          if (res?.code === 200) {
            await fetchBacklog()
            return res
          }
          items.value = snapshotItems
          backlogMap.value.set(gameId, existing)
          console.warn('[toggleWish] 取消想玩(移除)失败：', res)
          return res
        } catch (err) {
          items.value = snapshotItems
          backlogMap.value.set(gameId, existing)
          console.error('[toggleWish] 取消想玩(移除)异常：', err)
          throw err
        }
      }

      // ── Case 2：已有记录但 status !== 0 → 改 status 为 0 ──
      if (existing && existing.status !== 0) {
        const prevStatus = existing.status
        backlogMap.value.set(gameId, { id: existing.id, status: 0 })
        try {
          const res = await updateBacklogStatusApi(existing.id, 0)
          if (res?.code === 200) {
            fetchBacklog()
            return res
          }
          // 失败回滚
          backlogMap.value.set(gameId, { id: existing.id, status: prevStatus })
          console.warn('[toggleWish] 状态改 0 失败：', res)
          return res
        } catch (err) {
          backlogMap.value.set(gameId, { id: existing.id, status: prevStatus })
          console.error('[toggleWish] 状态改 0 异常：', err)
          throw err
        }
      }

      // ── Case 3：无记录 → 新增（status=0） ──
      try {
        const res = await addBacklogApi({ gameId, status: 0 })
        if (res?.code === 200 && res.data?.id) {
          backlogMap.value.set(gameId, { id: res.data.id, status: 0 })
          fetchBacklog()
          return res
        }
        if (res?.code === 409) {
          // 后端已存在（跨标签同步或上次未对齐）→ 拉一次最新映射
          await refreshWishMap()
          return res
        }
        console.warn('[toggleWish] 新增失败：', res)
        return res
      } catch (err) {
        console.error('[toggleWish] 新增异常：', err)
        throw err
      }
    } finally {
      wishPending.value.delete(gameId)
    }
  }

  async function checkin(
    backlogId: number,
    payload: { playTime: number; notes?: string; rating?: number; status?: number },
    listStatusFilter?: number,
  ) {
    const res = await checkinFromBacklogApi(backlogId, payload)
    if (res.code === 200) {
      await fetchBacklog(listStatusFilter)
    }
    return res
  }

  return {
    items, statusCounts, loading, loadedOnce, wishMapLoaded,
    backlogMap, wishPending,
    fetchBacklog, ensureBacklogLoaded, refreshWishMap,
    isInBacklog, isWanted, getBacklogStatus,
    updateStatus, batchSort, removeItem, addItem, toggleWish, checkin,
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useBacklogStore, import.meta.hot))
}
