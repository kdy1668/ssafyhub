<script setup>
import { computed, ref, reactive } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import BaseCard from "../components/common/BaseCard.vue";
import AppIcon from "../components/common/AppIcon.vue";
import ConfirmModal from "../components/common/ConfirmModal.vue";
import ResourceModal from "../components/manage/ResourceModal.vue";
import ControlButtons from "../components/manage/ControlButtons.vue";
import LogViewer from "../components/manage/LogViewer.vue";
import { useMyDeployment } from "../composables/useDeployments";
import { useDeploymentLogs } from "../composables/useDeploymentLogs";
import { deleteDeployment } from "../api/deployments";
import { useQueryClient } from "@tanstack/vue-query";
import { useToastStore } from "../stores/useToastStore";

const router = useRouter();
const toastStore = useToastStore();
const queryClient = useQueryClient();

// 내 배포 상태 1건 조회 (3초 폴링, 사용자당 배포는 1개)
const { data: currentDeployment, isLoading, isError, error } = useMyDeployment();

// 배포 진행 로그 (깃헙 액션식). deploymentId 있으면 3초 폴링으로 실시간 갱신.
const deploymentId = computed(() => currentDeployment.value?.deploymentId);
const {
  data: deploymentLogs,
  isFetching: isLogsFetching,
  refetch: refetchLogs,
} = useDeploymentLogs(deploymentId, computed(() => Boolean(deploymentId.value)));

// 아직 배포한 적이 없는 사용자(404)는 에러가 아니라 "배포 없음" 빈 상태로 취급
const hasNoDeployment = computed(() => isError.value && error.value?.status === 404);
const hasLoadError = computed(() => isError.value && error.value?.status !== 404);

// 리소스 모달 열림 상태 관리
const isResourceModalOpen = ref(false);

// --- 서버 제어 + 확인 모달 ---
const CONTROL_META = {
  start: { title: "서버 시작", message: "서버를 시작하시겠습니까?", confirmText: "시작", confirmVariant: "primary" },
  stop: { title: "서버 중지", message: "서버를 중지하시겠습니까? 서비스 접속이 중단됩니다.", confirmText: "중지", confirmVariant: "danger" },
  restart: { title: "서버 재시작", message: "서버를 재시작하시겠습니까? 잠시 서비스가 중단될 수 있습니다.", confirmText: "재시작", confirmVariant: "danger" },
  delete: {
    title: "배포 삭제",
    message: "배포를 삭제하시겠습니까? 실행 중인 서버가 즉시 내려가고 배포 이력까지 모두 삭제됩니다. 되돌릴 수 없습니다.",
    confirmText: "삭제",
    confirmVariant: "danger",
  },
};

const confirmState = reactive({
  open: false,
  action: null,
});

// 진행 중인 액션 (버튼 로딩 표시용)
const loadingAction = ref("");

const confirmMeta = computed(() => CONTROL_META[confirmState.action] || {});

// 제어 버튼 클릭 → 확인 모달 오픈
function requestControl(action) {
  confirmState.action = action;
  confirmState.open = true;
}

function cancelControl() {
  confirmState.open = false;
  confirmState.action = null;
}

// 확인 후 실제 동작. 삭제만 백엔드와 연동되어 있고, 시작/중지/재시작은 아직 연동 예정.
async function confirmControl() {
  const labels = { start: "시작", stop: "중지", restart: "재시작" };
  const action = confirmState.action;
  cancelControl();

  if (action !== "delete") {
    toastStore.showToast(`${labels[action] || action} 기능은 백엔드 연동 예정입니다.`, "info");
    return;
  }

  const deploymentId = currentDeployment.value?.deploymentId;
  if (!deploymentId) {
    toastStore.showToast("삭제할 배포를 찾을 수 없습니다.", "error");
    return;
  }

  loadingAction.value = "delete";
  try {
    await deleteDeployment(deploymentId);
    // 삭제 후 폴링 쿼리를 무효화해 "배포 없음" 빈 상태로 전환한다.
    await queryClient.invalidateQueries({ queryKey: ["deployments", "me"] });
    toastStore.showToast("배포가 삭제되었습니다.", "success");
  } catch (error) {
    toastStore.showToast(error.message || "배포 삭제에 실패했습니다.", "error");
  } finally {
    loadingAction.value = "";
  }
}

// 로그 수동 새로고침 (평소엔 3초 폴링으로 자동 갱신됨)
function handleRefreshLogs() {
  refetchLogs();
}

// 상태에 따른 UI(색상, 애니메이션, 아이콘) 처리
const statusConfig = computed(() => {
  if (!currentDeployment.value) return {};

  const status = currentDeployment.value.status;

  switch (status) {
    case "RUNNING":
      return { label: "실행 중", class: "bg-success-light text-success", dot: "bg-success" };
    case "FAILED":
      return { label: "배포 실패", class: "bg-danger-light text-danger", dot: "bg-danger" };
    case "PENDING":
      return { label: "대기 중", class: "bg-accent-light text-accent animate-pulse", dot: "bg-accent" };
    case "BUILDING":
      return { label: "빌드 중", class: "bg-primary-light text-primary animate-pulse", dot: "bg-primary" };
    case "DEPLOYING":
      return { label: "배포 중", class: "bg-primary-light text-primary animate-pulse", dot: "bg-primary" };
    default:
      return { label: status, class: "bg-black/5 text-ink-700", dot: "bg-ink-300" };
  }
});

