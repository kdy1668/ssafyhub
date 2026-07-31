<script setup>
import { ref, reactive, computed } from "vue";
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
import TechIcon from "../components/deploy/TechIcon.vue";
import StepHeader from "../components/deploy/StepHeader.vue";
import { useModalStore } from "../stores/useModalStore";
import { useToastStore } from "../stores/useToastStore";

// 페이지 상단 "어떻게 동작하나요?" 다이어그램 - AWS 콘솔의 서비스 개요 다이어그램 참고.
const HOW_IT_WORKS_STEPS = [
  { icon: "settings", title: "언어 선택", description: "Java·Python·Node 중 고르기" },
  { icon: "upload", title: "파일 업로드", description: "빌드 산출물만 올리면 끝" },
  { icon: "cube", title: "자동 빌드", description: "Dockerfile은 플랫폼이 대신 생성" },
  { icon: "rocket", title: "배포 완료", description: "몇 분 안에 내 URL로 접속" },
];

// 새로 만든 엔진(로직) import (경로를 프로젝트에 맞게 확인해주세요)
import { useCreateDeployment } from "../composables/useDeployments";

// 언어 선택 - 고르면 그 언어에 속한 프레임워크 카드들이 아래에 나타난다.
const LANGUAGE_OPTIONS = [
  { value: "JAVA", icon: "java", label: "Java", sublabel: "Spring Boot" },
  { value: "PYTHON", icon: "python", label: "Python", sublabel: "Django" },
  { value: "NODE", icon: "node", label: "Node.js", sublabel: "Express" },
];

// 언어 안에서 고를 프레임워크/마이그레이션 방식 - 선택에 따라 백엔드 아티팩트 형식,
// 런타임 버전 목록, DB 초기화 SQL 업로드 노출 여부(자동 마이그레이션 vs 수동)가 전부 바뀐다.
// 실제 Dockerfile은 플랫폼이 스택에 맞춰 자동 생성하므로 사용자는 올릴 필요가 없다.
const STACK_OPTIONS = [
  {
    value: "SPRING_JPA",
    language: "JAVA",
    label: "JPA / Hibernate",
    description: "자동 마이그레이션",
    artifactAccept: ".jar",
    artifactLabel: "백엔드 아티팩트 (.jar)",
    artifactHint: "빌드된 실행 파일(.jar) 하나만 올리면 됩니다. 실행 환경은 플랫폼이 자동으로 구성합니다.",
    manualMigration: false,
    runtimeOptions: [
      { value: "17", label: "Java 17" },
      { value: "21", label: "Java 21" },
    ],
  },
  {
    value: "SPRING_MYBATIS",
    language: "JAVA",
    label: "MyBatis",
    description: "직접 마이그레이션",
    artifactAccept: ".jar",
    artifactLabel: "백엔드 아티팩트 (.jar)",
    artifactHint: "빌드된 실행 파일(.jar) 하나만 올리면 됩니다. 테이블은 아래 'DB 초기화 SQL'로 생성합니다.",
    manualMigration: true,
    runtimeOptions: [
      { value: "17", label: "Java 17" },
      { value: "21", label: "Java 21" },
    ],
  },
  {
    value: "DJANGO",
    language: "PYTHON",
    label: "Django",
    description: "자동 마이그레이션",
    artifactAccept: ".zip",
    artifactLabel: "백엔드 아티팩트 (.zip)",
    artifactHint: "manage.py, requirements.txt가 포함된 프로젝트 소스를 zip으로 올려주세요. " +
      "배포 시 pip install과 DB 마이그레이션을 자동으로 실행합니다.",
    manualMigration: false,
    runtimeOptions: [
      { value: "3.11", label: "Python 3.11" },
      { value: "3.12", label: "Python 3.12" },
    ],
    zipExample: `backend.zip
├── manage.py
├── requirements.txt
├── myproject/
│   ├── settings.py
│   ├── urls.py
│   └── wsgi.py
└── myapp/
    ├── models.py
    ├── views.py
    └── migrations/`,
  },
  {
    value: "EXPRESS",
    language: "NODE",
    label: "Express",
    description: "직접 마이그레이션",
    artifactAccept: ".zip",
    artifactLabel: "백엔드 아티팩트 (.zip)",
    artifactHint: "package.json이 포함된 프로젝트 소스를 zip으로 올려주세요. package.json에 " +
      "\"start\" 스크립트가 있어야 하고, 앱은 process.env.PORT로 지정된 포트를 사용해야 합니다.",
    manualMigration: true,
    runtimeOptions: [
      { value: "18", label: "Node.js 18" },
      { value: "20", label: "Node.js 20" },
    ],
    zipExample: `backend.zip
├── package.json
├── package-lock.json
├── index.js        (package.json "main"/"scripts.start"이 가리키는 진입 파일)
└── src/
    └── routes/...`,
  },
];

