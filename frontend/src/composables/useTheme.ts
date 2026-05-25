import { ref } from 'vue'

const _isDark = ref(true)
let _ready = false

function _syncDom() {
  document.documentElement.setAttribute('data-theme', _isDark.value ? 'dark' : 'light')
}

export function useTheme() {
  function init() {
    if (_ready) return
    _ready = true
    const saved = localStorage.getItem('grecord-theme')
    _isDark.value = saved !== 'light'
    _syncDom()
  }

  function toggle() {
    _isDark.value = !_isDark.value
    localStorage.setItem('grecord-theme', _isDark.value ? 'dark' : 'light')
    _syncDom()
  }

  return { isDark: _isDark, init, toggle }
}
