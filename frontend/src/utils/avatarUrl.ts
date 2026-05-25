/** 去掉已有查询参数，保留路径 */
export function stripAvatarQuery(raw: string): string {
  return raw.trim().split('?')[0]
}

/**
 * 为头像 URL 附加版本号，避免浏览器强缓存导致上传/重登后不刷新。
 * 数据库存储应为纯路径，如 /avatar/user_3_avatar.jpg
 */
export function resolveAvatarUrl(raw: string | undefined | null, version?: number): string {
  if (!raw || typeof raw !== 'string') return ''
  const path = stripAvatarQuery(raw)
  if (path.length < 2) return ''

  const v = version ?? Date.now()
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return `${path}?v=${v}`
  }
  const normalized = path.startsWith('/') ? path : `/${path}`
  return `${normalized}?v=${v}`
}
