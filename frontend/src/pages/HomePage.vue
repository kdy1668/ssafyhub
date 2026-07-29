<script setup>
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useAuthStore } from "../stores/useAuthStore";

const router = useRouter();
const authStore = useAuthStore();
const { data: user } = useCurrentUser();

const HINTS = [
  { icon: "rocket", title: "배포 신청", desc: "JAR/ZIP을 업로드해 서비스를 배포" },
  { icon: "chart", title: "상태 확인", desc: "배포 상태와 리소스를 한눈에" },
];

function goDeploy() {
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: "/deploy/new" } });
    return;
  }
  router.push("/deploy/new");
}

function goManage() {
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: "/deployments" } });
    return;
  }
  router.push("/deployments");
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />

    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-12">
      <!-- 히어로 (화이트 + 은은한 블루 글로우) -->
      <section
        class="relative overflow-hidden rounded-2xl border border-border bg-surface px-7 py-9 shadow-card sm:px-10 sm:py-10"
      >
        <div class="pointer-events-none absolute -right-24 -top-28 h-60 w-60 rounded-full bg-primary/[0.07] blur-3xl" />
        <div class="pointer-events-none absolute -bottom-28 -left-20 h-60 w-60 rounded-full bg-sky/[0.08] blur-3xl" />

        <div class="relative z-10">
          <span class="inline-flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.24em] text-primary">
            <span class="h-1.5 w-1.5 rounded-full bg-primary" />
            SSAFYHUB
          </span>

          <h1 class="mt-3 text-[26px] font-bold leading-[1.2] tracking-tight text-ink-900 sm:text-4xl">
            <template v-if="user">{{ user.name }}님, 안녕하세요</template>
            <template v-else>SSAFY 팀의 배포를<br />한 곳에서 모아보세요</template>
          </h1>
          <p class="mt-3 max-w-md text-sm leading-relaxed text-ink-500 sm:text-[15px]">
            JAR/ZIP 파일을 업로드하고 배포 상태를 한눈에 확인하세요.
          </p>

          <!-- CTA -->
          <div class="mt-6 flex flex-wrap gap-2.5">
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-primary-hover"
              @click="goDeploy"
            >
              <AppIcon name="upload" class="h-4 w-4" />
              배포 신청하기
            </button>
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-primary ring-1 ring-primary/20 transition hover:bg-primary-light"
              @click="goManage"
            >
              <AppIcon name="cube" class="h-4 w-4" />
              내가 배포한 서비스
            </button>
          </div>
        </div>
      </section>

      <!-- 안내 카드 -->
      <div class="mt-10 overflow-hidden rounded-2xl border border-border bg-surface shadow-card">
        <div class="grid grid-cols-1 divide-y divide-border sm:grid-cols-2 sm:divide-x sm:divide-y-0">
          <div v-for="h in HINTS" :key="h.title" class="flex items-start gap-3 px-5 py-4">
            <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary-light text-primary">
              <AppIcon :name="h.icon" class="h-5 w-5" />
            </span>
            <div>
              <p class="text-sm font-semibold text-ink-900">{{ h.title }}</p>
              <p class="mt-0.5 text-xs leading-relaxed text-ink-500">{{ h.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
