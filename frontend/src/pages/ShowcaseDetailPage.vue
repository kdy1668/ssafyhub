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

    <main class="mx-auto max-w-2xl px-4 py-8 sm:px-6 sm:py-10">
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
