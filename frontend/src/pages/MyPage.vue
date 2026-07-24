<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import StatusBadge from "../components/manage/StatusBadge.vue";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useDeployments } from "../composables/useDeployments";
import { usePosts } from "../composables/usePosts";
import { useAuthStore } from "../stores/useAuthStore";

const router = useRouter();
const authStore = useAuthStore();

const { data: user, isLoading: isUserLoading } = useCurrentUser();
const { data: deployments } = useDeployments();
const { data: posts } = usePosts();

// 현재 사용자가 소유한 배포 서비스
const myDeployments = computed(() => {
  const list = deployments.value ?? [];
  if (!user.value) return list;
  return list.filter((d) => d.owner === user.value.name);
});

// 현재 사용자가 작성한 글 / 공유
const myPosts = computed(() => {
  const list = posts.value ?? [];
  if (!user.value) return [];
  return list.filter((p) => p.author === user.value.name);
});

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
        <p class="mt-1 text-xs text-ink-500">내 프로필과 배포 서비스, 작성한 글을 한곳에서 관리합니다.</p>
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

        <!-- 내가 배포한 서비스 -->
        <section>
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-base font-bold text-ink-900">📦 내가 배포한 서비스</h2>
            <button
              type="button"
              class="text-xs font-medium text-primary hover:underline"
              @click="router.push('/deployments')"
            >
              전체 보기 →
            </button>
          </div>

          <div v-if="myDeployments.length" class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <BaseCard
              v-for="dep in myDeployments"
              :key="dep.id"
              class="cursor-pointer p-4 transition-colors hover:border-primary/40"
              @click="router.push('/deployments')"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="font-mono text-sm font-semibold text-ink-900">{{ dep.projectName }}</span>
                <StatusBadge kind="deploy" :status="dep.deployStatus" />
              </div>
              <a
                v-if="dep.url"
                :href="dep.url"
                target="_blank"
                rel="noreferrer"
                class="mt-2 block truncate text-xs text-primary hover:underline"
                @click.stop
              >
                🔗 {{ dep.url }}
              </a>
              <p class="mt-1 text-xs text-ink-500">배포일 {{ formatDate(dep.createdAt) }}</p>
            </BaseCard>
          </div>
          <BaseCard v-else class="p-6 text-center text-sm text-ink-500">
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

        <!-- 내가 쓴 글 / 공유 -->
        <section>
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-base font-bold text-ink-900">📝 내가 쓴 글 / 공유</h2>
            <button
              type="button"
              class="text-xs font-medium text-primary hover:underline"
              @click="router.push('/board/new')"
            >
              새 글 작성 →
            </button>
          </div>

          <BaseCard v-if="myPosts.length" class="overflow-hidden">
            <ul>
              <li
                v-for="post in myPosts"
                :key="post.id"
                class="flex cursor-pointer items-center justify-between gap-3 border-b border-border px-4 py-3 last:border-b-0 hover:bg-black/5"
                @click="router.push(`/board/${post.id}`)"
              >
                <span class="flex min-w-0 items-center gap-2">
                  <span class="shrink-0 text-xs">{{ post.type === "deploy" ? "🚀" : "💬" }}</span>
                  <span class="truncate text-sm text-ink-900">{{ post.title }}</span>
                </span>
                <span class="shrink-0 text-xs text-ink-500">{{ formatDate(post.createdAt) }}</span>
              </li>
            </ul>
          </BaseCard>
          <BaseCard v-else class="p-6 text-center text-sm text-ink-500">
            아직 작성한 글이 없습니다.
          </BaseCard>
        </section>
      </div>
    </main>
  </div>
</template>
