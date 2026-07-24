<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import BaseCard from "../components/common/BaseCard.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import BaseBadge from "../components/common/BaseBadge.vue";
import StatusBadge from "../components/manage/StatusBadge.vue";
import { usePost } from "../composables/usePost";
import { useDeletePost } from "../composables/useDeletePost";
import { useToastStore } from "../stores/useToastStore";

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();

const postId = computed(() => route.params.id);
const { data: post, isLoading, isError, error } = usePost(postId);
const { mutate: deletePost, isPending: isDeleting } = useDeletePost();

function formatDate(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

function handleDelete() {
  if (!window.confirm("게시글을 삭제하시겠습니까?")) return;

  deletePost(postId.value, {
    onSuccess: () => {
      toastStore.showToast("게시글이 삭제되었습니다.", "success");
      router.push("/home");
    },
  });
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <main class="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-10">
      
      <!-- 상단 서브 헤더 / 네비게이션 -->
      <div class="mb-6 flex items-center justify-between border-b border-border/80 pb-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold uppercase tracking-wider text-primary">Community</span>
            <span class="rounded bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">Post Detail</span>
          </div>
          <h1 class="mt-1 text-xl font-bold text-ink-900">게시글 상세 정보</h1>
        </div>
        <BaseButton variant="ghost" class="text-xs sm:text-sm" @click="router.push('/home')">
          ← 목록으로 돌아가기
        </BaseButton>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="isLoading" class="rounded-lg border border-border bg-surface p-6 shadow-card space-y-4">
        <BaseSkeleton class="h-8 w-1/2 rounded-md" />
        <BaseSkeleton class="h-4 w-1/4 rounded-md" />
        <div class="space-y-2 pt-6">
          <BaseSkeleton class="h-4 w-full rounded-md" />
          <BaseSkeleton class="h-4 w-full rounded-md" />
          <BaseSkeleton class="h-4 w-3/4 rounded-md" />
        </div>
      </div>

      <!-- 에러 상태 -->
      <div
        v-else-if="isError"
        class="flex flex-col items-center justify-center rounded-lg border border-danger/30 bg-danger-light px-6 py-10 text-center shadow-card"
      >
        <div class="mb-2 text-3xl">⚠️</div>
        <p class="text-base font-semibold text-danger">게시글을 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
        <BaseButton class="mt-4" variant="secondary" @click="router.push('/home')">
          목록으로 이동
        </BaseButton>
      </div>

      <!-- 게시글 상세 카드 -->
      <template v-else-if="post">
        <BaseCard class="p-6 sm:p-8 shadow-card border border-border bg-surface rounded-lg">
          
          <!-- 게시글 헤더 영역 -->
          <div class="border-b border-border pb-5">
            <BaseBadge :tone="post.type === 'deploy' ? 'info' : 'neutral'" class="mb-2">
              {{ post.type === "deploy" ? "🚀 배포 공유" : "💬 자유" }}
            </BaseBadge>
            <h1 class="text-xl font-bold text-ink-900 sm:text-2xl leading-snug">
              {{ post.title }}
            </h1>
            <div class="mt-3 flex flex-wrap items-center gap-4 text-xs text-ink-500">
              <span class="inline-flex items-center gap-1.5">
                <span class="font-medium text-ink-700">작성일</span>
                <span>{{ formatDate(post.createdAt) }}</span>
              </span>
              <span v-if="post.author" class="inline-flex items-center gap-1.5">
                <span class="text-border">|</span>
                <span class="font-medium text-ink-700">작성자</span>
                <span>{{ post.author }}<template v-if="post.team"> · {{ post.team }}</template></span>
              </span>
            </div>
          </div>

          <!-- 배포 서비스 정보 (deploy 타입만) -->
          <div
            v-if="post.type === 'deploy' && post.deployment"
            class="mt-5 rounded-md border border-border bg-background p-4"
          >
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-mono text-sm font-semibold text-ink-900">
                {{ post.deployment.projectName }}
              </span>
              <StatusBadge kind="deploy" :status="post.deployment.deployStatus" />
            </div>
            <a
              v-if="post.deployment.url"
              :href="post.deployment.url"
              target="_blank"
              rel="noreferrer"
              class="mt-2 block truncate text-xs text-primary hover:underline"
            >
              🔗 {{ post.deployment.url }}
            </a>
          </div>

          <!-- 게시글 본문 영역 -->
          <div class="min-h-[220px] py-8 text-sm sm:text-base leading-relaxed text-ink-700 whitespace-pre-wrap">
            {{ post.content }}
          </div>

          <!-- 하단 컨트롤 영역 -->
          <div class="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between border-t border-border pt-6">
            <BaseButton variant="ghost" @click="router.push('/home')">
              ← 목록으로
            </BaseButton>
            <div class="flex items-center gap-2">
              <BaseButton variant="secondary" @click="router.push(`/board/${postId}/edit`)">
                수정
              </BaseButton>
              <BaseButton variant="danger" :loading="isDeleting" :disabled="isDeleting" @click="handleDelete">
                삭제
              </BaseButton>
            </div>
          </div>

        </BaseCard>
      </template>

    </main>
  </div>
</template>