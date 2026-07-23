<script setup>
import { computed, ref } from "vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import BaseCard from "../components/common/BaseCard.vue";
import ResourceModal from "../components/manage/ResourceModal.vue";
import { useDeployments } from "../composables/useDeployments";

// 3초 폴링이 적용된 조회 엔진
const { data: deployments, isLoading } = useDeployments();

// 리소스 모달 열림 상태 관리
const isResourceModalOpen = ref(false);

// 명세서 조건: "사용자별 배포 1건 고정 (단일 화면)"
const currentDeployment = computed(() => {
  return deployments.value && deployments.value.length > 0 
    ? deployments.value[0] 
    : null;
});

// 상태에 따른 UI(색상, 애니메이션, 아이콘) 처리
const statusConfig = computed(() => {
  if (!currentDeployment.value) return {};
  
  // 백엔드 응답 키값에 따라 status 혹은 deployStatus 사용
  const status = currentDeployment.value.status || currentDeployment.value.deployStatus;

  switch (status) {
    case "RUNNING":
    case "DEPLOYED": // 🚀 모크 환경에서 쓰이는 DEPLOYED도 성공으로 처리
      return { label: "실행 중", class: "bg-green-100 text-green-700", icon: "🟢" };
    case "FAILED":
      return { label: "배포 실패", class: "bg-red-100 text-red-700", icon: "🔴" };
    case "PENDING":
      return { label: "대기 중", class: "bg-yellow-100 text-yellow-700 animate-pulse", icon: "⏳" };
    case "BUILDING":
      return { label: "빌드 중", class: "bg-blue-100 text-blue-700 animate-pulse", icon: "🔨" };
    case "DEPLOYING":
      return { label: "배포 중", class: "bg-blue-100 text-blue-700 animate-pulse", icon: "🚀" };
    default:
      return { label: status, class: "bg-gray-100 text-gray-700", icon: "⚙️" };
  }
});

// 현재 배포가 성공 상태인지 판별 (RUNNING 또는 DEPLOYED)
const isRunningOrSuccess = computed(() => {
  if (!currentDeployment.value) return false;
  const status = currentDeployment.value.status || currentDeployment.value.deployStatus;
  return status === "RUNNING" || status === "DEPLOYED";
});
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">
      
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
      
      <!-- 배포 내역이 없을 때 -->
      <div v-else-if="!currentDeployment" class="flex flex-col items-center justify-center py-20">
        <p class="text-ink-600 mb-4">현재 등록된 배포 프로젝트가 없습니다.</p>
        <router-link to="/deploy/new" class="rounded bg-primary px-4 py-2 text-white hover:bg-primary-dark">
          새 프로젝트 배포하기
        </router-link>
      </div>

      <!-- 단일 배포 상태 화면 -->
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
              {{ statusConfig.icon }} {{ statusConfig.label }}
            </span>

            <!-- 성공/실행 중 상태일 때만 리소스 보기 버튼 노출 -->
            <button
              v-if="isRunningOrSuccess"
              @click="isResourceModalOpen = true"
              class="rounded border border-border bg-surface px-3 py-1 text-sm font-medium text-ink-700 hover:bg-ink-100"
            >
              📊 리소스
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
        </div>

        <div class="bg-blue-50 text-blue-700 p-4 rounded-md text-sm">
          <p v-if="isRunningOrSuccess">
            🎉 배포가 성공적으로 완료되었습니다! 서비스가 정상적으로 실행 중입니다.
          </p>
          <p v-else-if="(currentDeployment.status || currentDeployment.deployStatus) === 'FAILED'">
            ⚠️ 배포 중 오류가 발생했습니다. 다시 시도해 주세요.
          </p>
          <p v-else>
            🔄 현재 배포 프로세스가 진행 중입니다. 이 화면은 3초마다 자동으로 갱신됩니다.
          </p>
        </div>
      </BaseCard>
    </main>

    <!-- 리소스 모달 컴포넌트 (타입 경고 방지를 위해 deployment-id를 문자열로 안전하게 변환) -->
    <ResourceModal
      :open="isResourceModalOpen"
      :deployment-id="currentDeployment?.id ? String(currentDeployment.id) : undefined"
      :project-name="currentDeployment?.projectName"
      @close="isResourceModalOpen = false"
    />
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
