<script setup>
import { computed, ref, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import BaseCard from "../components/common/BaseCard.vue";
import AppIcon from "../components/common/AppIcon.vue";
import BaseBadge from "../components/common/BaseBadge.vue";
import BaseModal from "../components/common/BaseModal.vue";
import BaseTextarea from "../components/common/BaseTextarea.vue";
import ConfirmModal from "../components/common/ConfirmModal.vue";
import ResourceModal from "../components/manage/ResourceModal.vue";
import ControlButtons from "../components/manage/ControlButtons.vue";
import LogViewer from "../components/manage/LogViewer.vue";
import MetricsPanel from "../components/manage/MetricsPanel.vue";
import StatusBadge from "../components/manage/StatusBadge.vue";
import { useDeployments, useDeleteDeployment, useUpdateDeployment } from "../composables/useDeployments";
import { useDeploymentResources } from "../composables/useDeploymentResources";
import { useDeploymentLogs } from "../composables/useDeploymentLogs";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useToastStore } from "../stores/useToastStore";

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();

// 3초 폴링이 적용된 조회 엔진
const { data: deployments, isLoading } = useDeployments();
const { data: user } = useCurrentUser();

// 내가 등록한 배포 목록 (허브 목록)
const myDeployments = computed(() => {
  const list = deployments.value ?? [];
  if (!user.value) return list;
  return list.filter((d) => d.owner === user.value.name);
});

// ?id 가 있으면 상세, 없으면 목록 허브
const viewId = computed(() => route.query.id ?? null);
const isDetail = computed(() => Boolean(viewId.value && currentDeployment.value));

function goDetail(dep) {
  router.push({ path: "/deployments", query: { id: dep.id } });
}
function goList() {
  router.push({ path: "/deployments" });
}

// --- 공개여부/설명 수정 모달 (목록 카드) ---
const { mutate: updateDeployment, isPending: isSavingEdit } = useUpdateDeployment();
const editOpen = ref(false);
const editTarget = ref(null);
const editForm = reactive({ description: "", visibility: "PRIVATE" });

function openEdit(dep) {
  editTarget.value = dep;
  editForm.description = dep.description ?? "";
  editForm.visibility = dep.visibility === "PUBLIC" ? "PUBLIC" : "PRIVATE";
  editOpen.value = true;
}
function saveEdit() {
  if (!editTarget.value) return;
  updateDeployment(
    { id: editTarget.value.id, payload: { description: editForm.description, visibility: editForm.visibility } },
    {
      onSuccess: () => {
        editOpen.value = false;
        toastStore.showToast("배포 정보가 수정되었습니다.", "success");
      },
      onError: (e) => toastStore.showToast(e.message || "수정에 실패했습니다.", "error"),
    }
  );
}

// 공개 여부 즉시 토글
function toggleVisibility(dep) {
  if (!dep) return;
  const next = dep.visibility === "PUBLIC" ? "PRIVATE" : "PUBLIC";
  updateDeployment(
    { id: dep.id, payload: { visibility: next } },
    {
      onSuccess: () => toastStore.showToast(`${next === "PUBLIC" ? "공개" : "비공개"}로 전환했습니다.`, "success"),
      onError: (e) => toastStore.showToast(e.message || "변경에 실패했습니다.", "error"),
    }
  );
}

// 리소스 모달 열림 상태 관리
const isResourceModalOpen = ref(false);

// --- 서버 제어 + 확인 모달 ---
const { mutate: deleteDeployment, isPending: isDeleting } = useDeleteDeployment();

const CONTROL_META = {
  start: { title: "서버 시작", message: "서버를 시작하시겠습니까?", confirmText: "시작", confirmVariant: "primary" },
  stop: { title: "서버 중지", message: "서버를 중지하시겠습니까? 서비스 접속이 중단됩니다.", confirmText: "중지", confirmVariant: "danger" },
  restart: { title: "서버 재시작", message: "서버를 재시작하시겠습니까? 잠시 서비스가 중단될 수 있습니다.", confirmText: "재시작", confirmVariant: "danger" },
  delete: { title: "배포 삭제", message: "이 배포를 삭제하시겠습니까? 삭제하면 되돌릴 수 없습니다.", confirmText: "삭제", confirmVariant: "danger" },
};

