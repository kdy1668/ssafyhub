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

const VISIBILITY_OPTIONS = [
  { value: "PUBLIC", label: "공개 (PUBLIC)" },
  { value: "PRIVATE", label: "비공개 (PRIVATE)" },
];

const DESCRIPTION_MAX_LENGTH = 2000;

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

// S3에 직접 올릴 파일 상태
const backendFile = ref(null);
const frontendFile = ref(null);

// 텍스트/설정 폼 상태
const form = reactive({
  projectName: "",
  runtimeVersion: "21",
  internalPort: 8080,
  needDatabase: true,
  databaseName: "",
  databasePassword: "",
  neededServices: [], // 예: ["REDIS"] - MySQL 외 추가 백킹서비스
  envVars: [],
  visibility: "PUBLIC",
  description: "",
});

const BACKING_SERVICE_OPTIONS = [
  { value: "REDIS", label: "Redis" },
];

const errors = reactive({
  projectName: "",
  backendFile: "",
  frontendFile: "",
  databaseName: "",
  databasePassword: "",
  internalPort: "",
  env: "",
  description: "",
});

// 백엔드 검증 규칙과 동일
const ENV_KEY_RE = /^[A-Z_][A-Z0-9_]*$/;
const DB_NAME_RE = /^[A-Za-z][A-Za-z0-9_]{0,99}$/;

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

  // 파일: 존재 + 확장자(.zip - Dockerfile을 포함한 BYO 빌드만 허용)
  const backendName = backendFile.value?.name.toLowerCase() ?? "";
  if (!backendFile.value) errors.backendFile = "백엔드 아티팩트를 업로드하세요.";
  else if (!backendName.endsWith(".zip"))
    errors.backendFile = "백엔드 파일은 Dockerfile을 포함한 .zip 형식이어야 합니다.";
  else errors.backendFile = "";

  if (!frontendFile.value) errors.frontendFile = "프론트엔드 ZIP 파일을 업로드하세요.";
  else if (!frontendFile.value.name.toLowerCase().endsWith(".zip"))
    errors.frontendFile = "프론트엔드 파일은 .zip 형식이어야 합니다.";
  else errors.frontendFile = "";

  // DB는 선택 - "DB 필요" 체크했을 때만 이름/비밀번호를 검증한다.
  if (form.needDatabase) {
    if (!form.databaseName) errors.databaseName = "DB 이름은 필수입니다.";
    else if (!DB_NAME_RE.test(form.databaseName))
      errors.databaseName = "DB 이름은 영문자로 시작하고 영문/숫자/_ 만 사용할 수 있습니다.";
    else errors.databaseName = "";

    errors.databasePassword = form.databasePassword ? "" : "DB 비밀번호는 필수입니다.";
  } else {
    errors.databaseName = "";
    errors.databasePassword = "";
  }

  // 내부 포트: 1~65535
  const port = Number(form.internalPort);
  errors.internalPort =
    Number.isInteger(port) && port >= 1 && port <= 65535
      ? ""
      : "내부 포트는 1~65535 사이의 숫자여야 합니다.";

  // 환경변수: 최소 1개·최대 20개, KEY 규칙(대문자/숫자/_, 숫자로 시작 불가), 중복 불가
  const list = form.envVars.filter((v) => v.key || v.value);
  if (list.length === 0) {
    errors.env = "환경변수를 최소 1개 이상 입력해주세요.";
  } else if (list.length > 20) {
    errors.env = "환경변수는 최대 20개까지 입력할 수 있습니다.";
  } else if (list.some((v) => !v.key || !v.value)) {
    errors.env = "환경변수의 KEY와 VALUE를 모두 입력해주세요.";
  } else if (list.some((v) => !ENV_KEY_RE.test(v.key))) {
    errors.env = "환경변수 KEY는 영문 대문자·숫자·_ 만 쓸 수 있고 숫자로 시작할 수 없습니다.";
  } else if (new Set(list.map((v) => v.key)).size !== list.length) {
    errors.env = "환경변수 KEY가 중복되었습니다.";
  } else {
    errors.env = "";
  }

  // 설명: 선택 입력, 백엔드 제약과 동일하게 최대 2000자
  errors.description =
    form.description.length > DESCRIPTION_MAX_LENGTH
      ? `설명은 ${DESCRIPTION_MAX_LENGTH}자를 초과할 수 없습니다.`
      : "";

  return !Object.values(errors).some(Boolean);
}