// 배포 구조 - 분리형(백엔드/프론트 각각 업로드) vs 통합형(백엔드가 프론트를 직접 서빙,
// 예: Spring static 리소스, express.static). 통합형이면 프론트 업로드 자체를 받지 않는다.
const DEPLOYMENT_STRUCTURE_OPTIONS = [
  { value: "SEPARATE", label: "프론트/백엔드 분리", description: "각각 따로 빌드해서 업로드" },
  { value: "MONOLITHIC", label: "하나로 통합", description: "백엔드가 프론트도 직접 서빙" },
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
const schemaFile = ref(null);

// 텍스트/설정 폼 상태
const form = reactive({
  projectName: "",
  deploymentStructure: "SEPARATE", // SEPARATE | MONOLITHIC
  language: "JAVA",
  stack: "SPRING_JPA",
  runtimeVersion: "17",
  internalPort: 8080,
  databaseEngine: "NONE", // NONE | MYSQL
  databaseName: "",
  databasePassword: "",
  neededServices: [], // 예: ["REDIS", "MONGODB"] - 관계형 DB 외 추가 백킹서비스
  envVars: [],
  visibility: "PUBLIC",
  description: "",
});

const selectedStack = computed(
  () => STACK_OPTIONS.find((s) => s.value === form.stack) || STACK_OPTIONS[0]
);

// 현재 고른 언어에 속한 프레임워크 카드 목록
const availableStacks = computed(
  () => STACK_OPTIONS.filter((s) => s.language === form.language)
);

const DB_ENGINE_OPTIONS = [
  { value: "NONE", icon: "none", label: "사용 안 함", description: "DB 없음 / SQLite 등 내장 DB" },
  { value: "MYSQL", icon: "mysql", label: "MySQL", description: "관계형 DB" },
];

const BACKING_SERVICE_OPTIONS = [
  { value: "REDIS", icon: "redis", label: "Redis", description: "캐시 / 세션 저장소" },
  { value: "MONGODB", icon: "mongodb", label: "MongoDB", description: "문서형 NoSQL" },
];

const errors = reactive({
  projectName: "",
  backendFile: "",
  frontendFile: "",
  schemaFile: "",
  databaseName: "",
  databasePassword: "",
  internalPort: "",
  env: "",
  description: "",
});

// 백엔드 검증 규칙과 동일
const ENV_KEY_RE = /^[A-Z_][A-Z0-9_]*$/;
const DB_NAME_RE = /^[A-Za-z][A-Za-z0-9_]{0,99}$/;

function selectDeploymentStructure(value) {
  form.deploymentStructure = value;
  if (value === "MONOLITHIC") {
    frontendFile.value = null;
    errors.frontendFile = "";
  }
}

function selectLanguage(value) {
  form.language = value;
  const firstStack = STACK_OPTIONS.find((s) => s.language === value);
  selectStack(firstStack.value);
}

function selectStack(value) {
  form.stack = value;
  const stack = STACK_OPTIONS.find((s) => s.value === value);
  form.runtimeVersion = stack.runtimeOptions[0].value;
  backendFile.value = null;
  errors.backendFile = "";
  if (!stack.manualMigration) {
    schemaFile.value = null;
    errors.schemaFile = "";
  }
}

function handleBackendFileChange(e) {
  backendFile.value = e.target.files[0] || null;
  if (backendFile.value) errors.backendFile = "";
}

function handleFrontendFileChange(e) {
  frontendFile.value = e.target.files[0] || null;
  if (frontendFile.value) errors.frontendFile = "";
}

function handleSchemaFileChange(e) {
  schemaFile.value = e.target.files[0] || null;
  if (schemaFile.value) errors.schemaFile = "";
}

function selectDatabaseEngine(value) {
  form.databaseEngine = value;
  if (value === "NONE") {
    schemaFile.value = null;
    errors.schemaFile = "";
  }
}

function toggleNeededService(value) {
  const idx = form.neededServices.indexOf(value);
  if (idx === -1) form.neededServices.push(value);
  else form.neededServices.splice(idx, 1);
}

function validate() {
  errors.projectName = form.projectName ? "" : "프로젝트 이름은 필수입니다.";

  // 백엔드 파일: 존재 + 선택한 스택이 요구하는 확장자와 일치해야 함
  const backendName = backendFile.value?.name.toLowerCase() ?? "";
  if (!backendFile.value) errors.backendFile = "백엔드 아티팩트를 업로드하세요.";
  else if (!backendName.endsWith(selectedStack.value.artifactAccept))
    errors.backendFile = `백엔드 파일은 ${selectedStack.value.artifactAccept} 형식이어야 합니다.`;
  else errors.backendFile = "";

  // 통합형(백엔드가 프론트 직접 서빙)이면 프론트 파일 자체를 받지 않는다.
  if (form.deploymentStructure === "MONOLITHIC") {
    errors.frontendFile = "";
  } else if (!frontendFile.value) {
    errors.frontendFile = "프론트엔드 ZIP 파일을 업로드하세요.";
  } else if (!frontendFile.value.name.toLowerCase().endsWith(".zip")) {
    errors.frontendFile = "프론트엔드 파일은 .zip 형식이어야 합니다.";
  } else {
    errors.frontendFile = "";
  }

  // DB 초기화 SQL은 수동 마이그레이션 스택에서만 의미가 있고, 올렸다면 확장자를 지켜야 한다.
  if (selectedStack.value.manualMigration && schemaFile.value
      && !schemaFile.value.name.toLowerCase().endsWith(".sql")) {
    errors.schemaFile = "DB 초기화 SQL 파일은 .sql 형식이어야 합니다.";
  } else {
    errors.schemaFile = "";
  }

  // DB는 선택 - "사용 안 함"이 아닐 때만 이름/비밀번호를 검증한다.
  if (form.databaseEngine !== "NONE") {
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
  const needDatabase = form.databaseEngine !== "NONE";
  const canUseSchemaFile = needDatabase && selectedStack.value.manualMigration;
  return {
    backendFile: backendFile.value,
    // 통합형이면 프론트 파일 자체가 없다 - null로 보내 백엔드가 "별도 프론트 없음"으로 처리하게 한다.
    frontendFile: form.deploymentStructure === "MONOLITHIC" ? null : frontendFile.value,
    // 수동 마이그레이션 스택 + DB 사용 중 + 실제로 파일을 고른 경우에만 올린다.
    schemaFile: canUseSchemaFile ? schemaFile.value : null,
    projectName: form.projectName,
    stack: form.stack,
    runtimeVersion: form.runtimeVersion,
    internalPort: form.internalPort,
    // "사용 안 함"이면 빈 값으로 보내 백엔드가 "DB 프로비저닝 안 함"으로 처리하게 한다.
    databaseName: needDatabase ? form.databaseName : "",
    databasePassword: needDatabase ? form.databasePassword : "",
    databaseEngine: needDatabase ? form.databaseEngine : undefined,
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
          언어/프레임워크를 고르고 빌드 산출물을 업로드하면, 플랫폼이 알맞은 실행 환경을 자동으로
          구성해 클라우드에 배포합니다.
        </p>
      </div>

      <!-- 어떻게 동작하나요? (초심자용 개요 다이어그램) -->
      <div class="mb-6 rounded-lg border border-border bg-surface p-5 shadow-card sm:p-6">
        <p class="mb-4 text-xs font-bold uppercase tracking-wider text-primary">어떻게 동작하나요?</p>
        <div class="flex flex-col items-stretch gap-3 sm:flex-row sm:items-center sm:gap-2">
          <template v-for="(step, i) in HOW_IT_WORKS_STEPS" :key="step.title">
            <div class="flex flex-1 items-center gap-3 sm:flex-col sm:text-center">
              <span
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary-light text-primary"
              >
                <AppIcon :name="step.icon" class="h-5 w-5" />
              </span>
              <div class="sm:mt-1">
                <p class="text-sm font-semibold text-ink-900">{{ step.title }}</p>
                <p class="text-xs text-ink-500">{{ step.description }}</p>
              </div>
            </div>
            <div v-if="i < HOW_IT_WORKS_STEPS.length - 1" class="hidden shrink-0 text-lg text-ink-300 sm:block">
              →
            </div>
          </template>
        </div>
      </div>

      <!-- 배포 신청 폼 카드 -->
      <BaseCard class="p-6 sm:p-8 shadow-card border border-border bg-surface rounded-lg">
        <form class="flex flex-col gap-6" @submit.prevent="handleSubmit">

          <!-- 1. 프로젝트 정보 -->
          <StepHeader :step="1" title="프로젝트 정보" />
          <BaseInput
            id="projectName"
            label="프로젝트 이름"
            placeholder="demo-service"
            v-model="form.projectName"
            :error="errors.projectName"
          />

          <!-- 배포 구조 선택 -->
          <div class="flex flex-col gap-3">
            <span class="text-xs font-semibold uppercase tracking-wide text-ink-500">배포 구조</span>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button
                v-for="opt in DEPLOYMENT_STRUCTURE_OPTIONS"
                :key="opt.value"
                type="button"
                @click="selectDeploymentStructure(opt.value)"
                :class="[
                  'flex items-center gap-3 rounded-lg border p-3 text-left transition-colors',
                  form.deploymentStructure === opt.value
                    ? 'border-primary bg-primary-light ring-1 ring-primary'
                    : 'border-border bg-background hover:border-primary/50',
                ]"
              >
                <span class="flex flex-col">
                  <span class="text-sm font-semibold text-ink-900">{{ opt.label }}</span>
                  <span class="text-xs text-ink-500">{{ opt.description }}</span>
                </span>
              </button>
            </div>
          </div>

          <!-- 2. 언어 / 프레임워크 -->
          <StepHeader :step="2" title="언어 / 프레임워크" />

          <!-- 언어 선택 -->
          <div class="flex flex-col gap-3">
            <span class="text-xs font-semibold uppercase tracking-wide text-ink-500">언어</span>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <button
                v-for="lang in LANGUAGE_OPTIONS"
                :key="lang.value"
                type="button"
                @click="selectLanguage(lang.value)"
                :class="[
                  'flex items-center gap-3 rounded-lg border p-3 text-left transition-colors',
                  form.language === lang.value
                    ? 'border-primary bg-primary-light ring-1 ring-primary'
                    : 'border-border bg-background hover:border-primary/50',
                ]"
              >
                <TechIcon :kind="lang.icon" />
                <span class="flex flex-col">
                  <span class="text-sm font-semibold text-ink-900">{{ lang.label }}</span>
                  <span class="text-xs text-ink-500">{{ lang.sublabel }}</span>
                </span>
              </button>
            </div>
          </div>

          <!-- 프레임워크 선택 - 위에서 고른 언어에 속한 것만 나타난다 -->
          <div class="flex flex-col gap-3">
            <span class="text-xs font-semibold uppercase tracking-wide text-ink-500">프레임워크</span>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <button
                v-for="opt in availableStacks"
                :key="opt.value"
                type="button"
                @click="selectStack(opt.value)"
                :class="[
                  'flex items-center gap-3 rounded-lg border p-3 text-left transition-colors',
                  form.stack === opt.value
                    ? 'border-primary bg-primary-light ring-1 ring-primary'
                    : 'border-border bg-background hover:border-primary/50',
                ]"
              >
                <span class="flex flex-col">
                  <span class="text-sm font-semibold text-ink-900">{{ opt.label }}</span>
                  <span class="text-xs text-ink-500">{{ opt.description }}</span>
                </span>
              </button>
            </div>
          </div>

          <!-- 3. 실행 설정 -->
          <StepHeader :step="3" title="실행 설정" />

          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <BaseSelect
              id="runtimeVersion"
              label="런타임 버전"
              :options="selectedStack.runtimeOptions"
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

          <!-- 4. 빌드 파일 업로드 -->
          <div class="flex flex-col gap-4 border-y border-border py-6">
            <StepHeader :step="4" title="빌드 파일 업로드" />

            <div>
              <label class="mb-1 block text-sm font-medium text-ink-700">{{ selectedStack.artifactLabel }}</label>
              <input
                type="file"
                :accept="selectedStack.artifactAccept"
                @change="handleBackendFileChange"
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.backendFile" class="mt-1 text-xs text-red-500">{{ errors.backendFile }}</p>
              <p class="mt-1 text-xs text-ink-500">{{ selectedStack.artifactHint }}</p>
              <pre
                v-if="selectedStack.zipExample"
                class="mt-2 overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"
              ><code>{{ selectedStack.zipExample }}</code></pre>
            </div>

            <div v-if="form.deploymentStructure === 'SEPARATE'">
              <label class="mb-1 block text-sm font-medium text-ink-700">프론트엔드 아티팩트 (.zip)</label>
              <input
                type="file"
                accept=".zip"
                @change="handleFrontendFileChange"
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.frontendFile" class="mt-1 text-xs text-red-500">{{ errors.frontendFile }}</p>
              <p class="mt-1 text-xs text-ink-500">
                React/Vue 등으로 빌드한 정적 산출물(예: dist 폴더 안의 내용)을 그대로 zip으로 압축해서
                올려주세요. index.html이 zip 최상위(또는 폴더 한 겹 안)에 있어야 합니다.
              </p>
              <pre class="mt-2 overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code>frontend.zip
├── index.html
├── assets/
│   ├── index-xxxx.js
│   └── index-xxxx.css
└── ...</code></pre>
            </div>
            <p v-else class="text-xs text-ink-500">
              선택하신 "하나로 통합" 구조는 백엔드 아티팩트 안에 정적 리소스가 이미 포함돼 있다고 보고,
              별도 프론트 업로드 없이 백엔드 컨테이너가 모든 요청(API + 정적 자원)을 그대로 받습니다.
            </p>
          </div>

          <!-- 5. 데이터베이스 / 백킹서비스 설정 영역 -->
          <div class="flex flex-col gap-5 border-b border-border pb-6">
            <StepHeader :step="5" title="데이터베이스 / 백킹서비스" />
            <div class="flex flex-col gap-3">
              <span class="text-xs font-semibold uppercase tracking-wide text-ink-500">데이터베이스</span>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <button
                  v-for="opt in DB_ENGINE_OPTIONS"
                  :key="opt.value"
                  type="button"
                  @click="selectDatabaseEngine(opt.value)"
                  :class="[
                    'flex items-center gap-3 rounded-lg border p-3 text-left transition-colors',
                    form.databaseEngine === opt.value
                      ? 'border-primary bg-primary-light ring-1 ring-primary'
                      : 'border-border bg-background hover:border-primary/50',
                  ]"
                >
                  <TechIcon :kind="opt.icon" />
                  <span class="flex flex-col">
                    <span class="text-sm font-semibold text-ink-900">{{ opt.label }}</span>
                    <span class="text-xs text-ink-500">{{ opt.description }}</span>
                  </span>
                </button>
              </div>
              <p v-if="form.databaseEngine === 'NONE'" class="text-xs text-ink-500">
                SQLite처럼 컨테이너 안 파일로 저장하는 내장 DB를 쓴다면 이 옵션을 그대로 선택하세요
                - 플랫폼이 별도로 준비할 게 없습니다. 다만 컨테이너가 재시작되면 그 안의 파일은
                함께 사라지니, 데이터를 계속 보존해야 한다면 위의 MySQL을 선택해주세요.
              </p>
            </div>

            <div v-if="form.databaseEngine !== 'NONE'" class="flex flex-col gap-4">
              <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
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

              <!-- JPA/Django처럼 자동으로 테이블을 만들어주는 스택에서는 아예 노출하지 않는다. -->
              <div v-if="selectedStack.manualMigration">
                <label class="mb-1 block text-sm font-medium text-ink-700">
                  DB 초기화 SQL (선택 - .sql)
                </label>
                <input
                  type="file"
                  accept=".sql"
                  @change="handleSchemaFileChange"
                  class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
                />
                <p v-if="errors.schemaFile" class="mt-1 text-xs text-red-500">{{ errors.schemaFile }}</p>
                <p class="mt-1 text-xs text-ink-500">
                  선택한 스택은 테이블을 자동으로 만들어주지 않습니다.
                  <code class="rounded bg-ink-900 px-1 py-0.5 text-white">CREATE TABLE</code>
                  구문이 담긴 .sql 파일을 올려주세요 - 배포 시 DB에 한 번 실행해 드립니다.
                </p>
              </div>
            </div>

            <div class="flex flex-col gap-3">
              <span class="text-xs font-semibold uppercase tracking-wide text-ink-500">추가 백킹서비스</span>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <button
                  v-for="opt in BACKING_SERVICE_OPTIONS"
                  :key="opt.value"
                  type="button"
                  @click="toggleNeededService(opt.value)"
                  :class="[
                    'flex items-center gap-3 rounded-lg border p-3 text-left transition-colors',
                    form.neededServices.includes(opt.value)
                      ? 'border-primary bg-primary-light ring-1 ring-primary'
                      : 'border-border bg-background hover:border-primary/50',
                  ]"
                >
                  <TechIcon :kind="opt.icon" />
                  <span class="flex flex-col">
                    <span class="text-sm font-semibold text-ink-900">{{ opt.label }}</span>
                    <span class="text-xs text-ink-500">{{ opt.description }}</span>
                  </span>
                  <AppIcon
                    v-if="form.neededServices.includes(opt.value)"
                    name="check"
                    class="ml-auto h-4 w-4 shrink-0 text-primary"
                  />
                </button>
              </div>
            </div>
          </div>

          <!-- 6. 환경변수 영역 -->
          <div class="flex flex-col gap-3">
            <StepHeader :step="6" title="환경변수" />
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
