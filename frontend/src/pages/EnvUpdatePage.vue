<script setup>
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useQuery, useQueryClient } from "@tanstack/vue-query";
import AppHeader from "../components/common/AppHeader.vue";
import BaseButton from "../components/common/BaseButton.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseSkeleton from "../components/common/BaseSkeleton.vue";
import AppIcon from "../components/common/AppIcon.vue";
import EnvVarsEditor from "../components/deploy/EnvVarsEditor.vue";
import { useMyDeployment } from "../composables/useDeployments";
import { fetchDeploymentEnv, updateDeploymentEnv } from "../api/deployments";
import { useToastStore } from "../stores/useToastStore";

const ENV_KEY_RE = /^[A-Z_][A-Z0-9_]*$/;

const router = useRouter();
const toastStore = useToastStore();
const queryClient = useQueryClient();

const { data: currentDeployment, isLoading: isDeploymentLoading } = useMyDeployment();
const deploymentId = computed(() => currentDeployment.value?.deploymentId);

const { data: envData, isLoading: isEnvLoading } = useQuery({
  queryKey: computed(() => ["deployments", deploymentId.value, "env"]),
  queryFn: () => fetchDeploymentEnv(deploymentId.value),
  enabled: computed(() => Boolean(deploymentId.value)),
});

const envVars = ref([]);
watch(envData, (list) => {
  envVars.value = list && list.length > 0 ? list.map((e) => ({ ...e })) : [{ key: "", value: "" }];
}, { immediate: true });

const errorMessage = ref("");
const isSubmitting = ref(false);

function validate() {
  const list = envVars.value.filter((v) => v.key || v.value);
  if (list.length > 20) {
    errorMessage.value = "환경변수는 최대 20개까지 입력할 수 있습니다.";
  } else if (list.some((v) => !v.key || !v.value)) {
    errorMessage.value = "환경변수의 KEY와 VALUE를 모두 입력해주세요.";
  } else if (list.some((v) => !ENV_KEY_RE.test(v.key))) {
    errorMessage.value = "환경변수 KEY는 영문 대문자·숫자·_ 만 쓸 수 있고 숫자로 시작할 수 없습니다.";
  } else if (new Set(list.map((v) => v.key)).size !== list.length) {
    errorMessage.value = "환경변수 KEY가 중복되었습니다.";
  } else {
    errorMessage.value = "";
  }
  return !errorMessage.value ? list : null;
}

async function handleSubmit() {
  const list = validate();
  if (!list) return;

  isSubmitting.value = true;
  try {
    await updateDeploymentEnv(deploymentId.value, list);
    await queryClient.invalidateQueries({ queryKey: ["deployments", "me"] });
    toastStore.showToast("환경변수를 반영했습니다. 재배포가 시작됩니다.", "success");
    router.push("/deployments");
  } catch (error) {
    toastStore.showToast(error.message || "환경변수 반영에 실패했습니다.", "error");
  } finally {
    isSubmitting.value = false;
  }
}

const isLoading = computed(() => isDeploymentLoading.value || isEnvLoading.value);
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-2xl px-4 py-8 sm:px-6 sm:py-10">

      <!-- 뒤로가기 -->
      <BaseButton variant="ghost" class="mb-4 -ml-2" @click="router.back()">
        ← 뒤로가기
      </BaseButton>

      <div class="mb-6 flex flex-col gap-1 border-b border-border/80 pb-4">
        <h1 class="mt-1 text-2xl font-bold text-ink-900">환경변수 수정</h1>
        <p class="text-xs text-ink-500">
          아티팩트를 다시 올릴 필요 없이 환경변수만 바꾸고 같은 코드로 재배포합니다.
        </p>
      </div>

      <div v-if="isLoading" class="flex flex-col gap-4">
        <BaseSkeleton class="h-40 w-full rounded-lg" />
      </div>

      <BaseCard v-else class="flex flex-col gap-4 p-6 shadow-card border border-border bg-surface rounded-lg">
        <div class="flex items-start gap-2 rounded-md bg-primary-light p-3 text-xs text-primary">
          <AppIcon name="alert" class="mt-0.5 h-4 w-4 shrink-0" />
          <p>
            저장하면 바로 재배포가 시작됩니다. 배포가 진행 중(빌드/배포 중)일 때는 저장할 수 없고,
            서비스가 정상 실행 중이거나 실패 상태일 때만 가능합니다.
          </p>
        </div>

        <EnvVarsEditor v-model="envVars" />
        <p v-if="errorMessage" class="text-xs text-danger">{{ errorMessage }}</p>

        <div class="mt-2 flex justify-end gap-2 border-t border-border pt-4">
          <BaseButton variant="ghost" @click="router.push('/deployments')">취소</BaseButton>
          <BaseButton variant="primary" :loading="isSubmitting" @click="handleSubmit">
            저장하고 재배포
          </BaseButton>
        </div>
      </BaseCard>
    </main>
  </div>
</template>