// 현재 배포가 성공(실행 중) 상태인지 판별
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
        <h1 class="mt-1 text-2xl font-bold text-ink-900">내 배포 상태</h1>
        <p class="text-xs text-ink-500">
          현재 진행 중이거나 완료된 프로젝트의 배포 상태를 단일 화면에서 확인합니다.
        </p>
      </div>

      <!-- 로딩 중일 때 기존 BaseSkeleton 활용 -->
      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-20 w-full rounded-lg" />
        <BaseSkeleton class="h-40 w-full rounded-lg" />
      </div>

      <!-- 배포 내역이 없을 때 (404) -->
      <div v-else-if="hasNoDeployment" class="flex flex-col items-center justify-center py-20">
        <p class="text-ink-600 mb-4">현재 등록된 배포 프로젝트가 없습니다.</p>
        <router-link to="/deploy/new" class="rounded-md bg-primary px-4 py-2 text-white hover:bg-primary-hover">
          새 프로젝트 배포하기
        </router-link>
      </div>

      <!-- 조회 중 오류 -->
      <div
        v-else-if="hasLoadError"
        class="flex flex-col items-center justify-center rounded-xl border border-danger/30 bg-danger-light px-4 py-10 text-center"
      >
        <AppIcon name="alert" class="mb-2 h-7 w-7 text-danger" />
        <p class="text-sm font-semibold text-danger">배포 상태를 불러오지 못했습니다.</p>
        <p class="mt-1 text-xs text-ink-500">{{ error?.message }}</p>
      </div>

      <!-- 단일 배포 상태 화면 -->
      <BaseCard v-else-if="currentDeployment" class="p-6 sm:p-8 shadow-card border border-border bg-surface rounded-lg">

        <div class="flex items-center justify-between mb-6 border-b border-border pb-4">
          <h2 class="text-xl font-bold text-ink-900">
            {{ currentDeployment.projectName }}
          </h2>
          <div class="flex items-center gap-3">
            <span
              class="px-3 py-1 rounded-full text-sm font-semibold flex items-center gap-1.5"
              :class="statusConfig.class"
            >
              <span class="h-1.5 w-1.5 rounded-full" :class="statusConfig.dot" />
              {{ currentDeployment.displayStatus || statusConfig.label }}
            </span>

            <!-- 성공/실행 중 상태일 때만 리소스 보기 버튼 노출 -->
            <button
              v-if="isRunningOrSuccess"
              @click="isResourceModalOpen = true"
              class="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface px-3 py-1 text-sm font-medium text-ink-700 hover:bg-black/5"
            >
              <AppIcon name="chart" class="h-4 w-4" />
              리소스
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-6">
          <div>
            <span class="block text-xs font-medium text-ink-500 mb-1">공개 범위</span>
            <span class="text-sm font-semibold text-ink-900">{{ currentDeployment.visibility }}</span>
          </div>
          <div v-if="currentDeployment.endpointUrl">
            <span class="block text-xs font-medium text-ink-500 mb-1">접속 URL</span>
            <a
              :href="currentDeployment.endpointUrl"
              target="_blank"
              rel="noreferrer"
              class="truncate text-sm font-semibold text-primary hover:underline"
            >
              {{ currentDeployment.endpointUrl }}
            </a>
          </div>
          <div class="sm:col-span-2">
            <span class="block text-xs font-medium text-ink-500 mb-1">설명</span>
            <p class="text-sm text-ink-900">{{ currentDeployment.description || "등록된 설명이 없습니다." }}</p>
          </div>
        </div>

        <div class="rounded-md bg-primary-light p-4 text-sm text-primary">
          <p v-if="isRunningOrSuccess">
            배포가 성공적으로 완료되었습니다. 서비스가 정상적으로 실행 중입니다.
          </p>
          <p v-else-if="currentDeployment.status === 'FAILED'">
            {{ currentDeployment.statusMessage || "배포 중 오류가 발생했습니다. 다시 시도해 주세요." }}
          </p>
          <p v-else>
            현재 배포 프로세스가 진행 중입니다. 이 화면은 3초마다 자동으로 갱신됩니다.
          </p>
        </div>

        <!-- 서버 제어 버튼 (껍데기 UI, 백엔드 연동 예정) -->
        <div class="mt-6 flex items-center justify-between border-t border-border pt-6">
          <span class="text-sm font-bold text-ink-900">서버 제어</span>
          <ControlButtons
            :status="currentDeployment.status"
            :loading-action="loadingAction"
            @start="requestControl('start')"
            @stop="requestControl('stop')"
            @restart="requestControl('restart')"
            @delete="requestControl('delete')"
          />
        </div>
      </BaseCard>

      <!-- 배포 진행 로그 (깃헙 액션식) -->
      <div v-if="currentDeployment" class="mt-6">
        <LogViewer
          :logs="deploymentLogs || []"
          :loading="isLogsFetching"
          @refresh="handleRefreshLogs"
        />
      </div>
    </main>

    <!-- 리소스 모달 컴포넌트 (타입 경고 방지를 위해 deployment-id를 문자열로 안전하게 변환) -->
    <ResourceModal
      :open="isResourceModalOpen"
      :deployment-id="currentDeployment?.deploymentId ? String(currentDeployment.deploymentId) : undefined"
      :project-name="currentDeployment?.projectName"
      @close="isResourceModalOpen = false"
    />

    <!-- 서버 제어 확인 모달 -->
    <ConfirmModal
      :open="confirmState.open"
      :title="confirmMeta.title"
      :message="confirmMeta.message"
      :confirm-text="confirmMeta.confirmText"
      :confirm-variant="confirmMeta.confirmVariant"
      @confirm="confirmControl"
      @cancel="cancelControl"
    />
  </div>
</template>
