<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import BaseBadge from "../components/common/BaseBadge.vue";
import BaseTextarea from "../components/common/BaseTextarea.vue";
import AppIcon from "../components/common/AppIcon.vue";
import StatusBadge from "../components/manage/StatusBadge.vue";
import { usePost } from "../composables/usePost";
import { useDeletePost } from "../composables/useDeletePost";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useComments, useCreateComment, useDeleteComment } from "../composables/useComments";
import { useToggleLike } from "../composables/useToggleLike";
import { useAuthStore } from "../stores/useAuthStore";
import { useToastStore } from "../stores/useToastStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();

const postId = computed(() => route.params.id);
const { data: post, isLoading, isError, error } = usePost(postId);
const { mutate: deletePost, isPending: isDeleting } = useDeletePost();
const { data: user } = useCurrentUser();

// 본인 글 여부 (로그인 + 작성자 일치)
const isOwner = computed(() => Boolean(user.value && post.value && post.value.author === user.value.name));

// 댓글
const { data: comments, isLoading: isCommentsLoading } = useComments(postId);
const { mutate: createComment, isPending: isCommenting } = useCreateComment(postId);
const { mutate: removeComment } = useDeleteComment(postId);
const commentText = ref("");

// 좋아요
const { mutate: toggleLike, isPending: isLiking } = useToggleLike();
function handleToggleLike() {
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: route.fullPath } });
    return;
  }
  toggleLike(postId.value);
}

function canDeleteComment(comment) {
  return Boolean(user.value && comment.author === user.value.name);
}

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
  if (!isOwner.value) return;
  if (!window.confirm("게시글을 삭제하시겠습니까?")) return;

  deletePost(postId.value, {
    onSuccess: () => {
      toastStore.showToast("게시글이 삭제되었습니다.", "success");
      router.push("/");
    },
  });
}

function handleAddComment() {
  const content = commentText.value.trim();
  if (!content) return;
  createComment(
    { content },
    {
      onSuccess: () => {
        commentText.value = "";
        toastStore.showToast("댓글이 등록되었습니다.", "success");
      },
      onError: (err) => toastStore.showToast(err.message || "댓글 등록에 실패했습니다.", "error"),
    }
  );
}

