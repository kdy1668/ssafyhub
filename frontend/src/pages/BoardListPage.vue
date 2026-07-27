<script setup>
import { useRouter } from "vue-router";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import PostTable from "../components/board/PostTable.vue";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import { usePosts } from "../composables/usePosts";

const router = useRouter();
const { data: posts, isLoading, isError, error } = usePosts();
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 sm:py-10">
      <!-- 페이지 상단 헤더 영역 -->
      <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-border/80 pb-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold uppercase tracking-wider text-primary">Community</span>
            <span class="rounded bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">Board</span>
          </div>
          <h1 class="mt-1 text-2xl font-bold text-ink-900">자유게시판</h1>
          <p class="mt-0.5 text-xs text-ink-500">팀원들과 자유롭게 의견을 나누고 소통하는 공간입니다.</p>
        </div>
        <div class="flex items-center gap-3">
          <BaseButton class="shadow-card" @click="router.push('/board/new')">
            <AppIcon name="pencil" class="h-4 w-4" />
            새 글 작성
          </BaseButton>
        </div>
      </div>

      <!-- 게시글 컨테이너 카드 -->
      <div class="rounded-lg border border-border bg-surface p-6 shadow-card">
        <!-- 로딩 상태 -->
        <div v-if="isLoading" class="flex flex-col gap-3">
          <BaseSkeleton class="h-12 w-full rounded-md" />
          <BaseSkeleton class="h-12 w-full rounded-md" />
          <BaseSkeleton class="h-12 w-full rounded-md" />
          <BaseSkeleton class="h-12 w-full rounded-md" />
          <BaseSkeleton class="h-12 w-full rounded-md" />
        </div>

        <!-- 에러 상태 -->
        <div
          v-else-if="isError"
          class="flex flex-col items-center justify-center rounded-md border border-danger/30 bg-danger-light px-4 py-8 text-center"
        >
          <AppIcon name="alert" class="mb-2 h-7 w-7 text-danger" />
          <p class="text-sm font-semibold text-danger">게시글을 불러오지 못했습니다.</p>
          <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
        </div>

        <!-- 게시글 테이블 컴포넌트 -->
        <PostTable v-else :posts="posts ?? []" />
      </div>
    </main>
  </div>
</template>