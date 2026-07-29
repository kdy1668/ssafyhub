<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import BrandBar from "../components/common/BrandBar.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseInput from "../components/common/BaseInput.vue";
import BaseButton from "../components/common/BaseButton.vue";
import { useAuthStore } from "../stores/useAuthStore";
import { useToastStore } from "../stores/useToastStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();

const form = reactive({
  studentId: "",
  password: "",
});

const errors = reactive({
  studentId: "",
  password: "",
});

const isSubmitting = ref(false);

function validate() {
  errors.studentId = form.studentId ? "" : "학번을 입력해주세요.";
  errors.password = form.password ? "" : "비밀번호를 입력해주세요.";
  return !errors.studentId && !errors.password;
}

async function handleSubmit() {
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    // 백엔드로 로그인 요청 시 아이디 대신 학번(studentId)을 전달합니다.
    await authStore.login({ studentId: form.studentId, password: form.password });
    router.push(route.query.from || "/home");
  } catch (error) {
    toastStore.showToast(error.message, "error");
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <BrandBar />
    <div class="flex items-center justify-center px-4 py-16">
    <BaseCard class="w-full max-w-sm p-6">
      <h1 class="mb-6 text-center text-lg font-semibold text-ink-900">로그인</h1>
      <form class="flex flex-col gap-4" @submit.prevent="handleSubmit">
        <BaseInput
          id="studentId"
          label="학번"
          placeholder="학번"
          v-model="form.studentId"
          :error="errors.studentId"
        />
        <BaseInput
          id="password"
          type="password"
          label="비밀번호"
          placeholder="비밀번호"
          v-model="form.password"
          :error="errors.password"
        />
        <BaseButton
          type="submit"
          class="mt-2 w-full"
          :loading="isSubmitting"
          :disabled="isSubmitting"
        >
          로그인
        </BaseButton>
      </form>
      <p class="mt-6 text-center text-sm text-ink-500">
        계정이 없으신가요?
        <RouterLink to="/signup" class="font-medium text-primary hover:underline">
          회원가입
        </RouterLink>
      </p>
    </BaseCard>
    </div>
  </div>
</template>