<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import AppIcon from "../components/common/AppIcon.vue";
import ResourceDashboard from "../components/manage/ResourceDashboard.vue";
import { useMyDeployment } from "../composables/useDeployments";

const router = useRouter();

const { data: currentDeployment, isLoading, isError } = useMyDeployment();
const deploymentId = computed(() => currentDeployment.value?.deploymentId);

const isRunningOrSuccess = computed(() => currentDeployment.value?.status === "RUNNING");
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 뒤로가기 -->
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="router.back()">
        ← 뒤로가기
      </BaseButton>

      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <h1 class="mt-1 text-2xl font-bold text-ink-900">리소스 모니터링</h1>
        <p class="text-xs text-ink-500">
          {{ currentDeployment?.projectName || "내 배포" }}의 CPU·메모리·네트워크·디스크·요청 수를
          한 화면에서 확인합니다.
        </p>
      </div>

      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-20 w-full rounded-lg" />
        <BaseSkeleton class="h-40 w-full rounded-lg" />
      </div>

      <div
        v-else-if="isError || !deploymentId"
        class="flex flex-col items-center justify-center rounded-xl border border-danger/30 bg-danger-light px-4 py-10 text-center"
      >
        <AppIcon name="alert" class="mb-2 h-7 w-7 text-danger" />
        <p class="text-sm font-semibold text-danger">조회할 배포를 찾을 수 없습니다.</p>
      </div>

      <div
        v-else-if="!isRunningOrSuccess"
        class="flex flex-col items-center justify-center rounded-xl border border-border bg-surface px-4 py-10 text-center"
      >
        <p class="text-sm text-ink-600">서비스가 실행 중일 때만 리소스 지표를 확인할 수 있습니다.</p>
      </div>

      <ResourceDashboard v-else :deployment-id="String(deploymentId)" />
    </main>
  </div>
</template>
