<script setup>
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import AppIcon from "../components/common/AppIcon.vue";
import StatusBadge from "../components/manage/StatusBadge.vue";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useMyDeployment } from "../composables/useDeployments";
import { useAuthStore } from "../stores/useAuthStore";

const router = useRouter();
const authStore = useAuthStore();

const { data: user, isLoading: isUserLoading } = useCurrentUser();
const { data: myDeployment, isLoading: isDeploymentLoading, isError } = useMyDeployment();

function formatDate(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(iso));
}

function handleLogout() {
  authStore.logout();
  router.push("/");
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />

    <main class="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-10">
      <div class="mb-6 border-b border-border/80 pb-4">
        <h1 class="text-2xl font-bold text-ink-900">마이페이지</h1>
        <p class="mt-1 text-xs text-ink-500">내 프로필과 배포 서비스를 한곳에서 관리합니다.</p>
      </div>

      <div class="flex flex-col gap-6">
        <!-- 프로필 카드 -->
        <BaseCard class="p-6">
          <div v-if="isUserLoading" class="flex items-center gap-4">
            <BaseSkeleton class="h-16 w-16 rounded-full" />
            <div class="flex flex-col gap-2">
              <BaseSkeleton class="h-5 w-32" />
              <BaseSkeleton class="h-4 w-48" />
            </div>
          </div>
          <div v-else-if="user" class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex items-center gap-4">
              <div
                class="flex h-16 w-16 items-center justify-center rounded-full bg-primary-light text-2xl font-bold text-primary"
              >
                {{ user.name.slice(0, 1) }}
              </div>
              <div>
                <h2 class="text-lg font-bold text-ink-900">{{ user.name }}</h2>
                <p class="text-sm text-ink-500">{{ user.team }}</p>
                <p class="text-xs text-ink-500">{{ user.email }}</p>
              </div>
            </div>
            <BaseButton variant="secondary" class="w-fit" @click="handleLogout">로그아웃</BaseButton>
          </div>
        </BaseCard>

        <!-- 내가 배포한 서비스 (사용자당 배포는 1개) -->
        <section>
          <div class="mb-3 flex items-center justify-between">
            <h2 class="inline-flex items-center gap-2 text-base font-bold text-ink-900">
              <AppIcon name="cube" class="h-4 w-4 text-primary" />
              내가 배포한 서비스
            </h2>
            <button
              v-if="myDeployment"
              type="button"
              class="text-xs font-medium text-primary hover:underline"
              @click="router.push('/deployments')"
            >
              상세 보기 →
            </button>
          </div>

          <div v-if="isDeploymentLoading" class="flex flex-col gap-2">
            <BaseSkeleton class="h-20 w-full rounded-lg" />
          </div>

          <BaseCard
            v-else-if="myDeployment"
            class="cursor-pointer p-4 transition-colors hover:border-primary/40"
            @click="router.push('/deployments')"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="font-mono text-sm font-semibold text-ink-900">{{ myDeployment.projectName }}</span>
              <StatusBadge kind="deploy" :status="myDeployment.status" />
            </div>
            <a
              v-if="myDeployment.endpointUrl"
              :href="myDeployment.endpointUrl"
              target="_blank"
              rel="noreferrer"
              class="mt-2 inline-flex items-center gap-1 truncate text-xs text-primary hover:underline"
              @click.stop
            >
              <AppIcon name="link" class="h-3.5 w-3.5 shrink-0" />
              <span class="truncate">{{ myDeployment.endpointUrl }}</span>
            </a>
            <p class="mt-1 text-xs text-ink-500">최근 업데이트 {{ formatDate(myDeployment.updatedAt) }}</p>
          </BaseCard>

          <BaseCard v-else-if="isError" class="p-6 text-center text-sm text-ink-500">
            아직 배포한 서비스가 없습니다.
            <button
              type="button"
              class="ml-1 font-medium text-primary hover:underline"
              @click="router.push('/deploy/new')"
            >
              배포하러 가기 →
            </button>
          </BaseCard>
        </section>
      </div>
    </main>
  </div>
</template>
