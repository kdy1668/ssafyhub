<script setup>
import { useRouter } from "vue-router";
import BaseButton from "../common/BaseButton.vue";
import { useAuthStore } from "../../stores/useAuthStore";

const router = useRouter();
const authStore = useAuthStore();

function handleLogout() {
  authStore.logout();
  router.push("/");
}
</script>

<template>
  <header class="sticky top-0 z-10 border-b border-border bg-surface">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
      <span class="text-lg font-bold text-ink-900">DeployHub</span>
      <div class="flex items-center gap-2">
        <BaseButton variant="ghost">마이페이지</BaseButton>
        <BaseButton variant="secondary" @click="handleLogout">로그아웃</BaseButton>
      </div>
    </div>
  </header>
</template>
