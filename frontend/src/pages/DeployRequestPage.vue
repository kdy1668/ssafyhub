<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import BaseCard from "../components/common/BaseCard.vue";
import BaseInput from "../components/common/BaseInput.vue";
import BaseSelect from "../components/common/BaseSelect.vue";
import BaseButton from "../components/common/BaseButton.vue";
import AppIcon from "../components/common/AppIcon.vue";
import EnvVarsEditor from "../components/deploy/EnvVarsEditor.vue";
import PreviewModal from "../components/deploy/PreviewModal.vue";
import UploadProgress from "../components/deploy/UploadProgress.vue";
import { useModalStore } from "../stores/useModalStore";
import { useToastStore } from "../stores/useToastStore";

// 새로 만든 엔진(로직) import (경로를 프로젝트에 맞게 확인해주세요)
import { useCreateDeployment } from "../composables/useDeployments";

const RUNTIME_OPTIONS = [
  { value: "17", label: "Java 17" },
  { value: "21", label: "Java 21" },
];

const router = useRouter();
const modalStore = useModalStore();
const toastStore = useToastStore();
const {
  executeDeploy,
  retry,
  isDeploying,
  phase,
  uploads,
  hasError,
} = useCreateDeployment();

// 테스트용: 업로드 실패/재시도 화면을 확인하기 위한 실패 시뮬레이션 토글
const simulateFailure = ref(false);

// S3에 직접 올릴 파일 상태
const backendFile = ref(null);
const frontendFile = ref(null);

// 텍스트/설정 폼 상태
const form = reactive({
  projectName: "",
  runtimeVersion: "21",
  internalPort: 8080,
  databaseName: "",
  databasePassword: "",
  envVars: [],
});

const errors = reactive({
  projectName: "",
  backendFile: "",
  frontendFile: "",
  databasePassword: "",
});

function handleBackendFileChange(e) {
  backendFile.value = e.target.files[0] || null;
  if (backendFile.value) errors.backendFile = "";
}

function handleFrontendFileChange(e) {
  frontendFile.value = e.target.files[0] || null;
  if (frontendFile.value) errors.frontendFile = "";
}

function validate() {
  errors.projectName = form.projectName ? "" : "프로젝트 이름은 필수입니다.";
  errors.backendFile = backendFile.value ? "" : "백엔드 JAR 파일을 업로드하세요.";
  errors.frontendFile = frontendFile.value ? "" : "프론트엔드 ZIP 파일을 업로드하세요.";
  errors.databasePassword = form.databasePassword ? "" : "DB 비밀번호는 필수입니다.";

  return !Object.values(errors).some(Boolean);
}

function buildPayload() {
  return {
    backendFile: backendFile.value,
    frontendFile: frontendFile.value,
    projectName: form.projectName,
    runtimeVersion: form.runtimeVersion,
    internalPort: form.internalPort,
    databaseName: form.databaseName,
    databasePassword: form.databasePassword,
    envList: form.envVars.filter((v) => v.key && v.value),
    simulateFailure: simulateFailure.value,
  };
}

async function onDeploySuccess() {
  toastStore.showToast("배포 요청이 성공적으로 접수되었습니다.", "success");
  router.push("/deployments"); // 성공 시 상태 화면으로 이동 (3초 폴링 자동 시작)
}

async function handleSubmit() {
  if (!validate()) return;

  try {
    // 🚀 S3 업로드(진행률 추적) + 배포 엔진 실행
    await executeDeploy(buildPayload());
    await onDeploySuccess();
  } catch (error) {
    // 실패 시 페이지에 머물러 진행률/재시도 UI 를 노출한다.
    toastStore.showToast(error.message || "업로드에 실패했습니다. 다시 시도해 주세요.", "error");
  }
}