function buildPayload() {
  return {
    backendFile: backendFile.value,
    frontendFile: frontendFile.value,
    projectName: form.projectName,
    runtimeVersion: form.runtimeVersion,
    internalPort: form.internalPort,
    // DB 필요 없음 체크 시 빈 값으로 보내 백엔드가 "DB 프로비저닝 안 함"으로 처리하게 한다.
    databaseName: form.needDatabase ? form.databaseName : "",
    databasePassword: form.needDatabase ? form.databasePassword : "",
    neededServices: form.neededServices,
    envList: form.envVars.filter((v) => v.key && v.value),
    visibility: form.visibility,
    description: form.description,
  };
}

async function onDeploySuccess(deployment) {
  const expectedUrl = deployment?.expectedUrl;
  toastStore.showToast(
    expectedUrl
      ? `배포 요청이 접수되었습니다. 예상 주소: ${expectedUrl}`
      : "배포 요청이 접수되었습니다.",
    "success"
  );
  router.push("/deployments"); // 배포 상태 화면으로 이동 (3초 폴링 자동 시작)
}

async function handleSubmit() {
  if (!validate()) return;

  try {
    // 🚀 버튼 1회 클릭 → Presigned URL 발급 → S3 업로드 → 배포 생성 (순차 자동)
    const deployment = await executeDeploy(buildPayload());
    await onDeploySuccess(deployment);
  } catch (error) {
    // 실패 시 페이지에 머물러 진행률/재시도 UI 를 노출한다.
    toastStore.showToast(error.message || "배포 요청 중 오류가 발생했습니다.", "error");
  }
}

