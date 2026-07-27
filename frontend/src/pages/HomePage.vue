<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import FeedCard from "../components/home/FeedCard.vue";
import { usePosts } from "../composables/usePosts";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useAuthStore } from "../stores/useAuthStore";

const router = useRouter();
const authStore = useAuthStore();
const { data: posts, isLoading, isError, error } = usePosts();
const { data: user } = useCurrentUser();

const FILTERS = [
  { key: "all", label: "전체" },
  { key: "deploy", label: "배포 공유" },
  { key: "free", label: "자유" },
];
const activeFilter = ref("all");

const allPosts = computed(() => posts.value ?? []);
const filteredPosts = computed(() => {
  if (activeFilter.value === "all") return allPosts.value;
  return allPosts.value.filter((p) => (p.type ?? "free") === activeFilter.value);
});

const deployCount = computed(() => allPosts.value.filter((p) => p.type === "deploy").length);
const freeCount = computed(() => allPosts.value.filter((p) => (p.type ?? "free") === "free").length);

const HINTS = [
  { icon: "rocket", title: "배포 공유", desc: "내가 배포한 서비스를 팀에 소개" },
  { icon: "chat", title: "자유 게시판", desc: "질문·회고·잡담 자유롭게" },
  { icon: "chart", title: "상태 확인", desc: "배포 상태와 리소스를 한눈에" },
];

const emptyMessage = computed(() => {
  if (activeFilter.value === "deploy") return "아직 공유된 배포 글이 없어요.";
  if (activeFilter.value === "free") return "아직 자유 게시글이 없어요.";
  return "아직 게시물이 없어요.";
});

function goDeploy() {
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: "/deploy/new" } });
    return;
  }
  router.push("/deploy/new");
}

function goWrite(type) {
  const target = type ? `/board/new?type=${type}` : "/board/new";
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: target } });
    return;
  }
  router.push(target);
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
            팀원들이 배포한 서비스를 둘러보고, 소식과 의견을 자유롭게 나눠보세요.
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
              @click="goWrite()"
            >
              <AppIcon name="pencil" class="h-4 w-4" />
              글 작성
            </button>
          </div>

          <!-- 통계: 숫자 위계 + 구분선 -->
          <div class="mt-8 flex items-center gap-6 border-t border-border pt-5">
            <div class="flex items-baseline gap-1.5">
              <span class="text-2xl font-bold tracking-tight text-primary">{{ deployCount }}</span>
              <span class="text-xs text-ink-500">배포 공유</span>
            </div>
            <div class="h-8 w-px bg-border" />
            <div class="flex items-baseline gap-1.5">
              <span class="text-2xl font-bold tracking-tight text-primary">{{ freeCount }}</span>
              <span class="text-xs text-ink-500">자유 글</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 피드 헤더: 제목 + 세그먼트 필터 -->
      <div class="mb-6 mt-10 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 class="text-lg font-bold tracking-tight text-ink-900">팀 피드</h2>
          <p class="mt-0.5 text-xs text-ink-500">최근 소식과 배포 공유를 확인하세요.</p>
        </div>
        <div class="inline-flex rounded-full bg-white p-1 ring-1 ring-border">
          <button
            v-for="f in FILTERS"
            :key="f.key"
            type="button"
            :class="[
              'rounded-full px-4 py-1.5 text-xs font-semibold transition-colors',
              activeFilter === f.key
                ? 'bg-primary text-white shadow-sm'
                : 'text-ink-500 hover:text-ink-900',
            ]"
            @click="activeFilter = f.key"
          >
            {{ f.label }}
          </button>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-36 w-full rounded-lg" />
        <BaseSkeleton class="h-36 w-full rounded-lg" />
        <BaseSkeleton class="h-36 w-full rounded-lg" />
      </div>

      <!-- 에러 -->
      <div
        v-else-if="isError"
        class="flex flex-col items-center justify-center rounded-xl border border-danger/30 bg-danger-light px-4 py-10 text-center"
      >
        <AppIcon name="alert" class="mb-2 h-7 w-7 text-danger" />
        <p class="text-sm font-semibold text-danger">피드를 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
      </div>

      <!-- 피드 목록 -->
      <div v-else-if="filteredPosts.length" class="flex flex-col gap-4">
        <FeedCard v-for="post in filteredPosts" :key="post.id" :post="post" />
      </div>

      <!-- 풍성한 빈 상태 -->
      <div v-else class="overflow-hidden rounded-2xl border border-border bg-surface shadow-card">
        <div class="border-b border-border bg-primary-light/50 px-6 py-14 text-center">
          <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-primary shadow-sm ring-1 ring-primary/10">
            <AppIcon name="pencil" class="h-6 w-6" />
          </div>
          <p class="text-base font-bold tracking-tight text-ink-900">{{ emptyMessage }}</p>
          <p class="mt-1 text-sm text-ink-500">첫 번째 글을 남겨 팀 피드를 채워보세요.</p>

          <div class="mt-6 flex flex-wrap items-center justify-center gap-2.5">
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-primary-hover"
              @click="goWrite('deploy')"
            >
              <AppIcon name="rocket" class="h-4 w-4" />
              배포 공유 작성
            </button>
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-primary ring-1 ring-primary/25 transition hover:bg-primary-light"
              @click="goWrite('free')"
            >
              <AppIcon name="pencil" class="h-4 w-4" />
              자유글 작성
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 divide-y divide-border sm:grid-cols-3 sm:divide-x sm:divide-y-0">
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
