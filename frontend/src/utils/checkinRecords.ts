import type { Game } from '@/stores/game'
import { displayGameName } from '@/utils/gameDisplay'

export function formatPlayMinutes(mins: number): string {
  if (!mins || mins <= 0) return '0m'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0 && m > 0) return `${h}h ${m}m`
  if (h > 0) return `${h}h`
  return `${m}m`
}

export interface QuickPickItem {
  game: Game
  lastDate: string
  lastPlayTime: number
}

export interface GameHistorySummary {
  lastDate: string
  lastPlayTime: number
  lastRating: number | null
  lastStatus: number | null
  totalMinutes: number
  recordCount: number
}

function recordGameId(r: Record<string, unknown>): number {
  const game = r.game as Record<string, unknown> | undefined
  return Number(r.gameId ?? game?.id ?? 0)
}

function toGame(game: Record<string, unknown>, gameId: number): Game {
  return {
    id: gameId,
    name: String(game.name ?? ''),
    nameZh: game.nameZh != null ? String(game.nameZh) : (game.name_zh != null ? String(game.name_zh) : undefined),
    description: String(game.description ?? ''),
    icon: String(game.icon ?? ''),
    platforms: String(game.platforms ?? ''),
    gameTypes: String(game.gameTypes ?? ''),
    avgPlayTime: Number(game.avgPlayTime ?? 0),
    rating: Number(game.rating ?? 0),
    review: String(game.review ?? ''),
    developer: String(game.developer ?? ''),
    source: String(game.source ?? '')
  }
}

/** 按最近打卡去重，取前 limit 款游戏 */
export function buildQuickPicks(
  records: Record<string, unknown>[],
  limit = 5
): QuickPickItem[] {
  const seen = new Set<number>()
  const out: QuickPickItem[] = []

  for (let i = 0; i < records.length; i++) {
    const r = records[i]!
    const gameId = recordGameId(r)
    if (!gameId || seen.has(gameId)) continue
    seen.add(gameId)

    const gameRaw = (r.game as Record<string, unknown> | undefined) ?? {}
    out.push({
      game: toGame(gameRaw, gameId),
      lastDate: String(r.recordDate ?? '').slice(0, 10),
      lastPlayTime: Number(r.playTime ?? 0)
    })
    if (out.length >= limit) break
  }
  return out
}

export function buildGameHistory(
  records: Record<string, unknown>[],
  gameId: number
): GameHistorySummary | null {
  const related: Record<string, unknown>[] = []
  for (let i = 0; i < records.length; i++) {
    const r = records[i]!
    if (recordGameId(r) === gameId) related.push(r)
  }
  if (related.length === 0) return null

  related.sort((a, b) => {
    const da = String(a.recordDate ?? '')
    const db = String(b.recordDate ?? '')
    return db.localeCompare(da)
  })

  const latest = related[0]!
  let totalMinutes = 0
  for (let i = 0; i < related.length; i++) {
    totalMinutes += Number(related[i]!.playTime ?? 0)
  }

  const rating = latest.rating
  const status = latest.status

  return {
    lastDate: String(latest.recordDate ?? '').slice(0, 10),
    lastPlayTime: Number(latest.playTime ?? 0),
    lastRating: rating != null && rating !== '' ? Number(rating) : null,
    lastStatus: status != null && status !== '' ? Number(status) : null,
    totalMinutes,
    recordCount: related.length
  }
}

export const STATUS_LABELS: Record<number, string> = {
  1: '在玩',
  2: '通关',
  3: '搁置',
  4: '白金'
}
