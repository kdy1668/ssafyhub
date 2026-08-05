<script setup>
import { computed, nextTick, ref, watch } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import RuntimeLogDetectionPanel from "../components/manage/RuntimeLogDetectionPanel.vue";
import { useMyDeployment } from "../composables/useDeployments";
import { useRuntimeLogs } from "../composables/useRuntimeLogs";
import { useAiConfig } from "../composables/useAiConfig";
import { useRuntimeLogDetection } from "../composables/useRuntimeLogDetection";

const TAIL_OPTIONS = [100, 300, 1000];

const router = useRouter();

const { data: currentDeployment, isLoading: isDeploymentLoading } = useMyDeployment();
const deploymentId = computed(() => currentDeployment.value?.deploymentId);
const isRunningOrSuccess = computed(() => currentDeployment.value?.status === "RUNNING");

const tail = ref(300);
const autoScroll = ref(true);
const logBoxRef = ref(null);

const { data: logs, isLoading: isLogsLoading, isFetching, refetch } = useRuntimeLogs(
  deploymentId,
  isRunningOrSuccess,
  tail
);

// AI 이상 감지. 위 5초 폴링과 완전히 분리되어 버튼 클릭으로만 실행된다.
const { aiEnabled, cooldownSeconds } = useAiConfig();
const {
  mutate: detectAnomalies,
  data: detection,
  isPending: isDetecting,
  error: detectionError,
  cooldownRemaining,
} = useRuntimeLogDetection(deploymentId, cooldownSeconds);

watch(logs, async () => {
  if (!autoScroll.value || !logBoxRef.value) return;
  await nextTick();
  logBoxRef.value.scrollTop = logBoxRef.value.scrollHeight;
});

function handleScroll() {
  const el = logBoxRef.value;
  if (!el) return;
  // 사용자가 위로 스크롤해서 과거 로그를 보고 있으면 자동 스크롤을 멈춘다.
  autoScroll.value = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
}

const isLoading = computed(() => isDeploymentLoading.value);
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 뒤로가기 -->
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="router.back()">
        ← 뒤로가기
      </BaseButton>

      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <h1 class="mt-1 text-2xl font-bold text-ink-900">백엔드 런타임 로그</h1>
        <p class="text-xs text-ink-500">
          {{ currentDeployment?.projectName || "내 배포" }} 컨테이너의 표준 출력(stdout/stderr)을
          5초마다 새로 불러옵니다. 실시간 스트리밍은 아니라 아주 최근 로그는 약간 늦게 보일 수 있습니다.
        </p>
      </div>

      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-96 w-full rounded-lg" />
      </div>

      <div
        v-else-if="!isRunningOrSuccess"
        class="flex flex-col items-center justify-center rounded-xl border border-border bg-surface px-4 py-10 text-center"
      >
        <p class="text-sm text-ink-600">서비스가 실행 중일 때만 런타임 로그를 확인할 수 있습니다.</p>
      </div>

      <div v-else class="flex flex-col gap-3">
        <!-- AI 이상 감지 (버튼 클릭 시에만 호출 - 위 로그 폴링과 무관) -->
        <RuntimeLogDetectionPanel
          v-if="aiEnabled"
          :detection="detection"
          :loading="isDetecting"
          :error="detectionError"
          :cooldown-remaining="cooldownRemaining"
          @detect="detectAnomalies()"
        />

        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5">
            <button
              v-for="opt in TAIL_OPTIONS"
              :key="opt"
              @click="tail = opt"
              class="rounded-md border px-2.5 py-1 text-xs font-medium transition-colors"
              :class="tail === opt
                ? 'border-primary bg-primary-light text-primary'
                : 'border-border bg-surface text-ink-600 hover:bg-black/5'"
            >
              최근 {{ opt }}줄
            </button>
          </div>
          <button
            @click="refetch()"
            class="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-3 py-1 text-xs font-medium text-ink-700 hover:bg-black/5"
          >
            새로고침
          </button>
        </div>

        <div
          ref="logBoxRef"
          @scroll="handleScroll"
          class="h-[32rem] overflow-y-auto rounded-lg border border-border bg-ink-900 p-4 font-mono text-xs leading-relaxed text-white"
        >
          <BaseSkeleton v-if="isLogsLoading && !logs" class="h-full w-full bg-white/10" />
          <pre v-else class="whitespace-pre-wrap break-all">{{ logs || "(아직 출력된 로그가 없습니다)" }}</pre>
        </div>

        <div class="flex items-center justify-between text-xs text-ink-500">
          <span v-if="!autoScroll">위로 스크롤 중 - 자동 스크롤이 꺼졌습니다. 아래로 내리면 다시 켜집니다.</span>
          <span v-else />
          <span class="flex items-center gap-1">
            <span class="h-1.5 w-1.5 rounded-full" :class="isFetching ? 'bg-accent animate-pulse' : 'bg-success'" />
            5초마다 자동 갱신
          </span>
        </div>
      </div>
    </main>
  </div>
</template>
