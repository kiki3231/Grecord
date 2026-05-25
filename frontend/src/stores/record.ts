import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getHeatmapApi, getStatsApi, getRecordsWithGameApi, createRecordApi } from '@/api/record'
import { useBacklogStore } from '@/stores/backlog'

export interface HeatmapItem {
  date: string
  count: number
  totalMinutes: number
}

export const useRecordStore = defineStore('record', () => {
  const heatmapData = ref<HeatmapItem[]>([])
  const stats = ref<Record<string, unknown>>({})
  const topGames = ref<Record<string, unknown>[]>([])
  const recentRecords = ref<Record<string, unknown>[]>([])
  const loading = ref(false)

  async function fetchHeatmap(year?: number) {
    const res = await getHeatmapApi(year)
    if (res.code === 200) {
      heatmapData.value = res.data
    }
  }

  async function fetchStats() {
    const res = await getStatsApi()
    if (res.code === 200) {
      stats.value = res.data.summary || {}
      topGames.value = res.data.topGames || []
    }
  }

  /** @returns 是否成功拉取到列表 */
  async function fetchRecentRecords(): Promise<boolean> {
    try {
      const res = await getRecordsWithGameApi()
      if (res.code === 200) {
        recentRecords.value = Array.isArray(res.data) ? res.data : []
        return true
      }
      return false
    } catch {
      return false
    }
  }

  async function createRecord(data: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await createRecordApi(data)
      if (res.code === 200) {
        await fetchHeatmap()
        await fetchStats()
        try {
          await useBacklogStore().fetchBacklog()
        } catch (e) {
          console.warn('[record.createRecord] 刷新清单失败（可忽略）：', e)
        }
      }
      return res
    } finally {
      loading.value = false
    }
  }

  return { heatmapData, stats, topGames, recentRecords, loading, fetchHeatmap, fetchStats, fetchRecentRecords, createRecord }
})