// 실패한 업로드 재시도
async function handleRetry() {
  try {
    await retry();
    await onDeploySuccess();
  } catch (error) {
    toastStore.showToast(error.message || "재시도에 실패했습니다.", "error");
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
          <span class="text-xs font-bold uppercase tracking-wider text-primary">Deployment</span>
          <span class="rounded bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
            New Service
          </span>
        </div>
        <h1 class="mt-1 text-2xl font-bold text-ink-900">배포 신청</h1>
        <p class="text-xs text-ink-500">
          JAR 및 ZIP 파일을 업로드하고 환경 변수를 설정하여 클라우드 환경에 배포를 요청합니다.
        </p>
      </div>

      <!-- 배포 신청 폼 카드 -->
      <BaseCard class="p-6 sm:p-8 shadow-card border border-border bg-surface rounded-lg">
        <form class="flex flex-col gap-6" @submit.prevent="handleSubmit">
          
          <!-- 기본 설정 영역 -->
          <BaseInput
            id="projectName"
            label="프로젝트 이름"
            placeholder="demo-service"
            v-model="form.projectName"
            :error="errors.projectName"
          />

          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <BaseSelect
              id="runtimeVersion"
              label="Java 버전"
              :options="RUNTIME_OPTIONS"
              v-model="form.runtimeVersion"
            />
            
            <BaseInput
              id="internalPort"
              type="number"
              label="백엔드 내부 포트"
              placeholder="8080"
              v-model="form.internalPort"
            />
          </div>

          <!-- 파일 업로드 영역 -->
          <div class="flex flex-col gap-4 border-y border-border py-6">
            <h3 class="text-sm font-bold text-ink-900">빌드 파일 업로드</h3>
            
            <div>
              <label class="mb-1 block text-sm font-medium text-ink-700">백엔드 JAR 파일</label>
              <input 
                type="file" 
                accept=".jar" 
                @change="handleBackendFileChange" 
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.backendFile" class="mt-1 text-xs text-red-500">{{ errors.backendFile }}</p>
            </div>

            <div>
              <label class="mb-1 block text-sm font-medium text-ink-700">프론트엔드 ZIP 파일</label>
              <input 
                type="file" 
                accept=".zip" 
                @change="handleFrontendFileChange" 
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.frontendFile" class="mt-1 text-xs text-red-500">{{ errors.frontendFile }}</p>
            </div>
          </div>

          <!-- DB 설정 영역 -->
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <BaseInput
              id="databaseName"
              label="DB 이름"
              placeholder="app_db"
              v-model="form.databaseName"
            />

            <BaseInput
              id="databasePassword"
              type="password"
              label="DB 비밀번호 (필수)"
              placeholder="비밀번호를 입력하세요"
              v-model="form.databasePassword"
              :error="errors.databasePassword"
            />
          </div>

          <!-- 환경변수 영역 -->
          <EnvVarsEditor v-model="form.envVars" />

          <!-- 업로드 진행률 / 실패·재시도 패널 (업로드 시작 후에만 표시) -->
          <UploadProgress
            v-if="phase !== 'idle'"
            :uploads="uploads"
            :phase="phase"
            @retry="handleRetry"
          />

          <!-- 테스트용: 업로드 실패 시뮬레이션 토글 -->
          <label class="flex cursor-pointer items-center gap-2 text-xs text-ink-500">
            <input type="checkbox" v-model="simulateFailure" class="accent-danger" />
            업로드 실패 시뮬레이션 (테스트용 — 체크 시 프론트엔드 업로드가 실패합니다)
          </label>

          <!-- 하단 버튼 액션 영역 -->
          <div class="mt-2 flex items-center justify-end gap-3 border-t border-border pt-6">
            <BaseButton
              type="button"
              variant="secondary"
              :disabled="isDeploying"
              @click="modalStore.openModal('preview')"
            >
              <AppIcon name="eye" class="h-4 w-4" />
              미리보기
            </BaseButton>

            <BaseButton
              type="submit"
              :loading="isDeploying"
              :disabled="isDeploying"
              class="shadow-card"
            >
              <AppIcon v-if="!isDeploying" name="upload" class="h-4 w-4" />
              {{ isDeploying ? "업로드 및 배포 중..." : hasError ? "다시 배포하기" : "배포하기" }}
            </BaseButton>
          </div>
        </form>
      </BaseCard>
    </main>

    <!-- 미리보기 모달 -->
    <PreviewModal
      :open="modalStore.activeModal === 'preview'"
      :values="form"
      @close="modalStore.closeModal()"
    />
  </div>
</template>