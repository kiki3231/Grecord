/** 游戏展示名：优先库内 name_zh（Steam/豆瓣等权威来源），否则英文名 */
export function displayGameName(game: { name?: string | null; nameZh?: string | null; name_zh?: string | null }): string {
  const zh = (game.nameZh ?? game.name_zh ?? '').trim()
  if (zh) return zh
  return (game.name ?? '').trim() || '未知游戏'
}

const PLATFORM_ZH: Record<string, string> = {
  PC: 'PC',
  'PlayStation 5': 'PS5',
  'PlayStation 4': 'PS4',
  'PlayStation 3': 'PS3',
  'PlayStation 2': 'PS2',
  PlayStation: 'PlayStation',
  'Xbox Series S/X': 'Xbox Series',
  'Xbox One': 'Xbox One',
  'Xbox 360': 'Xbox 360',
  Xbox: 'Xbox',
  'Nintendo Switch': 'Switch',
  Wii: 'Wii',
  'Wii U': 'Wii U',
  'Game Boy Advance': 'GBA',
  'Game Boy Color': 'GBC',
  'Game Boy': 'Game Boy',
  NES: 'FC',
  SNES: 'SFC',
  macOS: 'Mac',
  Linux: 'Linux',
  iOS: 'iOS',
  Android: 'Android',
  Web: '网页',
}

const TYPE_ZH: Record<string, string> = {
  Action: '动作',
  Adventure: '冒险',
  RPG: '角色扮演',
  Strategy: '策略',
  Simulation: '模拟',
  Sports: '体育',
  Racing: '竞速',
  Puzzle: '解谜',
  Platformer: '平台',
  Shooter: '射击',
  Fighting: '格斗',
  Indie: '独立',
  Arcade: '街机',
  Casual: '休闲',
  'Massively Multiplayer': '大型多人在线',
  Family: '家庭',
  Board: '桌游',
  Card: '卡牌',
  Educational: '教育',
}

export function localizePlatform(name: string): string {
  return PLATFORM_ZH[name] ?? name
}

export function localizeGameType(name: string): string {
  return TYPE_ZH[name] ?? name
}

export function localizeCsv(csv: string | null | undefined, mapFn: (s: string) => string): string {
  if (!csv) return ''
  return csv
    .split(',')
    .map((s) => mapFn(s.trim()))
    .filter(Boolean)
    .join('、')
}
