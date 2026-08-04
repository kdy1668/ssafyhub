<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import { usePublicShowcases } from "../composables/useShowcase";

const router = useRouter();
const page = ref(0);
const size = 12;

const { data, isLoading, isError, error } = usePublicShowcases(page, size);

const items = computed(() => data.value?.deployments ?? []);
const totalElements = computed(() => data.value?.totalElements ?? 0);
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / size)));
const hasPrev = computed(() => page.value > 0);
const hasNext = computed(() => page.value + 1 < totalPages.value);

function formatDate(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(iso));
}

function goDetail(id) {
  router.push(`/showcase/${id}`);
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />

    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 sm:py-10">
      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <span class="text-xs font-bold uppercase tracking-wider text-primary">Showcase</span>
        <h1 class="mt-1 text-2xl font-bold text-ink-900">공개 서비스 쇼케이스</h1>
        <p class="text-xs text-ink-500">팀원들이 공개로 설정한 배포 서비스를 둘러보세요.</p>
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <BaseSkeleton class="h-32 w-full rounded-lg" />
        <BaseSkeleton class="h-32 w-full rounded-lg" />
        <BaseSkeleton class="h-32 w-full rounded-lg" />
        <BaseSkeleton class="h-32 w-full rounded-lg" />
      </div>

      <!-- 에러 -->
      <div
        v-else-if="isError"
        class="flex flex-col items-center justify-center rounded-xl border border-danger/30 bg-danger-light px-4 py-10 text-center"
      >
        <AppIcon name="alert" class="mb-2 h-7 w-7 text-danger" />
        <p class="text-sm font-semibold text-danger">쇼케이스를 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
      </div>

      <!-- 빈 상태 -->
      <div
        v-else-if="items.length === 0"
        class="flex flex-col items-center justify-center rounded-2xl border border-border bg-surface px-4 py-14 text-center shadow-card"
      >
        <AppIcon name="cube" class="mb-3 h-8 w-8 text-ink-300" />
        <p class="text-sm font-semibold text-ink-900">아직 공개된 서비스가 없어요.</p>
        <p class="mt-1 text-xs text-ink-500">배포 신청 시 공개 범위를 PUBLIC으로 설정하면 여기에 노출됩니다.</p>
      </div>

      <!-- 카드 그리드 -->
      <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <!-- iframe을 품어야 해서 button 대신 role=button div 사용(button 안엔 iframe 불가) -->
        <div
          v-for="item in items"
          :key="item.deploymentId"
          role="button"
          tabindex="0"
          class="group flex cursor-pointer flex-col overflow-hidden rounded-lg border border-border bg-surface text-left shadow-card transition hover:-translate-y-0.5 hover:border-primary/40"
          @click="goDetail(item.deploymentId)"
          @keydown.enter="goDetail(item.deploymentId)"
          @keydown.space.prevent="goDetail(item.deploymentId)"
        >
          <!-- 라이브 미리보기 썸네일: 데스크톱 폭으로 렌더한 뒤 축소(scale)해 미니 화면처럼 보이게.
               pointer-events-none로 클릭은 카드(상세 이동)로 통과. 차단 시 뒤 플레이스홀더가 보임. -->
          <div
            v-if="item.endpointUrl"
            class="relative h-40 w-full overflow-hidden border-b border-ink-900/10 bg-ink-900/5"
          >
            <div class="absolute inset-0 flex items-center justify-center">
              <AppIcon name="cube" class="h-7 w-7 text-ink-300" />
            </div>
            <iframe
              :src="item.endpointUrl"
              class="pointer-events-none absolute left-0 top-0 origin-top-left border-0"
              style="width: 300%; height: 300%; transform: scale(0.3333)"
              loading="lazy"
              scrolling="no"
              tabindex="-1"
              aria-hidden="true"
              referrerpolicy="no-referrer"
              sandbox="allow-scripts allow-same-origin"
              title="서비스 미리보기"
            ></iframe>
          </div>

          <div class="flex flex-col gap-2 p-5">
            <div class="flex items-center justify-between gap-2">
              <span class="font-mono text-sm font-semibold text-ink-900">{{ item.projectName }}</span>
              <span class="shrink-0 rounded-full bg-primary-light px-2 py-0.5 text-[11px] font-medium text-primary">
                {{ item.teamName }}
              </span>
            </div>
            <p class="line-clamp-2 text-xs leading-relaxed text-ink-500">
              {{ item.description || "설명이 없습니다." }}
            </p>
            <p class="mt-1 text-[11px] text-ink-400">업데이트 {{ formatDate(item.updatedAt) }}</p>
          </div>
        </div>
      </div>

      <!-- 페이지네이션 -->
      <div v-if="!isLoading && !isError && totalElements > 0" class="mt-8 flex items-center justify-center gap-3">
        <button
          type="button"
          class="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-ink-700 transition-colors hover:bg-black/5 disabled:cursor-not-allowed disabled:opacity-40"
          :disabled="!hasPrev"
          @click="page -= 1"
        >
          이전
        </button>
        <span class="text-xs text-ink-500">{{ page + 1 }} / {{ totalPages }}</span>
        <button
          type="button"
          class="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-ink-700 transition-colors hover:bg-black/5 disabled:cursor-not-allowed disabled:opacity-40"
          :disabled="!hasNext"
          @click="page += 1"
        >
          다음
        </button>
      </div>
    </main>
  </div>
</template>
