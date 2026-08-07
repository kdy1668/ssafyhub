<script setup>
import { ref } from "vue";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import { useDeployGuide } from "../composables/useDeployGuide";
import { useToastStore } from "../stores/useToastStore";

const toast = useToastStore();
const { mutateAsync, data: result, isPending, reset } = useDeployGuide();

// 브라우저에서 프로젝트 폴더를 훑어 서버로 보낼 최소 정보만 추린다(전체 업로드 X).
const IGNORE_DIRS = ["node_modules", ".git", "dist", "build", ".venv", "venv",
  "__pycache__", ".idea", ".gradle", "target", "out", ".next", ".cache", "coverage", ".vscode"];
const KEY_NAMES = ["package.json", "requirements.txt", "build.gradle", "build.gradle.kts", "pom.xml",
  "settings.py", "manage.py", "main.py", "app.py", "wsgi.py", "asgi.py",
  "vite.config.js", "vite.config.ts", "angular.json", "svelte.config.js", "next.config.js",
  "application.yml", "application.yaml", "application.properties",
  "dockerfile", "docker-compose.yml", "docker-compose.yaml", ".env.example",
  "schema.sql", "data.sql"];
const API_CLIENT_NAMES = ["client.js", "client.ts", "api.js", "api.ts", "http.js", "http.ts", "axios.js", "axios.ts"];
const KEY_PATH_PATTERNS = [
  /(^|\/)application(?:-[^/]+)?\.(?:ya?ml|properties)$/i,
  /(^|\/)(?:schema|data)\.sql$/i,
  /(^|\/)(?:db\/migration|migrations?|mappers?)\//i,
  /(^|\/)src\/(?:api|services|config)\//i,
  /(?:auth|oauth|security|cors|socket|websocket|database|datasource)[^/]*\.(?:js|ts|java|py)$/i,
];
const MAX_KEY_FILES = 25;
const MAX_FILE_CHARS = 8000;
const MAX_TREE_ENTRIES = 500;

const projectName = ref("");
const fileTree = ref("");
const keyFiles = ref([]);
const fileCount = ref(0);
const extracting = ref(false);
const pickError = ref("");

const ready = computed(() => Boolean(fileTree.value) && !extracting.value);

