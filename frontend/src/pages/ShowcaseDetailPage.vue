<script setup>
import { useRoute, useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import { usePublicShowcase } from "../composables/useShowcase";

const route = useRoute();
const router = useRouter();

const { data: item, isLoading, isError, error } = usePublicShowcase(() => route.params.id);

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
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />

    <main class="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-10">
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="router.back()">
        ← 목록으로
      </BaseButton>

      <!-- 로딩 -->
      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-8 w-2/3 rounded" />
        <BaseSkeleton class="h-24 w-full rounded-lg" />
      </div>

      <!-- 에러 -->
      <div
        v-else-if="isError"
        class="flex flex-col items-center justify-center rounded-xl border border-danger/30 bg-danger-light px-4 py-10 text-center"
      >
        <AppIcon name="alert" class="mb-2 h-7 w-7 text-danger" />
        <p class="text-sm font-semibold text-danger">서비스 정보를 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
      </div>

      <!-- 상세 -->
      <BaseCard v-else-if="item" class="p-6 sm:p-8">
        <div class="flex flex-wrap items-center gap-2">
          <span class="rounded-full bg-primary-light px-2.5 py-1 text-xs font-medium text-primary">
            {{ item.teamName }}
          </span>
          <span class="text-xs text-ink-400">업데이트 {{ formatDate(item.updatedAt) }}</span>
        </div>

        <h1 class="mt-3 font-mono text-2xl font-bold text-ink-900">{{ item.projectName }}</h1>

        <p class="mt-4 whitespace-pre-wrap text-sm leading-relaxed text-ink-700">
          {{ item.description || "등록된 설명이 없습니다." }}
        </p>

        <!-- 라이브 미리보기: 배포된 서비스를 iframe으로 그대로 임베드해 "들어가면 나오는 화면"을 보여준다.
             일부 앱은 X-Frame-Options/CSP로 임베드를 막을 수 있어(빈 화면), 새 탭 링크를 함께 둔다. -->
        <div v-if="item.endpointUrl" class="mt-6">
          <div class="mb-2 flex items-center justify-between">
            <span class="text-xs font-semibold text-ink-500">라이브 미리보기</span>
            <a
              :href="item.endpointUrl"
              target="_blank"
              rel="noreferrer"
              class="text-xs font-medium text-primary hover:underline"
            >
              새 탭에서 열기 ↗
            </a>
          </div>
          <div class="overflow-hidden rounded-xl border border-ink-900/10 shadow-sm">
            <!-- 브라우저 크롬 흉내 -->
            <div class="flex items-center gap-1.5 border-b border-ink-900/10 bg-ink-900/5 px-3 py-2">
              <span class="h-2.5 w-2.5 rounded-full bg-[#ff5f57]"></span>
              <span class="h-2.5 w-2.5 rounded-full bg-[#febc2e]"></span>
              <span class="h-2.5 w-2.5 rounded-full bg-[#28c840]"></span>
              <span class="ml-2 truncate font-mono text-[11px] text-ink-400">{{ item.endpointUrl }}</span>
            </div>
            <iframe
              :src="item.endpointUrl"
              class="h-[70vh] w-full bg-white"
              loading="lazy"
              referrerpolicy="no-referrer"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals"
              title="서비스 미리보기"
            ></iframe>
          </div>
          <p class="mt-2 text-xs text-ink-400">
            화면이 보이지 않으면 해당 서비스가 임베드를 차단한 경우입니다. '새 탭에서 열기'로 확인하세요.
          </p>
        </div>

        <a
          v-if="item.endpointUrl"
          :href="item.endpointUrl"
          target="_blank"
          rel="noreferrer"
          class="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-primary-hover"
        >
          <AppIcon name="link" class="h-4 w-4" />
          서비스 방문하기
        </a>
      </BaseCard>
    </main>
  </div>
</template>
