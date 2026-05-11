<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(async () => {
  if (localStorage.getItem('token')) {
    await userStore.fetchUserInfo()
  }
})
</script>
<template>
  <router-view />
</template>
