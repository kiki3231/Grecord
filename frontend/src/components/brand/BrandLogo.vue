<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    /** sm=32 md=40 lg=48 or pixel number */
    size?: 'sm' | 'md' | 'lg' | number
    /** Gradient orb (default) or icon only */
    variant?: 'orb' | 'mark'
    glow?: boolean
  }>(),
  { size: 'md', variant: 'orb', glow: true }
)

const SIZE_MAP = { sm: 32, md: 40, lg: 48 } as const

const px = computed(() =>
  typeof props.size === 'number' ? props.size : SIZE_MAP[props.size]
)
</script>

<template>
  <div
    class="brand-logo"
    :class="[
      `brand-logo--${variant}`,
      { 'brand-logo--glow': glow && variant === 'orb' }
    ]"
    :style="{ '--brand-logo-size': `${px}px` }"
    role="img"
    aria-label="GRecord"
  >
    <div v-if="variant === 'orb'" class="brand-logo__orb">
      <svg
        class="brand-logo__mark"
        viewBox="0 0 100 100"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        aria-hidden="true"
      >
        <path
          stroke="currentColor"
          stroke-width="4.5"
          stroke-linejoin="round"
          stroke-linecap="round"
          d="M 36 36 H 64 C 70 36 74 40 74 46 V 50 C 74 56 70 60 64 60 H 58 L 52 66 C 50 68 50 68 48 66 L 42 60 H 36 C 30 60 26 56 26 50 V 46 C 26 40 30 36 36 36 Z"
        />
        <path
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          d="M 36 45 H 42 M 39 42 V 48"
        />
        <circle cx="60" cy="44" r="2.2" fill="currentColor" />
        <circle cx="64.5" cy="48" r="2.2" fill="currentColor" />
      </svg>
    </div>
    <svg
      v-else
      class="brand-logo__mark brand-logo__mark--solo"
      viewBox="0 0 100 100"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      aria-hidden="true"
    >
      <path
        stroke="currentColor"
        stroke-width="4.5"
        stroke-linejoin="round"
        stroke-linecap="round"
        d="M 36 36 H 64 C 70 36 74 40 74 46 V 50 C 74 56 70 60 64 60 H 58 L 52 66 C 50 68 50 68 48 66 L 42 60 H 36 C 30 60 26 56 26 50 V 46 C 26 40 30 36 36 36 Z"
      />
      <path
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
        d="M 36 45 H 42 M 39 42 V 48"
      />
      <circle cx="60" cy="44" r="2.2" fill="currentColor" />
      <circle cx="64.5" cy="48" r="2.2" fill="currentColor" />
    </svg>
  </div>
</template>

<style scoped>
.brand-logo {
  --brand-logo-size: 40px;
  width: var(--brand-logo-size);
  height: var(--brand-logo-size);
  flex-shrink: 0;
}

.brand-logo__orb {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff5caa 0%, #d78ceb 52%, #8b7cf8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: var(--transition-bounce, transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1));
}

.brand-logo--glow .brand-logo__orb {
  box-shadow:
    0 0 18px rgba(255, 110, 181, 0.6),
    0 0 40px rgba(167, 139, 250, 0.2);
}

.brand-logo__mark {
  width: 58%;
  height: 58%;
  display: block;
}

.brand-logo__mark--solo {
  width: 100%;
  height: 100%;
  color: #fff;
}

.brand-logo--mark {
  border-radius: 12px;
  background: linear-gradient(135deg, #ff5caa 0%, #d78ceb 52%, #8b7cf8 100%);
  padding: 18%;
  box-shadow: var(--shadow-md, 0 4px 14px rgba(255, 110, 181, 0.35));
}
</style>
