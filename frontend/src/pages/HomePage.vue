<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import FeedCard from "../components/home/FeedCard.vue";
import { usePosts } from "../composables/usePosts";
import { useCurrentUser } from "../composables/useCurrentUser";

const router = useRouter();
const { data: posts, isLoading, isError, error } = usePosts();
const { data: user } = useCurrentUser();

const FILTERS = [
  { key: "all", label: "전체" },
  { key: "deploy", label: "🚀 배포 공유" },
  { key: "free", label: "💬 자유" },
];
const activeFilter = ref("all");

const filteredPosts = computed(() => {
  const list = posts.value ?? [];
  if (activeFilter.value === "all") return list;
  return list.filter((p) => (p.type ?? "free") === activeFilter.value);
});
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />

    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">
      <!-- 인사 배너 -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-ink-900">
          <template v-if="user">{{ user.name }}님, 안녕하세요 👋</template>
          <template v-else>DeployHub 피드</template>
        </h1>
        <p class="mt-1 text-sm text-ink-500">
          팀원들이 배포한 서비스를 한눈에 보고, 자유롭게 공유하고 소통하는 공간입니다.
        </p>
      </div>

      <!-- 필터 + 글쓰기 -->
      <div class="mb-5 flex items-center justify-between gap-3 border-b border-border/80 pb-4">
        <div class="flex items-center gap-1.5">
          <button
            v-for="f in FILTERS"
            :key="f.key"
            type="button"
            :class="[
              'rounded-full px-3 py-1.5 text-xs font-semibold transition-colors',
              activeFilter === f.key
                ? 'bg-primary text-white'
                : 'bg-black/5 text-ink-700 hover:bg-black/10',
            ]"
            @click="activeFilter = f.key"
          >
            {{ f.label }}
          </button>
        </div>
        <BaseButton class="shadow-card" @click="router.push('/board/new')">
          ✏️ 글쓰기
        </BaseButton>
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
        class="flex flex-col items-center justify-center rounded-lg border border-danger/30 bg-danger-light px-4 py-10 text-center"
      >
        <div class="mb-2 text-2xl">⚠️</div>
        <p class="text-sm font-semibold text-danger">피드를 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
      </div>

      <!-- 피드 목록 -->
      <div v-else-if="filteredPosts.length" class="flex flex-col gap-4">
        <FeedCard v-for="post in filteredPosts" :key="post.id" :post="post" />
      </div>

      <!-- 빈 상태 -->
      <div
        v-else
        class="flex flex-col items-center justify-center rounded-lg border border-dashed border-border bg-surface px-4 py-16 text-center"
      >
        <div class="mb-2 text-3xl">🗒️</div>
        <p class="text-sm text-ink-700">아직 게시물이 없습니다.</p>
        <BaseButton class="mt-4" @click="router.push('/board/new')">첫 글 작성하기</BaseButton>
      </div>
    </main>
  </div>
</template>
