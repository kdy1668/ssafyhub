<script setup>
import { computed, reactive, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import BaseCard from "../components/common/BaseCard.vue";
import BaseInput from "../components/common/BaseInput.vue";
import BaseTextarea from "../components/common/BaseTextarea.vue";
import BaseButton from "../components/common/BaseButton.vue";
import { usePost } from "../composables/usePost";
import { useCreatePost } from "../composables/useCreatePost";
import { useUpdatePost } from "../composables/useUpdatePost";
import { useToastStore } from "../stores/useToastStore";

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();

const postId = computed(() => route.params.id ?? null);
const isEdit = computed(() => Boolean(postId.value));

const { data: existingPost } = usePost(postId);
const { mutate: createPost, isPending: isCreating } = useCreatePost();
const { mutate: updatePost, isPending: isUpdating } = useUpdatePost();
const isPending = computed(() => isCreating.value || isUpdating.value);

const form = reactive({
  title: "",
  content: "",
});

const errors = reactive({
  title: "",
});

watch(
  existingPost,
  (post) => {
    if (post) {
      form.title = post.title;
      form.content = post.content;
    }
  },
  { immediate: true }
);

function validate() {
  errors.title = form.title ? "" : "제목은 필수입니다.";
  return !errors.title;
}

function handleSubmit() {
  if (!validate()) return;

  const payload = { title: form.title, content: form.content };

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
    <main class="mx-auto max-w-3xl px-4 py-8 sm:px-6 sm:py-10">
      
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
          <BaseInput
            id="title"
            label="제목"
            placeholder="제목을 입력하세요"
            v-model="form.title"
            :error="errors.title"
          />

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