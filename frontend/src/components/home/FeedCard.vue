<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import BaseBadge from "../common/BaseBadge.vue";
import AppIcon from "../common/AppIcon.vue";
import { useToggleLike } from "../../composables/useToggleLike";
import { useAuthStore } from "../../stores/useAuthStore";

const props = defineProps({
  post: { type: Object, required: true },
});

const router = useRouter();
const authStore = useAuthStore();
const { mutate: toggleLike, isPending: isLiking } = useToggleLike();

const isDeploy = computed(() => props.post.type === "deploy");

// 좋아요 토글 (로그인 필요, 카드 클릭 전파 방지)
function onToggleLike() {
  if (!authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: "/" } });
    return;
  }
  toggleLike(props.post.id);
}

const DEPLOY_STATUS_MAP = {
  DEPLOYED: { label: "배포 완료", tone: "success" },
  DEPLOYING: { label: "배포중", tone: "warning" },
  FAILED: { label: "배포 실패", tone: "danger" },
};

const deployStatus = computed(() => {
  const status = props.post.deployment?.deployStatus;
  return DEPLOY_STATUS_MAP[status] ?? { label: status, tone: "neutral" };
});

function formatDate(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

function goToDetail() {
  if (props.post.id === undefined || props.post.id === null) return;
  router.push(`/board/${props.post.id}`);
}
</script>

<template>
  <article
    class="group cursor-pointer rounded-xl border border-border bg-surface p-5 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-cardHover"
    @click="goToDetail"
  >
    <!-- 상단: 유형 뱃지 + 작성자 -->
    <div class="mb-2 flex items-center justify-between gap-2">
      <BaseBadge :tone="isDeploy ? 'info' : 'neutral'">
        <span
          class="mr-1.5 inline-block h-1.5 w-1.5 rounded-full"
          :class="isDeploy ? 'bg-primary' : 'bg-ink-300'"
        />
        {{ isDeploy ? "배포 공유" : "자유" }}
      </BaseBadge>
      <span class="text-xs text-ink-500">{{ formatDate(post.createdAt) }}</span>
    </div>

    <!-- 제목 -->
    <h3 class="text-base font-bold tracking-tight text-ink-900 transition-colors group-hover:text-primary">
      {{ post.title }}
    </h3>

    <!-- 본문 미리보기 -->
    <p class="mt-1.5 line-clamp-2 text-sm leading-relaxed text-ink-700">
      {{ post.content }}
    </p>

    <!-- 배포 서비스 정보 (deploy 타입만) -->
    <div
      v-if="isDeploy && post.deployment"
      class="mt-3 flex flex-wrap items-center gap-2 rounded-md border border-border bg-background px-3 py-2"
    >
      <span class="font-mono text-xs font-semibold text-ink-900">
        {{ post.deployment.projectName }}
      </span>
      <BaseBadge :tone="deployStatus.tone">{{ deployStatus.label }}</BaseBadge>
      <a
        v-if="post.deployment.url"
        :href="post.deployment.url"
        target="_blank"
        rel="noreferrer"
        class="ml-auto inline-flex items-center gap-1 truncate text-xs text-primary hover:underline"
        @click.stop
      >
        <AppIcon name="link" class="h-3.5 w-3.5 shrink-0" />
        <span class="truncate">{{ post.deployment.url }}</span>
      </a>
    </div>

    <!-- 하단: 작성자 + 소통 지표 -->
    <div class="mt-3 flex items-center justify-between border-t border-border/60 pt-3 text-xs text-ink-500">
      <span class="inline-flex items-center gap-1.5">
        <span
          class="flex h-5 w-5 items-center justify-center rounded-full bg-primary-light text-[10px] font-bold text-primary"
        >
          {{ (post.author ?? "?").slice(0, 1) }}
        </span>
        <span class="font-medium text-ink-700">{{ post.author ?? "익명" }}</span>
        <span v-if="post.team" class="text-ink-300">· {{ post.team }}</span>
      </span>
      <span class="flex items-center gap-3">
        <button
          type="button"
          :disabled="isLiking"
          class="inline-flex items-center gap-1 transition-colors hover:text-danger disabled:opacity-60"
          :class="post.liked ? 'text-danger' : ''"
          :aria-pressed="post.liked"
          aria-label="좋아요"
          @click.stop="onToggleLike"
        >
          <AppIcon name="heart" :filled="post.liked" class="h-3.5 w-3.5" />{{ post.likes ?? 0 }}
        </button>
        <span class="inline-flex items-center gap-1">
          <AppIcon name="comment" class="h-3.5 w-3.5" />{{ post.commentCount ?? 0 }}
        </span>
      </span>
    </div>
  </article>
</template>