const confirmState = reactive({
  open: false,
  action: null,
});

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

// 확인 후 실제 동작
function confirmControl() {
  const action = confirmState.action;

  // 삭제만 실제 처리(목록에서 제거 후 결과 알림 + 목록 복귀)
  if (action === "delete") {
    const id = currentDeployment.value?.id;
    deleteDeployment(id, {
      onSuccess: () => {
        cancelControl();
        toastStore.showToast("배포가 삭제되었습니다.", "success");
        goList();
      },
      onError: (e) => {
        cancelControl();
        toastStore.showToast(e.message || "삭제에 실패했습니다.", "error");
      },
    });
    return;
  }

  // 시작/중지/재시작은 백엔드 연동 예정 (껍데기)
  const labels = { start: "시작", stop: "중지", restart: "재시작" };
  cancelControl();
  toastStore.showToast(`${labels[action] || action} 기능은 백엔드 연동 예정입니다.`, "info");
}

// 로그 수동 새로고침
function handleRefreshLogs() {
  refetchLogs();
}

// ?id 로 지정된 배포 상세 (id 없으면 상세 없음 → 목록 표시)
const currentDeployment = computed(() => {
  const list = deployments.value;
  if (!list || list.length === 0 || viewId.value == null) return null;
  return list.find((d) => String(d.id) === String(viewId.value)) || null;
});

