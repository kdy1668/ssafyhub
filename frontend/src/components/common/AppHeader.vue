<script setup>
import { computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { storeToRefs } from "pinia";
import BaseButton from "./BaseButton.vue";
import AppIcon from "./AppIcon.vue";
import { useAuthStore } from "../../stores/useAuthStore";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { isAdmin, isAuthenticated } = storeToRefs(authStore);

// 로고 옆 목차(네비게이션) 항목. public: true 인 항목은 비로그인 상태에서도 바로 이동한다.
const BASE_NAV_ITEMS = [
  { label: "쇼케이스", to: "/showcase", icon: "eye", public: true },
  { label: "배포하기", to: "/deploy/new", icon: "upload" },
  { label: "내가 배포한 서비스", to: "/deployments", icon: "cube" },
  // 런타임 로그는 배포 상세 안의 버튼으로만 갈 수 있었다. 그 버튼은 실행 중일 때만 보이므로
  // 실패한 배포에서는 로그 화면(과 AI 이상 감지)에 도달할 방법이 아예 없다. 목차에 직접 노출한다.
  { label: "런타임 로그", to: "/deployments/logs", icon: "terminal" },
  { label: "마이페이지", to: "/mypage", icon: "user" },
];

// 관리자에게만 "관리자" 메뉴 노출
const NAV_ITEMS = computed(() =>
  isAdmin.value
    ? [...BASE_NAV_ITEMS, { label: "관리자", to: "/admin", icon: "settings" }]
    : BASE_NAV_ITEMS
);

// public 항목은 비로그인 상태에서도 바로 이동, 그 외는 로그인 화면으로 보낸다.
function navigate(item) {
  if (!item.public && !authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: item.to } });
    return;
  }
  router.push(item.to);
}

function isActive(to) {
  if (route.path === to) {
    return true;
  }
  if (!route.path.startsWith(to + "/")) {
    return false;
  }
  // 하위 경로에서는 상위 항목을 활성 표시한다(리소스/환경변수 화면에서 "내가 배포한 서비스"가 켜지는 동작).
  // 단 그 하위 경로 자체가 목차에 있으면 그 항목만 켜야 두 개가 동시에 강조되지 않는다.
  return !NAV_ITEMS.value.some((item) => item.to === route.path);
}

// 쿠키 삭제까지 끝난 뒤 이동해야 다음 화면의 요청이 만료된 쿠키를 보내지 않는다.
async function handleLogout() {
  await authStore.logout();
  router.push("/login");
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
          @click="router.push('/')"
        >
          <span class="text-primary">SSAFY</span>HUB
        </button>

        <nav class="hidden items-center gap-1 sm:flex">
          <button
            v-for="item in NAV_ITEMS"
            :key="item.to"
            type="button"
            :class="[
              'inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
              isActive(item.to)
                ? 'bg-primary-light text-primary'
                : 'text-ink-700 hover:bg-black/5',
            ]"
            @click="navigate(item)"
          >
            <AppIcon :name="item.icon" class="h-4 w-4" />{{ item.label }}
          </button>
        </nav>
      </div>

      <!-- 로그인 상태에 따라 로그인/로그아웃 버튼만 전환 -->
      <BaseButton v-if="isAuthenticated" variant="secondary" @click="handleLogout">로그아웃</BaseButton>
      <BaseButton v-else variant="primary" @click="router.push({ path: '/login', query: { from: route.fullPath } })">
        로그인
      </BaseButton>
    </div>

    <!-- 모바일 목차 -->
    <nav class="flex items-center gap-1 overflow-x-auto border-t border-border/60 px-3 py-2 sm:hidden">
      <button
        v-for="item in NAV_ITEMS"
        :key="item.to"
        type="button"
        :class="[
          'inline-flex items-center gap-1.5 whitespace-nowrap rounded-md px-3 py-1.5 text-xs font-medium transition-colors',
          isActive(item.to)
            ? 'bg-primary-light text-primary'
            : 'text-ink-700 hover:bg-black/5',
        ]"
        @click="navigate(item.to)"
      >
        <AppIcon :name="item.icon" class="h-3.5 w-3.5" />{{ item.label }}
      </button>
    </nav>
  </header>
</template>
