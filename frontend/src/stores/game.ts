import { acceptHMRUpdate, defineStore } from 'pinia'
import { ref } from 'vue'
import {
  searchGamesApi,
  getHotGamesApi,
  getGameFiltersApi,
  type SortKey,
  type FilterOption,
} from '@/api/game'

export interface Game {
  id: number
  name: string
  nameZh?: string | null
  description: string
  icon: string
  platforms: string
  gameTypes: string
  avgPlayTime: number
  rating: number
  review: string
  developer: string
  source: string
}

export const useGameStore = defineStore('game', () => {
  const searchResults = ref<Game[]>([])
  const hotGames = ref<Game[]>([])
  const searchLoading = ref(false)

  // Library state
  const libraryGames = ref<Game[]>([])
  const libraryTotal = ref(0)
  const libraryPage = ref(1)
  const libraryTotalPages = ref(1)
  const libraryLoading = ref(false)
  const libraryKeyword = ref('')
  const libraryPlatform = ref('')
  const libraryType = ref('')
  const librarySort = ref<SortKey>('rating_desc')
  const PAGE_SIZE = 24

  const platformOptions = ref<FilterOption[]>([])
  const typeOptions = ref<FilterOption[]>([])
  const filtersLoaded = ref(false)

  async function searchGames(keyword: string, page = 1, size = 20) {
    searchLoading.value = true
    try {
      const res = await searchGamesApi({ keyword, page, size })
      if (res.code === 200) {
        searchResults.value = res.data.records
      }
    } finally {
      searchLoading.value = false
    }
  }

  async function fetchHotGames(page = 1, size = 20) {
    const res = await getHotGamesApi(page, size)
    if (res.code === 200) {
      hotGames.value = res.data.records
    }
  }

  async function fetchFilters(force = false) {
    if (filtersLoaded.value && !force) return
    try {
      const res = await getGameFiltersApi()
      if (res.code === 200) {
        platformOptions.value = res.data.platforms
        typeOptions.value = res.data.types
        filtersLoaded.value = true
      }
    } catch (err) {
      console.error('[fetchFilters] 失败：', err)
    }
  }

  interface LibraryQuery {
    keyword?: string
    platform?: string
    type?: string
    sortBy?: SortKey
  }

  async function fetchLibrary(opts: LibraryQuery & { reset?: boolean } = {}) {
    const { reset = false, keyword, platform, type, sortBy } = opts

    if (reset) {
      libraryPage.value = 1
      libraryGames.value = []
      if (keyword !== undefined) libraryKeyword.value = keyword
      if (platform !== undefined) libraryPlatform.value = platform
      if (type !== undefined) libraryType.value = type
      if (sortBy !== undefined) librarySort.value = sortBy
    }

    libraryLoading.value = true
    try {
      const res = await searchGamesApi({
        keyword: libraryKeyword.value,
        platform: libraryPlatform.value,
        type: libraryType.value,
        sortBy: librarySort.value,
        page: libraryPage.value,
        size: PAGE_SIZE,
      })
      if (res.code === 200) {
        const page = res.data
        if (libraryPage.value === 1) {
          libraryGames.value = page.records
        } else {
          libraryGames.value = [...libraryGames.value, ...page.records]
        }
        libraryTotal.value = page.total
        libraryTotalPages.value = Math.ceil(page.total / PAGE_SIZE)
      } else {
        console.error('[fetchLibrary] 后端返回非 200：', res)
      }
    } catch (err) {
      console.error('[fetchLibrary] 请求失败：', err)
    } finally {
      libraryLoading.value = false
    }
  }

  async function loadMoreLibrary() {
    if (libraryPage.value >= libraryTotalPages.value || libraryLoading.value) return
    libraryPage.value++
    await fetchLibrary()
  }

  function resetLibraryFilters() {
    libraryKeyword.value = ''
    libraryPlatform.value = ''
    libraryType.value = ''
    librarySort.value = 'rating_desc'
    return fetchLibrary({ reset: true })
  }

  return {
    searchResults, hotGames, searchLoading, searchGames, fetchHotGames,
    libraryGames, libraryTotal, libraryPage, libraryTotalPages, libraryLoading,
    libraryKeyword, libraryPlatform, libraryType, librarySort,
    platformOptions, typeOptions, filtersLoaded,
    fetchFilters, fetchLibrary, loadMoreLibrary, resetLibraryFilters,
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useGameStore, import.meta.hot))
}