function handleDeleteComment(comment) {
  if (!canDeleteComment(comment)) return;
  if (!window.confirm("댓글을 삭제하시겠습니까?")) return;
  removeComment(comment.id, {
    onSuccess: () => toastStore.showToast("댓글이 삭제되었습니다.", "success"),
  });
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 상단 네비게이션 -->
      <div class="mb-6 flex items-center justify-between border-b border-border/80 pb-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="text-xs font-bold uppercase tracking-wider text-primary">Community</span>
            <span class="rounded bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">Post Detail</span>
          </div>
          <h1 class="mt-1 text-xl font-bold text-ink-900">게시글 상세</h1>
        </div>
        <BaseButton variant="ghost" class="text-xs sm:text-sm" @click="router.back()">
          ← 뒤로가기
        </BaseButton>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="isLoading" class="space-y-4 rounded-lg border border-border bg-surface p-6 shadow-card">
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
        <AppIcon name="alert" class="mb-2 h-8 w-8 text-danger" />
        <p class="text-base font-semibold text-danger">게시글을 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
        <BaseButton class="mt-4" variant="secondary" @click="router.push('/')">
          메인으로 이동
        </BaseButton>
      </div>

      <!-- 게시글 상세 -->
      <template v-else-if="post">
        <BaseCard class="rounded-lg border border-border bg-surface p-6 shadow-card sm:p-8">
          <!-- 헤더 -->
          <div class="border-b border-border pb-5">
            <BaseBadge :tone="post.type === 'deploy' ? 'info' : 'neutral'" class="mb-2">
              <span
                class="mr-1.5 inline-block h-1.5 w-1.5 rounded-full"
                :class="post.type === 'deploy' ? 'bg-primary' : 'bg-ink-300'"
              />
              {{ post.type === "deploy" ? "배포 공유" : "자유" }}
            </BaseBadge>
            <h1 class="text-xl font-bold leading-snug text-ink-900 sm:text-2xl">
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

          <!-- 배포 정보 (deploy 타입만) -->
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
              class="mt-2 inline-flex items-center gap-1 truncate text-xs text-primary hover:underline"
            >
              <AppIcon name="link" class="h-3.5 w-3.5 shrink-0" />
              <span class="truncate">{{ post.deployment.url }}</span>
            </a>
          </div>

          <!-- 본문 -->
          <div class="min-h-[180px] whitespace-pre-wrap py-8 text-sm leading-relaxed text-ink-700 sm:text-base">
            {{ post.content }}
          </div>

          <!-- 좋아요 -->
          <div class="flex justify-center pb-6">
            <button
              type="button"
              :disabled="isLiking"
              class="inline-flex items-center gap-2 rounded-full border px-5 py-2 text-sm font-semibold transition-colors disabled:opacity-60"
              :class="post.liked
                ? 'border-danger/40 bg-danger-light text-danger'
                : 'border-border bg-surface text-ink-700 hover:border-danger/40 hover:text-danger'"
              :aria-pressed="post.liked"
              @click="handleToggleLike"
            >
              <AppIcon name="heart" :filled="post.liked" class="h-4 w-4" />
              좋아요
              <span class="tabular-nums">{{ post.likes ?? 0 }}</span>
            </button>
          </div>

          <!-- 하단: 목록 + (본인 글만) 수정/삭제 -->
          <div class="flex flex-col-reverse gap-3 border-t border-border pt-6 sm:flex-row sm:items-center sm:justify-between">
            <BaseButton variant="ghost" @click="router.push('/')">
              ← 목록으로
            </BaseButton>
            <div v-if="isOwner" class="flex items-center gap-2">
              <BaseButton variant="secondary" @click="router.push(`/board/${postId}/edit`)">
                수정
              </BaseButton>
              <BaseButton variant="danger" :loading="isDeleting" :disabled="isDeleting" @click="handleDelete">
                삭제
              </BaseButton>
            </div>
          </div>
        </BaseCard>

        <!-- 댓글 -->
        <BaseCard class="mt-6 rounded-lg border border-border bg-surface p-6 shadow-card sm:p-8">
          <h2 class="mb-4 flex items-center gap-2 text-base font-bold text-ink-900">
            <AppIcon name="comment" class="h-4 w-4 text-primary" />
            댓글
            <span class="text-primary">{{ comments?.length ?? 0 }}</span>
          </h2>

          <!-- 작성 폼 (로그인 시) -->
          <div v-if="authStore.isAuthenticated" class="mb-6">
            <BaseTextarea
              id="comment"
              v-model="commentText"
              rows="3"
              placeholder="댓글을 입력하세요"
            />
            <div class="mt-2 flex justify-end">
              <BaseButton
                :loading="isCommenting"
                :disabled="isCommenting || !commentText.trim()"
                @click="handleAddComment"
              >
                댓글 등록
              </BaseButton>
            </div>
          </div>
          <div
            v-else
            class="mb-6 flex items-center justify-between rounded-md border border-border bg-background px-4 py-3 text-sm text-ink-500"
          >
            <span>댓글을 작성하려면 로그인이 필요합니다.</span>
            <BaseButton
              variant="secondary"
              @click="router.push({ path: '/login', query: { from: route.fullPath } })"
            >
              로그인
            </BaseButton>
          </div>

          <!-- 목록 -->
          <div v-if="isCommentsLoading" class="space-y-3">
            <BaseSkeleton class="h-14 w-full rounded-md" />
            <BaseSkeleton class="h-14 w-full rounded-md" />
          </div>
          <ul v-else-if="comments && comments.length" class="flex flex-col divide-y divide-border">
            <li v-for="c in comments" :key="c.id" class="flex gap-3 py-4">
              <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-light text-xs font-bold text-primary">
                {{ (c.author ?? "?").slice(0, 1) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-2">
                  <span class="text-sm font-semibold text-ink-900">
                    {{ c.author }}<span v-if="c.team" class="ml-1 text-xs font-normal text-ink-300">· {{ c.team }}</span>
                  </span>
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-ink-500">{{ formatDate(c.createdAt) }}</span>
                    <button
                      v-if="canDeleteComment(c)"
                      type="button"
                      class="text-ink-300 transition-colors hover:text-danger"
                      aria-label="댓글 삭제"
                      @click="handleDeleteComment(c)"
                    >
                      <AppIcon name="x" class="h-4 w-4" />
                    </button>
                  </div>
                </div>
                <p class="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-ink-700">{{ c.content }}</p>
              </div>
            </li>
          </ul>
          <p v-else class="py-8 text-center text-sm text-ink-500">
            아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
          </p>
        </BaseCard>
      </template>
    </main>
  </div>
</template>
