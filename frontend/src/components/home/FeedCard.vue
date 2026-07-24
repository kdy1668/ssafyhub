<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import BaseBadge from "../common/BaseBadge.vue";

const props = defineProps({
  post: { type: Object, required: true },
});

const router = useRouter();

const isDeploy = computed(() => props.post.type === "deploy");

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
    class="group cursor-pointer rounded-lg border border-border bg-surface p-5 shadow-card transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-md"
    @click="goToDetail"
  >
    <!-- 상단: 유형 뱃지 + 작성자 -->
    <div class="mb-2 flex items-center justify-between gap-2">
      <BaseBadge :tone="isDeploy ? 'info' : 'neutral'">
        {{ isDeploy ? "🚀 배포 공유" : "💬 자유" }}
      </BaseBadge>
      <span class="text-xs text-ink-500">{{ formatDate(post.createdAt) }}</span>
    </div>

    <!-- 제목 -->
    <h3 class="text-base font-bold text-ink-900 transition-colors group-hover:text-primary">
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
        class="ml-auto truncate text-xs text-primary hover:underline"
        @click.stop
      >
        🔗 {{ post.deployment.url }}
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
        <span>♥ {{ post.likes ?? 0 }}</span>
        <span>💬 {{ post.commentCount ?? 0 }}</span>
      </span>
    </div>
  </article>
</template>
