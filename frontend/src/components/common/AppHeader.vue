<script setup>
import { useRouter, useRoute } from "vue-router";
import BaseButton from "./BaseButton.vue";
import { useAuthStore } from "../../stores/useAuthStore";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 로고 옆 목차(네비게이션) 항목
const NAV_ITEMS = [
  { label: "배포하기", to: "/deploy/new", icon: "🚀" },
  { label: "내가 배포한 서비스", to: "/deployments", icon: "📦" },
  { label: "마이페이지", to: "/mypage", icon: "👤" },
];

// 로그인 상태에서만 이동. 비로그인 시 로그인 화면으로 보낸다.
function navigate(to) {
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: to } });
    return;
  }
  router.push(to);
}

function isActive(to) {
  return route.path === to || route.path.startsWith(to + "/");
}

function handleLogout() {
  authStore.logout();
  router.push("/");
}
</script>

<template>
  <header class="sticky top-0 z-10 border-b border-border bg-surface">
    <div class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
      <!-- 로고 + 목차 -->
      <div class="flex items-center gap-6">
        <button
          type="button"
          class="text-lg font-bold text-ink-900 transition-opacity hover:opacity-70"
          @click="navigate('/home')"
        >
          <span class="text-primary">Deploy</span>Hub
        </button>

        <nav class="hidden items-center gap-1 sm:flex">
          <button
            v-for="item in NAV_ITEMS"
            :key="item.to"
            type="button"
            :class="[
              'rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
              isActive(item.to)
                ? 'bg-primary-light text-primary'
                : 'text-ink-700 hover:bg-black/5',
            ]"
            @click="navigate(item.to)"
          >
            <span class="mr-1">{{ item.icon }}</span>{{ item.label }}
          </button>
        </nav>
      </div>

      <!-- 로그아웃 -->
      <BaseButton variant="secondary" @click="handleLogout">로그아웃</BaseButton>
    </div>

    <!-- 모바일 목차 -->
    <nav class="flex items-center gap-1 overflow-x-auto border-t border-border/60 px-3 py-2 sm:hidden">
      <button
        v-for="item in NAV_ITEMS"
        :key="item.to"
        type="button"
        :class="[
          'whitespace-nowrap rounded-md px-3 py-1.5 text-xs font-medium transition-colors',
          isActive(item.to)
            ? 'bg-primary-light text-primary'
            : 'text-ink-700 hover:bg-black/5',
        ]"
        @click="navigate(item.to)"
      >
        <span class="mr-1">{{ item.icon }}</span>{{ item.label }}
      </button>
    </nav>
  </header>
</template>
