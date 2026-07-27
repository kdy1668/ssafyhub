<script setup>
import { computed, reactive, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import BaseCard from "../components/common/BaseCard.vue";
import BaseInput from "../components/common/BaseInput.vue";
import BaseTextarea from "../components/common/BaseTextarea.vue";
import BaseButton from "../components/common/BaseButton.vue";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import { usePost } from "../composables/usePost";
import { useCreatePost } from "../composables/useCreatePost";
import { useUpdatePost } from "../composables/useUpdatePost";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useToastStore } from "../stores/useToastStore";

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();

const postId = computed(() => route.params.id ?? null);
const isEdit = computed(() => Boolean(postId.value));

const { data: existingPost } = usePost(postId);
const { data: user } = useCurrentUser();
const { mutate: createPost, isPending: isCreating } = useCreatePost();
const { mutate: updatePost, isPending: isUpdating } = useUpdatePost();
const isPending = computed(() => isCreating.value || isUpdating.value);

const POST_TYPES = [
  { value: "free", label: "자유게시판", desc: "팀원들과 자유롭게 나누는 글", icon: "chat" },
  { value: "deploy", label: "배포 공유", desc: "배포한 서비스를 소개하는 글", icon: "rocket" },
];

const form = reactive({
  // 새 글 진입 시 쿼리(?type=deploy)로 유형 미리 선택
  type: route.query.type === "deploy" ? "deploy" : "free",
  title: "",
  content: "",
  projectName: "",
  deployUrl: "",
});

const errors = reactive({
  title: "",
  projectName: "",
});

const isDeploy = computed(() => form.type === "deploy");

watch(
  [existingPost, user],
  ([post, currentUser]) => {
    if (!post) return;

    // 남의 글은 수정 불가 → 상세로 돌려보냄
    if (isEdit.value && currentUser && post.author && post.author !== currentUser.name) {
      toastStore.showToast("본인이 작성한 글만 수정할 수 있습니다.", "error");
      router.replace(`/board/${post.id}`);
      return;
    }

    form.type = post.type === "deploy" ? "deploy" : "free";
    form.title = post.title;
    form.content = post.content;
    form.projectName = post.deployment?.projectName ?? "";
    form.deployUrl = post.deployment?.url ?? "";
  },
  { immediate: true }
);

function validate() {
  errors.title = form.title ? "" : "제목은 필수입니다.";
  errors.projectName =
    isDeploy.value && !form.projectName ? "배포 공유 글은 프로젝트 이름이 필요합니다." : "";
  return !errors.title && !errors.projectName;
}

function handleSubmit() {
  if (!validate()) return;

  const payload = {
    type: form.type,
    title: form.title,
    content: form.content,
    deployment: isDeploy.value
      ? { projectName: form.projectName, url: form.deployUrl }
      : undefined,
  };

  if (isEdit.value) {
    updatePost(
      { id: postId.value, payload },
      {
        onSuccess: () => {
          toastStore.showToast("게시글이 수정되었습니다.", "success");
          router.push(`/board/${postId.value}`);
        },
      }
    );
  } else {
    createPost(payload, {
      onSuccess: (post) => {
        toastStore.showToast("게시글이 등록되었습니다.", "success");
        router.push(`/board/${post.id}`);
      },
    });
  }
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 뒤로가기 -->
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="router.back()">
        ← 뒤로가기
      </BaseButton>

      <!-- 상단 서브 헤더 -->
      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <div class="flex items-center gap-2">
          <span class="text-xs font-bold uppercase tracking-wider text-primary">Community</span>
          <span class="rounded bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
            {{ isEdit ? "Edit Post" : "New Post" }}
          </span>
        </div>
        <h1 class="mt-1 text-2xl font-bold text-ink-900">
          {{ isEdit ? "게시글 수정" : "게시글 작성" }}
        </h1>
        <p class="text-xs text-ink-500">
          {{ isEdit ? "기존 게시글의 내용을 수정하여 저장합니다." : "팀원들과 공유할 새로운 의견이나 소식을 작성해 주세요." }}
        </p>
      </div>

      <!-- 작성 / 수정 폼 카드 -->
      <BaseCard class="p-6 sm:p-8 shadow-card border border-border bg-surface rounded-lg">
        <form class="flex flex-col gap-6" @submit.prevent="handleSubmit">
          <!-- 글 유형 선택 -->
          <div>
            <span class="mb-1.5 block text-sm font-medium text-ink-700">글 유형</span>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button
                v-for="t in POST_TYPES"
                :key="t.value"
                type="button"
                :class="[
                  'flex flex-col items-start gap-0.5 rounded-md border px-4 py-3 text-left transition-colors',
                  form.type === t.value
                    ? 'border-primary bg-primary-light'
                    : 'border-border bg-background hover:border-primary/40',
                ]"
                @click="form.type = t.value"
              >
                <span class="flex items-center gap-2 text-sm font-semibold text-ink-900">
                  <AppIcon
                    :name="t.icon"
                    class="h-4 w-4"
                    :class="form.type === t.value ? 'text-primary' : 'text-ink-500'"
                  />
                  {{ t.label }}
                </span>
                <span class="text-xs text-ink-500">{{ t.desc }}</span>
              </button>
            </div>
          </div>

          <BaseInput
            id="title"
            label="제목"
            placeholder="제목을 입력하세요"
            v-model="form.title"
            :error="errors.title"
          />

          <!-- 배포 공유 전용: 배포 정보 -->
          <div
            v-if="isDeploy"
            class="grid grid-cols-1 gap-6 rounded-md border border-primary/30 bg-primary-light/40 p-4 sm:grid-cols-2"
          >
            <BaseInput
              id="projectName"
              label="프로젝트 이름"
              placeholder="예) shopping-api"
              v-model="form.projectName"
              :error="errors.projectName"
            />
            <BaseInput
              id="deployUrl"
              label="배포 URL (선택)"
              placeholder="https://my-service.example-deploy.com"
              v-model="form.deployUrl"
            />
          </div>

          <BaseTextarea
            id="content"
            label="내용"
            placeholder="내용을 입력하세요"
            v-model="form.content"
            rows="10"
          />

          <!-- 하단 버튼 액션 영역 -->
          <div class="mt-2 flex items-center justify-end gap-3 border-t border-border pt-6">
            <BaseButton type="button" variant="secondary" @click="router.back()">
              취소
            </BaseButton>
            <BaseButton type="submit" :loading="isPending" :disabled="isPending" class="shadow-card">
              {{ isEdit ? "수정 완료" : "등록하기" }}
            </BaseButton>
          </div>
        </form>
      </BaseCard>

    </main>
  </div>
</template>