// 상태에 따른 UI(색상, 애니메이션, 아이콘) 처리
const statusConfig = computed(() => {
  if (!currentDeployment.value) return {};
  
  // 백엔드 응답 키값에 따라 status 혹은 deployStatus 사용
  const status = currentDeployment.value.status || currentDeployment.value.deployStatus;

  switch (status) {
    case "RUNNING":
    case "DEPLOYED":
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

// 현재 배포가 성공 상태인지 판별 (RUNNING 또는 DEPLOYED)
const isRunningOrSuccess = computed(() => {
  if (!currentDeployment.value) return false;
  const status = currentDeployment.value.status || currentDeployment.value.deployStatus;
  return status === "RUNNING" || status === "DEPLOYED";
});

// --- 로그 / 메트릭 대시보드 (상세 화면) ---
const deploymentId = computed(() => currentDeployment.value?.id ?? null);
// 메트릭: 실행 중일 때만, 로그: 상세면 항상 (빌드/실패 로그도 확인)
const { data: metrics, isLoading: metricsLoading } = useDeploymentResources(deploymentId, isRunningOrSuccess);
const { data: logs, isLoading: logsLoading, refetch: refetchLogs } = useDeploymentLogs(deploymentId, isDetail);
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 뒤로가기 / 목록으로 -->
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="isDetail ? goList() : router.back()">
        {{ isDetail ? "← 목록으로" : "← 뒤로가기" }}
      </BaseButton>

      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <h1 class="mt-1 text-2xl font-bold text-ink-900">
          {{ isDetail ? "내 배포 상태" : "내 배포 목록" }}
        </h1>
        <p class="text-xs text-ink-500">
          {{ isDetail
            ? "진행 중이거나 완료된 프로젝트의 배포 상태를 확인합니다."
            : "내가 등록한 프로젝트를 한눈에 확인하고, 카드를 눌러 상세 상태로 이동하세요." }}
        </p>
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-20 w-full rounded-lg" />
        <BaseSkeleton class="h-40 w-full rounded-lg" />
      </div>

      <!-- ===== 목록 허브 (상세가 아닐 때) ===== -->
      <template v-else-if="!isDetail">
        <!-- 비어있음 -->
        <div v-if="myDeployments.length === 0" class="flex flex-col items-center justify-center py-20">
          <p class="mb-4 text-ink-600">아직 등록된 배포 프로젝트가 없습니다.</p>
          <router-link to="/deploy/new" class="rounded-md bg-primary px-4 py-2 text-white hover:bg-primary-hover">
            새 프로젝트 배포하기
          </router-link>
        </div>

        <!-- 프로젝트 카드 목록 -->
        <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div
            v-for="dep in myDeployments"
            :key="dep.id"
            role="button"
            tabindex="0"
            class="flex cursor-pointer flex-col gap-2 rounded-xl border border-border bg-surface p-5 text-left shadow-card transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-cardHover"
            @click="goDetail(dep)"
            @keydown.enter="goDetail(dep)"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="font-mono text-sm font-semibold text-ink-900">{{ dep.projectName }}</span>
              <StatusBadge kind="deploy" :status="dep.deployStatus" />
            </div>
            <div class="flex flex-wrap items-center gap-1.5">
              <StatusBadge v-if="dep.serverStatus" kind="server" :status="dep.serverStatus" />
              <BaseBadge v-if="dep.visibility" :tone="dep.visibility === 'PUBLIC' ? 'info' : 'neutral'">
                {{ dep.visibility === "PUBLIC" ? "공개" : "비공개" }}
              </BaseBadge>
            </div>
            <p v-if="dep.description" class="line-clamp-2 text-xs leading-relaxed text-ink-600">
              {{ dep.description }}
            </p>
            <div class="mt-1 flex items-center justify-between">
              <span class="inline-flex items-center gap-1 text-xs font-medium text-primary">
                상세 보기 →
              </span>
              <button
                type="button"
                class="inline-flex items-center gap-1 text-xs font-medium text-ink-500 hover:text-primary"
                @click.stop="openEdit(dep)"
              >
                <AppIcon name="pencil" class="h-3.5 w-3.5" />
                공개여부·설명 수정
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- ===== 단일 배포 상세 화면 ===== -->
      <BaseCard v-else class="p-6 sm:p-8 shadow-card border border-border bg-surface rounded-lg">
        
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
              {{ statusConfig.label }}
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
            <span class="block text-xs font-medium text-ink-500 mb-1">Java 버전</span>
            <span class="text-sm font-semibold text-ink-900">Java {{ currentDeployment.runtimeVersion || '21' }}</span>
          </div>
          <div>
            <span class="block text-xs font-medium text-ink-500 mb-1">내부 포트</span>
            <span class="text-sm font-semibold text-ink-900">{{ currentDeployment.internalPort || '8080' }}</span>
          </div>
          <div>
            <span class="block text-xs font-medium text-ink-500 mb-1">Database 이름</span>
            <span class="text-sm font-semibold text-ink-900">{{ currentDeployment.databaseName || '-' }}</span>
          </div>
          <div>
            <span class="block text-xs font-medium text-ink-500 mb-1">공개 여부</span>
            <div class="flex items-center gap-2">
              <BaseBadge :tone="currentDeployment.visibility === 'PUBLIC' ? 'info' : 'neutral'">
                {{ currentDeployment.visibility === "PUBLIC" ? "공개" : "비공개" }}
              </BaseBadge>
              <button
                type="button"
                :disabled="isSavingEdit"
                class="rounded-md border border-border bg-surface px-2.5 py-1 text-xs font-medium text-ink-700 transition-colors hover:bg-black/5 disabled:opacity-60"
                @click="toggleVisibility(currentDeployment)"
              >
                {{ currentDeployment.visibility === "PUBLIC" ? "비공개로 전환" : "공개로 전환" }}
              </button>
            </div>
          </div>
          <div v-if="currentDeployment.url" class="sm:col-span-2">
            <span class="block text-xs font-medium text-ink-500 mb-1">배포 URL</span>
            <a
              :href="currentDeployment.url"
              target="_blank"
              rel="noreferrer"
              class="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline"
            >
              <AppIcon name="link" class="h-3.5 w-3.5 shrink-0" />
              <span class="truncate">{{ currentDeployment.url }}</span>
            </a>
          </div>
          <div class="sm:col-span-2">
            <span class="block text-xs font-medium text-ink-500 mb-1">서비스 설명</span>
            <span class="text-sm text-ink-700">{{ currentDeployment.description || "-" }}</span>
          </div>
        </div>

        <div class="rounded-md bg-primary-light p-4 text-sm text-primary">
          <p v-if="isRunningOrSuccess">
            배포가 성공적으로 완료되었습니다. 서비스가 정상적으로 실행 중입니다.
          </p>
          <p v-else-if="(currentDeployment.status || currentDeployment.deployStatus) === 'FAILED'">
            배포 중 오류가 발생했습니다. 다시 시도해 주세요.
          </p>
          <p v-else>
            현재 배포 프로세스가 진행 중입니다. 이 화면은 3초마다 자동으로 갱신됩니다.
          </p>
        </div>

        <!-- 서버 제어 버튼 (껍데기 UI, 백엔드 연동 예정) -->
        <div class="mt-6 flex items-center justify-between border-t border-border pt-6">
          <span class="text-sm font-bold text-ink-900">서버 제어</span>
          <ControlButtons
            :status="currentDeployment.status || currentDeployment.deployStatus"
            :loading-action="isDeleting ? 'delete' : ''"
            @start="requestControl('start')"
            @stop="requestControl('stop')"
            @restart="requestControl('restart')"
            @delete="requestControl('delete')"
          />
        </div>
      </BaseCard>

      <!-- 로그 · 메트릭 대시보드 (상세 화면) -->
      <div v-if="isDetail" class="mt-6 flex flex-col gap-6">
        <MetricsPanel v-if="isRunningOrSuccess" :data="metrics" :loading="metricsLoading" />
        <LogViewer
          :logs="logs ?? []"
          :loading="logsLoading"
          @refresh="handleRefreshLogs"
        />
      </div>
    </main>

    <!-- 리소스 모달 컴포넌트 (타입 경고 방지를 위해 deployment-id를 문자열로 안전하게 변환) -->
    <ResourceModal
      :open="isResourceModalOpen"
      :deployment-id="currentDeployment?.id ? String(currentDeployment.id) : undefined"
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
      :loading="isDeleting"
      @confirm="confirmControl"
      @cancel="cancelControl"
    />

    <!-- 공개여부/설명 수정 모달 (목록) -->
    <BaseModal
      :open="editOpen"
      :title="`${editTarget?.projectName ?? ''} 배포 수정`"
      width="max-w-md"
      @close="editOpen = false"
    >
      <div class="flex flex-col gap-4">
        <BaseTextarea
          id="mngEditDescription"
          label="서비스 설명"
          placeholder="이 배포 서비스에 대한 설명"
          v-model="editForm.description"
          rows="3"
        />
        <div>
          <span class="mb-1.5 block text-sm font-medium text-ink-700">공개 여부</span>
          <div class="inline-flex rounded-lg bg-background p-1 ring-1 ring-border">
            <button
              type="button"
              :class="[
                'rounded-md px-4 py-1.5 text-sm font-semibold transition-colors',
                editForm.visibility === 'PRIVATE' ? 'bg-primary text-white shadow-sm' : 'text-ink-500 hover:text-ink-900',
              ]"
              @click="editForm.visibility = 'PRIVATE'"
            >
              비공개
            </button>
            <button
              type="button"
              :class="[
                'rounded-md px-4 py-1.5 text-sm font-semibold transition-colors',
                editForm.visibility === 'PUBLIC' ? 'bg-primary text-white shadow-sm' : 'text-ink-500 hover:text-ink-900',
              ]"
              @click="editForm.visibility = 'PUBLIC'"
            >
              공개
            </button>
          </div>
        </div>
      </div>
      <template #footer>
        <BaseButton variant="ghost" :disabled="isSavingEdit" @click="editOpen = false">취소</BaseButton>
        <BaseButton :loading="isSavingEdit" :disabled="isSavingEdit" @click="saveEdit">저장</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
    <!-- 기존코드
    <ResourceModal
      :open="isResourceModalOpen"
      :deployment-id="currentDeployment?.id"
      :project-name="currentDeployment?.projectName"
      @close="isResourceModalOpen = false"
    />
  </div>
</template>
-->
