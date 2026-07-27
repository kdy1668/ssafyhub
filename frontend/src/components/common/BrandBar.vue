<script setup>
import { RouterLink } from "vue-router";
</script>

<template>
  <!-- 로그인/회원가입 등 비로그인 화면용: 로고만 있는 상단바 (클릭 시 홈으로) -->
  <header class="sticky top-0 z-10 border-b border-border bg-surface">
    <div class="mx-auto flex max-w-6xl items-center px-4 py-3 sm:px-6">
      <RouterLink
        to="/"
        class="text-lg font-bold text-ink-900 transition-opacity hover:opacity-70"
      >
        <span class="text-primary">SSAFY</span>HUB
      </RouterLink>
    </div>
  </header>
</template>
