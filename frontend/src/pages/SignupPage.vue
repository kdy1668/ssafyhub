<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import BrandBar from "../components/common/BrandBar.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseInput from "../components/common/BaseInput.vue";
import BaseButton from "../components/common/BaseButton.vue";
import { useAuthStore } from "../stores/useAuthStore";
import { useToastStore } from "../stores/useToastStore";

const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();

const form = reactive({
  team: "",
  name: "",
  studentId: "",
  password: "",
  passwordConfirm: "",
});

const errors = reactive({
  team: "",
  name: "",
  studentId: "",
  password: "",
  passwordConfirm: "",
});

const isSubmitting = ref(false);

function validate() {
  errors.team = form.team ? "" : "팀명을 입력해주세요.";
  errors.name = form.name ? "" : "이름을 입력해주세요.";
  errors.studentId = form.studentId ? "" : "학번을 입력해주세요.";
  errors.password = form.password.length >= 8 ? "" : "비밀번호는 8자 이상이어야 합니다.";
  errors.passwordConfirm =
    form.passwordConfirm === form.password ? "" : "비밀번호가 일치하지 않습니다.";

  return (
    !errors.team &&
    !errors.name &&
    !errors.studentId &&
    !errors.password &&
    !errors.passwordConfirm
  );
}

async function handleSubmit() {
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    await authStore.signup({
      team: form.team,
      name: form.name,
      studentId: form.studentId,
      password: form.password,
      passwordConfirm: form.passwordConfirm,
    });
    toastStore.showToast("회원가입이 완료되었습니다. 로그인해주세요.", "success");
    router.push("/login");
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
      <h1 class="mb-6 text-center text-lg font-semibold text-ink-900">회원가입</h1>
      <form class="flex flex-col gap-4" @submit.prevent="handleSubmit">
        <BaseInput
          id="team"
          label="팀명"
          placeholder="팀명"
          v-model="form.team"
          :error="errors.team"
        />
        <BaseInput
          id="name"
          label="이름"
          placeholder="홍길동"
          v-model="form.name"
          :error="errors.name"
        />
        <BaseInput
          id="studentId"
          label="학번(아이디)"
          placeholder="학번"
          v-model="form.studentId"
          :error="errors.studentId"
        />
        <BaseInput
          id="password"
          type="password"
          label="비밀번호"
          placeholder="8자 이상"
          v-model="form.password"
          :error="errors.password"
        />
        <BaseInput
          id="passwordConfirm"
          type="password"
          label="비밀번호 확인"
          placeholder="비밀번호 확인"
          v-model="form.passwordConfirm"
          :error="errors.passwordConfirm"
        />
        <BaseButton
          type="submit"
          class="mt-2 w-full"
          :loading="isSubmitting"
          :disabled="isSubmitting"
        >
          회원가입
        </BaseButton>
      </form>
      <p class="mt-6 text-center text-sm text-ink-500">
        이미 계정이 있으신가요?
        <RouterLink to="/login" class="font-medium text-primary hover:underline">
          로그인
        </RouterLink>
      </p>
    </BaseCard>
    </div>
  </div>
</template>