// 실패한 업로드 재시도
async function handleRetry() {
  try {
    const deployment = await retry();
    await onDeploySuccess(deployment);
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
          Dockerfile을 포함한 ZIP 파일을 업로드하고 환경 변수를 설정하여 클라우드 환경에 배포를 요청합니다.
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
              label="런타임 표기 (참고용)"
              :options="RUNTIME_OPTIONS"
              v-model="form.runtimeVersion"
            />
            
            <BaseInput
              id="internalPort"
              type="number"
              label="백엔드 내부 포트"
              placeholder="8080"
              v-model="form.internalPort"
              :error="errors.internalPort"
            />

            <BaseSelect
              id="visibility"
              label="공개 범위"
              :options="VISIBILITY_OPTIONS"
              v-model="form.visibility"
            />
          </div>

          <div class="flex flex-col gap-1">
            <label for="description" class="text-sm font-medium text-ink-700">설명</label>
            <textarea
              id="description"
              rows="3"
              placeholder="서비스에 대한 간단한 설명을 입력하세요 (선택)"
              v-model="form.description"
              :class="[
                'rounded-md border px-3 py-2 text-sm text-ink-900 outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary',
                errors.description ? 'border-danger' : 'border-border',
              ]"
            ></textarea>
            <div class="flex items-center justify-between">
              <span v-if="errors.description" class="text-xs text-danger">{{ errors.description }}</span>
              <span class="ml-auto text-xs text-ink-500">
                {{ form.description.length }}/{{ DESCRIPTION_MAX_LENGTH }}
              </span>
            </div>
          </div>

          <!-- 파일 업로드 영역 -->
          <div class="flex flex-col gap-4 border-y border-border py-6">
            <h3 class="text-sm font-bold text-ink-900">빌드 파일 업로드</h3>

            <div>
              <label class="mb-1 block text-sm font-medium text-ink-700">백엔드 아티팩트 (.zip, Dockerfile 필수)</label>
              <input
                type="file"
                accept=".zip"
                @change="handleBackendFileChange"
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.backendFile" class="mt-1 text-xs text-red-500">{{ errors.backendFile }}</p>
              <p class="mt-1 text-xs text-ink-500">
                프로젝트 소스(또는 빌드 산출물)와 <strong>Dockerfile을 함께</strong> zip으로 묶어 올려주세요.
                언어/프레임워크는 무관하며, 앱을 어떻게 빌드/실행할지는 이 Dockerfile이 전부 결정합니다.
                DB 테이블/마이그레이션이 필요한 프레임워크(MyBatis 등 자동 생성이 안 되는 경우)라면, 앱
                컨테이너가 시작될 때 마이그레이션을 직접 실행하도록 Dockerfile(또는 시작 스크립트)에
                포함해주세요 - 플랫폼은 빈 데이터베이스만 준비해 드립니다.
              </p>
              <details class="mt-2 rounded-md border border-border bg-background p-3">
                <summary class="cursor-pointer text-xs font-semibold text-primary">
                  예시 보기: zip 구성 &amp; Dockerfile (Java/Gradle 기준)
                </summary>
                <div class="mt-2 flex flex-col gap-2 text-xs text-ink-700">
                  <p>zip 안에는 아래처럼 Dockerfile과 소스가 최상위(또는 폴더 한 겹 안)에 있어야 합니다.</p>
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-ink-50"><code>backend.zip
├── Dockerfile
├── build.gradle
├── settings.gradle
├── gradlew
├── gradle/
└── src/
    └── main/...</code></pre>
                  <p>Dockerfile 예시 (멀티스테이지: 소스를 컨테이너 안에서 직접 빌드):</p>
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-ink-50"><code>FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]</code></pre>
                  <p>
                    Java가 아니어도 됩니다 - 위 형식만 지키면(zip에 Dockerfile 포함) Node, Python, Go 등
                    어떤 언어든 동일하게 동작합니다. 컨테이너는 위에서 입력한 "백엔드 내부 포트"로 요청을
                    받도록 만들어주세요.
                  </p>
                </div>
              </details>
            </div>

            <div>
              <label class="mb-1 block text-sm font-medium text-ink-700">프론트엔드 아티팩트 (.zip, Dockerfile 필수)</label>
              <input
                type="file"
                accept=".zip"
                @change="handleFrontendFileChange"
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.frontendFile" class="mt-1 text-xs text-red-500">{{ errors.frontendFile }}</p>
              <p class="mt-1 text-xs text-ink-500">
                프론트도 백엔드와 동일하게 <strong>Dockerfile을 포함한 zip</strong>으로 올립니다. 단, 프론트는
                컨테이너를 계속 실행하는 게 아니라 <strong>정적 빌드 결과물만</strong> 뽑아서 배포하므로,
                Dockerfile의 마지막 단계에서 빌드 결과물을 <code class="rounded bg-ink-900 px-1 py-0.5 text-ink-50">/output</code>
                경로에 복사해두어야 합니다.
              </p>
              <details class="mt-2 rounded-md border border-border bg-background p-3">
                <summary class="cursor-pointer text-xs font-semibold text-primary">
                  예시 보기: zip 구성 &amp; Dockerfile (React/Vite 기준)
                </summary>
                <div class="mt-2 flex flex-col gap-2 text-xs text-ink-700">
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-ink-50"><code>frontend.zip
├── Dockerfile
├── package.json
├── vite.config.js
└── src/...</code></pre>
                  <p>Dockerfile 예시 (빌드 후 결과물을 /output에 복사):</p>
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-ink-50"><code>FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM scratch
COPY --from=build /app/dist /output</code></pre>
                  <p>
                    Vue/Vite도 기본 빌드 출력 폴더(dist)만 다를 뿐 동일한 형태입니다. React·Vue·Svelte 등
                    빌드 결과가 정적 파일(HTML/JS/CSS)로 나오는 프레임워크라면 전부 이 방식으로 올릴 수
                    있습니다. 반드시 <code class="rounded bg-ink-900 px-1 py-0.5 text-ink-50">/output</code>
                    경로에 최종 정적 파일(index.html 포함)을 두어야 합니다.
                  </p>
                </div>
              </details>
            </div>
          </div>

          <!-- DB / 백킹서비스 설정 영역 -->
          <div class="flex flex-col gap-4 border-b border-border pb-6">
            <h3 class="text-sm font-bold text-ink-900">데이터베이스 / 백킹서비스</h3>

            <label class="flex items-center gap-2 text-sm text-ink-700">
              <input type="checkbox" v-model="form.needDatabase" />
              MySQL 데이터베이스가 필요합니다
            </label>

            <div v-if="form.needDatabase" class="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <BaseInput
                id="databaseName"
                label="DB 이름"
                placeholder="app_db"
                v-model="form.databaseName"
                :error="errors.databaseName"
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

            <div class="flex flex-col gap-1">
              <span class="text-sm font-medium text-ink-700">추가 백킹서비스</span>
              <label
                v-for="opt in BACKING_SERVICE_OPTIONS"
                :key="opt.value"
                class="flex items-center gap-2 text-sm text-ink-700"
              >
                <input type="checkbox" :value="opt.value" v-model="form.neededServices" />
                {{ opt.label }}
              </label>
            </div>
          </div>

          <!-- 환경변수 영역 -->
          <div>
            <EnvVarsEditor v-model="form.envVars" />
            <p v-if="errors.env" class="mt-1 text-xs text-red-500">{{ errors.env }}</p>
          </div>

          <!-- 업로드 진행률 / 실패·재시도 패널 (업로드 시작 후에만 표시) -->
          <UploadProgress
            v-if="phase !== 'idle'"
            :uploads="uploads"
            :phase="phase"
            @retry="handleRetry"
          />

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