function isIgnored(path) {
  return path.split("/").some((seg) => IGNORE_DIRS.includes(seg.toLowerCase()));
}
function isKeyFile(path) {
  const name = path.split("/").pop().toLowerCase();
  return KEY_NAMES.includes(name) || API_CLIENT_NAMES.includes(name) || KEY_PATH_PATTERNS.some((pattern) => pattern.test(path));
}
function keyFileScore(path) {
  const name = path.split("/").pop().toLowerCase();
  if (KEY_NAMES.includes(name) || API_CLIENT_NAMES.includes(name)) return 3;
  if (/(^|\/)application(?:-[^/]+)?\.(?:ya?ml|properties)$/i.test(path)) return 3;
  if (/(^|\/)(?:schema|data)\.sql$|(^|\/)(?:db\/migration|migrations?)\//i.test(path)) return 2;
  return 1;
}

async function onPick(e) {
  const list = Array.from(e.target.files || []).filter(
    (f) => f.webkitRelativePath && !isIgnored(f.webkitRelativePath)
  );
  reset();
  pickError.value = "";
  if (!list.length) {
    pickError.value = "폴더에서 읽을 파일을 찾지 못했어요. 프로젝트 최상위 폴더를 선택해주세요.";
    fileTree.value = "";
    return;
  }
  extracting.value = true;
  try {
    const paths = list.map((f) => f.webkitRelativePath).sort();
    fileCount.value = paths.length;
    projectName.value = paths[0].split("/")[0] || "project";
    fileTree.value =
      paths.slice(0, MAX_TREE_ENTRIES).join("\n") +
      (paths.length > MAX_TREE_ENTRIES ? `\n…(+${paths.length - MAX_TREE_ENTRIES} more)` : "");

    const candidates = list
      .filter((f) => isKeyFile(f.webkitRelativePath))
      .sort((a, b) => keyFileScore(b.webkitRelativePath) - keyFileScore(a.webkitRelativePath)
        || a.webkitRelativePath.localeCompare(b.webkitRelativePath))
      .slice(0, MAX_KEY_FILES);
    const kf = [];
    for (const f of candidates) {
      let text = "";
      try {
        text = await f.text();
      } catch {
        continue;
      }
      if (text.length > MAX_FILE_CHARS) text = text.slice(0, MAX_FILE_CHARS) + "\n…(생략)";
      kf.push({ path: f.webkitRelativePath, content: text });
    }
    keyFiles.value = kf;
  } finally {
    extracting.value = false;
  }
}

async function generate() {
  if (!ready.value) return;
  try {
    await mutateAsync({
      projectName: projectName.value,
      fileTree: fileTree.value,
      keyFiles: keyFiles.value,
    });
  } catch (err) {
    toast.showToast(err?.response?.data?.message || "가이드 생성에 실패했어요.", "error");
  }
}

function downloadMd() {
  const blob = new Blob([result.value.markdown], { type: "text/markdown;charset=utf-8" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "SSAFYHUB.md";
  a.click();
  URL.revokeObjectURL(a.href);
}
async function copyMd() {
  await navigator.clipboard.writeText(result.value.markdown);
  toast.showToast("CLAUDE.md 내용을 복사했어요.", "success");
}
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />
    <main class="mx-auto max-w-4xl px-4 py-8 sm:px-6 sm:py-10">
      <div class="mb-6">
        <span class="text-xs font-bold uppercase tracking-wider text-primary">Onboarding</span>
        <h1 class="mt-1 text-2xl font-bold text-ink-900">배포 가이드 받기</h1>
        <p class="mt-1 text-sm text-ink-500">
          프로젝트 폴더를 고르면 AI가 분석해서 <b>어떻게 zip을 만들고 어떻게 올릴지</b> 알려주고,
          레포에 넣을 <code>CLAUDE.md</code> 가이드를 만들어줘요. (파일 전체가 아니라 구조·핵심파일만 읽습니다)
        </p>
      </div>

      <!-- 폴더 선택 -->
      <div class="rounded-2xl border border-border bg-surface p-6 shadow-card">
        <label
          class="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-primary/30 bg-primary-light/40 px-4 py-10 text-center transition hover:bg-primary-light"
        >
          <AppIcon name="upload" class="h-7 w-7 text-primary" />
          <span class="text-sm font-semibold text-ink-900">프로젝트 최상위 폴더 선택</span>
          <span class="text-xs text-ink-500">node_modules 등은 자동 제외돼요</span>
          <input type="file" webkitdirectory directory multiple class="hidden" @change="onPick" />
        </label>

        <p v-if="pickError" class="mt-3 text-xs font-medium text-danger">{{ pickError }}</p>
        <div v-if="fileTree" class="mt-4 flex flex-wrap items-center gap-2 text-xs text-ink-600">
          <span class="rounded-md bg-black/5 px-2 py-1 font-mono">{{ projectName }}</span>
          <span>파일 {{ fileCount }}개 · 핵심파일 {{ keyFiles.length }}개 감지</span>
        </div>

        <button
          class="mt-5 inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-hover disabled:opacity-60"
          :disabled="!ready || isPending"
          @click="generate"
        >
          <AppIcon name="rocket" class="h-4 w-4" />
          {{ isPending ? "분석 중..." : "가이드 생성" }}
        </button>
      </div>

      <!-- 결과 -->
      <div v-if="result" class="mt-8">
        <!-- CLAUDE.md -->
        <div class="mt-6 rounded-2xl border border-border bg-surface p-6 shadow-card">
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-lg font-bold text-ink-900">CLAUDE.md (레포에 넣기)</h2>
            <div class="flex gap-2">
              <button class="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-ink-700 hover:bg-black/5" @click="copyMd">복사</button>
              <button class="rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-white hover:bg-primary-hover" @click="downloadMd">다운로드</button>
            </div>
          </div>
          <pre class="max-h-[480px] overflow-auto rounded-lg bg-ink-900 p-4 font-mono text-xs leading-relaxed text-white/90 whitespace-pre-wrap">{{ result.markdown }}</pre>
          <p class="mt-2 text-xs text-ink-400">이 파일을 프로젝트 루트에 <code>CLAUDE.md</code>로 저장하면 코딩 에이전트가 배포 준비를 도와줘요. (모델: {{ result.model }})</p>
        </div>
      </div>
    </main>
  </div>
</template>
