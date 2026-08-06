<script setup>
import { useRouter } from "vue-router";
import AppHeader from "../components/common/AppHeader.vue";
import AppIcon from "../components/common/AppIcon.vue";
import { useCurrentUser } from "../composables/useCurrentUser";
import { useAuthStore } from "../stores/useAuthStore";

const router = useRouter();
const authStore = useAuthStore();
const { data: user } = useCurrentUser();

// 로그인이 필요한 경로는 미인증 시 로그인으로 우회시킨다.
function go(path, requiresAuth = false) {
  if (requiresAuth && !authStore.isAuthenticated) {
    router.push({ path: "/login", query: { from: path } });
    return;
  }
  router.push(path);
}

// 콘솔 홈 상단의 "서비스 바로가기" 카드 (AWS 콘솔의 서비스 그리드 느낌)
const ACTIONS = [
  { icon: "upload", title: "배포 신청", desc: "코드(zip/jar)를 업로드해 서비스를 배포합니다.", path: "/deploy/new", auth: true, primary: true },
  { icon: "cube", title: "내 서비스 관리", desc: "배포 상태·재배포·환경변수·삭제를 한 곳에서.", path: "/deployments", auth: true },
  { icon: "eye", title: "쇼케이스 둘러보기", desc: "다른 팀이 공개한 서비스를 미리보기로 구경.", path: "/showcase", auth: false },
];

// 플랫폼 주요 기능 (깊이를 보여주는 기능 카탈로그)
const FEATURES = [
  { icon: "server", title: "자동 빌드 & 배포", desc: "zip/jar만 올리면 Docker 빌드부터 Kubernetes 배포까지 자동으로 처리합니다." },
  { icon: "link", title: "HTTPS 서브도메인", desc: "배포하면 {학번}.ssafyhub.site 주소로 바로 접속할 수 있습니다." },
  { icon: "chart", title: "리소스 모니터링", desc: "CPU·메모리·네트워크 사용량을 실시간 시계열 그래프로 확인." },
  { icon: "terminal", title: "런타임 로그", desc: "컨테이너 stdout/stderr 로그를 콘솔에서 바로 조회합니다." },
  { icon: "settings", title: "백킹 서비스 프로비저닝", desc: "MySQL·Redis·MongoDB를 선택만 하면 함께 띄워 연결해 줍니다." },
  { icon: "chat", title: "AI 실패 진단", desc: "배포가 실패하면 로그를 분석해 원인과 해결책을 제안합니다." },
];

// 배포 파이프라인 흐름
const STEPS = [
  { icon: "upload", title: "코드 업로드", desc: "zip / jar" },
  { icon: "server", title: "자동 빌드", desc: "Docker 이미지" },
  { icon: "cube", title: "k8s 배포", desc: "컨테이너 기동" },
  { icon: "link", title: "바로 접속", desc: "HTTPS 서브도메인" },
];

