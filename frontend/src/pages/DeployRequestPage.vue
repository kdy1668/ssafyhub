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
import TechIcon from "../components/deploy/TechIcon.vue";
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
const schemaFile = ref(null);

// 텍스트/설정 폼 상태
const form = reactive({
  projectName: "",
  runtimeVersion: "21",
  internalPort: 8080,
  databaseEngine: "NONE", // NONE | MYSQL | POSTGRES
  databaseName: "",
  databasePassword: "",
  neededServices: [], // 예: ["REDIS", "MONGODB"] - 관계형 DB 외 추가 백킹서비스
  envVars: [],
  visibility: "PUBLIC",
  description: "",
});

const DB_ENGINE_OPTIONS = [
  { value: "NONE", icon: "none", label: "사용 안 함", description: "DB가 필요 없는 앱" },
  { value: "MYSQL", icon: "mysql", label: "MySQL", description: "관계형 DB" },
  { value: "POSTGRES", icon: "postgres", label: "PostgreSQL", description: "관계형 DB" },
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

  // 백엔드 파일: 존재 + 확장자(.jar 또는 Dockerfile 포함 .zip)
  const backendName = backendFile.value?.name.toLowerCase() ?? "";
  if (!backendFile.value) errors.backendFile = "백엔드 아티팩트를 업로드하세요.";
  else if (!backendName.endsWith(".jar") && !backendName.endsWith(".zip"))
    errors.backendFile = "백엔드 파일은 .jar 또는 .zip 형식이어야 합니다.";
  else errors.backendFile = "";

  if (!frontendFile.value) errors.frontendFile = "프론트엔드 ZIP 파일을 업로드하세요.";
  else if (!frontendFile.value.name.toLowerCase().endsWith(".zip"))
    errors.frontendFile = "프론트엔드 파일은 .zip 형식이어야 합니다.";
  else errors.frontendFile = "";

  // DB 초기화 SQL은 선택이지만, 올렸다면 확장자는 지켜야 한다.
  if (schemaFile.value && !schemaFile.value.name.toLowerCase().endsWith(".sql")) {
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
  return {
    backendFile: backendFile.value,
    frontendFile: frontendFile.value,
    // DB를 쓸 때만, 그리고 실제로 파일을 고른 경우에만 올린다.
    schemaFile: needDatabase ? schemaFile.value : null,
    projectName: form.projectName,
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
          빌드 산출물을 업로드하고 데이터베이스/환경 변수를 설정하여 클라우드 환경에 배포를 요청합니다.
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
              <label class="mb-1 block text-sm font-medium text-ink-700">백엔드 아티팩트 (.jar 또는 .zip)</label>
              <input
                type="file"
                accept=".jar,.zip"
                @change="handleBackendFileChange"
                class="block w-full cursor-pointer rounded-md border border-border bg-background text-sm text-ink-500 file:mr-4 file:cursor-pointer file:border-0 file:bg-primary-light file:px-4 file:py-2 file:text-sm file:font-semibold file:text-primary hover:file:bg-primary/20"
              />
              <p v-if="errors.backendFile" class="mt-1 text-xs text-red-500">{{ errors.backendFile }}</p>
              <p class="mt-1 text-xs text-ink-500">
                .jar: Java로 빌드된 실행 파일 하나만 올리면 됩니다 (실행 환경은 플랫폼이 자동 구성).<br />
                .zip: Java가 아닌 다른 언어/프레임워크를 쓴다면, 프로젝트 소스(또는 빌드 산출물)와
                <strong>Dockerfile을 함께</strong> 담아 올려주세요. 앱을 어떻게 빌드/실행할지는 이 Dockerfile이
                전부 결정합니다. DB 테이블/마이그레이션이 필요한 프레임워크(MyBatis 등 자동 생성이 안 되는
                경우)라면 아래 예시를 참고하거나, 앱 컨테이너가 시작될 때 마이그레이션을 직접 실행하도록
                Dockerfile(또는 시작 스크립트)에 포함해주세요 - 플랫폼은 빈 데이터베이스만 준비해 드립니다.
              </p>
              <details class="mt-2 rounded-md border border-border bg-background p-3">
                <summary class="cursor-pointer text-xs font-semibold text-primary">
                  예시 보기: zip 구성 &amp; Dockerfile (Java/Gradle 기준)
                </summary>
                <div class="mt-2 flex flex-col gap-2 text-xs text-ink-700">
                  <p>zip 안에는 아래처럼 Dockerfile과 소스가 최상위(또는 폴더 한 겹 안)에 있어야 합니다.</p>
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code>backend.zip
├── Dockerfile
├── build.gradle
├── settings.gradle
├── gradlew
├── gradle/
└── src/
    └── main/...</code></pre>
                  <p>Dockerfile 예시 (멀티스테이지: 소스를 컨테이너 안에서 직접 빌드):</p>
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code>FROM eclipse-temurin:17-jdk AS build
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

              <details class="mt-2 rounded-md border border-border bg-background p-3">
                <summary class="cursor-pointer text-xs font-semibold text-primary">
                  예시 보기: DB 테이블은 누가 만드나요? (JPA vs MyBatis 등)
                </summary>
                <div class="mt-2 flex flex-col gap-3 text-xs text-ink-700">
                  <p>
                    DB 접속 정보는
                    <code class="rounded bg-ink-900 px-1 py-0.5 text-white">DB_HOST</code>,
                    <code class="rounded bg-ink-900 px-1 py-0.5 text-white">DB_PORT</code>,
                    <code class="rounded bg-ink-900 px-1 py-0.5 text-white">DB_NAME</code>,
                    <code class="rounded bg-ink-900 px-1 py-0.5 text-white">DB_USERNAME</code>,
                    <code class="rounded bg-ink-900 px-1 py-0.5 text-white">DB_PASSWORD</code>
                    환경변수로 컨테이너에 주입됩니다. 플랫폼은 <strong>빈 데이터베이스</strong>만 만들어
                    드리고, 그 안에 테이블을 만드는 방식은 쓰신 프레임워크에 따라 다릅니다.
                  </p>

                  <div>
                    <p class="font-semibold text-ink-900">① Spring + JPA/Hibernate - 별도 작업 불필요</p>
                    <p>
                      <code class="rounded bg-ink-900 px-1 py-0.5 text-white">ddl-auto: update</code>(또는
                      create)를 쓰면 엔티티 클래스 기준으로 앱이 뜰 때 테이블을 자동으로 만들어줍니다.
                      DB 접속 정보를 아래처럼 환경변수로 읽게만 해주면 끝입니다.
                    </p>
                    <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code># application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&amp;serverTimezone=Asia/Seoul
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update</code></pre>
                  </div>

                  <div>
                    <p class="font-semibold text-ink-900">② MyBatis 등 자동 생성이 안 되는 경우 - 직접 마이그레이션</p>
                    <p>
                      MyBatis는 SQL을 매핑만 할 뿐 테이블을 만들어주지 않습니다. zip에 스키마 파일
                      (schema.sql)을 같이 담고, 앱을 실행하기 직전에 그 SQL을 직접 실행하는 entrypoint
                      스크립트를 두면 됩니다.
                    </p>
                    <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code># entrypoint.sh
#!/bin/sh
set -e
mysql -h "$DB_HOST" -P "$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" &lt; schema.sql
exec java -jar app.jar</code></pre>
                    <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code># Dockerfile (런타임 스테이지에 mysql client / 스키마 / entrypoint 추가)
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update &amp;&amp; apt-get install -y default-mysql-client &amp;&amp; rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar
COPY schema.sql schema.sql
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]</code></pre>
                    <p>
                      같은 원리로 Flyway/Liquibase(Java), Prisma/Sequelize/TypeORM(Node.js), Django의
                      <code class="rounded bg-ink-900 px-1 py-0.5 text-white">manage.py migrate</code> 처럼
                      언어/프레임워크가 제공하는 마이그레이션 도구를 그대로 써도 됩니다 - 핵심은 "컨테이너가
                      시작될 때 스스로 초기화를 끝낸다"는 점뿐입니다.
                    </p>
                  </div>
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
                Dockerfile의 마지막 단계에서 빌드 결과물을 <code class="rounded bg-ink-900 px-1 py-0.5 text-white">/output</code>
                경로에 복사해두어야 합니다.
              </p>
              <details class="mt-2 rounded-md border border-border bg-background p-3">
                <summary class="cursor-pointer text-xs font-semibold text-primary">
                  예시 보기: zip 구성 &amp; Dockerfile (React/Vite 기준)
                </summary>
                <div class="mt-2 flex flex-col gap-2 text-xs text-ink-700">
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code>frontend.zip
├── Dockerfile
├── package.json
├── vite.config.js
└── src/...</code></pre>
                  <p>Dockerfile 예시 (빌드 후 결과물을 /output에 복사):</p>
                  <pre class="overflow-x-auto rounded bg-ink-900 p-3 text-[11px] leading-relaxed text-white"><code>FROM node:20-alpine AS build
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
                    있습니다. 반드시 <code class="rounded bg-ink-900 px-1 py-0.5 text-white">/output</code>
                    경로에 최종 정적 파일(index.html 포함)을 두어야 합니다.
                  </p>
                </div>
              </details>
            </div>
          </div>

          <!-- DB / 백킹서비스 설정 영역 -->
          <div class="flex flex-col gap-5 border-b border-border pb-6">
            <div class="flex flex-col gap-3">
              <span class="text-sm font-bold text-ink-900">데이터베이스</span>
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

              <div>
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
                  JPA/Hibernate처럼 앱이 스스로 테이블을 만들어준다면 비워두세요. MyBatis 등 자동 생성이
                  안 되는 프레임워크를 쓴다면, <code class="rounded bg-ink-900 px-1 py-0.5 text-white">CREATE TABLE</code>
                  구문이 담긴 .sql 파일을 올려주세요 - 배포 시 DB에 한 번 실행해 드립니다.
                </p>
              </div>
            </div>

            <div class="flex flex-col gap-3">
              <span class="text-sm font-bold text-ink-900">추가 백킹서비스</span>
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