<script setup>
import { RouterLink } from "vue-router";
import BaseButton from "../common/BaseButton.vue";
</script>

<template>
  <header class="sticky top-0 z-10 border-b border-border bg-surface">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
      <span class="text-lg font-bold text-ink-900"><span class="text-primary">SSAFY</span>HUB</span>
      <RouterLink to="/login">
        <BaseButton variant="secondary">로그인</BaseButton>
      </RouterLink>
    </div>
  </header>
</template>
