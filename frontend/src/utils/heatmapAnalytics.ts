/** 热力图单日条目（与 record store 一致） */
export interface HeatmapDay {
  date: string
  count: number
  totalMinutes: number
}

export interface HeatmapAnalytics {
  activeDayCount: number
  longestStreak: number
  maxDayMinutes: number
  todayMinutes: number
  todayCheckedIn: boolean
  streakFromToday: number
}

/** 本地日历 YYYY-MM-DD，避免 toISOString 时区偏移导致「今日」判断错误 */
export function localDateKey(d = new Date()): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function dayDiffDays(prev: string, next: string): number {
  const ta = new Date(`${prev}T12:00:00`).getTime()
  const tb = new Date(`${next}T12:00:00`).getTime()
  return Math.round((tb - ta) / 86400000)
}

/**
 * 单次遍历 heatmap 数据，汇总洞察指标与连续打卡天数。
 * 数据量通常为全年 ≤366 条，计算成本可忽略。
 */
export function analyzeHeatmap(
  data: HeatmapDay[],
  todayKey = localDateKey()
): HeatmapAnalytics {
  const activeDates: string[] = []
  let maxDayMinutes = 0
  let todayMinutes = 0
  let todayCheckedIn = false

  for (let i = 0; i < data.length; i++) {
    const item = data[i]
    if (!item?.date || item.count <= 0) continue
    activeDates.push(item.date)
    const mins = item.totalMinutes || 0
    if (mins > maxDayMinutes) maxDayMinutes = mins
    if (item.date === todayKey) {
      todayCheckedIn = true
      todayMinutes = mins
    }
  }

  activeDates.sort()

  let longestStreak = 0
  if (activeDates.length > 0) {
    let run = 1
    let best = 1
    for (let i = 1; i < activeDates.length; i++) {
      const prev = activeDates[i - 1]!
      const curr = activeDates[i]!
      if (dayDiffDays(prev, curr) === 1) {
        run++
        if (run > best) best = run
      } else {
        run = 1
      }
    }
    longestStreak = best
  }

  const dateSet = new Set(activeDates)
  let streakFromToday = 0
  const cursor = new Date()
  while (dateSet.has(localDateKey(cursor))) {
    streakFromToday++
    cursor.setDate(cursor.getDate() - 1)
  }

  return {
    activeDayCount: activeDates.length,
    longestStreak,
    maxDayMinutes,
    todayMinutes,
    todayCheckedIn,
    streakFromToday
  }
}