const STACKS = ["Spring Boot", "Django", "FastAPI", "Flask", "Express"];
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <AppHeader />

    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10">
      <!-- 히어로: 딥블루 → 스카이 그라디언트 콘솔 배너 -->
      <section
        class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary to-sky px-7 py-10 shadow-cardHover sm:px-12 sm:py-14"
      >
        <div class="pointer-events-none absolute -right-24 -top-28 h-72 w-72 rounded-full bg-white/10 blur-3xl" />
        <div class="pointer-events-none absolute -bottom-28 -left-16 h-72 w-72 rounded-full bg-white/10 blur-2xl" />
        <!-- 은은한 그리드 텍스처 -->
        <div
          class="pointer-events-none absolute inset-0 opacity-[0.08]"
          style="background-image: linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px); background-size: 40px 40px;"
        />

        <div class="relative z-10 grid items-center gap-10 lg:grid-cols-[1.05fr_0.95fr]">
          <div class="max-w-xl">
          <span class="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.22em] text-white ring-1 ring-white/25">
            <span class="h-1.5 w-1.5 rounded-full bg-white" />
            SSAFYHUB CONSOLE
          </span>

          <h1 class="mt-4 text-3xl font-bold leading-[1.15] tracking-tight text-white sm:text-[42px]">
            <template v-if="user">{{ user.name }}님, 환영합니다 👋</template>
            <template v-else>코드만 올리면,<br />배포는 저희가 합니다.</template>
          </h1>
          <p class="mt-4 max-w-xl text-sm leading-relaxed text-white/85 sm:text-base">
            빌드·컨테이너·도메인·DB까지 신경 쓸 필요 없이, zip 하나로 팀 프로젝트를 클라우드에 올리는
            SSAFY 전용 간편 배포 플랫폼입니다.
          </p>

          <div class="mt-7 flex flex-wrap gap-3">
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-white px-5 py-2.5 text-sm font-semibold text-primary shadow-sm transition hover:-translate-y-0.5 hover:bg-primary-light"
              @click="go('/deploy/new', true)"
            >
              <AppIcon name="upload" class="h-4 w-4" />
              배포 신청하기
            </button>
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-white/10 px-5 py-2.5 text-sm font-semibold text-white ring-1 ring-white/40 backdrop-blur transition hover:bg-white/20"
              @click="go('/deployments', true)"
            >
              <AppIcon name="cube" class="h-4 w-4" />
              내 서비스 관리
            </button>
          </div>

          <!-- 지원 스택 배지 -->
          <div class="mt-7 flex flex-wrap items-center gap-2">
            <span class="text-[11px] font-medium uppercase tracking-wider text-white/60">지원 스택</span>
            <span
              v-for="s in STACKS"
              :key="s"
              class="rounded-md bg-white/12 px-2.5 py-1 text-xs font-medium text-white ring-1 ring-white/20"
            >
              {{ s }}
            </span>
          </div>
          </div>

          <!-- 오른쪽: 콘솔 미리보기 목업 (넓은 화면에서만) -->
          <div class="relative hidden lg:block">
            <div class="absolute -inset-4 rounded-3xl bg-white/5 blur-2xl" />
            <div class="relative rounded-2xl bg-white/10 p-3 shadow-2xl ring-1 ring-white/25 backdrop-blur">
              <!-- 브라우저 크롬 -->
              <div class="mb-3 flex items-center gap-2 px-1">
                <span class="h-2.5 w-2.5 rounded-full bg-white/40" />
                <span class="h-2.5 w-2.5 rounded-full bg-white/40" />
                <span class="h-2.5 w-2.5 rounded-full bg-white/40" />
                <span class="ml-2 flex-1 truncate rounded-md bg-white/15 px-2.5 py-1 font-mono text-[11px] text-white/80">
                  https://1551343.ssafyhub.site
                </span>
              </div>

              <!-- 배포 상태 카드 -->
              <div class="rounded-xl bg-white p-4 text-ink-900">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-bold">richman</span>
                  <span class="inline-flex items-center gap-1.5 rounded-full bg-success-light px-2.5 py-0.5 text-[11px] font-semibold text-success">
                    <span class="h-1.5 w-1.5 rounded-full bg-success" /> RUNNING
                  </span>
                </div>
                <p class="mt-0.5 text-[11px] text-ink-500">Django · MySQL · 배포 성공</p>

                <div class="mt-4 space-y-3">
                  <div>
                    <div class="mb-1 flex justify-between text-[10px] font-medium text-ink-500">
                      <span>CPU</span><span>32%</span>
                    </div>
                    <div class="h-1.5 overflow-hidden rounded-full bg-primary-light">
                      <div class="h-full w-1/3 rounded-full bg-primary" />
                    </div>
                  </div>
                  <div>
                    <div class="mb-1 flex justify-between text-[10px] font-medium text-ink-500">
                      <span>메모리</span><span>210MB</span>
                    </div>
                    <div class="h-1.5 overflow-hidden rounded-full bg-sky-light">
                      <div class="h-full w-2/5 rounded-full bg-sky" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 미니 배포 로그 -->
              <div class="mt-3 space-y-1.5 rounded-xl bg-black/20 p-3 font-mono text-[10px] leading-relaxed text-white/70">
                <p>✓ 이미지 빌드 완료</p>
                <p>✓ Kubernetes 배포 완료</p>
                <p class="text-white">▸ https://1551343.ssafyhub.site 접속 가능</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 서비스 바로가기 -->
      <section class="mt-10">
        <h2 class="mb-4 text-sm font-bold uppercase tracking-wider text-ink-500">바로가기</h2>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <button
            v-for="a in ACTIONS"
            :key="a.title"
            type="button"
            class="group flex flex-col items-start rounded-2xl border bg-surface p-5 text-left shadow-card transition hover:-translate-y-1 hover:shadow-cardHover"
            :class="a.primary ? 'border-primary/30 ring-1 ring-primary/10' : 'border-border'"
            @click="go(a.path, a.auth)"
          >
            <span
              class="mb-3 flex h-11 w-11 items-center justify-center rounded-xl transition group-hover:scale-105"
              :class="a.primary ? 'bg-primary text-white' : 'bg-primary-light text-primary'"
            >
              <AppIcon :name="a.icon" class="h-5 w-5" />
            </span>
            <span class="flex w-full items-center justify-between">
              <span class="text-base font-bold text-ink-900">{{ a.title }}</span>
              <AppIcon name="arrow" class="h-4 w-4 text-ink-300 transition group-hover:translate-x-1 group-hover:text-primary" />
            </span>
            <span class="mt-1 text-sm leading-relaxed text-ink-500">{{ a.desc }}</span>
          </button>
        </div>
      </section>

      <!-- 주요 기능 -->
      <section class="mt-12">
        <h2 class="mb-4 text-sm font-bold uppercase tracking-wider text-ink-500">주요 기능</h2>
        <div class="grid grid-cols-1 gap-px overflow-hidden rounded-2xl border border-border bg-border sm:grid-cols-2 lg:grid-cols-3">
          <div v-for="f in FEATURES" :key="f.title" class="flex items-start gap-3.5 bg-surface p-5">
            <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-sky-light text-sky">
              <AppIcon :name="f.icon" class="h-5 w-5" />
            </span>
            <div>
              <p class="text-sm font-bold text-ink-900">{{ f.title }}</p>
              <p class="mt-1 text-xs leading-relaxed text-ink-500">{{ f.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 배포 흐름 -->
      <section class="mt-12">
        <h2 class="mb-4 text-sm font-bold uppercase tracking-wider text-ink-500">배포는 이렇게 진행돼요</h2>
        <div class="rounded-2xl border border-border bg-surface p-6 shadow-card sm:p-8">
          <div class="flex flex-col items-stretch gap-3 sm:flex-row sm:items-center">
            <template v-for="(step, i) in STEPS" :key="step.title">
              <div class="flex flex-1 items-center gap-3 rounded-xl bg-background px-4 py-3">
                <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-light text-primary">
                  <AppIcon :name="step.icon" class="h-5 w-5" />
                </span>
                <div>
                  <p class="text-sm font-bold text-ink-900">{{ step.title }}</p>
                  <p class="text-[11px] text-ink-500">{{ step.desc }}</p>
                </div>
              </div>
              <AppIcon
                v-if="i < STEPS.length - 1"
                name="arrow"
                class="mx-auto h-5 w-5 shrink-0 rotate-90 text-ink-300 sm:mx-0 sm:rotate-0"
              />
            </template>
          </div>
        </div>
      </section>

      <!-- 하단 CTA -->
      <section class="mt-12">
        <div class="flex flex-col items-center justify-between gap-4 rounded-2xl border border-primary/20 bg-primary-light px-7 py-7 sm:flex-row sm:px-10">
          <div>
            <p class="text-lg font-bold text-ink-900">지금 첫 서비스를 배포해 보세요</p>
            <p class="mt-1 text-sm text-ink-500">준비물은 빌드된 zip 또는 jar 파일 하나면 충분합니다.</p>
          </div>
          <button
            type="button"
            class="inline-flex shrink-0 items-center gap-2 rounded-lg bg-primary px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-primary-hover"
            @click="go('/deploy/new', true)"
          >
            <AppIcon name="rocket" class="h-4 w-4" />
            배포 시작하기
          </button>
        </div>
      </section>
    </main>
  </div>
</template>
