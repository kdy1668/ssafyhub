<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import DeploymentsTable from "../components/manage/DeploymentsTable.vue";
import ResourceModal from "../components/manage/ResourceModal.vue";
import { useDeployments } from "../composables/useDeployments";

const router = useRouter();

// 3초 폴링이 적용된 조회 엔진 (전체 배포 목록)
const { data: deployments, isLoading } = useDeployments();

// 상세보기 → 리소스 모달
const isResourceModalOpen = ref(false);
const selected = ref(null);

function openDetail(item) {
  selected.value = item;
  isResourceModalOpen.value = true;
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-5xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 뒤로가기 -->
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="router.back()">
        ← 뒤로가기
      </BaseButton>

      <!-- 상단 헤더 -->
      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <div class="flex items-center gap-2">
          <span class="text-xs font-bold uppercase tracking-wider text-primary">Admin</span>
          <span class="rounded bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
            관리자 전용
          </span>
        </div>
        <h1 class="mt-1 text-2xl font-bold text-ink-900">배포 이력 관리</h1>
        <p class="text-xs text-ink-500">
          전체 사용자의 배포 이력과 서버/배포 상태를 조회합니다. 이 화면은 3초마다 자동으로 갱신됩니다.
        </p>
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="flex flex-col gap-3">
        <BaseSkeleton class="h-12 w-full rounded-lg" />
        <BaseSkeleton class="h-12 w-full rounded-lg" />
        <BaseSkeleton class="h-12 w-full rounded-lg" />
      </div>

      <!-- 비어있음 -->
      <div v-else-if="!deployments || deployments.length === 0" class="flex flex-col items-center justify-center py-20">
        <p class="text-ink-600">등록된 배포 이력이 없습니다.</p>
      </div>

      <!-- 전체 배포 이력 목록 -->
      <DeploymentsTable v-else :deployments="deployments" @view-detail="openDetail" />
    </main>

    <!-- 리소스 상세 모달 -->
    <ResourceModal
      :open="isResourceModalOpen"
      :deployment-id="selected?.id ? String(selected.id) : undefined"
      :project-name="selected?.projectName"
      @close="isResourceModalOpen = false"
    />
  </div>
</template>
