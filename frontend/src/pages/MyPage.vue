<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import BaseModal from "../components/common/BaseModal.vue";
import BaseTextarea from "../components/common/BaseTextarea.vue";
import BaseBadge from "../components/common/BaseBadge.vue";
import AppIcon from "../components/common/AppIcon.vue";
import StatusBadge from "../components/manage/StatusBadge.vue";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useDeployments, useUpdateDeployment } from "../composables/useDeployments";
import { usePosts } from "../composables/usePosts";
import { useAuthStore } from "../stores/useAuthStore";
import { useToastStore } from "../stores/useToastStore";

const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();

const { data: user, isLoading: isUserLoading } = useCurrentUser();
const { data: deployments } = useDeployments();
const { data: posts } = usePosts();

// 배포 설명/공개여부 수정 모달
const { mutate: updateDeployment, isPending: isSaving } = useUpdateDeployment();
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
            <h2 class="inline-flex items-center gap-2 text-base font-bold text-ink-900">
              <AppIcon name="cube" class="h-4 w-4 text-primary" />
              내가 배포한 서비스
            </h2>
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
              @click="router.push({ path: '/deployments', query: { id: dep.id } })"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="font-mono text-sm font-semibold text-ink-900">{{ dep.projectName }}</span>
                <div class="flex items-center gap-1.5">
                  <BaseBadge :tone="dep.visibility === 'PUBLIC' ? 'info' : 'neutral'">
                    {{ dep.visibility === "PUBLIC" ? "공개" : "비공개" }}
                  </BaseBadge>
                  <StatusBadge kind="deploy" :status="dep.deployStatus" />
                </div>
              </div>

              <!-- 설명 -->
              <p v-if="dep.description" class="mt-2 line-clamp-2 text-xs leading-relaxed text-ink-700">
                {{ dep.description }}
              </p>

              <a
                v-if="dep.url"
                :href="dep.url"
                target="_blank"
                rel="noreferrer"
                class="mt-2 inline-flex items-center gap-1 truncate text-xs text-primary hover:underline"
                @click.stop
              >
                <AppIcon name="link" class="h-3.5 w-3.5 shrink-0" />
                <span class="truncate">{{ dep.url }}</span>
              </a>

              <div class="mt-2 flex items-center justify-between">
                <p class="text-xs text-ink-500">배포일 {{ formatDate(dep.createdAt) }}</p>
                <button
                  type="button"
                  class="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                  @click.stop="openEdit(dep)"
                >
                  <AppIcon name="pencil" class="h-3.5 w-3.5" />
                  수정
                </button>
              </div>
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
            <h2 class="inline-flex items-center gap-2 text-base font-bold text-ink-900">
              <AppIcon name="pencil" class="h-4 w-4 text-primary" />
              내가 쓴 글 / 공유
            </h2>
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
                  <span
                    class="h-1.5 w-1.5 shrink-0 rounded-full"
                    :class="post.type === 'deploy' ? 'bg-primary' : 'bg-ink-300'"
                  />
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

    <!-- 배포 설명/공개여부 수정 모달 -->
    <BaseModal
      :open="editOpen"
      :title="`${editTarget?.projectName ?? ''} 배포 수정`"
      width="max-w-md"
      @close="editOpen = false"
    >
      <div class="flex flex-col gap-4">
        <BaseTextarea
          id="editDescription"
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
        <BaseButton variant="ghost" :disabled="isSaving" @click="editOpen = false">취소</BaseButton>
        <BaseButton :loading="isSaving" :disabled="isSaving" @click="saveEdit">저장</BaseButton>
      </template>
    </BaseModal>
  </div>
</template>
